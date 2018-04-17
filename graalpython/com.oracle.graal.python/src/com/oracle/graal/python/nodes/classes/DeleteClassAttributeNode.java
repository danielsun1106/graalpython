/*
 * Copyright (c) 2017, Oracle and/or its affiliates.
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
package com.oracle.graal.python.nodes.classes;

import com.oracle.graal.python.builtins.objects.PNone;
import com.oracle.graal.python.builtins.objects.dict.PDict;
import com.oracle.graal.python.builtins.objects.frame.PFrame;
import com.oracle.graal.python.builtins.objects.function.PArguments;
import com.oracle.graal.python.nodes.NodeFactory;
import com.oracle.graal.python.nodes.PNode;
import com.oracle.graal.python.nodes.argument.ReadIndexedArgumentNode;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "delete_class_member")
public abstract class DeleteClassAttributeNode extends PNode {
    private final String identifier;

    @Child private PNode deleteNsItem;

    DeleteClassAttributeNode(String identifier) {
        this.identifier = identifier;

        NodeFactory factory = getNodeFactory();
        ReadIndexedArgumentNode namespace = ReadIndexedArgumentNode.create(0);

        this.deleteNsItem = factory.createDeleteItem(namespace, this.identifier);
    }

    public static PNode create(String name) {
        return DeleteClassAttributeNodeGen.create(name);
    }

    PDict getLocalsDict(VirtualFrame frame) {
        PFrame pFrame = PArguments.getPFrame(frame);
        if (pFrame != null) {
            return pFrame.getLocalsDict();
        }
        return null;
    }

    @Specialization(guards = "localsDict != null")
    Object deleteFromLocals(@SuppressWarnings("unused") VirtualFrame frame,
                    @Cached("getLocalsDict(frame)") PDict localsDict) {
        // class namespace overrides closure
        if (localsDict.hasKey(identifier)) {
            localsDict.delItem(identifier);
        }
        return PNone.NONE;
    }

    @Specialization
    Object delete(VirtualFrame frame) {
        // delete attribute actual attribute
        return deleteNsItem.execute(frame);
    }
}
