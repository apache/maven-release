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
package org.apache.maven.shared.release.policy.semver;

import org.apache.maven.shared.release.policy.version.VersionPolicy;
import org.apache.maven.shared.release.policy.version.VersionPolicyRequest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SemVerMajorDevelopmentVersionPolicyTest {
    private final VersionPolicy versionPolicy = new SemVerMajorDevelopmentVersionPolicy();

    @ParameterizedTest
    @CsvSource({
        "1.0.0, 2.0.0-SNAPSHOT",
        "1.2.3, 2.0.0-SNAPSHOT",
        "1.0.0-alpha, 1.0.0-SNAPSHOT",
        "1.0.0+build.1, 1.0.0-SNAPSHOT"
    })
    public void testConvertToSnapshot(String requested, String expected) throws Exception {
        String suggestedVersion = versionPolicy
                .getDevelopmentVersion(newVersionPolicyRequest(requested))
                .getVersion();

        assertEquals(expected, suggestedVersion);
    }

    @ParameterizedTest
    @CsvSource({
        "1.0.0-SNAPSHOT, 1.0.0",
        "1.2.3-SNAPSHOT, 1.2.3",
        "1.0.0-alpha-SNAPSHOT, 1.0.0",
        "1.0.0+build.1-SNAPSHOT, 1.0.0"
    })
    public void testConvertToRelease(String requested, String expected) throws Exception {
        String suggestedVersion = versionPolicy
                .getReleaseVersion(newVersionPolicyRequest(requested))
                .getVersion();

        assertEquals(expected, suggestedVersion);
    }

    private static VersionPolicyRequest newVersionPolicyRequest(String version) {
        return new VersionPolicyRequest().setVersion(version);
    }
}
