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
package org.apache.maven.shared.release.util;

import java.util.Properties;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author <a href="mailto:mikhail_kolesnikov@outlook.com">Mikhail Kolesnikov</a>
 */
class MavenExpressionTest {

    static Stream<Arguments> parameters() {
        return Stream.of(
                Arguments.of("123456", "${revision}${sha1}${changelist}"),
                Arguments.of("12-34-56", "${revision}-${sha1}-${changelist}"),
                Arguments.of("12-null-56", "${revision}-${unknown}-${changelist}"));
    }

    @ParameterizedTest(name = "expected result {0} for expression {1}")
    @MethodSource("parameters")
    void testEvaluate(String expected, String expression) {
        Properties properties = new Properties();
        properties.setProperty("revision", "12");
        properties.setProperty("sha1", "34");
        properties.setProperty("changelist", "56");
        assertEquals(expected, MavenExpression.evaluate(expression, properties));
    }
}
