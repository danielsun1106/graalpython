#ifndef PYTHON_H
#define PYTHON_H

#define HAVE_UTIME_H
#define HAVE_UNISTD_H
#define HAVE_SIGNAL_H
#define HAVE_FCNTL_H
#define HAVE_SYS_WAIT_H

#include <truffle.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/dir.h>
#include <dirent.h>
#include <locale.h>
#include <langinfo.h>
#include <assert.h>
#include <unistd.h>

#include "pyport.h"
#include "pymacro.h"
#include "object.h"
#include "abstract.h"
#include "methodobject.h"
#include "moduleobject.h"
#include "unicodeobject.h"
#include "pystate.h"
#include "pyarena.h"
#include "pythonrun.h"
#include "ceval.h"
#include "pyerrors.h"
#include "modsupport.h"
#include "tupleobject.h"
#include "structseq.h"
#include "structmember.h"
#include "pytime.h"
#include "pymem.h"
#include "objimpl.h"
#include "bytesobject.h"
#include "longobject.h"
#include "boolobject.h"
#include "floatobject.h"
#include "dictobject.h"
#include "setobject.h"
#include "complexobject.h"
#include "listobject.h"
#include "sliceobject.h"
#include "descrobject.h"
#include "fileobject.h"
#include "pyctype.h"
#include "bytearrayobject.h"
#include "warnings.h"

// our impls
#ifdef Py_None
#undef Py_None
#define Py_None truffle_invoke(PY_TRUFFLE_CEXT, "Py_None")
#endif

#ifdef Py_True
#undef Py_True
#define Py_True truffle_invoke(PY_TRUFFLE_CEXT, "Py_True")
#endif

#ifdef Py_False
#undef Py_False
#define Py_False truffle_invoke(PY_TRUFFLE_CEXT, "Py_False")
#endif

#undef Py_NoValue
#define Py_NoValue truffle_invoke(PY_TRUFFLE_CEXT, "Py_NoValue")

#define PY_TRUFFLE_CEXT ((void*)truffle_import_cached("python_cext"))
#define PY_BUILTIN ((void*)truffle_import_cached("python_builtins"))

// TODO: we must extend the refcounting behavior to support handles to managed objects
#undef Py_DECREF
#define Py_DECREF(o)
#undef Py_INCREF
#define Py_INCREF(o)


/* 
 * #define Py_INCREF(op) (                         \
 *     _Py_INC_REFTOTAL  _Py_REF_DEBUG_COMMA       \
 *     ((PyObject *)(op))->ob_refcnt++)
 * 
 * #define Py_DECREF(op)                                                   \
 *     do {                                                                \
 *         void* handle = op;                                              \
 *         PyObject *_py_decref_tmp = (PyObject *)((truffle_is_handle_to_managed(handle) ? truffle_managed_from_handle(handle) : handle)); \
 *         if (_Py_DEC_REFTOTAL  _Py_REF_DEBUG_COMMA                       \
 *             --(_py_decref_tmp)->ob_refcnt != 0) {                       \
 *             _Py_CHECK_REFCNT(_py_decref_tmp)                            \
 *             else                                                        \
 *                 _Py_Dealloc(_py_decref_tmp);                            \
 *     } while (0)
 */


#undef Py_RETURN_NONE
#define Py_RETURN_NONE return Py_None;

#define PyMem_RawMalloc(size) malloc(size)
#define PyMem_RawCalloc(nelem, elsize) calloc(nelem, elsize)
#define PyMem_RawRealloc(ptr, new_size) realloc(ptr, new_size)
#define PyMem_RawFree(ptr) free(ptr)

#define _PyLong_FromTime_t(o) ((long)o)

extern int PyTruffle_Arg_ParseTupleAndKeywords(PyObject *argv, PyObject *kwds, const char *format, char** kwdnames, int outc, void *v0, void *v1, void *v2, void *v3, void *v4, void *v5, void *v6, void *v7, void *v8, void *v9);
extern PyObject* PyTruffle_BuildValue(const char *format, void *v1, void *v2, void *v3, void *v4, void *v5, void *v6, void *v7, void *v8, void *v9, void *v10);

