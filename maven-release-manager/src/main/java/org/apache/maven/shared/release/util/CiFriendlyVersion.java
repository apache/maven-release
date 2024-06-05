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
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.transform.jdom2.JDomProperties;

public class CiFriendlyVersion {

    /**
     * Regular expression pattern matching Maven expressions (i.e. references to Maven properties).
     * The first group selects the property name the expression refers to.
     */
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\$\\{(.+?)\\}");

    /**
     * All Maven properties allowed to be referenced in parent versions via expressions
     * @see <a href="https://maven.apache.org/maven-ci-friendly.html">CI-Friendly Versions</a>
     */
    private static final String REVISION = "revision";

    private static final String SHA1 = "sha1";
    private static final String CHANGELIST = "changelist";
    private static final Set<String> CI_FRIENDLY_PROPERTIES = new HashSet<>(Arrays.asList(REVISION, SHA1, CHANGELIST));

    private static final String SNAPSHOT = "-SNAPSHOT";

    private CiFriendlyVersion() {}

    /**
     * Extracts the Maven property name from a given expression.
     * @param expression the expression
     * @return either {@code null} if value is no expression otherwise the property referenced in the expression
     */
    public static String extractPropertyFromExpression(String expression) {
        Matcher matcher = EXPRESSION_PATTERN.matcher(expression);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1);
    }

    public static boolean isCiFriendlyVersion(String version) {
        return containsCiFriendlyProperties(extractPropertyFromExpression(version));
    }

    public static boolean containsCiFriendlyProperties(String property) {
        return CI_FRIENDLY_PROPERTIES.contains(property);
    }

    public static void rewriteVersionAndProperties(
            String version, String versionElement, JDomProperties jDomProperties, ReleaseDescriptor releaseDescriptor) {
        // try to rewrite property if CI friendly expression is used
        String ciFriendlyPropertyName = extractPropertyFromExpression(versionElement);
        if (jDomProperties != null) {
            String sha1 = resolveSha1Property(jDomProperties, releaseDescriptor);
            // assume that everybody follows the example and properties are simply chained
            //  and the changelist can only be '-SNAPSHOT'
            if (ArtifactUtils.isSnapshot(version)) {
                if (jDomProperties.containsKey(CHANGELIST)) {
                    jDomProperties.setProperty(
                            ciFriendlyPropertyName, version.replace(sha1, "").replace(SNAPSHOT, ""));
                    jDomProperties.setProperty(CHANGELIST, SNAPSHOT);
                } else {
                    jDomProperties.setProperty(ciFriendlyPropertyName, version.replace(sha1, ""));
                }
                if (jDomProperties.containsKey(SHA1)) {
                    // drop the value for the next version
                    jDomProperties.setProperty(SHA1, "");
                }
            } else {
                jDomProperties.setProperty(
                        ciFriendlyPropertyName, version.replace(sha1, "").replace(SNAPSHOT, ""));
                if (jDomProperties.containsKey(CHANGELIST)) {
                    jDomProperties.setProperty(CHANGELIST, "");
                }
                if (jDomProperties.containsKey(SHA1) && !sha1.isEmpty()) {
                    // we need this to restore the revision for the next development
                    // or release:prepare should provide sha1 after a commit
                    // or a user should provide it as an additional `arguments` in plugin configuration
                    // see maven-release-plugin/src/it/projects/prepare/ci-friendly-multi-module
                    jDomProperties.setProperty(SHA1, sha1);
                }
            }
        }
    }

    private static String resolveSha1Property(JDomProperties jDomProperties, ReleaseDescriptor releaseDescriptor) {
        String scmVersion = releaseDescriptor.getScmReleasedPomRevision();
        return System.getProperty(SHA1, scmVersion == null ? "" : scmVersion);
    }
}
