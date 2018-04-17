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

import static com.oracle.graal.python.runtime.exception.PythonErrorType.KeyError;
import static com.oracle.graal.python.runtime.exception.PythonErrorType.NameError;

import com.oracle.graal.python.builtins.objects.PNone;
import com.oracle.graal.python.builtins.objects.function.PArguments;
import com.oracle.graal.python.nodes.PNode;
import com.oracle.graal.python.nodes.attributes.ReadAttributeFromObjectNode;
import com.oracle.graal.python.nodes.subscript.GetItemNode;
import com.oracle.graal.python.runtime.exception.PException;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.ConditionProfile;

@NodeInfo(shortName = "read_global")
public abstract class ReadGlobalOrBuiltinNode extends GlobalNode implements ReadNode {
    @Child private ReadAttributeFromObjectNode readFromModuleNode = ReadAttributeFromObjectNode.create();
    @Child private GetItemNode readFromDictNode = GetItemNode.create();
    @Child private ReadAttributeFromObjectNode readFromBuiltinsNode = ReadAttributeFromObjectNode.create();
    protected final String attributeId;
    protected final ConditionProfile isGlobalProfile = ConditionProfile.createBinaryProfile();
    protected final ConditionProfile isBuiltinProfile = ConditionProfile.createBinaryProfile();

    protected ReadGlobalOrBuiltinNode(String attributeId) {
        this.attributeId = attributeId;
    }

    public static ReadGlobalOrBuiltinNode create(String attributeId) {
        return ReadGlobalOrBuiltinNodeGen.create(attributeId);
    }

    @Override
    public PNode makeWriteNode(PNode rhs) {
        return WriteGlobalNode.create(attributeId, rhs);
    }

    @Specialization(guards = "isInModule(frame)")
    protected Object readGlobal(VirtualFrame frame) {
        final Object result = readFromModuleNode.execute(PArguments.getGlobals(frame), attributeId);
        return returnGlobalOrBuiltin(result);
    }

    @Specialization(guards = "isInDict(frame)", rewriteOn = PException.class)
    protected Object readGlobalDict(VirtualFrame frame) {
        final Object result = readFromDictNode.execute(PArguments.getGlobals(frame), attributeId);
        return returnGlobalOrBuiltin(result);
    }

    @Specialization(guards = "isInDict(frame)")
    protected Object readGlobalDictWithException(VirtualFrame frame,
                    @Cached("createBinaryProfile()") ConditionProfile errorProfile) {
        try {
            final Object result = readFromDictNode.execute(PArguments.getGlobals(frame), attributeId);
            return returnGlobalOrBuiltin(result);
        } catch (PException e) {
            e.expect(KeyError, getCore(), errorProfile);
            return returnGlobalOrBuiltin(PNone.NO_VALUE);
        }
    }

    public Object readGlobal(Object globals) {
        return returnGlobalOrBuiltin(globals);
    }

    private Object returnGlobalOrBuiltin(final Object result) {
        if (isGlobalProfile.profile(result != PNone.NO_VALUE)) {
            return result;
        } else {
            final Object builtin = readFromBuiltinsNode.execute(getCore().isInitialized() ? getContext().getBuiltins() : getCore().lookupBuiltinModule("builtins"), attributeId);
            if (isBuiltinProfile.profile(builtin != PNone.NO_VALUE)) {
                return builtin;
            } else {
                throw raise(NameError, "name '%s' is not defined", attributeId);
            }
        }
    }

    public String getAttributeId() {
        return attributeId;
    }
}
