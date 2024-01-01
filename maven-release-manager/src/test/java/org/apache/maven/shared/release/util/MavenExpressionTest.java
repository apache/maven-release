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

import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * @author <a href="mailto:mikhail_kolesnikov@outlook.com">Mikhail Kolesnikov</a>
 */
@RunWith(Parameterized.class)
public class MavenExpressionTest extends TestCase {

    private final String expected;
    private final String expression;
    private final Properties properties = new Properties();

    public MavenExpressionTest(String expected, String expression) {
        this.expected = expected;
        this.expression = expression;
        properties.setProperty("revision", "12");
        properties.setProperty("sha1", "34");
        properties.setProperty("changelist", "56");
    }

    @Parameters(name = "expected result {0} for expression {1}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(
                new Object[] {"123456", "${revision}${sha1}${changelist}"},
                new Object[] {"12-34-56", "${revision}-${sha1}-${changelist}"},
                new Object[] {"12-null-56", "${revision}-${unknown}-${changelist}"});
    }

    @Test
    public void testEvaluate() {
        assertEquals(expected, MavenExpression.evaluate(expression, properties));
    }
}
