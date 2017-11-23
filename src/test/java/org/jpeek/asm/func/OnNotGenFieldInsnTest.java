/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Yegor Bugayenko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.jpeek.asm.func;

import java.util.ArrayList;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * Test cases for {@link OnNotGenFieldInsn} function.
 *
 * @author Sergey Karazhenets (sergeykarazhenets@gmail.com)
 * @version $Id$
 * @since 0.13
 * @checkstyle JavadocMethodCheck (500 lines)
 */
public final class OnNotGenFieldInsnTest {

    @Test
    public void doNotReactOnGeneratedFieldInsn() throws Exception {
        final List<Object> insns = new ArrayList<>(0);
        MatcherAssert.assertThat(insns, Matchers.empty());
        new OnNotGenFieldInsn(
            (opcode, owner, name, desc) -> insns.add(name)
        ).exec(
            // @checkstyle MagicNumberCheck (1 line)
            180, "ClassUsesNotOwnAttr$InnerKeyProvider",
            "this$0", "LClassUsesNotOwnAttr;"
        );
        MatcherAssert.assertThat(insns, Matchers.empty());
    }

    @Test
    public void reactOnNotGeneratedField() throws Exception {
        final String insn = "value";
        final List<Object> insns = new ArrayList<>(0);
        MatcherAssert.assertThat(insns, Matchers.empty());
        new OnNotGenFieldInsn(
            (opcode, owner, name, desc) -> insns.add(name)
        // @checkstyle MagicNumberCheck (1 line)
        ).exec(180, "org/jpeek/metrics/Summary", insn, "D");
        MatcherAssert.assertThat(insns, Matchers.hasSize(1));
        MatcherAssert.assertThat(insns, Matchers.hasItem(insn));
    }
}
