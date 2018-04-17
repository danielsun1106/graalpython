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

/* Tuples */
PyObject* PyTuple_New(Py_ssize_t size) {
    return (PyObject*)truffle_invoke(PY_TRUFFLE_CEXT, "PyTuple_New", size);
}

int PyTuple_SetItem(PyObject* tuple, Py_ssize_t position, PyObject* item) {
    return truffle_invoke_i(PY_TRUFFLE_CEXT, "PyTuple_SetItem", to_java(tuple), position, to_java(item));
}

PyObject* PyTuple_GetItem(PyObject* tuple, Py_ssize_t position) {
	PyObject* result = truffle_invoke(PY_TRUFFLE_CEXT, "PyTuple_GetItem", to_java(tuple), position, ERROR_MARKER);
	if (result == ERROR_MARKER) {
		return NULL;
	}
    return to_sulong(result);
}

Py_ssize_t PyTuple_Size(PyObject *op) {
    return truffle_invoke_i(PY_TRUFFLE_CEXT, "PyTuple_Size", to_java(op));
}

PyObject* PyTuple_GetSlice(PyObject *tuple, Py_ssize_t i, Py_ssize_t j) {
    PyObject* result = truffle_invoke(PY_TRUFFLE_CEXT, "PyTuple_GetSlice", to_java(tuple), i, j, ERROR_MARKER);
    if (result == ERROR_MARKER) {
    	return NULL;
    }
    return to_sulong(result);
}

PyObject* PyTuple_Pack(Py_ssize_t n, ...) {
    va_list vargs;

    va_start(vargs, n);
    PyObject *result = PyTuple_New(n);
    if (result == NULL) {
        va_end(vargs);
        return NULL;
    }
    for (Py_ssize_t i = 0; i < n; i++) {
        PyObject *o = va_arg(vargs, PyObject *);
        PyTuple_SetItem(result, i, o);
    }
    va_end(vargs);
    return result;
}
