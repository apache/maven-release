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

import javax.inject.Inject;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.apache.maven.shared.release.config.ReleaseUtils;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.testing.PlexusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@PlexusTest
class RewritePomsWithPreviousVersionsTest extends AbstractReleaseTestCase {
    @Inject
    private PlexusContainer container;

    @TempDir
    private Path directory;

    @ParameterizedTest
    @ValueSource(
            strings = {
                "rewrite-pom-versions",
                "rewrite-poms-for-release",
                "rewrite-poms-for-development",
                "rewrite-poms-for-branch"
            })
    void preservePreviousVersionInPluginDependency(String phase) throws Exception {
        Model model = model(phase);
        model.getProperties().setProperty("previous.version", "1.0");
        Plugin plugin = new Plugin();
        plugin.setArtifactId("maven-antrun-plugin");
        plugin.setVersion("3.1.0");
        plugin.addDependency(dependency("self", "${previous.version}"));
        Build build = new Build();
        build.addPlugin(plugin);
        model.setBuild(build);
        Model rewritten = rewrite(phase, model, false);
        assertEquals("1.0", rewritten.getProperties().getProperty("previous.version"));
        assertEquals(
                "${previous.version}",
                rewritten
                        .getBuild()
                        .getPlugins()
                        .get(0)
                        .getDependencies()
                        .get(0)
                        .getVersion());
        assertEquals(nextVersion(phase), rewritten.getVersion());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "rewrite-pom-versions",
                "rewrite-poms-for-release",
                "rewrite-poms-for-development",
                "rewrite-poms-for-branch"
            })
    void preservePreviousVersionInOrdinaryDependency(String phase) throws Exception {
        Model model = model(phase);
        model.getProperties().setProperty("previous.version", "1.0");
        model.addDependency(dependency("other", "${previous.version}"));
        model.addDependency(dependency("literal", "1.0"));
        Model rewritten = rewrite(phase, model, false);
        assertEquals("1.0", rewritten.getProperties().getProperty("previous.version"));
        assertEquals("${previous.version}", rewritten.getDependencies().get(0).getVersion());
        assertEquals("1.0", rewritten.getDependencies().get(1).getVersion());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "rewrite-pom-versions",
                "rewrite-poms-for-release",
                "rewrite-poms-for-development",
                "rewrite-poms-for-branch"
            })
    void updateSharedCurrentVersionOnce(String phase) throws Exception {
        Model model = model(phase);
        model.getProperties().setProperty("current.version", model.getVersion());
        model.addDependency(dependency("other", "${current.version}"));
        model.addDependency(dependency("literal", "${current.version}"));
        assertEquals(
                nextVersion(phase), rewrite(phase, model, false).getProperties().getProperty("current.version"));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "rewrite-pom-versions",
                "rewrite-poms-for-release",
                "rewrite-poms-for-development",
                "rewrite-poms-for-branch"
            })
    void rejectConflictingCurrentVersions(String phase) throws Exception {
        Model model = model(phase);
        model.getProperties().setProperty("current.version", model.getVersion());
        model.addDependency(dependency("other", "${current.version}"));
        model.addDependency(dependency("literal", "${current.version}"));
        assertThrows(ReleaseFailureException.class, () -> rewrite(phase, model, true));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void rejectPropertySharedByPreviousAndCurrentVersions(boolean previousFirst) throws Exception {
        Model model = model("rewrite-pom-versions");
        model.getProperties().setProperty("shared.version", "1.0");
        Dependency previous = dependency("other", "${shared.version}");
        Dependency current = dependency("old-reactor", "${shared.version}");
        model.addDependency(previousFirst ? previous : current);
        model.addDependency(previousFirst ? current : previous);
        assertThrows(ReleaseFailureException.class, () -> rewrite("rewrite-pom-versions", model, false));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void rejectPropertySharedAcrossPluginsAndDependencies(boolean previousFirst) throws Exception {
        Model model = model("rewrite-pom-versions");
        model.getProperties().setProperty("shared.version", "1.0");
        Plugin plugin = new Plugin();
        plugin.setGroupId("example");
        plugin.setArtifactId(previousFirst ? "other" : "old-reactor");
        plugin.setVersion("${shared.version}");
        Build build = new Build();
        build.addPlugin(plugin);
        model.setBuild(build);
        model.addDependency(dependency(previousFirst ? "old-reactor" : "other", "${shared.version}"));
        assertThrows(ReleaseFailureException.class, () -> rewrite("rewrite-pom-versions", model, false));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "rewrite-pom-versions",
                "rewrite-poms-for-release",
                "rewrite-poms-for-development",
                "rewrite-poms-for-branch"
            })
    void preservePreviousVersionEqualToMappedVersion(String phase) throws Exception {
        Model model = model(phase);
        model.getProperties().setProperty("previous.version", nextVersion(phase));
        model.addDependency(dependency("other", "${previous.version}"));
        assertEquals(
                nextVersion(phase), rewrite(phase, model, false).getProperties().getProperty("previous.version"));
    }

    @Test
    void rejectPreviousVersionChangedByCiFriendlyProjectVersion() throws Exception {
        Model model = model("rewrite-pom-versions");
        model.setVersion("${revision}");
        model.getProperties().setProperty("revision", "1.0");
        model.addDependency(dependency("other", "${revision}"));
        ReleaseFailureException failure =
                assertThrows(ReleaseFailureException.class, () -> rewrite("rewrite-pom-versions", model, false));
        assertEquals(
                "The expression (${revision}) in the project (example:self) is shared by artifacts requiring "
                        + "different versions (1.0 and 2.1-SNAPSHOT).",
                failure.getMessage());
    }

    private Model model(String phase) {
        Model model = new Model();
        model.setModelVersion("4.0.0");
        model.setGroupId("example");
        model.setArtifactId("self");
        model.setVersion("rewrite-poms-for-development".equals(phase) ? "2.0" : "2.0-SNAPSHOT");
        return model;
    }

    private Dependency dependency(String artifactId, String version) {
        Dependency dependency = new Dependency();
        dependency.setGroupId("example");
        dependency.setArtifactId(artifactId);
        dependency.setVersion(version);
        return dependency;
    }

    private String nextVersion(String phase) {
        return "rewrite-poms-for-release".equals(phase) || "rewrite-poms-for-branch".equals(phase)
                ? "2.0"
                : "2.1-SNAPSHOT";
    }

    private Model rewrite(String phaseName, Model model, boolean conflictingVersions) throws Exception {
        Path pom = directory.resolve("pom.xml");
        try (Writer writer = Files.newBufferedWriter(pom)) {
            new MavenXpp3Writer().write(writer, model);
        }
        MavenProject project = new MavenProject(model.clone());
        project.setFile(pom.toFile());
        if ("rewrite-poms-for-development".equals(phaseName)) {
            // prepare keeps the original effective project while the POM has already been released.
            project.setVersion("2.0-SNAPSHOT");
            if (project.getProperties().containsKey("current.version")) {
                project.getProperties().setProperty("current.version", "2.0-SNAPSHOT");
            }
        }
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setWorkingDirectory(directory.toString());
        builder.setScmSourceUrl("scm:svn:file://localhost/repository/trunk");
        builder.setScmReleaseLabel("release-label");
        builder.setUpdateDependencies(true);
        for (String artifactId : new String[] {"self", "other", "literal", "old-reactor"}) {
            String key = "example:" + artifactId;
            builder.putOriginalVersion(key, "old-reactor".equals(artifactId) ? "1.0" : "2.0-SNAPSHOT");
            builder.addReleaseVersion(key, conflictingVersions && "literal".equals(artifactId) ? "3.0" : "2.0");
            builder.addDevelopmentVersion(
                    key, conflictingVersions && "literal".equals(artifactId) ? "3.1-SNAPSHOT" : "2.1-SNAPSHOT");
        }
        if ("${revision}".equals(model.getVersion())) {
            String revision = model.getProperties().getProperty("revision");
            project.setVersion(revision);
            builder.putOriginalVersion("example:self", revision);
        }
        ReleasePhase phase = container.lookup(ReleasePhase.class, phaseName);
        phase.execute(
                ReleaseUtils.buildReleaseDescriptor(builder),
                new DefaultReleaseEnvironment(),
                Collections.singletonList(project));
        try (Reader reader = Files.newBufferedReader(pom)) {
            return new MavenXpp3Reader().read(reader);
        }
    }
}
