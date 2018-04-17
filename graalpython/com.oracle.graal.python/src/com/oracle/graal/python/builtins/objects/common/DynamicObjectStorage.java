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
package com.oracle.graal.python.builtins.objects.common;

import java.util.ArrayList;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Layout;
import com.oracle.truffle.api.object.ObjectType;
import com.oracle.truffle.api.object.Shape;

public abstract class DynamicObjectStorage extends HashingStorage {

    public static final int SIZE_THRESHOLD = 100;

    private static final Layout LAYOUT = Layout.createLayout();
    private static final Shape EMPTY_SHAPE = LAYOUT.createShape(new ObjectType());

    private final DynamicObject store;

    private DynamicObjectStorage() {
        store = LAYOUT.newInstance(EMPTY_SHAPE);
    }

    private DynamicObjectStorage(DynamicObject store) {
        this.store = store;
    }

    @Override
    public int length() {
        return store.size();
    }

    @Override
    public boolean hasKey(Object key, Equivalence eq) {
        assert eq == HashingStorage.DEFAULT_EQIVALENCE;
        return store.containsKey(key);
    }

    @Override
    @TruffleBoundary
    public Object getItem(Object key, Equivalence eq) {
        assert eq == HashingStorage.DEFAULT_EQIVALENCE;
        return store.get(key);
    }

    @Override
    @TruffleBoundary
    public void setItem(Object key, Object value, Equivalence eq) {
        assert eq == HashingStorage.DEFAULT_EQIVALENCE;
        if (store.containsKey(key)) {
            store.set(key, value);
        } else {
            store.define(key, value);
            store.updateShape();
        }
    }

    @Override
    @TruffleBoundary
    public boolean remove(Object key, Equivalence eq) {
        assert eq == HashingStorage.DEFAULT_EQIVALENCE;
        boolean result = store.delete(key);
        store.updateShape();
        return result;
    }

    @Override
    public Iterable<Object> keys() {
        return wrapJavaIterable(store.getShape().getKeys());
    }

    @Override
    @TruffleBoundary
    public Iterable<Object> values() {
        ArrayList<Object> entries = new ArrayList<>(store.size());
        Shape shape = store.getShape();
        for (Object key : shape.getKeys()) {
            entries.add(store.get(key));
        }
        return wrapJavaIterable(entries);
    }

    @Override
    @TruffleBoundary
    public Iterable<DictEntry> entries() {
        ArrayList<DictEntry> entries = new ArrayList<>(store.size());
        Shape shape = store.getShape();
        for (Object key : shape.getKeys()) {
            entries.add(new DictEntry(key, store.get(key)));
        }
        return wrapJavaIterable(entries);
    }

    @Override
    @TruffleBoundary
    public void clear() {
        store.setShapeAndResize(store.getShape(), EMPTY_SHAPE);
        store.updateShape();
    }

    public DynamicObject getStore() {
        return store;
    }

    public static class FastDictStorage extends DynamicObjectStorage {
        public FastDictStorage() {
        }

        public FastDictStorage(DynamicObject copy) {
            super(copy);
        }

        @Override
        @TruffleBoundary
        public HashingStorage copy(Equivalence eq) {
            assert eq == HashingStorage.DEFAULT_EQIVALENCE;
            return new FastDictStorage(getStore().copy(getStore().getShape()));
        }
    }

    public static class PythonObjectDictStorage extends DynamicObjectStorage {
        public PythonObjectDictStorage(DynamicObject store) {
            super(store);
        }

        @Override
        @TruffleBoundary
        public HashingStorage copy(Equivalence eq) {
            assert eq == HashingStorage.DEFAULT_EQIVALENCE;
            return new PythonObjectDictStorage(getStore().copy(getStore().getShape()));
        }
    }

}