#define PyTruffle_BuildValue_1(FORMAT, V1) PyTruffle_BuildValue(FORMAT, (void*)V1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
#define PyTruffle_BuildValue_2(FORMAT, V1, V2) PyTruffle_BuildValue(FORMAT, (void*)V1, (void*)V2, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
#define PyTruffle_BuildValue_3(FORMAT, V1, V2, V3) PyTruffle_BuildValue(FORMAT, (void*)V1, (void*)V2, (void*)V3, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
#define PyTruffle_BuildValue_4(FORMAT, V1, V2, V3, V4) PyTruffle_BuildValue(FORMAT, (void*)V1, (void*)V2, (void*)V3, (void*)V4, NULL, NULL, NULL, NULL, NULL, NULL)
#define PyTruffle_BuildValue_5(FORMAT, V1, V2, V3, V4, V5) PyTruffle_BuildValue(FORMAT, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, NULL, NULL, NULL, NULL, NULL)
#define PyTruffle_BuildValue_6(FORMAT, V1, V2, V3, V4, V5, V6) PyTruffle_BuildValue(FORMAT, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, (void*)V6, NULL, NULL, NULL, NULL)
#define PyTruffle_BuildValue_7(FORMAT, V1, V2, V3, V4, V5, V6, V7) PyTruffle_BuildValue(FORMAT, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, (void*)V6, (void*)V7, NULL, NULL, NULL)
#define PyTruffle_BuildValue_8(FORMAT, V1, V2, V3, V4, V5, V6, V7, V8) PyTruffle_BuildValue(FORMAT, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, (void*)V6, (void*)V7, (void*)V8, NULL, NULL)
#define PyTruffle_BuildValue_9(FORMAT, V1, V2, V3, V4, V5, V6, V7, V8, V9) PyTruffle_BuildValue(FORMAT, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, (void*)V6, (void*)V7, (void*)V8, (void*)V9, NULL)
#define PyTruffle_BuildValue_10(FORMAT, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10) PyTruffle_BuildValue(FORMAT, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, (void*)V6, (void*)V7, (void*)V8, (void*)V9, (void*)V10)
#define BUILD_VALUE_IMPL(_1, _2, _3, _4, _5, _6, _7, _8, _9, _10, NAME, ...) NAME
#ifdef Py_BuildValue
#undef Py_BuildValue
#endif
#define Py_BuildValue(FORMAT, ...) BUILD_VALUE_IMPL(__VA_ARGS__, PyTruffle_BuildValue_10, PyTruffle_BuildValue_9, PyTruffle_BuildValue_8, PyTruffle_BuildValue_7, PyTruffle_BuildValue_6, PyTruffle_BuildValue_5, PyTruffle_BuildValue_4, PyTruffle_BuildValue_3, PyTruffle_BuildValue_2, PyTruffle_BuildValue_1)(FORMAT, __VA_ARGS__)

