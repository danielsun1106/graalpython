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
package com.oracle.graal.python.nodes.control;

import static com.oracle.graal.python.nodes.SpecialMethodNames.__STR__;
import static com.oracle.graal.python.runtime.exception.PythonErrorType.SystemExit;

import java.io.IOException;

import com.oracle.graal.python.PythonLanguage;
import com.oracle.graal.python.builtins.objects.PNone;
import com.oracle.graal.python.builtins.objects.exception.PBaseException;
import com.oracle.graal.python.builtins.objects.function.PArguments;
import com.oracle.graal.python.builtins.objects.function.PKeyword;
import com.oracle.graal.python.builtins.objects.ints.PInt;
import com.oracle.graal.python.builtins.objects.module.PythonModule;
import com.oracle.graal.python.builtins.objects.traceback.PTraceback;
import com.oracle.graal.python.builtins.objects.type.PythonClass;
import com.oracle.graal.python.nodes.BuiltinNames;
import com.oracle.graal.python.nodes.argument.CreateArgumentsNode;
import com.oracle.graal.python.nodes.call.CallNode;
import com.oracle.graal.python.nodes.call.special.LookupAndCallUnaryNode;
import com.oracle.graal.python.runtime.PythonContext;
import com.oracle.graal.python.runtime.PythonCore;
import com.oracle.graal.python.runtime.PythonOptions;
import com.oracle.graal.python.runtime.exception.PException;
import com.oracle.graal.python.runtime.exception.PythonExitException;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;

public class TopLevelExceptionHandler extends RootNode {
    private final RootCallTarget innerCallTarget;
    private final PythonContext context;
    @Child private CreateArgumentsNode createArgs = CreateArgumentsNode.create();
    @Child private LookupAndCallUnaryNode callStrNode = LookupAndCallUnaryNode.create(__STR__);
    @Child private CallNode callNode = CallNode.create();
    private SourceSection sourceSection;

    public TopLevelExceptionHandler(PythonLanguage language, RootNode child) {
        super(language);
        sourceSection = child.getSourceSection();
        context = language.getContextReference().get();
        innerCallTarget = Truffle.getRuntime().createCallTarget(child);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object result = null;
        try {
            result = run(frame);
        } catch (PException e) {
            printExc(e);
        } catch (Throwable e) {
            if (PythonOptions.getOption(context, PythonOptions.WithJavaStacktrace)) {
                boolean exitException = e instanceof TruffleException && ((TruffleException) e).isExit();
                if (!exitException) {
                    printStackTrace(e);
                }
            }
            throw e;
        }
        return result;
    }

    @Override
    public SourceSection getSourceSection() {
        return sourceSection;
    }

    /**
     * This function is kind-of analogous to PyErr_PrintEx. TODO (timfel): Figure out if we should
     * move this somewhere else
     */
    private void printExc(PException e) {
        CompilerDirectives.transferToInterpreter();
        PythonCore core = context.getCore();
        if (core.getErrorClass(SystemExit) == e.getType()) {
            handleSystemExit(e);
        }

        PBaseException value = e.getExceptionObject();
        PythonClass type = value.getPythonClass();
        PTraceback tb = value.getTraceback(core.factory());

        PythonModule sys = core.lookupBuiltinModule("sys");
        sys.setAttribute(BuiltinNames.LAST_TYPE, type);
        sys.setAttribute(BuiltinNames.LAST_VALUE, value);
        sys.setAttribute(BuiltinNames.LAST_TRACEBACK, tb);

        Object hook = sys.getAttribute(BuiltinNames.EXCEPTHOOK);
        if (PythonOptions.getOption(context, PythonOptions.AlwaysRunExcepthook)) {
            if (hook != PNone.NO_VALUE) {
                try {
                    callNode.execute(hook, new Object[]{type, value, tb}, PKeyword.EMPTY_KEYWORDS);
                } catch (PException internalError) {
                    // More complex handling of errors in exception printing is done in our
                    // Python code, if we get here, we just fall back to the launcher
                    throw e;
                }
                if (!getSourceSection().getSource().isInteractive()) {
                    throw new PythonExitException(this, 1);
                }
            } else {
                try {
                    context.getEnv().err().write("sys.excepthook is missing\n".getBytes());
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }
        throw e;
    }

    private void handleSystemExit(PException e) {
        if (PythonOptions.getOption(context, PythonOptions.PythonInspectFlag) && !getSourceSection().getSource().isInteractive()) {
            // Don't exit if -i flag was given and we're not yet running interactively
            return;
        }
        Object attribute = e.getExceptionObject().getAttribute("code");
        Integer exitcode = null;
        if (attribute instanceof Number) {
            exitcode = ((Number) attribute).intValue();
        } else if (attribute instanceof PInt) {
            exitcode = ((PInt) attribute).intValue();
        } else if (attribute instanceof PNone) {
            exitcode = 0; // "goto done" case in CPython
        }
        if (exitcode != null) {
            throw new PythonExitException(this, exitcode);
        }
        if (PythonOptions.getOption(context, PythonOptions.AlwaysRunExcepthook)) {
            // If we failed to dig out the exit code we just print and leave
            try {
                context.getEnv().err().write(callStrNode.executeObject(e.getExceptionObject()).toString().getBytes());
                context.getEnv().err().write('\n');
            } catch (IOException e1) {
            }
            throw new PythonExitException(this, 1);
        }
        e.setExit(true);
        throw e;
    }

    @TruffleBoundary
    private static void printStackTrace(Throwable e) {
        e.printStackTrace();
    }

    private Object run(VirtualFrame frame) {
        Object[] arguments = createArgs.execute(frame.getArguments());
        PArguments.setGlobals(arguments, context.getMainModule());
        return innerCallTarget.call(arguments);
    }

    @Override
    public boolean isInternal() {
        return true;
    }
}
