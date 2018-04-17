/*
 * Copyright (c) 2017, Oracle and/or its affiliates.
 * Copyright (c) 2014, Regents of the University of California
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
package com.oracle.graal.python.builtins.objects.bytes;

import static com.oracle.graal.python.builtins.objects.bytes.BytesUtils.__repr__;
import static com.oracle.graal.python.runtime.exception.PythonErrorType.ValueError;

import java.util.Arrays;

import com.oracle.graal.python.PythonLanguage;
import com.oracle.graal.python.builtins.objects.type.PythonClass;
import com.oracle.graal.python.runtime.PythonCore;
import com.oracle.graal.python.runtime.object.PythonObjectFactory;
import com.oracle.graal.python.runtime.sequence.PImmutableSequence;
import com.oracle.graal.python.runtime.sequence.PSequence;
import com.oracle.graal.python.runtime.sequence.SequenceUtil;
import com.oracle.graal.python.runtime.sequence.storage.ByteSequenceStorage;
import com.oracle.graal.python.runtime.sequence.storage.SequenceStorage;
import com.oracle.truffle.api.CompilerDirectives;

public final class PBytes extends PImmutableSequence implements PIBytesLike {

    private final ByteSequenceStorage store;

    public PBytes(PythonClass cls, byte[] bytes) {
        super(cls);
        store = new ByteSequenceStorage(bytes);
    }

    public PBytes(PythonClass cls, ByteSequenceStorage storage) {
        super(cls);
        store = storage;
    }

    @Override
    public int len() {
        return store.length();
    }

    @Override
    public Object getItem(int idx) {
        int index = SequenceUtil.normalizeIndex(idx, store.length(), "array index out of range");
        return getItemNormalized(index);
    }

    public Object getItemNormalized(int index) {
        return store.getItemNormalized(index);
    }

    @Override
    public Object getSlice(PythonObjectFactory factory, int start, int stop, int step, int length) {
        return factory.createBytes(getPythonClass(), store.getSliceInBound(start, stop, step, length));
    }

    @Override
    public int index(Object value) {
        int index = store.index(value);

        if (index != -1) {
            return index;
        }

        CompilerDirectives.transferToInterpreter();
        throw PythonLanguage.getCore().raise(ValueError, "%s is not in bytes literal", value);
    }

    @Override
    public SequenceStorage getSequenceStorage() {
        return store;
    }

    @Override
    public boolean lessThan(PSequence sequence) {
        return false;
    }

    @Override
    public String toString() {
        return __repr__(store.getInternalByteArray());
    }

    @Override
    public final boolean equals(Object other) {
        if (!(other instanceof PSequence)) {
            return false;
        } else {
            return equals((PSequence) other);
        }
    }

    public final boolean equals(PSequence other) {
        PSequence otherSeq = other;
        SequenceStorage otherStore = otherSeq.getSequenceStorage();
        return store.equals(otherStore);
    }

    @Override
    public final int hashCode() {
        return Arrays.hashCode(store.getInternalByteArray());
    }

    public byte[] join(PythonCore core, Object... values) {
        return BytesUtils.join(core, store.getInternalByteArray(), values);
    }

    @Override
    public byte[] getInternalByteArray() {
        return store.getInternalByteArray();
    }

    @Override
    public PIBytesLike createFromBytes(PythonObjectFactory factory, byte[] bytes) {
        return factory.createBytes(bytes);
    }
}
