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
package org.jpeek.web;

import com.jcabi.dynamo.Attributes;
import com.jcabi.dynamo.QueryValve;
import com.jcabi.dynamo.Table;
import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.cactoos.iterable.Filtered;
import org.cactoos.iterable.Mapped;
import org.jpeek.Version;
import org.xembly.Directive;
import org.xembly.Directives;

/**
 * Futures for {@link AsyncReports}.
 *
 * <p>There is no thread-safety guarantee.
 *
 * @author Yegor Bugayenko (yegor256@gmail.com)
 * @version $Id$
 * @since 0.8
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class Results {

    /**
     * DynamoDB table.
     */
    private final Table table;

    /**
     * Ctor.
     */
    Results() {
        this(new Dynamo().table("jpeek-results"));
    }

    /**
     * Ctor.
     * @param tbl Table
     */
    Results(final Table tbl) {
        this.table = tbl;
    }

    /**
     * Add result.
     * @param artifact The artifact, like "org.jpeek:jpeek"
     * @param dir Directory with files
     * @throws IOException If fails
     */
    public void add(final String artifact, final Path dir)
        throws IOException {
        final XML index = new XMLDocument(
            dir.resolve("index.xml").toFile()
        );
        this.table.put(
            new Attributes()
                .with("good", "true")
                .with("artifact", artifact)
                .with(
                    "score",
                    new DyNum(index.xpath("/index/@score").get(0)).longValue()
                )
                .with(
                    "diff",
                    new DyNum(index.xpath("/index/@diff").get(0)).longValue()
                )
                .with(
                    "defects",
                    new DyNum(index.xpath("/index/@defects").get(0)).longValue()
                )
                .with(
                    "classes",
                    Integer.parseInt(
                        index.xpath("/index/metric[1]/classes/text()").get(0)
                    )
                )
                .with("version", new Version().value())
                .with("added", System.currentTimeMillis())
                .with(
                    "ttl",
                    System.currentTimeMillis() / TimeUnit.SECONDS.toMillis(1L)
                        // @checkstyle MagicNumber (1 line)
                        + TimeUnit.DAYS.toSeconds(100L)
                )
        );
    }

    /**
     * Recent artifacts..
     * @return List of them
     */
    public Iterable<Iterable<Directive>> recent() {
        return new Mapped<>(
            item -> {
                final String[] parts = item.get("artifact").getS().split(":");
                return new Directives()
                    .add("repo")
                    .add("group").set(parts[0]).up()
                    .add("artifact").set(parts[1]).up()
                    .add("defects")
                    .set(new DyNum(item, "defects").doubleValue())
                    .up()
                    .add("classes")
                    .set(Integer.parseInt(item.get("classes").getN()))
                    .up()
                    .up();
            },
            this.table.frame()
                .where("good", "true")
                .through(
                    new QueryValve()
                        .withScanIndexForward(false)
                        .withIndexName("recent")
                        .withConsistentRead(false)
                        // @checkstyle MagicNumber (1 line)
                        .withLimit(50)
                        .withAttributesToGet("artifact", "classes", "defects")
                )
        );
    }

    /**
     * Best artifacts.
     * @return List of them
     * @throws IOException If fails
     */
    public Iterable<Iterable<Directive>> best() throws IOException {
        return new Mapped<>(
            item -> {
                final String[] parts = item.get("artifact").getS().split(":");
                return new Directives()
                    .add("repo")
                    .add("group").set(parts[0]).up()
                    .add("artifact").set(parts[1]).up()
                    .add("score")
                    .set(new DyNum(item, "score").doubleValue())
                    .up()
                    .add("diff")
                    .set(new DyNum(item, "diff").doubleValue())
                    .up()
                    .add("defects")
                    .set(new DyNum(item, "defects").doubleValue())
                    .up()
                    .add("classes")
                    .set(Integer.parseInt(item.get("classes").getN()))
                    .up()
                    .up();
            },
            new Filtered<>(
                // @checkstyle MagicNumber (1 line)
                item -> Long.parseLong(item.get("classes").getN()) >= 100L,
                this.table.frame()
                    .where("version", new Version().value())
                    .through(
                        new QueryValve()
                            .withScanIndexForward(false)
                            .withIndexName("scores")
                            .withConsistentRead(false)
                            // @checkstyle MagicNumber (1 line)
                            .withLimit(40)
                            .withAttributesToGet(
                                "artifact", "score", "diff", "defects",
                                "classes"
                            )
                    )
            )
        );
    }

}
