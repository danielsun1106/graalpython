/*
 * Copyright (c) 2017, Oracle and/or its affiliates.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or data
 * (collectively the "Software"), free of charge and under any and all copyright
 * rights in the Software, and any and all patent rights owned or freely
 * licensable by each licensor hereunder covering either (i) the unmodified
 * Software as contributed to or provided by such licensor, or (ii) the Larger
 * Works (as defined below), to deal in both
 *
 * (a) the Software, and
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 *     one is included with the Software (each a "Larger Work" to which the
 *     Software is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.graal.python.test.interop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Context.Builder;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.oracle.graal.python.test.PythonTests;
import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.nodes.Node;

public class JavaInteropTest extends PythonTests {
    private ByteArrayOutputStream os;
    private Context context;
    private ByteArrayOutputStream err;

    @Before
    public void setUpTest() {
        os = new ByteArrayOutputStream();
        err = new ByteArrayOutputStream();
        Builder builder = Context.newBuilder();
        builder.allowAllAccess(true);
        builder.out(os);
        builder.err(err);
        context = builder.build();
    }

    @After
    public void tearDown() {
        context.close();
    }

    @Test
    public void evalFailsOnError() {
        boolean didFail = false;
        try {
            context.eval(Source.create("python", "invalid code"));
        } catch (Throwable t) {
            didFail = true;
        }
        assertTrue(didFail);
    }

    @Test
    public void truffleMethodExport() throws Exception {
        String source = "import polyglot\n" +
                        "@polyglot.export_value\n" +
                        "def foo():\n" +
                        "    print('Called')\n\n";
        Source script = Source.create("python", source);
        context.eval(script);
        Value main = context.getPolyglotBindings().getMember("foo");
        assertTrue(main.canExecute());
    }

    @Test
    public void testPassingFloats() throws Exception {
        String source = "import polyglot\n" +
                        "@polyglot.export_value\n" +
                        "def foo(x, y):\n" +
                        "    print(x * y)\n\n";
        Source script = Source.create("python", source);
        context.eval(script);
        Value main = context.getPolyglotBindings().getMember("foo");
        main.execute((float) 1.0, (float) 2.0);
        assertEquals("2.0\n", os.toString("UTF-8"));
    }

    @Test
    public void testAsFunction() throws Exception {
        String source = "import polyglot\n" +
                        "@polyglot.export_value\n" +
                        "def foo():\n" +
                        "    print('Called')\n\n";
        Source script = Source.create("python", source);
        context.eval(script);
        Value main = context.getPolyglotBindings().getMember("foo");
        main.execute();
        assertEquals("Called\n", os.toString("UTF-8"));
    }

    @Test
    public void testAsFunctionVarArgs() throws Exception {
        String source = "import polyglot\n" +
                        "@polyglot.export_value\n" +
                        "def foo(a, b):\n" +
                        "    print(a, b)\n\n";
        Source script = Source.create("python", source);
        context.eval(script);
        Value main = context.getPolyglotBindings().getMember("foo");
        main.execute("Hello", "World");
        assertEquals("Hello World\n", os.toString("UTF-8"));
    }

    @Test
    public void mainFunctionsAreImplicitlyImporteable() throws Exception {
        String source = "def foo(a, b):\n" +
                        "    print(a, b)\n\n";
        Source script = Source.create("python", source);
        context.eval(script);
        Value main = context.getBindings("python").getMember("foo");
        main.execute("Hello", "World");
        assertEquals("Hello World\n", os.toString("UTF-8"));
    }

    @Test
    public void builtinFunctionsAreImporteable() throws Exception {
        String source = "pass";
        Source script = Source.create("python", source);
        context.eval(script);
        Value main = context.getBindings("python").getMember("__builtins__").getMember("print");
        main.execute("Hello", "World");
        assertEquals("Hello World\n", os.toString("UTF-8"));
    }

    @Test
    public void testMultipleInvocationsAreInSameScope() throws Exception {
        String source = "def foo(a, b):\n" +
                        "    print(a, b)\n" +
                        "foo";
        Source script = Source.create("python", source);
        Value foo = context.eval(script);
        foo.execute("Hello", "World");
        assertEquals("Hello World\n", os.toString("UTF-8"));

        source = "def bar(a, b):\n" +
                        "    foo(a, b)\n" +
                        "bar";
        script = Source.create("python", source);
        Value bar = context.eval(script);
        bar.execute("Hello", "World");
        assertEquals("Hello World\nHello World\n", os.toString("UTF-8"));

        source = "invalid syntax";
        script = Source.create("python", source);
        try {
            context.eval(script);
        } catch (Throwable t) {
        }
        bar.execute("Hello", "World");
        assertEquals("Hello World\nHello World\nHello World\n", os.toString("UTF-8"));
        foo.execute("Hello", "World");
        assertEquals("Hello World\nHello World\nHello World\nHello World\n", os.toString("UTF-8"));
    }

    @Test
    public void accessSuitePy() throws IOException {
        Source suitePy = Source.newBuilder("python", "{ \"libraries\" : {\n" +
                        "\n" +
                        "    # ------------- Libraries -------------\n" +
                        "\n" +
                        "    \"DACAPO\" : {\n" +
                        "      \"urls\" : [\n" +
                        "        \"https://lafo.ssw.uni-linz.ac.at/pub/graal-external-deps/dacapo-9.12-bach-patched.jar\",\n" +
                        "      ],\n" +
                        "      \"sha1\" : \"e39957904b7e79caf4fa54f30e8e4ee74d4e9e37\",\n" +
                        "    }," +
                        "  }" +
                        "}",
                        "suite.py").build();
        Value suite = context.eval(suitePy);

        Value libraries = suite.getMember("libraries");
        assertNotNull("libraries found", libraries);
        final Set<String> suiteKeys = suite.getMemberKeys();
        assertTrue("Libraries found among keys: " + suiteKeys, suiteKeys.contains("libraries"));

        Value dacapo = null;
        for (String k : libraries.getMemberKeys()) {
            System.err.println("k " + k);
            if ("DACAPO".equals(k)) {
                dacapo = libraries.getMember(k);
            }
        }
        assertNotNull("Dacapo found", dacapo);
        assertEquals("'e39957904b7e79caf4fa54f30e8e4ee74d4e9e37'", dacapo.getMember("sha1").toString());
    }

    public static class ForeignObjectWithOOInvoke implements TruffleObject {
        public String getMyName() {
            return getClass().getName();
        }

        public ForeignAccess getForeignAccess() {
            return JavaObjectKindMRForeign.ACCESS;
        }
    }

    @MessageResolution(receiverType = ForeignObjectWithOOInvoke.class)
    public static class JavaObjectKindMR {
        @Resolve(message = "INVOKE")
        abstract static class InvokeNode extends Node {
            Object access(ForeignObjectWithOOInvoke object, String name, Object[] arguments) {
                if (name.equals("getMyName") && arguments.length == 0) {
                    return object.getMyName();
                } else {
                    throw UnknownIdentifierException.raise(name);
                }
            }
        }

        @CanResolve
        abstract static class CheckFunction extends Node {
            protected static boolean test(TruffleObject receiver) {
                return receiver instanceof ForeignObjectWithOOInvoke;
            }
        }
    }

    public static class ForeignObjectWithoutOOInvoke implements TruffleObject {
        public String getMyName() {
            return getClass().getName();
        }

        public ForeignAccess getForeignAccess() {
            return JavaNonOOKindMRForeign.ACCESS;
        }
    }

    @MessageResolution(receiverType = ForeignObjectWithOOInvoke.class)
    public static class JavaNonOOKindMR {
        static class GetMyNameMethod implements TruffleObject {
            private ForeignObjectWithoutOOInvoke self;

            public GetMyNameMethod(ForeignObjectWithoutOOInvoke object2) {
                this.self = object2;
            }

            public ForeignAccess getForeignAccess() {
                return JavaNonOOKindMRForeign.ACCESS;
            }
        }

        @Resolve(message = "READ")
        abstract static class InvokeNode extends Node {
            Object access(ForeignObjectWithoutOOInvoke object, String name) {
                if (name.equals("getMyName")) {
                    return new GetMyNameMethod(object);
                } else {
                    throw UnknownIdentifierException.raise(name);
                }
            }
        }

        @Resolve(message = "EXECUTE")
        abstract static class ExecuteNode extends Node {
            Object access(GetMyNameMethod method, Object[] arguments) {
                assert arguments.length == 0;
                return method.self.getMyName();
            }
        }

        @CanResolve
        abstract static class CheckFunction extends Node {
            protected static boolean test(TruffleObject receiver) {
                return receiver instanceof ForeignObjectWithoutOOInvoke || receiver instanceof GetMyNameMethod;
            }
        }
    }

    @Test
    public void tryInvokeThenReadExecute() {
        String source = "def foo(a, b):\n" +
                        "    return (a.getMyName(), b.getMyName())\n" +
                        "foo";
        Source script = Source.create("python", source);
        Value foo = context.eval(script);
        Value execute = foo.execute(new ForeignObjectWithOOInvoke(), new ForeignObjectWithoutOOInvoke());
        Object[] result = execute.as(Object[].class);
        assert result[0].equals(ForeignObjectWithOOInvoke.class.getName());
        assert result[1].equals(ForeignObjectWithoutOOInvoke.class.getName());
    }
}
