# Copyright (c) 2018, Oracle and/or its affiliates.
# Copyright (c) 2013-2016, Regents of the University of California
#
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without modification, are
# permitted provided that the following conditions are met:
#
# 1. Redistributions of source code must retain the above copyright notice, this list of
# conditions and the following disclaimer.
# 2. Redistributions in binary form must reproduce the above copyright notice, this list of
# conditions and the following disclaimer in the documentation and/or other materials provided
# with the distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
# OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
# MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
# COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
# EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
# GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
# AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
# NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
# OF THE POSSIBILITY OF SUCH DAMAGE.

def test_add_long_overflow():
    # max long value is written as long primitive
    val = 0x7fffffffffffffff
    assert val + 128 == 0x800000000000007f


def test_mul_long_overflow():
    # max long value is written as long primitive
    val = 0x7fffffffffffffff
    assert val * 2 == 0xfffffffffffffffe


def test_add_bool():
    assert True.__add__(True) == 2, "True.__add__(True)"
    assert True.__add__(False) == 1, "True.__add__(False)"
    assert False.__add__(True) == 1, "False.__add__(True)"
    assert False.__add__(False) == 0, "False.__add__(False)"
    assert False.__add__(0) == 0, "False.__add__(0)"
    assert True.__add__(0) == 1, "True.__add__(0)"
    assert False.__add__(1) == 1, "False.__add__(1)"
    assert True.__add__(1) == 2, "True.__add__(1)"
    assert False.__add__(0x7fffffff) == 0x7fffffff, "False.__add__(0x7fffffff)"
    assert True.__add__(0x7fffffff) == 0x80000000, "True.__add__(0x7fffffff)"
    assert False.__add__(0x7fffffffffffffff) == 0x7fffffffffffffff, "False.__add__(0x7fffffffffffffff)"
    assert True.__add__(0x7fffffffffffffff) == 0x8000000000000000, "True.__add__(0x7fffffffffffffff)"
    assert True.__add__(0.0) == NotImplemented, "True.__add__(0.0)"


def test_mul_bool():
    assert False.__mul__(False) == 0, "False.__mul__(False)"
    assert False.__mul__(True) == 0, "False.__mul__(True)"
    assert False.__mul__(1) == 0, "False.__mul__(1)"
    assert False.__mul__(0) == 0, "False.__mul__(0)"
    assert True.__mul__(0) == 0, "True.__mul__(0)"
    assert True.__mul__(1) == 1, "True.__mul__(1)"
    assert True.__mul__(0x7fffffff) == 0x7fffffff, "True.__mul__(0x7fffffff)"
    assert True.__mul__(0x7fffffffffffffff) == 0x7fffffffffffffff, "True.__mul__(0x7fffffffffffffff)"
    assert True.__mul__(0.0) == NotImplemented, "True.__mul__(0.0)"
    assert True * 1.0 == 1.0, "True * 1.0"
    assert False * 1.0 == 0.0, "False * 0.0"


def expectError(callable):
    try:
        callable()
        return False
    except TypeError as e:
        return True


def test_bin_comparison():
    class A:
        pass
    class B:
        def __gt__(self, other):
            return True
        def __ge__(self, other):
            return True
        def __lt__(self, other):
            return True
        def __le__(self, other):
            return True
    a = A()
    b = B()
    assert a > b, "Comparison 'a > b' failed"
    assert a >= b, "Comparison 'a >= b' failed"
    assert a < b, "Comparison 'a < b' failed"
    assert a <= b, "Comparison 'a <= b' failed"
    assert b > a, "Comparison 'b > a' failed"
    assert b >= a, "Comparison 'b >= a' failed"
    assert b < a, "Comparison 'b < a' failed"
    assert b <= a, "Comparison 'b <= a' failed"


def test_bin_comparison_wo_eq_ne():
    class A():
        def __eq__(self, o):
            return NotImplemented
    assert A() != A()
    a = A()
    assert a == a
    try:
        assert a <= a
    except TypeError:
        pass
    else:
        assert False
        
        
def test_floor_div():
    assert True // True == True
    assert True // 2 == 0
    assert True // 2.0 == 0.0
    assert 0 // True == 0
    assert 3 // 2 == 1
    assert 15 // 5 == 3
    assert 0 // 1 == 0
    assert 15.5 // True == 15.0
    assert 15.5 // 5 == 3.0
    assert 16.5 // 5.5 == 3.0
    assert 16.5 // 3.2 == 5.0
    assert_exception(lambda: True // False, ZeroDivisionError)
    assert_exception(lambda: True // 0, ZeroDivisionError)
    assert_exception(lambda: True // 0.0, ZeroDivisionError)
    assert_exception(lambda: 3 // False, ZeroDivisionError)
    assert_exception(lambda: 3 // 0, ZeroDivisionError)
    assert_exception(lambda: 3 // 0.0, ZeroDivisionError)
    assert_exception(lambda: 3.0 // False, ZeroDivisionError)
    assert_exception(lambda: 5.4 // 0, ZeroDivisionError)
    assert_exception(lambda: 5.4 // 0.0, ZeroDivisionError)


def test_subclass_ordered_binop():
    class A(int):
        def __add__(self, other):
            return 0xa

    class B(A):
        def __add__(self, other):
            return 0xb

    class C(B):
        __radd__ = B.__add__

    assert A(1) + A(1) == 0xa
    assert 1 + A(10) == 11
    assert A(10) + 1 == 0xa

    # TODO: we're doing this wrong right now
    # assert A(1) + B(1) == 0xa
    assert B(1) + A(2) == 0xb

    assert A(1) + C(3) == 0xb
    assert C(3) + A(1) == 0xb
    
    
def assert_exception(op, ex_type):
    try:
        op()
        assert False, "expected exception %s" % ex_type
    except BaseException as e:
        if type(e) == AssertionError:
            raise e
        else:
            assert type(e) == ex_type, "expected exception %s but got %s" % (ex_type, type(e))