#define PyTruffle_Arg_ParseTupleAndKeywords_1(ARGV, KWDS, FORMAT, KWDNAMES, V1) PyTruffle_Arg_ParseTupleAndKeywords(ARGV, KWDS, FORMAT, KWDNAMES, 1, (void*)V1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
#define PyTruffle_Arg_ParseTupleAndKeywords_2(ARGV, KWDS, FORMAT, KWDNAMES, V1, V2) PyTruffle_Arg_ParseTupleAndKeywords(ARGV, KWDS, FORMAT, KWDNAMES, 2, (void*)V1, (void*)V2, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
#define PyTruffle_Arg_ParseTupleAndKeywords_3(ARGV, KWDS, FORMAT, KWDNAMES, V1, V2, V3) PyTruffle_Arg_ParseTupleAndKeywords(ARGV, KWDS, FORMAT, KWDNAMES, 3, (void*)V1, (void*)V2, (void*)V3, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
#define PyTruffle_Arg_ParseTupleAndKeywords_4(ARGV, KWDS, FORMAT, KWDNAMES, V1, V2, V3, V4) PyTruffle_Arg_ParseTupleAndKeywords(ARGV, KWDS, FORMAT, KWDNAMES, 4, (void*)V1, (void*)V2, (void*)V3, (void*)V4, NULL, NULL, NULL, NULL, NULL, NULL)
#define PyTruffle_Arg_ParseTupleAndKeywords_5(ARGV, KWDS, FORMAT, KWDNAMES, V1, V2, V3, V4, V5) PyTruffle_Arg_ParseTupleAndKeywords(ARGV, KWDS, FORMAT, KWDNAMES, 5, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, NULL, NULL, NULL, NULL, NULL)
#define PyTruffle_Arg_ParseTupleAndKeywords_6(ARGV, KWDS, FORMAT, KWDNAMES, V1, V2, V3, V4, V5, V6) PyTruffle_Arg_ParseTupleAndKeywords(ARGV, KWDS, FORMAT, KWDNAMES, 6, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, (void*)V6, NULL, NULL, NULL, NULL)
#define PyTruffle_Arg_ParseTupleAndKeywords_7(ARGV, KWDS, FORMAT, KWDNAMES, V1, V2, V3, V4, V5, V6, V7) PyTruffle_Arg_ParseTupleAndKeywords(ARGV, KWDS, FORMAT, KWDNAMES, 7, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, (void*)V6, (void*)V7, NULL, NULL, NULL)
#define PyTruffle_Arg_ParseTupleAndKeywords_8(ARGV, KWDS, FORMAT, KWDNAMES, V1, V2, V3, V4, V5, V6, V7, V8) PyTruffle_Arg_ParseTupleAndKeywords(ARGV, KWDS, FORMAT, KWDNAMES, 8, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, (void*)V6, (void*)V7, (void*)V8, NULL, NULL)
#define PyTruffle_Arg_ParseTupleAndKeywords_9(ARGV, KWDS, FORMAT, KWDNAMES, V1, V2, V3, V4, V5, V6, V7, V8, V9) PyTruffle_Arg_ParseTupleAndKeywords(ARGV, KWDS, FORMAT, KWDNAMES, 9, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, (void*)V6, (void*)V7, (void*)V8, (void*)V9, NULL)
#define PyTruffle_Arg_ParseTupleAndKeywords_10(ARGV, KWDS, FORMAT, KWDNAMES, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10) PyTruffle_Arg_ParseTupleAndKeywords(ARGV, KWDS, FORMAT, KWDNAMES, 10, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, (void*)V6, (void*)V7, (void*)V8, (void*)V9, (void*)V10)
#define ARG_PARSE_TUPLE_IMPL(_1, _2, _3, _4, _5, _6, _7, _8, _9, _10, NAME, ...) NAME
#ifdef PyArg_ParseTupleAndKeywords
#undef PyArg_ParseTupleAndKeywords
#endif
#define PyArg_ParseTupleAndKeywords(ARGV, KWDS, FORMAT, KWDNAMES, ...) ARG_PARSE_TUPLE_IMPL(__VA_ARGS__, PyTruffle_Arg_ParseTupleAndKeywords_10, PyTruffle_Arg_ParseTupleAndKeywords_9, PyTruffle_Arg_ParseTupleAndKeywords_8, PyTruffle_Arg_ParseTupleAndKeywords_7, PyTruffle_Arg_ParseTupleAndKeywords_6, PyTruffle_Arg_ParseTupleAndKeywords_5, PyTruffle_Arg_ParseTupleAndKeywords_4, PyTruffle_Arg_ParseTupleAndKeywords_3, PyTruffle_Arg_ParseTupleAndKeywords_2, PyTruffle_Arg_ParseTupleAndKeywords_1)(ARGV, KWDS, FORMAT, KWDNAMES, __VA_ARGS__)

#ifdef PyArg_ParseTuple
#undef PyArg_ParseTuple
#endif
#define PyArg_ParseTuple(ARGV, FORMAT, ...) PyArg_ParseTupleAndKeywords(ARGV, PyDict_New(), FORMAT, (char*[]) { NULL }, __VA_ARGS__)

#ifdef _PyArg_ParseTupleAndKeywordsFast
#undef _PyArg_ParseTupleAndKeywordsFast
#endif
#define _PyArg_ParseTupleAndKeywordsFast(ARGS, KWARGS, PARSER, ...) PyArg_ParseTupleAndKeywords(ARGS, KWARGS, (PARSER)->format, (PARSER)->keywords, __VA_ARGS__)

#ifdef PyArg_Parse
#undef PyArg_Parse
#endif
#define PyArg_Parse(ARGV, FORMAT, ...) PyArg_ParseTupleAndKeywords(ARGV, PyDict_New(), FORMAT, (char*[]) { NULL }, __VA_ARGS__)

