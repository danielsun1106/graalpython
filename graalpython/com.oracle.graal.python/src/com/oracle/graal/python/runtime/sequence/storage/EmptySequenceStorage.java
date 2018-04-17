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
package com.oracle.graal.python.runtime.sequence.storage;

import static com.oracle.graal.python.runtime.exception.PythonErrorType.ValueError;

import com.oracle.graal.python.PythonLanguage;
import com.oracle.graal.python.builtins.objects.list.PList;
import com.oracle.graal.python.builtins.objects.tuple.PTuple;
import com.oracle.graal.python.runtime.PythonOptions;
import com.oracle.truffle.api.CompilerDirectives;

public final class EmptySequenceStorage extends SequenceStorage {

    public static final EmptySequenceStorage INSTANCE = new EmptySequenceStorage();

    @Override
    public SequenceStorage generalizeFor(Object value) {
        final SequenceStorage generalized;

        if (value instanceof Integer) {
            if (!PythonOptions.getOption(PythonLanguage.getContext(), PythonOptions.ForceLongType)) {
                generalized = new IntSequenceStorage();
            } else {
                generalized = new LongSequenceStorage();
            }
        } else if (value instanceof Long) {
            generalized = new LongSequenceStorage();
        } else if (value instanceof Double) {
            generalized = new DoubleSequenceStorage();
        } else if (value instanceof PList) {
            generalized = new ListSequenceStorage(((PList) value).getSequenceStorage());
        } else if (value instanceof PTuple) {
            generalized = new TupleSequenceStorage();
        } else {
            generalized = new ObjectSequenceStorage(new Object[0]);
        }

        logGeneralization(generalized);

        return generalized;
    }

    @Override
    public Object getIndicativeValue() {
        return null;
    }

    @Override
    public int length() {
        return 0;
    }

    @Override
    public int index(Object value) {
        return -1;
    }

    @Override
    public SequenceStorage copy() {
        return this;
    }

    @Override
    public SequenceStorage createEmpty(int newCapacity) {
        return this;
    }

    @Override
    public Object[] getInternalArray() {
        return new Object[]{};
    }

    @Override
    public Object[] getCopyOfInternalArray() {
        return getInternalArray();
    }

    @Override
    public Object getItemNormalized(int idx) {
        CompilerDirectives.transferToInterpreter();
        throw PythonLanguage.getCore().raise(ValueError, "list index out of range");
    }

    @Override
    public void setItemNormalized(int idx, Object value) throws SequenceStoreException {
        CompilerDirectives.transferToInterpreter();
        throw PythonLanguage.getCore().raise(ValueError, "list assignment index out of range");
    }

    @Override
    public void insertItem(int idx, Object value) throws SequenceStoreException {
        assert idx == 0;
        throw SequenceStoreException.INSTANCE;
    }

    @Override
    public SequenceStorage getSliceInBound(int start, int stop, int step, int length) {
        assert start == stop && stop == 0;
        return this;
    }

    @Override
    public void setSliceInBound(int start, int stop, int step, SequenceStorage sequence) throws SequenceStoreException {
        throw SequenceStoreException.INSTANCE;
    }

    @Override
    public void delSlice(int start, int stop) {
        // the slice is empty. Do nothing here
    }

    @Override
    public void delItemInBound(int idx) {
        throw new UnsupportedOperationException("Cannot delete from empty storage");
    }

    @Override
    public Object popInBound(int idx) {
        return new UnsupportedOperationException();
    }

    @Override
    public void append(Object value) throws SequenceStoreException {
        throw SequenceStoreException.INSTANCE;
    }

    @Override
    public void extend(SequenceStorage other) throws SequenceStoreException {
        // allow an empty storage to be extended by an empty storage
        if (!(other instanceof EmptySequenceStorage)) {
            throw SequenceStoreException.INSTANCE;
        }
    }

    @Override
    public void reverse() {
    }

    @Override
    public void sort() {
    }

    @Override
    public boolean equals(SequenceStorage other) {
        return other == EmptySequenceStorage.INSTANCE;
    }

}
