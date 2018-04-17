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
package com.oracle.graal.python.nodes.frame;

import com.oracle.graal.python.builtins.objects.PNone;
import com.oracle.graal.python.builtins.objects.common.KeywordsStorage;
import com.oracle.graal.python.builtins.objects.function.PKeyword;
import com.oracle.graal.python.builtins.objects.ints.PInt;
import com.oracle.graal.python.nodes.PNode;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "write_local")
@NodeChild(value = "rightNode", type = PNode.class)
public abstract class WriteLocalVariableNode extends FrameSlotNode implements WriteIdentifierNode {
    public WriteLocalVariableNode(FrameSlot slot) {
        super(slot);
    }

    public static PNode create(FrameSlot slot, PNode right) {
        return WriteLocalVariableNodeGen.create(slot, right);
    }

    public abstract PNode getRightNode();

    @Override
    public PNode getRhs() {
        return getRightNode();
    }

    @Override
    public Object getIdentifier() {
        return getSlot().getIdentifier();
    }

    @Override
    public Object doWrite(VirtualFrame frame, boolean value) {
        return executeWith(frame, value);
    }

    @Override
    public Object doWrite(VirtualFrame frame, int value) {
        return executeWith(frame, value);
    }

    @Override
    public Object doWrite(VirtualFrame frame, long value) {
        return executeWith(frame, value);
    }

    @Override
    public Object doWrite(VirtualFrame frame, double value) {
        return executeWith(frame, value);
    }

    @Override
    public Object doWrite(VirtualFrame frame, Object value) {
        return executeWith(frame, value);
    }

    public abstract Object executeWith(VirtualFrame frame, boolean value);

    public abstract Object executeWith(VirtualFrame frame, int value);

    public abstract Object executeWith(VirtualFrame frame, long value);

    public abstract Object executeWith(VirtualFrame frame, double value);

    public abstract Object executeWith(VirtualFrame frame, Object value);

    @Specialization(guards = "isBooleanKind(frame)")
    public PNone write(VirtualFrame frame, PNone right) {
        frame.setObject(frameSlot, PNone.NONE);
        return right;
    }

    @Specialization(guards = "isBooleanKind(frame)")
    public boolean write(VirtualFrame frame, boolean right) {
        frame.setBoolean(frameSlot, right);
        return right;
    }

    @Specialization(guards = "isIntegerKind(frame)")
    public int write(VirtualFrame frame, int value) {
        frame.setInt(frameSlot, value);
        return value;
    }

    @Specialization(guards = {"isLongOrObjectKind(frame)", "isPrimitiveInt(value)"}, rewriteOn = ArithmeticException.class)
    public PInt writePIntAsLong(VirtualFrame frame, PInt value) {
        frame.setLong(frameSlot, value.longValueExact());
        frameSlot.setKind(FrameSlotKind.Long);
        return value;
    }

    @Specialization(guards = "isLongOrObjectKind(frame)")
    public PInt writePIntAsObject(VirtualFrame frame, PInt value) {
        frameSlot.setKind(FrameSlotKind.Object);
        frame.setObject(frameSlot, value);
        return value;
    }

    @Specialization(guards = "isDoubleKind(frame)")
    public double write(VirtualFrame frame, double right) {
        frame.setDouble(frameSlot, right);
        return right;
    }

    @Specialization(guards = "isObjectKind(frame)")
    @ExplodeLoop
    public Object write(VirtualFrame frame, PKeyword[] right) {
        frame.setObject(frameSlot, factory().createDict(KeywordsStorage.create(right)));
        return right;
    }

    @Specialization(guards = "isObjectKind(frame)")
    public Object write(VirtualFrame frame, Object right) {
        frame.setObject(frameSlot, right);
        return right;
    }
}