extern PyObject* PyTruffle_Unicode_FromFormat(const char* fmt, int s, void* v0, void* v1, void* v2, void* v3, void* v4, void* v5, void* v6, void* v7, void* v8, void* v9);
#define PyTruffle_Unicode_FromFormat_1(FORMAT, V1) PyTruffle_Unicode_FromFormat(FORMAT, 1, (void*)V1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
#define PyTruffle_Unicode_FromFormat_2(FORMAT, V1, V2) PyTruffle_Unicode_FromFormat(FORMAT, 2, (void*)V1, (void*)V2, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
#define PyTruffle_Unicode_FromFormat_3(FORMAT, V1, V2, V3) PyTruffle_Unicode_FromFormat(FORMAT, 3, (void*)V1, (void*)V2, (void*)V3, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
#define PyTruffle_Unicode_FromFormat_4(FORMAT, V1, V2, V3, V4) PyTruffle_Unicode_FromFormat(FORMAT, 4, (void*)V1, (void*)V2, (void*)V3, (void*)V4, NULL, NULL, NULL, NULL, NULL, NULL)
#define PyTruffle_Unicode_FromFormat_5(FORMAT, V1, V2, V3, V4, V5) PyTruffle_Unicode_FromFormat(FORMAT, 5, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, NULL, NULL, NULL, NULL, NULL)
#define PyTruffle_Unicode_FromFormat_6(FORMAT, V1, V2, V3, V4, V5, V6) PyTruffle_Unicode_FromFormat(FORMAT, 6, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, (void*)V6, NULL, NULL, NULL, NULL)
#define PyTruffle_Unicode_FromFormat_7(FORMAT, V1, V2, V3, V4, V5, V6, V7) PyTruffle_Unicode_FromFormat(FORMAT, 7, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, (void*)V6, (void*)V7, NULL, NULL, NULL)
#define PyTruffle_Unicode_FromFormat_8(FORMAT, V1, V2, V3, V4, V5, V6, V7, V8) PyTruffle_Unicode_FromFormat(FORMAT, 8, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, (void*)V6, (void*)V7, (void*)V8, NULL, NULL)
#define PyTruffle_Unicode_FromFormat_9(FORMAT, V1, V2, V3, V4, V5, V6, V7, V8, V9) PyTruffle_Unicode_FromFormat(FORMAT, 9, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, (void*)V6, (void*)V7, (void*)V8, (void*)V9, NULL)
#define PyTruffle_Unicode_FromFormat_10(FORMAT, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10) PyTruffle_Unicode_FromFormat(FORMAT, 10, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, (void*)V6, (void*)V7, (void*)V8, (void*)V9, (void*)V10)
#define ARG_PARSE_UNICODE_FORMAT_IMPL(_1, _2, _3, _4, _5, _6, _7, _8, _9, _10, NAME, ...) NAME
#define PyUnicode_FromFormat(FORMAT, ...) ARG_PARSE_UNICODE_FORMAT_IMPL(__VA_ARGS__, PyTruffle_Unicode_FromFormat_10, PyTruffle_Unicode_FromFormat_9, PyTruffle_Unicode_FromFormat_8, PyTruffle_Unicode_FromFormat_7, PyTruffle_Unicode_FromFormat_6, PyTruffle_Unicode_FromFormat_5, PyTruffle_Unicode_FromFormat_4, PyTruffle_Unicode_FromFormat_3, PyTruffle_Unicode_FromFormat_2, PyTruffle_Unicode_FromFormat_1)(FORMAT, __VA_ARGS__)

