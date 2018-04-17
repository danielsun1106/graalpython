/*
 * Copyright (c) 2018, Oracle and/or its affiliates.
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
package com.oracle.graal.python.test.debug;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Context.Builder;
import org.graalvm.polyglot.Source;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.oracle.truffle.api.debug.Breakpoint;
import com.oracle.truffle.api.debug.DebugStackFrame;
import com.oracle.truffle.api.debug.DebugValue;
import com.oracle.truffle.api.debug.DebuggerSession;
import com.oracle.truffle.api.debug.SuspendedCallback;
import com.oracle.truffle.api.debug.SuspendedEvent;
import com.oracle.truffle.tck.DebuggerTester;

public class PythonDebugTest {

    private DebuggerTester tester;

    @Before
    public void before() {
        Builder newBuilder = Context.newBuilder();
        newBuilder.allowAllAccess(true);
        tester = new DebuggerTester(newBuilder);
    }

    @After
    public void dispose() {
        tester.close();
    }

    @Test
    public void testSteppingAsExpected() throws Throwable {
        // test that various elements step as expected, including generators, statement level atomic
        // expressions, and roots
        final Source source = Source.newBuilder("python", "" +
                        "import sys\n" +
                        "from sys import version\n" +
                        "\n" +
                        "def a():\n" +
                        "  x = [1]\n" +
                        "  x[0]\n" +
                        "  x.append(1)\n" +
                        "  for i in genfunc():\n" +
                        "    return i\n" +
                        "\n" +
                        "def genfunc():\n" +
                        "  yield 1\n" +
                        "  yield 2\n" +
                        "  yield 3\n" +
                        "  return\n" +
                        "\n" +
                        "a()\n" +
                        "a()\n", "test_stepping.py").buildLiteral();

        try (DebuggerSession session = tester.startSession()) {
            session.install(Breakpoint.newBuilder(DebuggerTester.getSourceImpl(source)).lineIs(1).build());
            tester.startEval(source);

            expectSuspended((SuspendedEvent event) -> {
                DebugStackFrame frame = event.getTopStackFrame();
                assertEquals(1, frame.getSourceSection().getStartLine());
                event.prepareStepOver(1);
            });
            expectSuspended((SuspendedEvent event) -> {
                DebugStackFrame frame = event.getTopStackFrame();
                assertEquals(2, frame.getSourceSection().getStartLine());
                event.prepareStepOver(1);
            });
            expectSuspended((SuspendedEvent event) -> {
                DebugStackFrame frame = event.getTopStackFrame();
                assertEquals(4, frame.getSourceSection().getStartLine());
                event.prepareStepOver(2);
            });
            expectSuspended((SuspendedEvent event) -> {
                DebugStackFrame frame = event.getTopStackFrame();
                assertEquals(17, frame.getSourceSection().getStartLine());
                event.prepareStepOver(1);
            });
            expectSuspended((SuspendedEvent event) -> {
                DebugStackFrame frame = event.getTopStackFrame();
                assertEquals(18, frame.getSourceSection().getStartLine());
                event.prepareStepInto(1);
            });
            expectSuspended((SuspendedEvent event) -> {
                DebugStackFrame frame = event.getTopStackFrame();
                assertEquals(5, frame.getSourceSection().getStartLine());
                event.prepareStepInto(1);
            });
            expectSuspended((SuspendedEvent event) -> {
                DebugStackFrame frame = event.getTopStackFrame();
                assertEquals(6, frame.getSourceSection().getStartLine());
                event.prepareStepInto(1);
            });
            expectSuspended((SuspendedEvent event) -> {
                DebugStackFrame frame = event.getTopStackFrame();
                assertEquals(7, frame.getSourceSection().getStartLine());
                event.prepareStepOver(1);
            });
            expectSuspended((SuspendedEvent event) -> {
                DebugStackFrame frame = event.getTopStackFrame();
                assertEquals(8, frame.getSourceSection().getStartLine());
                event.prepareStepInto(1);
            });
            expectSuspended((SuspendedEvent event) -> {
                DebugStackFrame frame = event.getTopStackFrame();
                assertEquals(12, frame.getSourceSection().getStartLine());
                event.prepareStepOver(1);
            });
            expectSuspended((SuspendedEvent event) -> {
                DebugStackFrame frame = event.getTopStackFrame();
                assertEquals(9, frame.getSourceSection().getStartLine());
                event.prepareStepOut(1);
            });
            expectSuspended((SuspendedEvent event) -> {
                DebugStackFrame frame = event.getTopStackFrame();
                assertEquals(18, frame.getSourceSection().getStartLine());
                event.prepareStepOver(1);
            });
            assertEquals("1", tester.expectDone());
        }
    }

    @Test
    public void testInlineEvaluation() throws Throwable {
        final Source source = Source.newBuilder("python", "" +
                        "y = 4\n" +
                        "def foo(x):\n" +
                        "  a = 1\n" +
                        "  b = 2\n" +
                        "  def bar():\n" +
                        "    return a + b\n" +
                        "  return bar() + x + y\n" +
                        "foo(3)", "test_inline.py").buildLiteral();

        try (DebuggerSession session = tester.startSession()) {
            session.install(Breakpoint.newBuilder(DebuggerTester.getSourceImpl(source)).lineIs(5).build());
            session.install(Breakpoint.newBuilder(DebuggerTester.getSourceImpl(source)).lineIs(7).build());
            tester.startEval(source);

            expectSuspended((SuspendedEvent event) -> {
                DebugStackFrame frame = event.getTopStackFrame();
                assertEquals(5, frame.getSourceSection().getStartLine());
                assertEquals("3", frame.eval("a + b").as(String.class));
                event.prepareContinue();
            });
            expectSuspended((SuspendedEvent event) -> {
                DebugStackFrame frame = event.getTopStackFrame();
                assertEquals(7, frame.getSourceSection().getStartLine());
                assertEquals("6", frame.eval("bar() * 2").as(String.class));
                event.prepareContinue();
            });
            assertEquals("10", tester.expectDone());
        }
    }

    @Test
    public void testConditionalBreakpointInFunction() throws Throwable {
        final Source source = Source.newBuilder("python", "" +
                        "def fun():\n" +
                        "  def prod(n):\n" +
                        "    p = 1\n" +
                        "    for i in range(n):\n" +
                        "      p = p * 2\n" +
                        "    return p\n" +
                        "  sum = 0\n" +
                        "  for i in range(0, 10):\n" +
                        "    sum = sum + i\n" +
                        "  return sum\n" +
                        "fun()", "test_cond_break.py").buildLiteral();

        try (DebuggerSession session = tester.startSession()) {
            Breakpoint b = session.install(Breakpoint.newBuilder(DebuggerTester.getSourceImpl(source)).lineIs(9).build());
            b.setCondition("i == 5");
            tester.startEval(source);

            expectSuspended((SuspendedEvent event) -> {
                DebugStackFrame frame = event.getTopStackFrame();
                assertEquals(9, frame.getSourceSection().getStartLine());
                assertEquals("10", frame.eval("sum").as(String.class));
                assertEquals("16", frame.eval("prod(4)").as(String.class));
                event.prepareContinue();
            });
            assertEquals("45", tester.expectDone());
        }

        try (DebuggerSession session = tester.startSession()) {
            Breakpoint b = session.install(Breakpoint.newBuilder(DebuggerTester.getSourceImpl(source)).lineIs(9).build());
            b.setCondition("prod(i) == 16");
            tester.startEval(source);

            expectSuspended((SuspendedEvent event) -> {
                DebugStackFrame frame = event.getTopStackFrame();
                assertEquals(9, frame.getSourceSection().getStartLine());
                assertEquals("4", frame.eval("i").as(String.class));
                event.prepareContinue();
            });
            assertEquals("45", tester.expectDone());
        }
    }

    @Test
    public void testConditionalBreakpointGlobal() throws Throwable {
        final Source source = Source.newBuilder("python", "" +
                        "values = []\n" +
                        "for i in range(0, 10):\n" +
                        "  values.append(i)\n" +
                        "sum(values)\n", "test_cond_break_global.py").buildLiteral();

        try (DebuggerSession session = tester.startSession()) {
            Breakpoint b = session.install(Breakpoint.newBuilder(DebuggerTester.getSourceImpl(source)).lineIs(3).build());
            b.setCondition("i == 5");
            tester.startEval(source);

            expectSuspended((SuspendedEvent event) -> {
                DebugStackFrame frame = event.getTopStackFrame();
                assertEquals(3, frame.getSourceSection().getStartLine());
                assertEquals("10", frame.eval("sum(values)").as(String.class));
                event.prepareContinue();
            });
            assertEquals("45", tester.expectDone());
        }
    }

    @Test
    public void testReenterArgumentsAndValues() throws Throwable {
        // Test that after a re-enter, arguments are kept and variables are cleared.
        final Source source = Source.newBuilder("python", "" +
                        "def main():\n" +
                        "  gi = geni()\n" +
                        "  return fnc(next(gi), 20)\n" +
                        "\n" +
                        "def fnc(n, m):\n" +
                        "  x = n + m\n" +
                        "  n = m - n\n" +
                        "  m = m / 2\n" +
                        "  x = x + n + m\n" +
                        "  return x\n" +
                        "\n" +
                        "def geni():\n" +
                        "  i = 10\n" +
                        "  while (True):\n" +
                        "    i += 1\n" +
                        "    yield i\n" +
                        "\n" +
                        "main()\n", "testReenterArgsAndVals.py").buildLiteral();

        try (DebuggerSession session = tester.startSession()) {
            session.install(Breakpoint.newBuilder(DebuggerTester.getSourceImpl(source)).lineIs(6).build());
            tester.startEval(source);

            expectSuspended((SuspendedEvent event) -> {
                DebugStackFrame frame = event.getTopStackFrame();
                assertEquals(6, frame.getSourceSection().getStartLine());
                checkStack(frame, "fnc", "n", "11", "m", "20");
                event.prepareStepOver(4);
            });
            expectSuspended((SuspendedEvent event) -> {
                DebugStackFrame frame = event.getTopStackFrame();
                assertEquals(10, frame.getSourceSection().getStartLine());
                checkStack(frame, "fnc", "n", "9", "m", "10.0", "x", "50.0");
                event.prepareUnwindFrame(frame);
            });
            expectSuspended((SuspendedEvent event) -> {
                DebugStackFrame frame = event.getTopStackFrame();
                assertEquals(3, frame.getSourceSection().getStartLine());
                assertEquals("main", frame.getName());
            });
            expectSuspended((SuspendedEvent event) -> {
                DebugStackFrame frame = event.getTopStackFrame();
                assertEquals(6, frame.getSourceSection().getStartLine());
                checkStack(frame, "fnc", "n", "11", "m", "20");
            });
            assertEquals("50.0", tester.expectDone());
        }
    }

    private void expectSuspended(SuspendedCallback callback) {
        tester.expectSuspended(callback);
    }

    private static void checkStack(DebugStackFrame frame, String name, String... expectedFrame) {
        assertEquals(name, frame.getName());
        checkDebugValues("variables", frame.getScope().getDeclaredValues(), expectedFrame);
    }

    private static void checkDebugValues(String msg, Iterable<DebugValue> values, String... expectedFrame) {
        Map<String, DebugValue> valMap = new HashMap<>();
        for (DebugValue value : values) {
            valMap.put(value.getName(), value);
        }
        String message = String.format("Frame %s expected %s got %s", msg, Arrays.toString(expectedFrame), values.toString());
        assertEquals(message, expectedFrame.length / 2, valMap.size());
        for (int i = 0; i < expectedFrame.length; i = i + 2) {
            String expectedIdentifier = expectedFrame[i];
            String expectedValue = expectedFrame[i + 1];
            DebugValue value = valMap.get(expectedIdentifier);
            assertNotNull(expectedIdentifier + " not found", value);
            assertEquals(expectedValue, value.as(String.class));
        }
    }

}
