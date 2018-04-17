/*
 * Copyright (c) 2018, Oracle and/or its affiliates.
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
package com.oracle.graal.python.builtins.objects.cell;

import java.util.ArrayList;
import java.util.List;

import com.oracle.graal.python.PythonLanguage;
import com.oracle.graal.python.builtins.PythonBuiltinClassType;
import com.oracle.graal.python.builtins.objects.PNone;
import com.oracle.graal.python.builtins.objects.object.PythonBuiltinObject;
import com.oracle.graal.python.builtins.objects.type.PythonBuiltinClass;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

public class PCell extends PythonBuiltinObject {
    private Object ref;

    public PCell() {
        super(PythonLanguage.getCore().lookupType(PythonBuiltinClassType.PCell));
    }

    public Object getRef() {
        return ref;
    }

    public void clearRef() {
        ref = null;
    }

    public void setRef(Object ref) {
        this.ref = ref;
    }

    public Object getPythonRef() {
        if (ref == null) {
            return PNone.NONE;
        }
        return ref;
    }

    @Override
    public List<String> getAttributeNames() {
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add("cell_contents");
        return arrayList;
    }

    @Override
    @TruffleBoundary
    public String toString() {
        if (ref == null) {
            return String.format("<cell at %s: empty>", this.hashCode());
        }
        PythonBuiltinClass refClass = PythonLanguage.getCore().lookupType(ref.getClass());
        return String.format("<cell at %s: %s object at %s>", this.hashCode(), refClass, ref.hashCode());
    }
}