extern PyObject* PyTruffle_Object_CallFunction(PyObject* callable, const char* fmt, int c, void* v0, void* v1, void* v2, void* v3, void* v4, void* v5, void* v6, void* v7, void* v8, void* v9);
#define PyTruffle_Object_CallFunction_1(F, FMT, V1) PyTruffle_Object_CallFunction(F, FMT, 1, (void*)V1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
#define PyTruffle_Object_CallFunction_2(F, FMT, V1, V2) PyTruffle_Object_CallFunction(F, FMT, 2, (void*)V1, (void*)V2, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
#define PyTruffle_Object_CallFunction_3(F, FMT, V1, V2, V3) PyTruffle_Object_CallFunction(F, FMT, 3, (void*)V1, (void*)V2, (void*)V3, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
#define PyTruffle_Object_CallFunction_4(F, FMT, V1, V2, V3, V4) PyTruffle_Object_CallFunction(F, FMT, 4, (void*)V1, (void*)V2, (void*)V3, (void*)V4, NULL, NULL, NULL, NULL, NULL, NULL)
#define PyTruffle_Object_CallFunction_5(F, FMT, V1, V2, V3, V4, V5) PyTruffle_Object_CallFunction(F, FMT, 5, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, NULL, NULL, NULL, NULL, NULL)
#define PyTruffle_Object_CallFunction_6(F, FMT, V1, V2, V3, V4, V5, V6) PyTruffle_Object_CallFunction(F, FMT, 6, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, (void*)V6, NULL, NULL, NULL, NULL)
#define PyTruffle_Object_CallFunction_7(F, FMT, V1, V2, V3, V4, V5, V6, V7) PyTruffle_Object_CallFunction(F, FMT, 7, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, (void*)V6, (void*)V7, NULL, NULL, NULL)
#define PyTruffle_Object_CallFunction_8(F, FMT, V1, V2, V3, V4, V5, V6, V7, V8) PyTruffle_Object_CallFunction(F, FMT, 8, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, (void*)V6, (void*)V7, (void*)V8, NULL, NULL)
#define PyTruffle_Object_CallFunction_9(F, FMT, V1, V2, V3, V4, V5, V6, V7, V8, V9) PyTruffle_Object_CallFunction(F, FMT, 9, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, (void*)V6, (void*)V7, (void*)V8, (void*)V9, NULL)
#define PyTruffle_Object_CallFunction_10(F, FMT, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10) PyTruffle_Object_CallFunction(F, FMT, 10, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, (void*)V6, (void*)V7, (void*)V8, (void*)V9, (void*)V10)
#define CALL_FUNCTION_IMPL(_1, _2, _3, _4, _5, _6, _7, _8, _9, _10, NAME, ...) NAME
#ifdef PyObject_CallFunction
#undef PyObject_CallFunction
#endif
#define PyObject_CallFunction(F, FMT, ...) CALL_FUNCTION_IMPL(__VA_ARGS__, PyTruffle_Object_CallFunction_10, PyTruffle_Object_CallFunction_9, PyTruffle_Object_CallFunction_8, PyTruffle_Object_CallFunction_7, PyTruffle_Object_CallFunction_6, PyTruffle_Object_CallFunction_5, PyTruffle_Object_CallFunction_4, PyTruffle_Object_CallFunction_3, PyTruffle_Object_CallFunction_2, PyTruffle_Object_CallFunction_1)(F, FMT, __VA_ARGS__)

#ifdef PyObject_CallFunctionObjArgs
#undef PyObject_CallFunctionObjArgs
#endif
#define PyTruffle_Object_CallFunctionObjArgs_1(F, V1) PyTruffle_Object_CallFunction(F, "", 0, (void*)V1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
#define PyTruffle_Object_CallFunctionObjArgs_2(F, V1, V2) PyTruffle_Object_CallFunction(F, "O", 1, (void*)V1, (void*)V2, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
#define PyTruffle_Object_CallFunctionObjArgs_3(F, V1, V2, V3) PyTruffle_Object_CallFunction(F, "OO", 2, (void*)V1, (void*)V2, (void*)V3, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
#define PyTruffle_Object_CallFunctionObjArgs_4(F, V1, V2, V3, V4) PyTruffle_Object_CallFunction(F, "OOO", 3, (void*)V1, (void*)V2, (void*)V3, (void*)V4, NULL, NULL, NULL, NULL, NULL, NULL)
#define PyTruffle_Object_CallFunctionObjArgs_5(F, V1, V2, V3, V4, V5) PyTruffle_Object_CallFunction(F, "OOOO", 4, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, NULL, NULL, NULL, NULL, NULL)
#define PyTruffle_Object_CallFunctionObjArgs_6(F, V1, V2, V3, V4, V5, V6) PyTruffle_Object_CallFunction(F, "OOOOO", 5, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, (void*)V6, NULL, NULL, NULL, NULL)
#define PyTruffle_Object_CallFunctionObjArgs_7(F, V1, V2, V3, V4, V5, V6, V7) PyTruffle_Object_CallFunction(F, "OOOOOO", 6, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, (void*)V6, (void*)V7, NULL, NULL, NULL)
#define PyTruffle_Object_CallFunctionObjArgs_8(F, V1, V2, V3, V4, V5, V6, V7, V8) PyTruffle_Object_CallFunction(F, "OOOOOOO", 7, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, (void*)V6, (void*)V7, (void*)V8, NULL, NULL)
#define PyTruffle_Object_CallFunctionObjArgs_9(F, V1, V2, V3, V4, V5, V6, V7, V8, V9) PyTruffle_Object_CallFunction(F, "OOOOOOOO", 8, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, (void*)V6, (void*)V7, (void*)V8, (void*)V9, NULL)
#define PyTruffle_Object_CallFunctionObjArgs_10(F, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10) PyTruffle_Object_CallFunction(F, "OOOOOOOOO", 9, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, (void*)V6, (void*)V7, (void*)V8, (void*)V9, (void*)V10)
#define CALL_FUNCTION_IMPL(_1, _2, _3, _4, _5, _6, _7, _8, _9, _10, NAME, ...) NAME
#define PyObject_CallFunctionObjArgs(F, ...) CALL_FUNCTION_IMPL(__VA_ARGS__, PyTruffle_Object_CallFunctionObjArgs_10, PyTruffle_Object_CallFunctionObjArgs_9, PyTruffle_Object_CallFunctionObjArgs_8, PyTruffle_Object_CallFunctionObjArgs_7, PyTruffle_Object_CallFunctionObjArgs_6, PyTruffle_Object_CallFunctionObjArgs_5, PyTruffle_Object_CallFunctionObjArgs_4, PyTruffle_Object_CallFunctionObjArgs_3, PyTruffle_Object_CallFunctionObjArgs_2, PyTruffle_Object_CallFunctionObjArgs_1)(F, __VA_ARGS__)


