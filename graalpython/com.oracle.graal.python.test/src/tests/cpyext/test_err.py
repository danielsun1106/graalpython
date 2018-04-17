# Copyright (c) 2018, Oracle and/or its affiliates.
#
# All rights reserved.
import sys
import warnings
from . import CPyExtTestCase, CPyExtFunction, CPyExtFunctionOutVars, CPyExtFunctionVoid, unhandled_error_compare, GRAALPYTHON
__dir__ = __file__.rpartition("/")[0]


def _reference_setstring(args):
    raise args[0](args[1])


def _reference_setnone(args):
    raise args[0]()


def _reference_format(args):
    raise args[0](args[1] % args[2:])


def _is_exception_class(exc):
    return isinstance(exc, type) and issubclass(exc, BaseException)


def _reference_givenexceptionmatches(args):
    err = args[0]
    exc = args[1]
    if isinstance(exc, tuple):
        for e in exc:
            if _reference_givenexceptionmatches((err, e)):
                return 1
        return 0
    if isinstance(err, BaseException):
        err = type(err)
    if _is_exception_class(err) and _is_exception_class(exc):
        return issubclass(err, exc)
    return exc is err


def _reference_nomemory(args):
    raise MemoryError


class Dummy:
    pass


class TestPyNumber(CPyExtTestCase):
    def compile_module(self, name):
        type(self).mro()[1].__dict__["test_%s" % name].create_module(name)
        super(TestPyNumber, self).compile_module(name)


    test_PyErr_SetString = CPyExtFunctionVoid(
        _reference_setstring,
        lambda: (
            (ValueError, "hello"),
            (TypeError, "world"),
            (KeyError, "key"),
        ),
        resultspec="O",
        argspec='Os',
        arguments=["PyObject* v", "char* msg"],
        resultval="NULL",
        cmpfunc=unhandled_error_compare
    )

    test_PyErr_SetObject = CPyExtFunctionVoid(
        _reference_setstring,
        lambda: (
            (ValueError, "hello"),
            (TypeError, "world"),
            (KeyError, "key"),
        ),
        resultspec="O",
        argspec='OO',
        arguments=["PyObject* v", "PyObject* msg"],
        resultval="NULL",
        cmpfunc=unhandled_error_compare
    )

    test_PyErr_SetNone = CPyExtFunctionVoid(
        _reference_setnone,
        lambda: (
            (ValueError, ),
            (TypeError, ),
            (KeyError, ),
        ),
        resultspec="O",
        argspec='O',
        arguments=["PyObject* v"],
        resultval="NULL",
        cmpfunc=unhandled_error_compare
    )

    test_PyErr_Format = CPyExtFunctionVoid(
        _reference_format,
        lambda: (
            (ValueError, "hello %s %s", "", ""),
            (TypeError, "world %s %s", "", ""),
            (KeyError, "key %s %s", "", ""),
            (KeyError, "unknown key: %s %s", "some_key", ""),
        ),
        resultspec="O",
        argspec='OsOO',
        arguments=["PyObject* v", "char* msg", "PyObject* arg0", "PyObject* arg1"],
        resultval="NULL",
        cmpfunc=unhandled_error_compare
    )

    test_PyErr_PrintEx = CPyExtFunction(
        lambda args: None,
        lambda: (
            (True,),
        ),
        code="""PyObject* wrap_PyErr_PrintEx(int n) {
            PyErr_SetString(PyExc_KeyError, "unknown key whatsoever");
            PyErr_PrintEx(n);
            return Py_None;
        }
        """,
        resultspec="O",
        argspec='i',
        arguments=["int n"],
        callfunction="wrap_PyErr_PrintEx",
        cmpfunc=unhandled_error_compare
    )

    test_PyErr_GivenExceptionMatches = CPyExtFunction(
        _reference_givenexceptionmatches,
        lambda: (
            (ValueError(), ValueError),
            (ValueError(), BaseException),
            (ValueError(), KeyError),
            (ValueError(), (KeyError, SystemError, OverflowError)),
            (ValueError(), Dummy),
            (ValueError(), Dummy()),
            (Dummy(), Dummy()),
            (Dummy(), Dummy),
            (Dummy(), KeyError),
        ),
        resultspec="i",
        argspec='OO',
        arguments=["PyObject* err", "PyObject* exc"],
        cmpfunc=unhandled_error_compare
    )
    
    test_PyErr_Occurred = CPyExtFunction(
        lambda args: args[0] if _is_exception_class(args[0]) else SystemError,
        lambda: (
            (ValueError, "hello"),
            (KeyError, "world"),
            (ValueError, Dummy()),
            (Dummy, ""),
        ),
        code="""PyObject* wrap_PyErr_Occurred(PyObject* exc, PyObject* msg) {
            PyObject* result;
            PyErr_SetObject(exc, msg);
            result = PyErr_Occurred();
            PyErr_Clear();
            return result;
        }
        """,
        resultspec="O",
        argspec='OO',
        arguments=["PyObject* err", "PyObject* msg"],
        callfunction="wrap_PyErr_Occurred",
        cmpfunc=unhandled_error_compare
    )
    
    test_PyErr_ExceptionMatches = CPyExtFunction(
        _reference_givenexceptionmatches,
        lambda: (
            (ValueError, ValueError),
            (ValueError, BaseException),
            (ValueError, KeyError),
            (ValueError, (KeyError, SystemError, OverflowError)),
            (ValueError, Dummy),
            (ValueError, Dummy()),
        ),
        code="""int wrap_PyErr_ExceptionMatches(PyObject* err, PyObject* exc) {
            int res;
            PyErr_SetNone(err);
            res = PyErr_ExceptionMatches(exc);
            PyErr_Clear();
            return res;
        }
        """,
        resultspec="i",
        argspec='OO',
        arguments=["PyObject* err", "PyObject* exc"],
        callfunction="wrap_PyErr_ExceptionMatches",
        cmpfunc=unhandled_error_compare
    )
    
    test_PyErr_WarnEx = CPyExtFunctionVoid(
        lambda args: warnings.warn(args[1], args[0], args[2]),
        lambda: (
            (UserWarning, "custom warning", 1),
        ),
        resultspec="O",
        argspec='Osn',
        arguments=["PyObject* category", "char* msg", "Py_ssize_t level"],
        cmpfunc=unhandled_error_compare
    )

    test_PyErr_NoMemory = CPyExtFunctionVoid(
        _reference_nomemory,
        lambda: (
            tuple(),
        ),
        resultspec="O",
        argspec='',
        argumentnames="",
        arguments=["PyObject* dummy"],
        resultval="NULL",
        cmpfunc=unhandled_error_compare
    )

    test_PyErr_WriteUnraisable = CPyExtFunctionVoid(
        lambda args: None,
        lambda: (
            ("hello",),
        ),
        resultspec="O",
        argspec='O',
        arguments=["PyObject* obj"],
        cmpfunc=unhandled_error_compare
    )
