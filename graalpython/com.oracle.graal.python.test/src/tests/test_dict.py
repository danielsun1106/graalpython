# Copyright (c) 2018, Oracle and/or its affiliates.
#
# The Universal Permissive License (UPL), Version 1.0
#
# Subject to the condition set forth below, permission is hereby granted to any
# person obtaining a copy of this software, associated documentation and/or data
# (collectively the "Software"), free of charge and under any and all copyright
# rights in the Software, and any and all patent rights owned or freely
# licensable by each licensor hereunder covering either (i) the unmodified
# Software as contributed to or provided by such licensor, or (ii) the Larger
# Works (as defined below), to deal in both
#
# (a) the Software, and
# (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
#     one is included with the Software (each a "Larger Work" to which the
#     Software is contributed by such licensors),
#
# without restriction, including without limitation the rights to copy, create
# derivative works of, display, perform, and distribute the Software and make,
# use, sell, offer for sale, import, export, have made, and have sold the
# Software and the Larger Work(s), and to sublicense the foregoing rights on
# either these or other terms.
#
# This license is subject to the following condition:
#
# The above copyright notice and either this complete permission notice or at a
# minimum a reference to the UPL must be included in all copies or substantial
# portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.

def assert_raises(err, fn, *args, **kwargs):
    raised = False
    try:
        fn(*args, **kwargs)
    except err:
        raised = True
    assert raised


def test_views():
    d = dict()
    d['a'] = 1
    d['b'] = 2
    d['c'] = 3

    assert len(d) == 3
    # assert d.keys() == {'a', 'b', 'c'}
    assert len(d.keys()) == 3, "keys view has invalid length"
    assert set(d.keys()) == {'a', 'b', 'c'}, "keys view invalid"
    assert len(d.values()) == 3, "values view has invalid length"
    assert set(d.values()) == {1, 2, 3}, "values view invalid"
    assert len(d.items()) == 3, "items view has invalid length"
    assert set(d.items()) == {('a', 1), ('b', 2), ('c', 3)}, "items view invalid"


def test_generator():
    lst = ['a', 'b', 'c']
    d = {k: 1 for k in lst}
    assert len(d) == 3
    # assert d.keys() == {'a', 'b', 'c'}
    assert set(d.values()) == {1, 1, 1}


def test_fromkeys():
    d = dict.fromkeys(['a', 'b', 'c'])
    assert len(d) == 3
    assert set(d.keys()) == {'a', 'b', 'c'}
    assert set(d.values()) == {None}

    d = dict.fromkeys(['a', 'b', 'c'], 1)
    assert len(d) == 3
    assert set(d.keys()) == {'a', 'b', 'c'}
    assert set(d.values()) == {1}

    d = dict.fromkeys(['a', 'b', 'c'], 1.0)
    assert len(d) == 3
    assert set(d.keys()) == {'a', 'b', 'c'}
    assert set(d.values()) == {1.0}

    d = dict.fromkeys(['a', 'b', 'c'], 'd')
    assert len(d) == 3
    assert set(d.keys()) == {'a', 'b', 'c'}
    assert set(d.values()) == {'d'}

    d = dict.fromkeys(['a', 'b', 'c'], None)
    assert len(d) == 3
    assert set(d.keys()) == {'a', 'b', 'c'}
    assert set(d.values()) == {None}

    o = object()
    d = dict.fromkeys(['a', 'b', 'c'], o)
    assert len(d) == 3
    assert set(d.keys()) == {'a', 'b', 'c'}
    assert set(d.values()) == {o}


def test_init():
    d = dict(a=1, b=2, c=3)
    assert len(d) == 3
    assert set(d.keys()) == {'a', 'b', 'c'}

    d = dict.fromkeys(['a', 'b', 'c'])
    assert len(d) == 3
    assert set(d.keys()) == {'a', 'b', 'c'}
    assert list(d.values()) == [None, None, None]

    d = dict.fromkeys(['a', 'b', 'c'], 1)
    assert len(d) == 3
    assert set(d.keys()) == {'a', 'b', 'c'}
    assert list(d.values()) == [1, 1, 1]

    assert_raises(TypeError, dict.fromkeys, 10)

def test_init1():
    try:
        dict([("a", 1), ("b", 2)], [("c", 3), ("d", 4)])
        assert False, "expected TypeError"
    except TypeError as e:
        assert "dict expected at most 1 arguments, got 2" == str(e), "invalid error message"


def test_init2():
    try:
        dict([("a", 1), ("b", 2), ("c", 3), ("d", 4), 5])
        assert False, "expected TypeError"
    except TypeError as e:
        assert "cannot convert dictionary update sequence element #4 to a sequence" == str(e), "invalid error message: %s" % str(e)


