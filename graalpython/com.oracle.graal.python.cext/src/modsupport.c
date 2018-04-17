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

#include <stdio.h>

PyObject* get_arg_or_kw(PyObject* argv, PyObject* kwds, char** kwdnames, int argnum, int is_optional, int is_keyword) {
    void* argv_w = to_java(argv);
    if (!is_keyword) {
        int l = truffle_invoke_i(PY_TRUFFLE_CEXT, "PyObject_LEN", argv_w);
        if (argnum < l) {
            return PyTuple_GetItem(argv_w, argnum);
        }
    }
    const char* kwdname = kwdnames[argnum];
    void* kwarg = PyDict_GetItem(kwds, truffle_read_string(kwdname));
    if (kwarg == Py_None) {
        return NULL;
    } else {
        return kwarg;
    }
}

/* argparse */
int PyTruffle_Arg_ParseTupleAndKeywords(PyObject *argv, PyObject *kwds, const char *format, char** kwdnames, int outc, void *v0, void *v1, void *v2, void *v3, void *v4, void *v5, void *v6, void *v7, void *v8, void *v9) {
    int outputn = 0;
    int formatn = 0;
    int valuen = 0;
    int rest_optional = 0;
    int rest_keywords = 0;

#   define ASSIGN(T, arg) _ASSIGN(T, outputn, arg); outputn++
#   define _ASSIGN(T, n, arg)                   \
    switch(n) {                                 \
    case 0: __ASSIGN(T, 0, arg); break;         \
    case 1: __ASSIGN(T, 1, arg); break;         \
    case 2: __ASSIGN(T, 2, arg); break;         \
    case 3: __ASSIGN(T, 3, arg); break;         \
    case 4: __ASSIGN(T, 4, arg); break;         \
    case 5: __ASSIGN(T, 5, arg); break;         \
    case 6: __ASSIGN(T, 6, arg); break;         \
    case 7: __ASSIGN(T, 7, arg); break;         \
    case 8: __ASSIGN(T, 8, arg); break;         \
    case 9: __ASSIGN(T, 9, arg); break;         \
    }
#   define __ASSIGN(T, num, arg) *((T*)_ARG(num)) = (T)arg
#   define _ARG(num) v ## num
#   define ARG(n) (((n) == 0) ? v0 : (((n) == 1) ? v1 : (((n) == 2) ? v2 : (((n) == 3) ? v3 : (((n) == 4) ? v4 : (((n) == 5) ? v5 : (((n) == 6) ? v6 : (((n) == 7) ? v7 : (((n) == 8) ? v8 : (((n) == 9) ? v9 : NULL))))))))))

#   define PEEKFMT format[formatn]
#   define POPFMT format[formatn++]
#   define POPARG get_arg_or_kw(argv, kwds, kwdnames, valuen++, rest_optional, rest_keywords)
#   define POPOUTPUTVARIABLE ARG(outputn++)

    int max = strlen(format);
    while (outputn < outc) {
        char c = POPFMT;

        if (c == 's' || c == 'z' || c == 'y') {
            PyObject* arg = POPARG;
            if (c == 'z' && arg == Py_None) {
                ASSIGN(const char*, NULL);
                if (PEEKFMT == '#') {
                    ASSIGN(int, NULL);
                    POPFMT;
                }
            } else if (PEEKFMT == '*') {
                // TODO: bytes
                ASSIGN(const char*, as_char_pointer(arg));
                POPFMT;
            } else if (PEEKFMT == '#') {
                ASSIGN(const char*, as_char_pointer(arg));
                ASSIGN(int, as_int(truffle_invoke(to_java(arg), "__len__")));
                POPFMT;
            } else {
                ASSIGN(const char*, as_char_pointer(arg));
            }
        } else if (c == 'S') {
            PyObject* arg = POPARG;
            truffle_invoke(PY_TRUFFLE_CEXT, "check_argtype", outputn, to_java(arg), to_java(truffle_read(PY_BUILTIN, "bytes")));
            ASSIGN(PyObject*, arg);
        } else if (c == 'Y') {
            goto error;
        } else if (c == 'u' || c == 'Z') {
            if (PEEKFMT == '#') {
                POPFMT;
            }
            goto error;
        } else if (c == 'U') {
            goto error;
        } else if (c == 'w' && PEEKFMT == '*') {
            POPFMT;
            goto error;
        } else if (c == 'e') {
            c = POPFMT;
            if (c == 's') {
                if (PEEKFMT == '#') {
                    POPFMT;
                }
            } else if (c == 't') {
            }
            goto error;
        } else if (c == 'b') {
            ASSIGN(unsigned char, as_uchar(POPARG));
        } else if (c == 'B') {
            ASSIGN(unsigned char, as_uchar(POPARG));
        } else if (c == 'h') {
            ASSIGN(short int, as_short(POPARG));
        } else if (c == 'H') {
            ASSIGN(short int, as_short(POPARG));
        } else if (c == 'i') {
            ASSIGN(int, as_int(POPARG));
        } else if (c == 'I') {
            ASSIGN(int, as_int(POPARG));
        } else if (c == 'l') {
            ASSIGN(long, as_long(POPARG));
        } else if (c == 'k') {
            ASSIGN(unsigned long, as_long(POPARG));
        } else if (c == 'L') {
            ASSIGN(long long, POPARG);
        } else if (c == 'K') {
            ASSIGN(unsigned long long, POPARG);
        } else if (c == 'n') {
            ASSIGN(Py_ssize_t, as_long(POPARG));
        } else if (c == 'c') {
            PyObject* arg = POPARG;
            ASSIGN(char, as_char(truffle_invoke(to_java(arg), "__getitem__", 0)));
        } else if (c == 'C') {
            PyObject* arg = POPARG;
            ASSIGN(int, as_int(truffle_invoke(to_java(arg), "__getitem__", 0)));
        } else if (c == 'f') {
            ASSIGN(float, as_float(POPARG));
        } else if (c == 'd') {
            ASSIGN(double, as_double(POPARG));
        } else if (c == 'D') {
            goto error;
        } else if (c == 'O') {
            if (PEEKFMT == '!') {
                POPFMT;
                PyTypeObject* typeobject = (PyTypeObject*)POPOUTPUTVARIABLE;
                PyObject* arg = POPARG;
                if (!(Py_TYPE(arg) == typeobject)) {
                    goto error;
                } else {
                    ASSIGN(PyObject*, arg);
                }
            } else if (PEEKFMT == '&') {
                POPFMT;
                void* (*converter)(PyObject*,void*) = POPOUTPUTVARIABLE;
                PyObject* arg = POPARG;
                int status = converter(arg, POPOUTPUTVARIABLE);
                if (!status) { // converter should have set exception
                    return NULL;
                }
            } else {
                ASSIGN(PyObject*, POPARG);
            }
        } else if (c == 'p') {
            ASSIGN(int, as_int(truffle_invoke(to_java(POPARG), "__bool__")));
        } else if (c == '(') {
            goto error;
        } else if (c == '|') {
            rest_optional = 1;
        } else if (c == '$') {
            rest_keywords = 1;
        } else if (c == ':') {
            break;
        } else if (c == ';') {
            break;
        } else {
            goto error;
        }
    }
    return outputn;

 error:
    fprintf(stderr, "ERROR: unimplemented format '%s'\n", format + formatn -1);
    return outputn;

#   undef ASSIGN
#   undef _ASSIGN
#   undef __ASSIGN
#   undef _ARG
#   undef ARG
#   undef PEEKFMT
#   undef POPFMT
#   undef POPARG
#   undef POPOUTPUTVARIABLE
}

