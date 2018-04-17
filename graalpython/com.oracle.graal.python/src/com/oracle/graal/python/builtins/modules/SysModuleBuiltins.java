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
package com.oracle.graal.python.builtins.modules;

import static com.oracle.graal.python.runtime.exception.PythonErrorType.ValueError;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.List;

import com.oracle.graal.python.PythonLanguage;
import com.oracle.graal.python.builtins.Builtin;
import com.oracle.graal.python.builtins.CoreFunctions;
import com.oracle.graal.python.builtins.PythonBuiltins;
import com.oracle.graal.python.builtins.objects.PNone;
import com.oracle.graal.python.builtins.objects.exception.PBaseException;
import com.oracle.graal.python.builtins.objects.ints.PInt;
import com.oracle.graal.python.builtins.objects.str.PString;
import com.oracle.graal.python.nodes.function.PythonBuiltinNode;
import com.oracle.graal.python.runtime.PythonContext;
import com.oracle.graal.python.runtime.PythonCore;
import com.oracle.graal.python.runtime.exception.PException;
import com.oracle.graal.python.runtime.exception.PythonErrorType;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.TruffleOptions;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.RootNode;

@CoreFunctions(defineModule = "sys")
public class SysModuleBuiltins extends PythonBuiltins {
    private static final String LICENSE = "Copyright (c) Oracle and/or its affiliates. Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.";
    private static final String COMPILE_TIME;
    static {
        String compile_time;
        try {
            compile_time = new Date(PythonBuiltins.class.getResource("PythonBuiltins.class").openConnection().getLastModified()).toString();
        } catch (IOException e) {
            compile_time = "";
        }
        COMPILE_TIME = compile_time;
    }

    public static final String[] SYS_PREFIX_ATTRIBUTES = new String[]{"prefix", "exec_prefix", "base_prefix", "base_exec_prefix"};

    @Override
    protected List<? extends NodeFactory<? extends PythonBuiltinNode>> getNodeFactories() {
        return SysModuleBuiltinsFactory.getFactories();
    }

    @Override
    public void initialize(PythonCore core) {
        builtinConstants.put("abiflags", "");
        builtinConstants.put("byteorder", ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? "little" : "big");
        builtinConstants.put("copyright", LICENSE);
        builtinConstants.put("dont_write_bytecode", true);
        if (TruffleOptions.AOT) {
            builtinConstants.put("executable", Compiler.command(new Object[]{"com.oracle.svm.core.posix.GetExecutableName"}));
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(System.getProperty("java.home")).append(PythonCore.FILE_SEPARATOR).append("bin").append(PythonCore.FILE_SEPARATOR).append("java ");
            for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
                sb.append(arg).append(' ');
            }
            sb.append("-classpath ");
            sb.append(System.getProperty("java.class.path")).append(' ');
            // we really don't care what the main class or its arguments were - this should
            // always help us launch Graal.Python
            sb.append("com.oracle.graal.python.shell.GraalPythonMain");
            builtinConstants.put("executable", sb.toString());
        }
        builtinConstants.put("modules", core.factory().createDict());
        builtinConstants.put("path", core.factory().createList());
        builtinConstants.put("builtin_module_names", core.factory().createTuple(core.builtinModuleNames()));
        builtinConstants.put("maxsize", Integer.MAX_VALUE);
        builtinConstants.put("version_info", core.factory().createTuple(new Object[]{PythonLanguage.MAJOR, PythonLanguage.MINOR, PythonLanguage.MICRO, "dev", 0}));
        builtinConstants.put("version", PythonLanguage.VERSION +
                        " (" + COMPILE_TIME + ")" +
                        "\n[" + Truffle.getRuntime().getName() + ", Java " + System.getProperty("java.version") + "]");
        builtinConstants.put("flags", core.factory().createTuple(new Object[]{
                        false, // bytes_warning
                        false, // debug
                        true,  // dont_write_bytecode
                        false, // hash_randomization
                        false, // ignore_environment
                        false, // inspect
                        false, // interactive
                        false, // isolated
                        false, // no_site
                        false, // no_user_site
                        false, // optimize
                        false, // quiet
                        false, // verbose
        }));
        // the default values taken from JPython
        builtinConstants.put("float_info", core.factory().createTuple(new Object[]{
                        Double.MAX_VALUE,       // DBL_MAX
                        Double.MAX_EXPONENT,    // DBL_MAX_EXP
                        308,                    // DBL_MIN_10_EXP
                        Double.MIN_VALUE,       // DBL_MIN
                        Double.MIN_EXPONENT,    // DBL_MIN_EXP
                        -307,                   // DBL_MIN_10_EXP
                        10,                     // DBL_DIG
                        53,                     // DBL_MANT_DIG
                        2.2204460492503131e-16, // DBL_EPSILON
                        2,                      // FLT_RADIX
                        1                       // FLT_ROUNDS
        }));