extern PyObject* PyTruffle_Object_CallMethod(PyObject* object, const char* method, const char* fmt, int c, void* v0, void* v1, void* v2, void* v3, void* v4, void* v5, void* v6, void* v7, void* v8, void* v9);
#define PyTruffle_Object_CallMethod_1(O, M, FMT, V1) PyTruffle_Object_CallMethod(O, M, FMT, 1, (void*)V1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
#define PyTruffle_Object_CallMethod_2(O, M, FMT, V1, V2) PyTruffle_Object_CallMethod(O, M, FMT, 2, (void*)V1, (void*)V2, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
#define PyTruffle_Object_CallMethod_3(O, M, FMT, V1, V2, V3) PyTruffle_Object_CallMethod(O, M, FMT, 3, (void*)V1, (void*)V2, (void*)V3, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
#define PyTruffle_Object_CallMethod_4(O, M, FMT, V1, V2, V3, V4) PyTruffle_Object_CallMethod(O, M, FMT, 4, (void*)V1, (void*)V2, (void*)V3, (void*)V4, NULL, NULL, NULL, NULL, NULL, NULL)
#define PyTruffle_Object_CallMethod_5(O, M, FMT, V1, V2, V3, V4, V5) PyTruffle_Object_CallMethod(O, M, FMT, 5, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, NULL, NULL, NULL, NULL, NULL)
#define PyTruffle_Object_CallMethod_6(O, M, FMT, V1, V2, V3, V4, V5, V6) PyTruffle_Object_CallMethod(O, M, FMT, 6, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, (void*)V6, NULL, NULL, NULL, NULL)
#define PyTruffle_Object_CallMethod_7(O, M, FMT, V1, V2, V3, V4, V5, V6, V7) PyTruffle_Object_CallMethod(O, M, FMT, 7, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, (void*)V6, (void*)V7, NULL, NULL, NULL)
#define PyTruffle_Object_CallMethod_8(O, M, FMT, V1, V2, V3, V4, V5, V6, V7, V8) PyTruffle_Object_CallMethod(O, M, FMT, 8, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, (void*)V6, (void*)V7, (void*)V8, NULL, NULL)
#define PyTruffle_Object_CallMethod_9(O, M, FMT, V1, V2, V3, V4, V5, V6, V7, V8, V9) PyTruffle_Object_CallMethod(O, M, FMT, 9, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, (void*)V6, (void*)V7, (void*)V8, (void*)V9, NULL)
#define PyTruffle_Object_CallMethod_10(O, M, FMT, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10) PyTruffle_Object_CallMethod(O, M, FMT, 10, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, (void*)V6, (void*)V7, (void*)V8, (void*)V9, (void*)V10)
#define CALL_FUNCTION_IMPL(_1, _2, _3, _4, _5, _6, _7, _8, _9, _10, NAME, ...) NAME
#ifdef PyObject_CallMethod
#undef PyObject_CallMethod
#endif
#define PyObject_CallMethod(O, M, FMT, ...) CALL_FUNCTION_IMPL(__VA_ARGS__, PyTruffle_Object_CallMethod_10, PyTruffle_Object_CallMethod_9, PyTruffle_Object_CallMethod_8, PyTruffle_Object_CallMethod_7, PyTruffle_Object_CallMethod_6, PyTruffle_Object_CallMethod_5, PyTruffle_Object_CallMethod_4, PyTruffle_Object_CallMethod_3, PyTruffle_Object_CallMethod_2, PyTruffle_Object_CallMethod_1)(O, M, FMT, __VA_ARGS__)


