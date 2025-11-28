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
package org.apache.maven.shared.release.config;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Scm;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder.BuilderReleaseDescriptor;
import org.apache.maven.shared.release.phase.AbstractReleaseTestCase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ReleaseDescriptor Tester.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
class ReleaseUtilsTest {
    @Test
    void testEquals() throws IOException {
        ReleaseDescriptorBuilder originalReleaseDescriptor = createReleaseDescriptor();
        ReleaseDescriptorBuilder builder = copyReleaseDescriptor(originalReleaseDescriptor.build());
        doEqualsAssertions(builder, originalReleaseDescriptor, "other", new File("target/test-working-directory"));
        originalReleaseDescriptor = createReleaseDescriptor();
        builder = copyReleaseDescriptor(originalReleaseDescriptor.build());
        doEqualsAssertions(originalReleaseDescriptor, builder, "other", new File("target/test-working-directory"));

        originalReleaseDescriptor = createReleaseDescriptor();
        builder = copyReleaseDescriptor(originalReleaseDescriptor.build());
        doEqualsAssertions(builder, originalReleaseDescriptor, null, null);
        originalReleaseDescriptor = createReleaseDescriptor();
        builder = copyReleaseDescriptor(originalReleaseDescriptor.build());
        doEqualsAssertions(originalReleaseDescriptor, builder, null, null);

        assertEquals(builder, builder, "test ==");
        Object obj = this;
        assertNotEquals(builder, obj, "test class instance");
    }

