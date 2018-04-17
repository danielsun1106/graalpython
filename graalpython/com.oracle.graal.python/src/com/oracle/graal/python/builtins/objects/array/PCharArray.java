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
package com.oracle.graal.python.builtins.objects.array;

import com.oracle.graal.python.builtins.objects.type.PythonClass;
import com.oracle.graal.python.runtime.object.PythonObjectFactory;
import com.oracle.graal.python.runtime.sequence.SequenceUtil;

public final class PCharArray extends PArray {

    private final char[] array;

    public PCharArray(PythonClass clazz, char[] elements) {
        super(clazz);
        this.array = elements;
    }

    public char[] getSequence() {
        return array;
    }

    @Override
    public Object getItem(int idx) {
        int index = SequenceUtil.normalizeIndex(idx, array.length, "array index out of range");
        return getCharItemNormalized(index);
    }

    @Override
    public Object getItemNormalized(int idx) {
        return getCharItemNormalized(idx);
    }

    public char getCharItemNormalized(int idx) {
        return array[idx];
    }

    public void setCharItemNormalized(int idx, char value) {
        array[idx] = value;
    }

    @Override
    public PCharArray getSlice(PythonObjectFactory factory, int start, int stop, int step, int length) {
        char[] newArray = new char[length];
        if (step == 1) {
            System.arraycopy(array, start, newArray, 0, stop - start);
            return factory.createCharArray(newArray);
        }
        for (int i = start, j = 0; j < length; i += step, j++) {
            newArray[j] = array[i];
        }
        return factory.createCharArray(newArray);
    }

    @Override
    public int len() {
        return array.length;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("array('c', [");
        for (int i = 0; i < array.length - 1; i++) {
            buf.append(array[i] + " ");
        }
        buf.append(array[array.length - 1]);
        buf.append("])");
        return buf.toString();
    }
}