extern PyObject* PyTruffle_Err_Format(PyObject* exception, const char* fmt, int s, void* v0, void* v1, void* v2, void* v3, void* v4, void* v5, void* v6, void* v7, void* v8, void* v9);
#define PyTruffle_Err_Format_0(EXC, FORMAT) PyTruffle_Err_Format(EXC, FORMAT, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
#define PyTruffle_Err_Format_1(EXC, FORMAT, V1) PyTruffle_Err_Format(EXC, FORMAT, 1, (void*)V1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
#define PyTruffle_Err_Format_2(EXC, FORMAT, V1, V2) PyTruffle_Err_Format(EXC, FORMAT, 2, (void*)V1, (void*)V2, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
#define PyTruffle_Err_Format_3(EXC, FORMAT, V1, V2, V3) PyTruffle_Err_Format(EXC, FORMAT, 3, (void*)V1, (void*)V2, (void*)V3, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
#define PyTruffle_Err_Format_4(EXC, FORMAT, V1, V2, V3, V4) PyTruffle_Err_Format(EXC, FORMAT, 4, (void*)V1, (void*)V2, (void*)V3, (void*)V4, NULL, NULL, NULL, NULL, NULL, NULL)
#define PyTruffle_Err_Format_5(EXC, FORMAT, V1, V2, V3, V4, V5) PyTruffle_Err_Format(EXC, FORMAT, 5, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, NULL, NULL, NULL, NULL, NULL)
#define PyTruffle_Err_Format_6(EXC, FORMAT, V1, V2, V3, V4, V5, V6) PyTruffle_Err_Format(EXC, FORMAT, 6, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, (void*)V6, NULL, NULL, NULL, NULL)
#define PyTruffle_Err_Format_7(EXC, FORMAT, V1, V2, V3, V4, V5, V6, V7) PyTruffle_Err_Format(EXC, FORMAT, 7, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, (void*)V6, (void*)V7, NULL, NULL, NULL)
#define PyTruffle_Err_Format_8(EXC, FORMAT, V1, V2, V3, V4, V5, V6, V7, V8) PyTruffle_Err_Format(EXC, FORMAT, 8, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, (void*)V6, (void*)V7, (void*)V8, NULL, NULL)
#define PyTruffle_Err_Format_9(EXC, FORMAT, V1, V2, V3, V4, V5, V6, V7, V8, V9) PyTruffle_Err_Format(EXC, FORMAT, 9, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, (void*)V6, (void*)V7, (void*)V8, (void*)V9, NULL)
#define PyTruffle_Err_Format_10(EXC, FORMAT, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10) PyTruffle_Err_Format(EXC, FORMAT, 10, (void*)V1, (void*)V2, (void*)V3, (void*)V4, (void*)V5, (void*)V6, (void*)V7, (void*)V8, (void*)V9, (void*)V10)
#define ARG_PARSE_ERR_FORMAT_IMPL(_0, _1, _2, _3, _4, _5, _6, _7, _8, _9, _10, NAME, ...) NAME
#ifdef PyErr_Format
#undef PyErr_Format
#endif
#define PyErr_Format(EXC, ...) ARG_PARSE_ERR_FORMAT_IMPL(__VA_ARGS__, PyTruffle_Err_Format_10, PyTruffle_Err_Format_9, PyTruffle_Err_Format_8, PyTruffle_Err_Format_7, PyTruffle_Err_Format_6, PyTruffle_Err_Format_5, PyTruffle_Err_Format_4, PyTruffle_Err_Format_3, PyTruffle_Err_Format_2, PyTruffle_Err_Format_1, PyTruffle_Err_Format_0)(EXC, __VA_ARGS__)


#endif