    private static void doEqualsAssertions(
            ReleaseDescriptorBuilder releaseDescriptor,
            ReleaseDescriptorBuilder originalReleaseDescriptor,
            String other,
            File otherFile)
            throws IOException {
        BuilderReleaseDescriptor origConfig = originalReleaseDescriptor.build();
        ReleaseDescriptorBuilder configBuilder = releaseDescriptor;
        assertEquals(configBuilder.build(), origConfig, "Check original comparison");

        configBuilder.setScmSourceUrl(other);
        assertNotEquals(configBuilder.build(), origConfig, "Check original comparison");
        configBuilder.setScmSourceUrl(origConfig.getScmSourceUrl());

        configBuilder.setAdditionalArguments(other);
        assertNotEquals(configBuilder.build(), origConfig, "Check original comparison");
        configBuilder.setAdditionalArguments(origConfig.getAdditionalArguments());

        configBuilder.setAddSchema(!origConfig.isAddSchema());
        assertNotEquals(configBuilder.build(), origConfig, "Check original comparison");
        configBuilder.setAddSchema(origConfig.isAddSchema());

        configBuilder.setGenerateReleasePoms(!origConfig.isAddSchema());
        assertNotEquals(configBuilder.build(), origConfig, "Check original comparison");
        configBuilder.setGenerateReleasePoms(origConfig.isGenerateReleasePoms());

        configBuilder.setScmUseEditMode(!origConfig.isScmUseEditMode());
        assertNotEquals(configBuilder.build(), origConfig, "Check original comparison");
        configBuilder.setScmUseEditMode(origConfig.isScmUseEditMode());

        configBuilder.setInteractive(!origConfig.isInteractive());
        assertNotEquals(configBuilder.build(), origConfig, "Check original comparison");
        configBuilder.setInteractive(origConfig.isInteractive());

        configBuilder.setCommitByProject(!origConfig.isCommitByProject());
        assertNotEquals(configBuilder.build(), origConfig, "Check original comparison");
        configBuilder.setCommitByProject(origConfig.isCommitByProject());

        configBuilder.setCompletedPhase(other);
        assertNotEquals(configBuilder.build(), origConfig, "Check original comparison");
        configBuilder.setCompletedPhase(origConfig.getCompletedPhase());

        configBuilder.setScmPrivateKeyPassPhrase(other);
        assertNotEquals(configBuilder.build(), origConfig, "Check original comparison");
        configBuilder.setScmPrivateKeyPassPhrase(origConfig.getScmPrivateKeyPassPhrase());

        configBuilder.setScmPassword(other);
        assertNotEquals(configBuilder.build(), origConfig, "Check original comparison");
        configBuilder.setScmPassword(origConfig.getScmPassword());

        configBuilder.setScmUsername(other);
        assertNotEquals(configBuilder.build(), origConfig, "Check original comparison");
        configBuilder.setScmUsername(origConfig.getScmUsername());

        configBuilder.setScmPrivateKey(other);
        assertNotEquals(configBuilder.build(), origConfig, "Check original comparison");
        configBuilder.setScmPrivateKey(origConfig.getScmPrivateKey());

        configBuilder.setPomFileName(other);
        assertNotEquals(configBuilder.build(), origConfig, "Check original comparison");
        configBuilder.setPomFileName(origConfig.getPomFileName());

        configBuilder.setPreparationGoals(other);
        assertNotEquals(configBuilder.build(), origConfig, "Check original comparison");
        configBuilder.setPreparationGoals(origConfig.getPreparationGoals());

        configBuilder.setScmReleaseLabel(other);
        assertNotEquals(configBuilder.build(), origConfig, "Check original comparison");
        configBuilder.setScmReleaseLabel(origConfig.getScmReleaseLabel());

        configBuilder.setScmTagBase(other);
        assertNotEquals(configBuilder.build(), origConfig, "Check original comparison");
        configBuilder.setScmTagBase(origConfig.getScmTagBase());

        if (otherFile != null) {
            if (!otherFile.exists()) {
                assertTrue(
                        otherFile.mkdirs(),
                        "Failed to create the directory, along with all necessary parent directories");
            }
            configBuilder.setWorkingDirectory(AbstractReleaseTestCase.getPath(otherFile));
            assertNotEquals(configBuilder.build(), origConfig, "Check original comparison");
        }

        configBuilder.setWorkingDirectory(origConfig.getWorkingDirectory());

        // sanity check the test was resetting correctly
        assertEquals(configBuilder.build(), origConfig, "Check original comparison");

        configBuilder.addDevelopmentVersion("groupId:artifactId", "1.0-SNAPSHOT");
        assertNotEquals(configBuilder.build(), origConfig, "Check original comparison");
        configBuilder = copyReleaseDescriptor(origConfig);

        configBuilder.addReleaseVersion("groupId:artifactId", "1.0");
        assertNotEquals(configBuilder.build(), origConfig, "Check original comparison");
        configBuilder = copyReleaseDescriptor(origConfig);

        configBuilder.addOriginalScmInfo("groupId:artifactId", new Scm());
        assertNotEquals(configBuilder.build(), origConfig, "Check original comparison");
        configBuilder = copyReleaseDescriptor(origConfig);

        configBuilder.addOriginalScmInfo("groupId:artifactId", new Scm());
        origConfig.addOriginalScmInfo("foo", new Scm());
        assertNotEquals(configBuilder.build(), origConfig, "Check original comparison");
        origConfig = createReleaseDescriptor().build();
        configBuilder = copyReleaseDescriptor(origConfig);

        configBuilder.addOriginalScmInfo("groupId:artifactId", new Scm());
        origConfig.addOriginalScmInfo("groupId:artifactId", new Scm());
        assertEquals(configBuilder.build(), origConfig, "Check original comparison");
        origConfig = createReleaseDescriptor().build();
        configBuilder = copyReleaseDescriptor(origConfig);

        configBuilder.addOriginalScmInfo("groupId:artifactId", getScm("conn", "dev", "url", "tag"));
        origConfig.addOriginalScmInfo("groupId:artifactId", getScm("conn", "dev", "url", "tag"));
        assertEquals(configBuilder.build(), origConfig, "Check original comparison");
        origConfig = createReleaseDescriptor().build();
        configBuilder = copyReleaseDescriptor(origConfig);

        configBuilder.addOriginalScmInfo("groupId:artifactId", getScm("-", "dev", "url", "tag"));
        origConfig.addOriginalScmInfo("groupId:artifactId", getScm("conn", "dev", "url", "tag"));
        assertNotEquals(configBuilder.build(), origConfig, "Check original comparison");
        origConfig = createReleaseDescriptor().build();
        configBuilder = copyReleaseDescriptor(origConfig);

        configBuilder.addOriginalScmInfo("groupId:artifactId", getScm("conn", "-", "url", "tag"));
        origConfig.addOriginalScmInfo("groupId:artifactId", getScm("conn", "dev", "url", "tag"));
        assertNotEquals(configBuilder.build(), origConfig, "Check original comparison");
        origConfig = createReleaseDescriptor().build();
        configBuilder = copyReleaseDescriptor(origConfig);

        configBuilder.addOriginalScmInfo("groupId:artifactId", getScm("conn", "dev", "-", "tag"));
        origConfig.addOriginalScmInfo("groupId:artifactId", getScm("conn", "dev", "url", "tag"));
        assertNotEquals(configBuilder.build(), origConfig, "Check original comparison");
        origConfig = createReleaseDescriptor().build();
        configBuilder = copyReleaseDescriptor(origConfig);

        configBuilder.addOriginalScmInfo("groupId:artifactId", getScm("conn", "dev", "url", "-"));
        origConfig.addOriginalScmInfo("groupId:artifactId", getScm("conn", "dev", "url", "tag"));
        assertNotEquals(configBuilder.build(), origConfig, "Check original comparison");
    }

