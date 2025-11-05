/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.shared.release.exec;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test the consumer that tees output both to a stream and into an internal buffer for later.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
class TeeConsumerTest {
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    private final TeeConsumer consumer = new TeeConsumer(new PrintStream(out), "xxx ");

    private static final String LS = System.getProperty("line.separator");

    @Test
    void testConsumeLine() {
        consumer.consumeLine("line");

        assertEquals("xxx line" + LS, out.toString(), "Check output");

        assertEquals("line" + LS, consumer.getContent(), "Check content");

        assertEquals("line" + LS, consumer.toString(), "Check toString");
    }
}