def test_init3():
    try:
        dict([("a", 1), ("b", 2), ("c", 3), ("d", 4), [5]])
        assert False, "expected ValueError"
    except ValueError as e:
        assert "dictionary update sequence element #4 has length 1; 2 is required" == str(e), "invalid error message"


def test_init4():
    pairs = []
    for i in range(0, 100000):
        pairs.append((str(i * 100), i * 100))
    d = dict(pairs)
    assert len(d) == 100000, "invalid length, expected 100.000 but was %d".format(len(d))

def test_init5():
    key_set = { 'a', 'b', 'c', 'd' }
    class CustomMappingObject:
        def __init__(self, keys):
            self.__keys = keys
        def keys(self):
            return self.__keys
        def __getitem__(self, key):
            if key in self.__keys:
                return ord(key)
            raise KeyError(key)
        def __len__(self):
            return len(self.keys)
    d = dict(CustomMappingObject(key_set))
    assert len(d) == 4, "invalid length, expected 4 but was %d" % len(d)
    assert set(d.keys()) == key_set, "unexpected keys: %s" % str(d.keys())
    assert set(d.values()) == { 97, 98, 99, 100 }, "unexpected values: %s" % str(d.values())


def test_custom_key_object0():
    class CollisionKey:
        def __init__(self, val):
            self.val = val
        def __hash__(self):
            return 1234
        def __eq__(self, other):
            if type(other) == type(self):
                return self.val == other.val
            return False
    key0 = CollisionKey(0)
    key1 = CollisionKey(1)
    key1eq = CollisionKey(1)
    key2 = CollisionKey(2)
    d = { key0: "hello", key1: "world"}
    assert key0 in d, "key0 should be contained"
    assert key1 in d, "key1 should be contained"
    assert key1 is not key1eq, "key1 and key1eq are not the same object"
    assert key1eq in d, "key1eq should be contained"
    assert key2 not in d, "key2 should NOT be contained"


def test_custom_key_object1():
    class MyInt(int):
        def __hash__(self):
            return MyInt(int.__hash__(self))

    class LongInt(int):
        def __hash__(self):
            return 2**32 + int.__hash__(self) - 2**32

    d = { i:r for i,r in enumerate(range(100)) }
    d[MyInt(10)] = "hello"
    d[LongInt(20)] = "world"
    assert MyInt(10) in d, "MyInt(10) should be contained"
    assert LongInt(20) in d, "LongInt(20) should be contained"
    assert d[MyInt(20)] == d[LongInt(20)], "MyInt(20) should be considered the same as LongInt(20)"
    assert d[LongInt(10)] == d[MyInt(10)], "MyInt(10) should be considered the same as LongInt(10)"


def test_mutable_key():
    def insert_unhashable(d, mutable_key):
        try:
            d[mutable_key] = "hello"
            assert False, "unhashable key must raise exception"
        except TypeError as e:
            assert "unhashable" in str(e), "invalid exception %s" % str(e)
    insert_unhashable(dict(), [1,2,3])
    insert_unhashable(dict(), {"a" : 1, "b" : 2})
    # this should work since tuples are imutable
    d = { }
    d[("a", "b")] = "hello"


def test_copy():
    d0 = dict(a=1, b=2)
    assert set(d0.keys()) == {'a', 'b'}

    d1 = d0.copy()
    assert set(d1.keys()) == {'a', 'b'}

    d1['c'] = 3
    assert set(d0.keys()) == {'a', 'b'}
    assert set(d1.keys()) == {'a', 'b', 'c'}


def test_keywords():
    def modifying(**kwargs):
        kwargs["a"] = 10
        kwargs["b"] = 20
        return kwargs
    def reading(**kwargs):
        assert kwargs["a"] == 1
        assert kwargs["b"] == 2
        res = modifying(**kwargs)
        assert kwargs["a"] == 1
        assert kwargs["b"] == 2
        return res
    res = reading(a=1, b=2)
    assert res["a"] == 10
    assert res["b"] == 20


def test_fixed_storage():
    class Foo:
        pass
    obj = Foo()
    d = obj.__dict__
    for i in range(200):
        attrName = "method" + str(i)
        d[attrName] = lambda: attrName

    for i in range(200):
        attrName = "method" + str(i)
        method_to_call = getattr(obj, attrName)
        assert method_to_call() == attrName


def test_get_default():
    d = {"a": 1}
    assert d.get("a") == 1
    assert d.get("a", 2) == 1
    assert d.get("b") == None
    assert d.get("b", 2) == 2


def test_in_dict_keys():
    d = {'a': 1, 'b': 2, 'c': 3}
    keys = d.keys()
    assert 'a' in keys
