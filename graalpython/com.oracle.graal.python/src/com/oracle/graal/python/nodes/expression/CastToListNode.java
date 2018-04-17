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
package com.oracle.graal.python.nodes.expression;

import static com.oracle.graal.python.runtime.exception.PythonErrorType.TypeError;

import com.oracle.graal.python.builtins.PythonBuiltinClassType;
import com.oracle.graal.python.builtins.objects.list.PList;
import com.oracle.graal.python.builtins.objects.object.PythonObject;
import com.oracle.graal.python.builtins.objects.tuple.PTuple;
import com.oracle.graal.python.builtins.objects.type.PythonClass;
import com.oracle.graal.python.nodes.PNode;
import com.oracle.graal.python.nodes.attributes.GetAttributeNode;
import com.oracle.graal.python.nodes.builtins.ListNodes.ConstructListNode;
import com.oracle.graal.python.nodes.literal.BuiltinsLiteralNode;
import com.oracle.graal.python.nodes.literal.StringLiteralNode;
import com.oracle.graal.python.nodes.object.GetClassNode;
import com.oracle.graal.python.runtime.exception.PException;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

@NodeChildren({@NodeChild(value = "value", type = PNode.class)})
public abstract class CastToListNode extends PNode {

    public static CastToListNode create() {
        return CastToListNodeGen.create(null);
    }

    @Child private GetClassNode getClassNode;

    public abstract PList executeWith(Object list);

    public abstract PNode getValue();

    protected PythonClass getClass(Object value) {
        if (getClassNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getClassNode = insert(GetClassNode.create());
        }
        return getClassNode.execute(value);
    }

    public Object[] getArray(VirtualFrame frame) {
        Object result = execute(frame);
        if (result instanceof PTuple) {
            return ((PTuple) result).getArray();
        } else if (result instanceof PList) {
            return ((PList) result).getSequenceStorage().getInternalArray();
        } else {
            throw new RuntimeException("Got an unexpected result when casting to a list");
        }
    }

    @Specialization(guards = {"cannotBeOverridden(getClass(v))", "cachedLength == v.len()", "cachedLength < 32"})
    @ExplodeLoop
    protected PList starredTupleCachedLength(PTuple v,
                    @Cached("v.len()") int cachedLength) {
        Object[] array = new Object[cachedLength];
        Object[] objects = v.getArray();
        for (int i = 0; i < cachedLength; i++) {
            array[i] = objects[i];
        }
        return factory().createList(array);
    }

    @Specialization(replaces = "starredTupleCachedLength", guards = "cannotBeOverridden(getClass(v))")
    protected PList starredTuple(PTuple v) {
        return factory().createList(v.getArray().clone());
    }

    @Specialization(guards = "cannotBeOverridden(getClass(v))")
    protected PList starredList(PList v) {
        return v;
    }

    protected GetAttributeNode getList() {
        return GetAttributeNode.create(new BuiltinsLiteralNode(), new StringLiteralNode("list"));
    }

    @Specialization(rewriteOn = PException.class)
    protected PList starredIterable(PythonObject value,
                    @Cached("create()") ConstructListNode constructListNode) {
        PythonClass valueClass = getClass(value);
        return constructListNode.execute(lookupClass(PythonBuiltinClassType.PList), value, valueClass);
    }

    @Specialization
    protected PList starredGeneric(Object v) {
        throw raise(TypeError, "%s is not iterable", v);
    }
}
