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
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CiFriendlyVersion {
    private static final Logger LOGGER = LoggerFactory.getLogger(CiFriendlyVersion.class);

    /**
     * All Maven properties allowed to be referenced in parent versions via expressions
     * @see <a href="https://maven.apache.org/maven-ci-friendly.html">CI-Friendly Versions</a>
     */
    public static final String REVISION = "revision";

    public static final String SHA1 = "sha1";
    public static final String CHANGELIST = "changelist";

    private static final Set<String> CI_FRIENDLY_PROPERTIES = new HashSet<>(Arrays.asList(REVISION, SHA1, CHANGELIST));

    private static final String SNAPSHOT = "-SNAPSHOT";

    private CiFriendlyVersion() {}

    public static boolean isCiFriendlyVersion(String version) {
        if (StringUtils.isEmpty(version)) {
            return false;
        }
        return isCiFriendlyProperty(MavenExpression.extractPropertyFromExpression(version));
    }

    public static boolean isCiFriendlyProperty(String property) {
        return CI_FRIENDLY_PROPERTIES.contains(property);
    }

    public static void rewriteVersionAndProperties(
            String version, Properties properties, ReleaseDescriptor releaseDescriptor) {
        // try to rewrite property if CI friendly expression is used
        if (properties != null) {
            String sha1 = resolveSha1Property(properties, releaseDescriptor);
            // assume that everybody follows the example and properties are simply chained
            //  and the changelist can only be '-SNAPSHOT'
            if (ArtifactUtils.isSnapshot(version)) {
                if (properties.containsKey(CHANGELIST)) {
                    String revision = version.replace(sha1, "").replace(SNAPSHOT, "");
                    setAndLogPropertyChange(properties, REVISION, revision);
                    setAndLogPropertyChange(properties, CHANGELIST, SNAPSHOT);
                } else {
                    String revision = version.replace(sha1, "");
                    setAndLogPropertyChange(properties, REVISION, revision);
                }
                if (properties.containsKey(SHA1)) {
                    // drop the value for the next version
                    setAndLogPropertyChange(properties, SHA1, "");
                }
            } else {
                properties.setProperty(REVISION, version.replace(sha1, ""));
                if (properties.containsKey(CHANGELIST)) {
                    setAndLogPropertyChange(properties, CHANGELIST, "");
                }
                if (properties.containsKey(SHA1) && !sha1.isEmpty()) {
                    // we need this to restore the revision for the next development
                    // or release:prepare should provide sha1 after a commit
                    // or a user should provide it as an additional `arguments` in plugin configuration
                    // see maven-release-plugin/src/it/projects/prepare/ci-friendly-multi-module
                    setAndLogPropertyChange(properties, SHA1, sha1);
                }
            }
        }
    }

    private static void setAndLogPropertyChange(Properties properties, String key, String value) {
        LOGGER.info("Updating {} property to {}", key, value);
        properties.setProperty(key, value);
    }

    public static String resolveSha1Property(Properties properties, ReleaseDescriptor releaseDescriptor) {
        String sha1 = properties.getProperty(SHA1);
        String scmVersion = releaseDescriptor.getScmReleasedPomRevision();
        String systemSha1 = System.getProperty(SHA1);
        String result = StringUtils.isNotEmpty(systemSha1)
                ? systemSha1
                : StringUtils.isNotEmpty(sha1) ? sha1 : scmVersion != null ? scmVersion : "";
        LOGGER.info("Resolved SHA1 property value {}", result);
        return result;
    }
}
