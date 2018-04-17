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

def make_super_class():
    import sys


    class super(object):
        def __init__(self, cls=None, obj=None):
            # https://github.com/python/cpython/blob/2102c789035ccacbac4362589402ac68baa2cd29/Objects/typeobject.c#L7612
            if cls is None:
                try:
                    super_frame = sys._getframe(1)
                except ValueError:
                    raise RuntimeError("super(): no current frame")
                obj = super_frame.__truffle_getargument__(0)
                if obj is None:
                    raise RuntimeError("super(): no arguments")
                cls = super_frame.__truffle_get_class_scope__()
                if cls is None:
                    raise RuntimeError("super(): empty __class__ cell")
                if not isinstance(cls, type):
                    raise RuntimeError("super(): __class__ is not a type (%s)" % type(cls))
            self.__type__ = cls
            self.__obj__ = obj

        def __get__(self, obj, type=None):
            if object.__getattribute__(self, "__obj__") is None and obj is not None:
                return super(object.__getattribute__(self, "__type__"), obj)
            else:
                return self

        def __getattribute__(self, attr):
            obj = object.__getattribute__(self, "__obj__")
            typ = object.__getattribute__(self, "__type__")
            if isinstance(obj, typ):
                start_type = obj.__class__
            else:
                start_type = obj
            mro = iter(start_type.__mro__)
            found_start = False
            for cls in mro:
                if cls is typ:
                    found_start = True
                elif found_start:
                    if attr in cls.__dict__:
                        x = cls.__dict__[attr]
                        if hasattr(x, "__get__"):
                            x = x.__get__(obj, cls)
                        return x
            raise AttributeError(attr)

        def __repr__(self):
            obj = object.__getattribute__(self, "__obj__")
            typ = object.__getattribute__(self, "__type__")
            return '<super: %s, %s>' % (typ, obj)


    return super


super = make_super_class()
