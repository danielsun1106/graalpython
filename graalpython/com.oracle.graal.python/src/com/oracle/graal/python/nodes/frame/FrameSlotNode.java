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

import com.oracle.graal.python.builtins.PythonBuiltinClassType;
import com.oracle.graal.python.builtins.objects.ints.PInt;
import com.oracle.graal.python.builtins.objects.type.PythonClass;
import com.oracle.graal.python.nodes.PNode;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;

public abstract class FrameSlotNode extends PNode {
    private final ConditionProfile isPrimitiveProfile = ConditionProfile.createBinaryProfile();
    @CompilationFinal private PythonClass intClass;

    protected boolean isPrimitiveInt(PInt cls) {
        if (intClass == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            intClass = getCore().lookupType(PythonBuiltinClassType.PInt);
        }
        return isPrimitiveProfile.profile(cls.getPythonClass() == intClass);
    }

    protected final FrameSlot frameSlot;

    public FrameSlotNode(FrameSlot slot) {
        this.frameSlot = slot;
    }

    public final FrameSlot getSlot() {
        return frameSlot;
    }

    protected final void setObject(Frame frame, Object value) {
        frame.setObject(frameSlot, value);
    }

    protected final int getInteger(Frame frame) {
        return FrameUtil.getIntSafe(frame, frameSlot);
    }

    protected final long getLong(Frame frame) {
        return FrameUtil.getLongSafe(frame, frameSlot);
    }

    protected final boolean getBoolean(Frame frame) {
        return FrameUtil.getBooleanSafe(frame, frameSlot);
    }

    protected final double getDouble(Frame frame) {
        return FrameUtil.getDoubleSafe(frame, frameSlot);
    }

    protected final Object getObject(Frame frame) {
        return FrameUtil.getObjectSafe(frame, frameSlot);
    }

    @SuppressWarnings("unused")
    protected final boolean isNotIllegal(Frame frame) {
        return frameSlot.getKind() != FrameSlotKind.Illegal;
    }

    @SuppressWarnings("unused")
    protected final boolean isBooleanKind(Frame frame) {
        return isKind(FrameSlotKind.Boolean);
    }

    @SuppressWarnings("unused")
    protected final boolean isIntegerKind(Frame frame) {
        return isKind(FrameSlotKind.Int);
    }

    @SuppressWarnings("unused")
    protected final boolean isLongKind(Frame frame) {
        return isKind(FrameSlotKind.Long);
    }

    @SuppressWarnings("unused")
    protected final boolean isDoubleKind(Frame frame) {
        return isKind(FrameSlotKind.Double);
    }

    @SuppressWarnings("unused")
    protected final boolean isIntOrObjectKind(Frame frame) {
        return isKind(FrameSlotKind.Int) || isKind(FrameSlotKind.Object);
    }

    @SuppressWarnings("unused")
    protected final boolean isLongOrObjectKind(Frame frame) {
        return isKind(FrameSlotKind.Long) || isKind(FrameSlotKind.Object);
    }

    @SuppressWarnings("unused")
    protected final boolean isObjectKind(Frame frame) {
        if (frameSlot.getKind() != FrameSlotKind.Object) {
            CompilerDirectives.transferToInterpreter();
            frameSlot.setKind(FrameSlotKind.Object);
        }
        return true;
    }

    private boolean isKind(FrameSlotKind kind) {
        return frameSlot.getKind() == kind || initialSetKind(kind);
    }

    private boolean initialSetKind(FrameSlotKind kind) {
        if (frameSlot.getKind() == FrameSlotKind.Illegal) {
            CompilerDirectives.transferToInterpreter();
            frameSlot.setKind(kind);
            return true;
        }
        return false;
    }

    /**
     * To be Overridden by {@link WriteNode}s. {@link ReadNode}s should throw Unsupported Error.
     */
    public abstract Object doWrite(VirtualFrame frame, Object value);
}
