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
#include "capi.h"

#include <pyerrors.h>

#define PY_EXCEPTION(__EXC_NAME__) ((PyObject*)truffle_import_cached("python_" __EXC_NAME__))

PyObject * PyExc_BaseException = NULL;
PyObject * PyExc_Exception = NULL;
PyObject * PyExc_AttributeError = NULL;
PyObject * PyExc_FloatingPointError = NULL;
PyObject * PyExc_OSError = NULL;
PyObject * PyExc_ImportError = NULL;
PyObject * PyExc_IndexError = NULL;
PyObject * PyExc_KeyError = NULL;
PyObject * PyExc_MemoryError = NULL;
PyObject * PyExc_NameError = NULL;
PyObject * PyExc_OverflowError = NULL;
PyObject * PyExc_RuntimeError = NULL;
PyObject * PyExc_SystemError = NULL;
PyObject * PyExc_TypeError = NULL;
PyObject * PyExc_ValueError = NULL;
PyObject * PyExc_IOError = NULL;
PyObject * PyExc_Warning = NULL;
PyObject * PyExc_UserWarning = NULL;
PyObject * PyExc_DeprecationWarning = NULL;
PyObject * PyExc_RuntimeWarning = NULL;
PyObject * PyExc_FutureWarning = NULL;
PyObject * PyExc_PendingDeprecationWarning = NULL;
PyObject * PyExc_SyntaxWarning = NULL;
PyObject * PyExc_ImportWarning = NULL;
PyObject * PyExc_UnicodeWarning = NULL;
PyObject * PyExc_BytesWarning = NULL;
PyObject * PyExc_ResourceWarning = NULL;

void initialize_exceptions() {
	PyExc_AttributeError = PY_EXCEPTION("AttributeError");
	PyExc_BaseException = PY_EXCEPTION("BaseException");
	PyExc_BytesWarning = PY_EXCEPTION("BytesWarning");
	PyExc_DeprecationWarning = PY_EXCEPTION("DeprecationWarning");
	PyExc_Exception = PY_EXCEPTION("Exception");
	PyExc_FloatingPointError = PY_EXCEPTION("FloatingPointError");
	PyExc_IOError = PY_EXCEPTION("IOError");
	PyExc_ImportError = PY_EXCEPTION("ImportError");
	PyExc_ImportWarning = PY_EXCEPTION("ImportWarning");
	PyExc_IndexError = PY_EXCEPTION("IndexError");
	PyExc_KeyError = PY_EXCEPTION("KeyError");
	PyExc_MemoryError = PY_EXCEPTION("MemoryError");
	PyExc_NameError = PY_EXCEPTION("NameError");
	PyExc_OSError = PY_EXCEPTION("OSError");
	PyExc_OverflowError = PY_EXCEPTION("OverflowError");
	PyExc_PendingDeprecationWarning = PY_EXCEPTION("PendingDeprecationWarning");
	PyExc_ResourceWarning = PY_EXCEPTION("ResourceWarning");
	PyExc_RuntimeError = PY_EXCEPTION("RuntimeError");
	PyExc_RuntimeWarning = PY_EXCEPTION("RuntimeWarning");
	PyExc_SyntaxWarning = PY_EXCEPTION("SyntaxWarning");
	PyExc_SystemError = PY_EXCEPTION("SystemError");
	PyExc_TypeError = PY_EXCEPTION("TypeError");
	PyExc_UnicodeWarning = PY_EXCEPTION("UnicodeWarning");
	PyExc_UserWarning = PY_EXCEPTION("UserWarning");
	PyExc_ValueError = PY_EXCEPTION("ValueError");
	PyExc_Warning = PY_EXCEPTION("Warning");
}

