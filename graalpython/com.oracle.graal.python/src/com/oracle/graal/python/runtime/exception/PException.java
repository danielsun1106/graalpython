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
package com.oracle.graal.python.runtime.exception;

import com.oracle.graal.python.builtins.objects.exception.PBaseException;
import com.oracle.graal.python.builtins.objects.type.PythonClass;
import com.oracle.graal.python.runtime.PythonCore;
import com.oracle.truffle.api.TruffleException;
import com.oracle.truffle.api.TruffleStackTraceElement;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;

public final class PException extends RuntimeException implements TruffleException {
    private static final long serialVersionUID = -6437116280384996361L;
    private Node location;
    private String message = null;
    private boolean isIncompleteSource;
    private boolean exit;
    private final PBaseException pythonException;

    public PException(PBaseException actual, Node node) {
        this.pythonException = actual;
        this.location = node;
    }

    @Override
    public String getMessage() {
        if (message == null) {
            message = pythonException.toString();
        }
        return message;
    }

    public void setMessage(Object object) {
        message = object.toString();
    }

    public PythonClass getType() {
        return pythonException.getPythonClass();
    }

    public TruffleStackTraceElement getTruffleStackTraceElement(int index) {
        assert index >= 0 && index < getExceptionObject().getStackTrace().size() : "PException: stacktrace, index out of bounds";
        return getExceptionObject().getStackTrace().get(index);
    }

    @Override
    public String toString() {
        return getMessage();
    }

    @SuppressWarnings("sync-override")
    @Override
    public Throwable fillInStackTrace() {
        return null;
    }

    @Override
    public Node getLocation() {
        return location;
    }

    @Override
    public PBaseException getExceptionObject() {
        return pythonException;
    }

    @Override
    public boolean isInternalError() {
        return false;
    }

    @Override
    public int getStackTraceElementLimit() {
        return 20;
    }

    @Override
    public boolean isSyntaxError() {
        return getType() != null && getType().getName().equals("SyntaxError");
    }

    public void setIncompleteSource(boolean val) {
        isIncompleteSource = val;
    }

    @Override
    public boolean isIncompleteSource() {
        return isSyntaxError() && isIncompleteSource;
    }

    public void setExit(boolean val) {
        exit = val;
    }

    @Override
    public boolean isExit() {
        return exit;
    }

    public void expectIndexError(PythonCore core, ConditionProfile profile) {
        if (profile.profile(getType() != core.getErrorClass(PythonErrorType.IndexError))) {
            throw this;
        }
    }

    public void expectStopIteration(PythonCore core, ConditionProfile profile) {
        if (profile.profile(getType() != core.getErrorClass(PythonErrorType.StopIteration))) {
            throw this;
        }
    }

    public void expect(PythonErrorType error, PythonCore core, ConditionProfile profile) {
        if (profile.profile(getType() != core.getErrorClass(error))) {
            throw this;
        }
    }
}
