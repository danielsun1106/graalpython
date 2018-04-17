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
package com.oracle.graal.python.parser;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oracle.graal.python.runtime.PythonCore;
import com.oracle.graal.python.runtime.PythonParseResult;
import com.oracle.graal.python.runtime.exception.PException;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;

public class PythonTreeTranslator extends PythonBaseTreeTranslator<PythonParseResult> {

    private PythonParseResult result;

    public PythonTreeTranslator(PythonCore core, String name, ParserRuleContext input, TranslationEnvironment environment, Source source) {
        super(core, name, environment, source);
        RootNode rootNode = null;

        try {
            Object parseResult = input.accept(this);
            if (parseResult instanceof RootNode) {
                rootNode = (RootNode) parseResult;
            } else {
                throw new RuntimeException("Unexpected parse result");
            }
        } catch (PException e) {
            throw e;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException("Failed in " + this + " with error " + t, t);
        }
        this.result = new PythonParseResult(rootNode, core);
    }

    @Override
    public PythonParseResult getTranslationResult() {
        return result;
    }
}
