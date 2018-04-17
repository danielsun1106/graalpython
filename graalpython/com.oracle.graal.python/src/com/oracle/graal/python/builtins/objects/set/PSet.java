/*
 * Copyright (c) 2017, Oracle and/or its affiliates.
 * Copyright (c) 2013, Regents of the University of California
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.graal.python.builtins.objects.set;

import com.oracle.graal.python.builtins.objects.PNone;
import com.oracle.graal.python.builtins.objects.common.HashingStorage;
import com.oracle.graal.python.builtins.objects.type.PythonClass;

public final class PSet extends PBaseSet {

    public PSet(PythonClass clazz) {
        super(clazz);
    }

    public PSet(PythonClass clazz, HashingStorage storage) {
        super(clazz, storage);
    }

    // add
    public void add(Object o) {
        this.set.setItem(o, PNone.NO_VALUE, HashingStorage.getSlowPathEquivalence(o));
    }

    // remove
    @SuppressWarnings({"unused", "static-method"})
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    // discard
    @SuppressWarnings({"unused", "static-method"})
    public boolean discard(Object o) {
        throw new UnsupportedOperationException();
    }

    // pop
    @SuppressWarnings("static-method")
    public boolean pop() {
        throw new UnsupportedOperationException();
    }

    // clear
    public void clear() {
        set.clear();
    }

    @Override
    public void setDictStorage(HashingStorage newStorage) {
        throw new RuntimeException("set has fixed storage");
    }

}