        String property = System.getProperty("os.name");
        String os = "java";
        if (property.toLowerCase().contains("cygwin")) {
            os = "cygwin";
        } else if (property.toLowerCase().contains("linux")) {
            os = "linux";
        } else if (property.toLowerCase().contains("mac")) {
            os = "darwin";
        } else if (property.toLowerCase().contains("windows")) {
            os = "win32";
        } else if (property.toLowerCase().contains("sunos")) {
            os = "sunos";
        } else if (property.toLowerCase().contains("freebsd")) {
            os = "freebsd";
        }
        builtinConstants.put("platform", os);

        String architecture = System.getProperty("os.arch");
        builtinConstants.put("__gmultiarch", architecture + "-" + os);

        super.initialize(core);
    }

    @Builtin(name = "exc_info", fixedNumOfArguments = 0)
    @GenerateNodeFactory
    public static abstract class ExcInfoNode extends PythonBuiltinNode {
        @Specialization
        public Object run() {
            PythonContext context = getContext();
            PException currentException = context == null ? getCore().getCurrentException() : context.getCurrentException();
            if (currentException == null) {
                return factory().createTuple(new PNone[]{PNone.NONE, PNone.NONE, PNone.NONE});
            } else {
                PBaseException exception = currentException.getExceptionObject();
                exception.reifyException();
                return factory().createTuple(new Object[]{currentException.getType(), exception, exception.getTraceback(factory())});
            }
        }
    }

    @Builtin(name = "_getframe", minNumOfArguments = 0, maxNumOfArguments = 1)
    @GenerateNodeFactory
    public static abstract class GetFrameNode extends PythonBuiltinNode {

        @Child private DirectCallNode call;

        @Specialization
        Object first(@SuppressWarnings("unused") PNone arg) {
            return counted(0);
        }

        /*
         * This is necessary for the time being to be compatible with the old TruffleException
         * behavior. (it only captures the frames if a CallTarget boundary is crossed)
         */
        private static final class GetStackTraceRootNode extends RootNode {

            protected GetStackTraceRootNode(TruffleLanguage<?> language) {
                super(language);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                CompilerDirectives.transferToInterpreter();
                throw PythonLanguage.getCore().raise(ValueError);
            }

            @Override
            public boolean isCaptureFramesForTrace() {
                return true;
            }
        }

        @Specialization
        @TruffleBoundary
        Object counted(int num) {
            if (call == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                GetStackTraceRootNode rootNode = new GetStackTraceRootNode(getRootNode().getLanguage(PythonLanguage.class));
                call = insert(Truffle.getRuntime().createDirectCallNode(Truffle.getRuntime().createCallTarget(rootNode)));
            }
            int actual = num + 1; // skip dummy frame
            try {
                call.call(new Object[0]);
                throw raise(PythonErrorType.SystemError, "should not reach here");
            } catch (PException e) {
                PBaseException exception = e.getExceptionObject();
                exception.reifyException();
                if (actual >= exception.getStackTrace().size()) {
                    throw raiseCallStackDepth();
                }
                return exception.getPFrame(factory(), Math.max(0, actual));
            }
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        Object countedLong(long num) {
            return counted(PInt.intValueExact(num));
        }

        @Specialization
        Object countedLongOvf(long num) {
            try {
                return counted(PInt.intValueExact(num));
            } catch (ArithmeticException e) {
                throw raiseCallStackDepth();
            }
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        Object countedPInt(PInt num) {
            return counted(num.intValueExact());
        }

        @Specialization
        Object countedPIntOvf(PInt num) {
            try {
                return counted(num.intValueExact());
            } catch (ArithmeticException e) {
                throw raiseCallStackDepth();
            }
        }

        private PException raiseCallStackDepth() {
            return raise(PythonErrorType.ValueError, "call stack is not deep enough");
        }

    }

    @Builtin(name = "getfilesystemencoding", fixedNumOfArguments = 0)
    @GenerateNodeFactory
    public static abstract class GetFileSystemEncodingNode extends PythonBuiltinNode {
        @Specialization
        protected String getFileSystemEncoding() {
            return System.getProperty("file.encoding");
        }
    }

    @Builtin(name = "getfilesystemencodeerrors", fixedNumOfArguments = 0)
    @GenerateNodeFactory
    public static abstract class GetFileSystemEncodeErrorsNode extends PythonBuiltinNode {
        @Specialization
        protected String getFileSystemEncoding() {
            return "surrogateescape";
        }
    }

    @Builtin(name = "intern", fixedNumOfArguments = 1)
    @GenerateNodeFactory
    abstract static class InternNode extends PythonBuiltinNode {
        @Specialization
        @TruffleBoundary
        String doBytes(String s) {
            return s.intern();
        }

        @Specialization
        @TruffleBoundary
        PString doBytes(PString ps) {
            String s = ps.getValue();
            return factory().createString(s.intern());
        }
    }
}
