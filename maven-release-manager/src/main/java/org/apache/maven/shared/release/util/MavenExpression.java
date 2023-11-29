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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:mikhail_kolesnikov@outlook.com">Mikhail Kolesnikov</a>
 */
public class MavenExpression {
    public static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\$\\{(.+?)\\}");

    private MavenExpression() {}

    public static String evaluate(String expression, Properties properties) {
        StringBuilder result = new StringBuilder(expression);
        Matcher matcher = EXPRESSION_PATTERN.matcher(result);
        while (matcher.find()) {
            String propertyName = matcher.group(1);
            String propertyValue = properties.getProperty(propertyName);
            result.replace(matcher.start(), matcher.end(), String.valueOf(propertyValue));
            matcher.reset();
        }
        return result.toString();
    }
}
