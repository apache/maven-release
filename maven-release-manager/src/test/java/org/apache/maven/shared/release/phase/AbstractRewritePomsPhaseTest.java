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
package org.apache.maven.shared.release.phase;

import java.util.Properties;

import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.apache.maven.shared.release.config.ReleaseUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbstractRewritePomsPhaseTest extends AbstractReleaseTestCase {

    private AbstractRewritePomsPhase phase;

    private static final String PROJECT_KEY = "mygroup:myproject";
    private static final String ARTIFACT_KEY = "mygroup:myotherproject";

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();

        phase = lookup(RewritePomVersionsPhase.class, "rewrite-pom-versions");
    }

    @Test
    public void testRewritePropertyUsedInVersionExpression() throws ReleaseFailureException {
        ReleaseResult result = new ReleaseResult();
        ReleaseDescriptor releaseDescriptor = ReleaseUtils.buildReleaseDescriptor(new ReleaseDescriptorBuilder());
        // unresolvable property (no local properties available)
        assertFalse(phase.rewritePropertyUsedInVersionExpression(
                PROJECT_KEY,
                ARTIFACT_KEY,
                "${project.version}",
                "1.1.0",
                "1.0.0",
                "project.version",
                null,
                result,
                releaseDescriptor));
        assertNotEquals(ReleaseResult.ERROR, result.getResultCode());
        // unresolvable property (only other local properties available)
        Properties properties = new Properties();
        properties.setProperty("myprop", "1.0.0");
        assertFalse(phase.rewritePropertyUsedInVersionExpression(
                PROJECT_KEY,
                ARTIFACT_KEY,
                "${unresolvableprop}",
                "1.1.0",
                "1.0.0",
                "unresolvableprop",
                properties,
                result,
                releaseDescriptor));
        // resolvable local property
        assertTrue(phase.rewritePropertyUsedInVersionExpression(
                PROJECT_KEY,
                ARTIFACT_KEY,
                "${myprop}",
                "1.1.0",
                "1.0.0",
                "myprop",
                properties,
                result,
                releaseDescriptor));
        assertNotEquals(ReleaseResult.ERROR, result.getResultCode());
        // property value already up to date
        properties.setProperty("myprop", "1.1.0");
        assertFalse(phase.rewritePropertyUsedInVersionExpression(
                PROJECT_KEY,
                ARTIFACT_KEY,
                "${myprop}",
                "1.1.0",
                "1.0.0",
                "myprop",
                properties,
                result,
                releaseDescriptor));
        assertNotEquals(ReleaseResult.ERROR, result.getResultCode());
        // property value references another read-only expression
        assertFalse(phase.rewritePropertyUsedInVersionExpression(
                PROJECT_KEY,
                ARTIFACT_KEY,
                "${myprop}",
                "${project.version}",
                "1.0.0",
                "myprop",
                properties,
                result,
                releaseDescriptor));
        assertNotEquals(ReleaseResult.ERROR, result.getResultCode());
        // property value not equal to either original nor mapped version
        assertThrows(ReleaseFailureException.class, () ->
            phase.rewritePropertyUsedInVersionExpression(
                    PROJECT_KEY,
                    ARTIFACT_KEY,
                    "${myprop}",
                    "2.0.0",
                    "1.0.0",
                    "myprop",
                    properties,
                    result,
                    releaseDescriptor));
        assertNotEquals(ReleaseResult.ERROR, result.getResultCode());
        // ci-friendly property (no local properties available)
        assertFalse(phase.rewritePropertyUsedInVersionExpression(
                PROJECT_KEY, ARTIFACT_KEY, "${sha1}", "1.1.0", "1.0.0", "sha1", null, result, releaseDescriptor));
        assertNotEquals(ReleaseResult.ERROR, result.getResultCode());
        // ci-friendly property (only other local properties available)
        assertFalse(phase.rewritePropertyUsedInVersionExpression(
                PROJECT_KEY, ARTIFACT_KEY, "${sha1}", "1.1.0", "1.0.0", "sha1", properties, result, releaseDescriptor));
        assertNotEquals(ReleaseResult.ERROR, result.getResultCode());
        // ci-friendly property (set locally as property)
        properties.setProperty("sha1", "1.0.0");
        assertTrue(phase.rewritePropertyUsedInVersionExpression(
                PROJECT_KEY, ARTIFACT_KEY, "${sha1}", "1.1.0", "1.0.0", "sha1", properties, result, releaseDescriptor));
        assertNotEquals(ReleaseResult.ERROR, result.getResultCode());
    }
}
