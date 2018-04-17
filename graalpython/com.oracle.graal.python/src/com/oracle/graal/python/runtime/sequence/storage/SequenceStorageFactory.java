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

import java.math.BigInteger;

import com.oracle.graal.python.PythonLanguage;
import com.oracle.graal.python.builtins.objects.list.PList;
import com.oracle.graal.python.builtins.objects.tuple.PTuple;
import com.oracle.graal.python.runtime.PythonOptions;

public class SequenceStorageFactory {

    private final boolean unboxSequenceStorage;
    private final boolean forceLongType;

    public SequenceStorageFactory() {
        unboxSequenceStorage = PythonOptions.getOption(PythonLanguage.getContext(), PythonOptions.UnboxSequenceStorage);
        forceLongType = PythonOptions.getOption(PythonLanguage.getContext(), PythonOptions.ForceLongType);
    }

    public SequenceStorage createStorage(Object[] values) {
        assert values != null;
        assert values.getClass() == Object[].class : "cannot use non-Object array for modifiable list";
        if (!unboxSequenceStorage) {
            return new ObjectSequenceStorage(values);
        }

        /**
         * Try to use unboxed SequenceStorage.
         */
        if (values.length == 0) {
            return EmptySequenceStorage.INSTANCE;
        }

        if (canSpecializeToInt(values)) {
            if (!forceLongType)
                return new IntSequenceStorage(specializeToInt(values));
            else
                return new LongSequenceStorage(specializeToLong(values));
        } else if (canSpecializeToDouble(values)) {
            return new DoubleSequenceStorage(specializeToDouble(values));
        } else if (canSpecializeToLong(values)) {
            return new LongSequenceStorage(specializeToLong(values));
        } else if (canSpecializeToBool(values)) {
            return new BoolSequenceStorage(specializeToBool(values));
        } else if (canSpecializeToList(values)) {
            return new ListSequenceStorage(specializeToList(values));
        } else if (canSpecializeToTuple(values)) {
            return new TupleSequenceStorage(specializeToTuple(values));
        } else {
            return new ObjectSequenceStorage(values);
        }
    }

    public static boolean canSpecializeToInt(Object[] values) {
        if (!(values[0] instanceof Integer)) {
            return false;
        }

        for (Object item : values) {
            if (!(item instanceof Integer)) {
                return false;
            }
        }

        return true;
    }

    public static int[] specializeToInt(Object[] values) {
        final int[] intVals = new int[values.length];

        for (int i = 0; i < values.length; i++) {
            intVals[i] = (int) values[i];
        }

        return intVals;
    }

    public static boolean canSpecializeToLong(Object[] values) {
        Object val = values[0];
        val = (val instanceof Integer) ? BigInteger.valueOf((int) val).longValue() : val;
        if (!(values[0] instanceof Long)) {
            return false;
        }

        for (Object item : values) {
            val = item;
            val = (val instanceof Integer) ? BigInteger.valueOf((int) val).longValue() : val;
            if (!(item instanceof Long)) {
                return false;
            }
        }

        return true;
    }

    public static long[] specializeToLong(Object[] values) {
        final long[] intVals = new long[values.length];
        for (int i = 0; i < values.length; i++) {
            long value = (values[i] instanceof Integer) ? BigInteger.valueOf((int) values[i]).longValue() : (long) values[i];
            intVals[i] = value;
        }

        return intVals;
    }

    public static boolean canSpecializeToDouble(Object[] values) {
        if (!(values[0] instanceof Double)) {
            return false;
        }

        for (Object item : values) {
            if (!(item instanceof Double)) {
                return false;
            }
        }

        return true;
    }

    public static double[] specializeToDouble(Object[] values) {
        final double[] doubles = new double[values.length];

        for (int i = 0; i < values.length; i++) {
            doubles[i] = (double) values[i];
        }

        return doubles;
    }

    public static boolean canSpecializeToBool(Object[] values) {
        if (!(values[0] instanceof Boolean)) {
            return false;
        }

        for (Object item : values) {
            if (!(item instanceof Boolean)) {
                return false;
            }
        }

        return true;
    }

    public static boolean[] specializeToBool(Object[] values) {
        final boolean[] bools = new boolean[values.length];

        for (int i = 0; i < values.length; i++) {
            bools[i] = (boolean) values[i];
        }

        return bools;
    }

    public static boolean canSpecializeToList(Object[] values) {
        if (!(values[0] instanceof PList)) {
            return false;
        }

        for (Object item : values) {
            if (!(item instanceof PList)) {
                return false;
            }
        }

        return true;
    }

    public static PList[] specializeToList(Object[] values) {
        final PList[] list = new PList[values.length];

        for (int i = 0; i < values.length; i++) {
            list[i] = (PList) values[i];
        }

        return list;
    }

    public static boolean canSpecializeToTuple(Object[] values) {
        if (!(values[0] instanceof PTuple)) {
            return false;
        }

        for (Object item : values) {
            if (!(item instanceof PTuple)) {
                return false;
            }
        }

        return true;
    }

    public static PTuple[] specializeToTuple(Object[] values) {
        final PTuple[] list = new PTuple[values.length];

        for (int i = 0; i < values.length; i++) {
            list[i] = (PTuple) values[i];
        }

        return list;
    }

}
