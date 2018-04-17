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
package com.oracle.graal.python.nodes.argument;

import java.util.Arrays;

import com.oracle.graal.python.builtins.objects.function.PArguments;
import com.oracle.graal.python.builtins.objects.function.PKeyword;
import com.oracle.graal.python.nodes.PNode;
import com.oracle.graal.python.runtime.PythonOptions;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

public abstract class ReadVarKeywordsNode extends PNode {
    @CompilationFinal(dimensions = 1) private final String[] keywordNames;
    private final boolean doWrap;

    public static ReadVarKeywordsNode create(String[] keywordNames) {
        return ReadVarKeywordsNodeGen.create(keywordNames, false);
    }

    public static ReadVarKeywordsNode createForUserFunction(String[] names) {
        return ReadVarKeywordsNodeGen.create(names, true);
    }

    ReadVarKeywordsNode(String[] keywordNames, boolean doWrap) {
        this.keywordNames = keywordNames;
        this.doWrap = doWrap;
    }

    protected int getLimit() {
        return Math.max(keywordNames.length, 5);
    }

    protected int getAndCheckKwargLen(VirtualFrame frame) {
        int length = getKwargLen(frame);
        if (length >= PythonOptions.getIntOption(getContext(), PythonOptions.VariableArgumentReadUnrollingLimit)) {
            return -1;
        }
        return length;
    }

    protected static int getKwargLen(VirtualFrame frame) {
        return PArguments.getKeywordArguments(frame).length;
    }

    private Object returnValue(PKeyword[] keywords) {
        if (doWrap) {
            return factory().createDict(keywords);
        } else {
            return keywords;
        }
    }

    @Specialization(guards = {"getKwargLen(frame) == cachedLen"}, limit = "getLimit()")
    @ExplodeLoop
    Object extractKwargs(VirtualFrame frame,
                    @Cached("getAndCheckKwargLen(frame)") int cachedLen) {
        PKeyword[] keywordArguments = PArguments.getKeywordArguments(frame);
        PKeyword[] remArguments = new PKeyword[keywordArguments.length];
        CompilerAsserts.compilationConstant(keywordNames.length);
        int i = 0;
        for (int j = 0; j < cachedLen; j++) {
            PKeyword keyword = keywordArguments[j];
            String kwName = keyword.getName();
            boolean kwFound = false;
            for (String name : keywordNames) {
                if (kwName.equals(name)) {
                    // Note: rather than skipping the rest of the loop,
                    // to properly explode the loop in this case, we want
                    // constant iteration count
                    kwFound = true;
                }
            }
            if (!kwFound) {
                remArguments[i] = keyword;
                i++;
            }
        }
        return returnValue(Arrays.copyOf(remArguments, i));
    }

    @Specialization(replaces = "extractKwargs")
    Object extractVariableKwargs(VirtualFrame frame) {
        PKeyword[] keywordArguments = PArguments.getKeywordArguments(frame);
        PKeyword[] remArguments = new PKeyword[keywordArguments.length];
        int i = 0;
        outer: for (PKeyword keyword : keywordArguments) {
            String kwName = keyword.getName();
            for (String name : keywordNames) {
                if (kwName.equals(name)) {
                    continue outer;
                }
            }
            remArguments[i] = keyword;
            i++;
        }
        return returnValue(Arrays.copyOf(remArguments, i));
    }
}