int _PyArg_ParseStack_SizeT(PyObject** args, Py_ssize_t nargs, PyObject* kwnames, struct _PyArg_Parser* parser, ...) {
    va_list vl;
    va_start(vl, parser);
    int* fd = va_arg(vl,int*);
    *fd = args[0];
    return 1;
}

PyObject* PyTruffle_BuildValue(const char *format, void *v1, void *v2, void *v3, void *v4, void *v5, void *v6, void *v7, void *v8, void *v9, void *v10) {
    void* arg;
    int valuen = 1;
    int max = strlen(format) > 10 ? 10 : strlen(format);
    PyObject* tuple = to_java(PyTuple_New(max));

    while (valuen <= max) {
        switch (valuen) {
        case 1: arg = v1; break;
        case 2: arg = v2; break;
        case 3: arg = v3; break;
        case 4: arg = v4; break;
        case 5: arg = v5; break;
        case 6: arg = v6; break;
        case 7: arg = v7; break;
        case 8: arg = v8; break;
        case 9: arg = v9; break;
        case 10: arg = v10; break;
        }

        switch(format[valuen - 1]) {
        case 'n':
            truffle_invoke(PY_TRUFFLE_CEXT, "PyTuple_SetItem", tuple, valuen - 1, (Py_ssize_t)arg);
            break;
        case 'i':
            truffle_invoke(PY_TRUFFLE_CEXT, "PyTuple_SetItem", tuple, valuen - 1, (int)arg);
        	break;
        case 's':
            if (arg == NULL) {
                truffle_invoke(PY_TRUFFLE_CEXT, "PyTuple_SetItem", tuple, valuen - 1, to_java(Py_None));
            } else {
                truffle_invoke(PY_TRUFFLE_CEXT, "PyTuple_SetItem", tuple, valuen - 1, PyTruffle_Unicode_FromUTF8((char*)arg, Py_None));
            }
        	break;
        case 'd':
        	truffle_invoke(PY_TRUFFLE_CEXT, "PyTuple_SetItem", tuple, valuen - 1, PyFloat_FromDouble((double)(unsigned long long)arg));
        	break;
        case 'l':
        	truffle_invoke(PY_TRUFFLE_CEXT, "PyTuple_SetItem", tuple, valuen - 1, as_long((long)arg));
        	break;
        case 'L':
        	truffle_invoke(PY_TRUFFLE_CEXT, "PyTuple_SetItem", tuple, valuen - 1, as_long((long long)arg));
        	break;
        case 'k':
        	truffle_invoke(PY_TRUFFLE_CEXT, "PyTuple_SetItem", tuple, valuen - 1, as_long((unsigned long)arg));
        	break;
        case 'K':
        	truffle_invoke(PY_TRUFFLE_CEXT, "PyTuple_SetItem", tuple, valuen - 1, as_long((unsigned long long)arg));
        	break;
        case 'N':
        case 'S':
        case 'O':
        	if (arg == NULL && !PyErr_Occurred()) {
                /* If a NULL was passed because a call that should have constructed a value failed, that's OK,
                 * and we pass the error on; but if no error occurred it's not clear that the caller knew what she was doing. */
                PyErr_SetString(PyExc_SystemError, "NULL object passed to Py_BuildValue");
        	} else {
        		truffle_invoke(PY_TRUFFLE_CEXT, "PyTuple_SetItem", tuple, valuen - 1, to_java(arg));
        	}
        	break;
        default:
            fprintf(stderr, "error: unsupported format starting at %d : '%s'\n", valuen - 1, format);
            exit(-1);
        }

        valuen++;
    }

    if (valuen == 1) {
        return Py_None;
    } else if (valuen == 2) {
        // we're not using PyTuple_GetItem here because we definitely want a
        // java object and not call to_sulong on it
        return truffle_invoke(tuple, "__getitem__", 0);
    } else {
        PyObject* outputtuple = to_java(PyTuple_New(valuen - 1));
        for (valuen--; valuen > 0; valuen--) {
            truffle_invoke(PY_TRUFFLE_CEXT, "PyTuple_SetItem", outputtuple, valuen - 1, to_java(PyTuple_GetItem(tuple, valuen - 1)));
        }
        return outputtuple;
    }
}