    @Test
    void testHashCode() throws IOException {
        ReleaseDescriptor releaseDescriptor = createReleaseDescriptor().build();

        assertEquals(
                releaseDescriptor.hashCode(),
                createReleaseDescriptor(releaseDescriptor.getWorkingDirectory())
                        .build()
                        .hashCode(),
                "Check hash code");
    }

    @Test
    void testLoadResolvedDependencies() {
        Properties properties = new Properties();
        String dependencyKey = ArtifactUtils.versionlessKey("com.groupId", "artifactId");
        properties.put("dependency." + dependencyKey + ".release", "1.3");
        properties.put("dependency." + dependencyKey + ".development", "1.3-SNAPSHOT");

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        ReleaseUtils.copyPropertiesToReleaseDescriptor(properties, builder);
        ReleaseDescriptor descriptor = builder.build();

        assertEquals("1.3", descriptor.getDependencyReleaseVersion(dependencyKey));
        assertEquals("1.3-SNAPSHOT", descriptor.getDependencyDevelopmentVersion(dependencyKey));
    }

    // MRELEASE-750
    @Test
    void testArtifactIdEndswithDependency() {
        Properties properties = new Properties();
        String relDependencyKey = ArtifactUtils.versionlessKey("com.release.magic", "dependency");
        properties.put("dependency." + relDependencyKey + ".release", "1.3");
        String devDependencyKey = ArtifactUtils.versionlessKey("com.development.magic", "dependency");
        properties.put("dependency." + devDependencyKey + ".development", "1.3-SNAPSHOT");

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        ReleaseUtils.copyPropertiesToReleaseDescriptor(properties, builder);
        ReleaseDescriptor descriptor = builder.build();

        assertEquals("1.3", descriptor.getDependencyReleaseVersion(relDependencyKey));
        assertEquals("1.3-SNAPSHOT", descriptor.getDependencyDevelopmentVersion(devDependencyKey));
    }

    // MRELEASE-834
    @Test
    void testSystemPropertyStartingWithDependency() {
        Properties properties = new Properties();
        properties.setProperty("dependency.locations.enabled", "false");
        ReleaseUtils.copyPropertiesToReleaseDescriptor(properties, new ReleaseDescriptorBuilder());
    }

    // MRELEASE-1038
    @Test
    void testActiveProfilesProperty() {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        Properties properties = new Properties();
        properties.setProperty("exec.activateProfiles", "aProfile,anotherOne");
        ReleaseUtils.copyPropertiesToReleaseDescriptor(properties, builder);

        assertEquals(Arrays.asList("aProfile", "anotherOne"), builder.build().getActivateProfiles());
    }

    private static ReleaseDescriptorBuilder copyReleaseDescriptor(ReleaseDescriptor originalReleaseDescriptor) {
        return createReleaseDescriptor(originalReleaseDescriptor.getWorkingDirectory());
    }

    private static Scm getScm(String connection, String developerConnection, String url, String tag) {
        Scm scm = new Scm();
        scm.setConnection(connection);
        scm.setDeveloperConnection(developerConnection);
        scm.setTag(tag);
        scm.setUrl(url);
        return scm;
    }

    private static ReleaseDescriptorBuilder createReleaseDescriptor() throws IOException {
        File workingDirectory = new File(".");

        return createReleaseDescriptor(AbstractReleaseTestCase.getPath(workingDirectory));
    }

    private static ReleaseDescriptorBuilder createReleaseDescriptor(String workingDirectory) {
        ReleaseDescriptorBuilder releaseDescriptor = new ReleaseDescriptorBuilder();
        releaseDescriptor.setScmSourceUrl("scm-url");
        releaseDescriptor.setCompletedPhase("completed-phase");
        releaseDescriptor.setScmPrivateKeyPassPhrase("passphrase");
        releaseDescriptor.setScmPassword("password");
        releaseDescriptor.setScmPrivateKey("private-key");
        releaseDescriptor.setScmTagBase("tag-base");
        releaseDescriptor.setScmReleaseLabel("tag");
        releaseDescriptor.setScmUsername("username");
        releaseDescriptor.setWorkingDirectory(workingDirectory);
        releaseDescriptor.setAdditionalArguments("additional-arguments");
        releaseDescriptor.setPomFileName("pom-file-name");
        releaseDescriptor.setPreparationGoals("preparation-goals");

        return releaseDescriptor;
    }
}
