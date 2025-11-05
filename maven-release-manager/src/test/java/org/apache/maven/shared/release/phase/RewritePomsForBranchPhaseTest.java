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
import javax.inject.Named;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.provider.svn.repository.SvnScmProviderRepository;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.apache.maven.shared.release.config.ReleaseUtils;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.codehaus.plexus.testing.PlexusTest;
import org.junit.jupiter.api.Test;

import static org.codehaus.plexus.testing.PlexusExtension.getTestFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the SCM modification check phase.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
@PlexusTest
class RewritePomsForBranchPhaseTest extends AbstractEditModeRewritingReleasePhaseTestCase {
    private static final String NEXT_VERSION = "1.0-SNAPSHOT";

    private static final String ALTERNATIVE_NEXT_VERSION = "2.0-SNAPSHOT";

    @Inject
    @Named("rewrite-poms-for-branch")
    private ReleasePhase phase;

    @Override
    protected ReleasePhase getTestedPhase() {
        return phase;
    }

    @Override
    protected Path getWorkingDirectory(String workingDir) {
        return super.getWorkingDirectory("rewrite-for-branch/" + workingDir);
    }

    @Override
    protected List<MavenProject> prepareReactorProjects(String path) throws Exception {
        String dir = "rewrite-for-branch/" + Objects.toString(path, "");
        return createReactorProjects(dir, path, null);
    }

    @Override
    protected String readTestProjectFile(String fileName) throws IOException {
        return ReleaseUtil.readXmlFile(getTestFile("target/test-classes/projects/rewrite-for-branch/" + fileName));
    }

    @Test
    void testSimulateRewrite() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects("basic-pom");
        ReleaseDescriptorBuilder builder = createDescriptorFromBasicPom(reactorProjects, "basic-pom");
        builder.addReleaseVersion("groupId:artifactId", NEXT_VERSION);

        String expected = readTestProjectFile("basic-pom/pom.xml");

        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        String actual = readTestProjectFile("basic-pom/pom.xml");
        assertEquals(expected, actual, "Check the original POM untouched");

        expected = readTestProjectFile("basic-pom/expected-pom.xml");
        actual = readTestProjectFile("basic-pom/pom.xml.branch");
        assertEquals(expected, actual, "Check the transformed POM");
    }

    @Test
    void testRewriteWithDashedComments() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects("basic-pom-with-dashes-in-comment");
        ReleaseDescriptorBuilder builder =
                createDescriptorFromBasicPom(reactorProjects, "basic-pom-with-dashes-in-comment");
        builder.addReleaseVersion("groupId:artifactId", NEXT_VERSION);

        String expected = readTestProjectFile("basic-pom-with-dashes-in-comment/pom.xml");

        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        String actual = readTestProjectFile("basic-pom-with-dashes-in-comment/pom.xml");
        assertEquals(expected, actual, "Check the original POM is untouched");

        expected = readTestProjectFile("basic-pom-with-dashes-in-comment/expected-pom.xml");
        actual = readTestProjectFile("basic-pom-with-dashes-in-comment/pom.xml.branch");
        assertEquals(expected, actual, "Check the transformed POM");
    }

    @Test
    void testClean() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects("basic-pom");
        ReleaseDescriptorBuilder builder = createDescriptorFromProjects(reactorProjects, "basic-pom");
        builder.addReleaseVersion("groupId:artifactId", NEXT_VERSION);

        File testFile = getTestFile("target/test-classes/projects/rewrite-for-branch/basic-pom/pom.xml.branch");
        testFile.delete();
        assertFalse(testFile.exists());

        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        assertTrue(testFile.exists());

        ((ResourceGenerator) phase).clean(reactorProjects);

        assertFalse(testFile.exists());
    }

    @Test
    void testCleanNotExists() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects("basic-pom");
        ReleaseDescriptorBuilder builder = createDescriptorFromBasicPom(reactorProjects, "basic-pom");
        builder.addReleaseVersion("groupId:artifactId", NEXT_VERSION);

        File testFile = getTestFile("target/test-classes/projects/rewrite-for-branch/basic-pom/pom.xml.branch");
        testFile.delete();
        assertFalse(testFile.exists());

        ((ResourceGenerator) phase).clean(reactorProjects);

        assertFalse(testFile.exists());
    }

    // MRELEASE-116
    @Test
    void testScmOverridden() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects("pom-with-overridden-scm");
        ReleaseDescriptorBuilder builder =
                createConfigurationForWithParentNextVersion(reactorProjects, "pom-with-overridden-scm");
        builder.addReleaseVersion("groupId:subsubproject", NEXT_VERSION);

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        assertTrue(comparePomFiles(reactorProjects));
    }

    @Override
    protected void mapAlternateNextVersion(ReleaseDescriptorBuilder config, String projectId) {
        config.addReleaseVersion(projectId, ALTERNATIVE_NEXT_VERSION);
    }

    @Override
    protected void mapNextVersion(ReleaseDescriptorBuilder config, String projectId) {
        config.addReleaseVersion(projectId, NEXT_VERSION);
    }

    @Override
    protected ReleaseDescriptorBuilder createConfigurationForPomWithParentAlternateNextVersion(
            List<MavenProject> reactorProjects, String workingDirectory) {
        ReleaseDescriptorBuilder builder = createDescriptorFromProjects(reactorProjects, workingDirectory);

        builder.addReleaseVersion("groupId:artifactId", NEXT_VERSION);
        builder.addReleaseVersion("groupId:subproject1", ALTERNATIVE_NEXT_VERSION);
        return builder;
    }

    @Override
    protected ReleaseDescriptorBuilder createConfigurationForWithParentNextVersion(
            List<MavenProject> reactorProjects, String workingDirectory) {
        ReleaseDescriptorBuilder builder = createDescriptorFromProjects(reactorProjects, workingDirectory);

        builder.addReleaseVersion("groupId:artifactId", NEXT_VERSION);
        builder.addReleaseVersion("groupId:subproject1", NEXT_VERSION);
        return builder;
    }

    @Override
    protected void unmapNextVersion(ReleaseDescriptorBuilder config, String projectId) {
        // nothing to do
    }

    @Test
    void testRewriteBasicPomWithGit() throws Exception {

        List<MavenProject> reactorProjects = createReactorProjects("basic-pom-with-git");
        ReleaseDescriptorBuilder builder = createDescriptorFromProjects(reactorProjects, "basic-pom-with-git");
        mapNextVersion(builder, "groupId:artifactId");

        String sourceUrl = "scm:git:git://localhost/repo";
        SvnScmProviderRepository scmProviderRepository = new SvnScmProviderRepository(sourceUrl);
        ScmRepository repository = new ScmRepository("git", scmProviderRepository);
        scmManager.addScmRepositoryForUrl("scm:git:git://localhost/repo", repository);

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        assertTrue(comparePomFiles(reactorProjects));
    }

    @Test
    void testRewriteBasicPomWithScmExpression() throws Exception {

        List<MavenProject> reactorProjects = createReactorProjects("basic-pom-with-scm-expression");
        ReleaseDescriptorBuilder builder =
                createDescriptorFromProjects(reactorProjects, "basic-pom-with-scm-expression");
        mapNextVersion(builder, "groupId:artifactId");

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        assertTrue(comparePomFiles(reactorProjects));
    }

    @Test
    void testRewriteBasicPomWithTagBase() throws Exception {

        List<MavenProject> reactorProjects = createReactorProjects("basic-pom-with-tag-base");
        ReleaseDescriptorBuilder builder = createDescriptorFromProjects(reactorProjects, "basic-pom-with-tag-base");
        builder.setScmBranchBase("file://localhost/tmp/scm-repo/branches");
        mapNextVersion(builder, "groupId:artifactId");

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        assertTrue(comparePomFiles(reactorProjects));
    }

    @Test
    void testRewriteBasicPomWithTagBaseAndVaryingScmUrls() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects("basic-pom-with-tag-base-and-varying-scm-urls");
        ReleaseDescriptorBuilder builder =
                createDescriptorFromProjects(reactorProjects, "basic-pom-with-tag-base-and-varying-scm-urls");
        builder.setScmBranchBase("file://localhost/tmp/scm-repo/allprojects/branches");
        mapNextVersion(builder, "groupId:artifactId");

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        assertTrue(comparePomFiles(reactorProjects));
    }

    @Test
    void testRewriteBasicPomWithGitFromTag() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects("basic-pom-with-git-from-tag");
        ReleaseDescriptorBuilder builder = createDescriptorFromProjects(reactorProjects, "basic-pom-with-git-from-tag");
        mapNextVersion(builder, "groupId:artifactId");

        String sourceUrl = "scm:git:git://localhost/repo";
        SvnScmProviderRepository scmProviderRepository = new SvnScmProviderRepository(sourceUrl);
        ScmRepository repository = new ScmRepository("git", scmProviderRepository);
        scmManager.addScmRepositoryForUrl("scm:git:git://localhost/repo", repository);

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        assertTrue(comparePomFiles(reactorProjects));
    }

    @Test
    void testRewriteBasicPomWithEmptyScm() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects("basic-pom-with-empty-scm");
        ReleaseDescriptorBuilder builder = createDescriptorFromProjects(reactorProjects, "basic-pom-with-empty-scm");
        mapNextVersion(builder, "groupId:artifactId");

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        assertTrue(comparePomFiles(reactorProjects));
    }

    @Test
    void testRewriteInterpolatedVersions() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects("interpolated-versions");
        ReleaseDescriptorBuilder builder = createMappedConfiguration(reactorProjects, "interpolated-versions");

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        assertTrue(comparePomFiles(reactorProjects));
    }

    @Test
    void testRewriteInterpolatedVersionsDifferentVersion() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects("interpolated-versions");
        ReleaseDescriptorBuilder builder = createDescriptorFromProjects(reactorProjects, "interpolated-versions");

        builder.addReleaseVersion("groupId:artifactId", NEXT_VERSION);
        builder.addReleaseVersion("groupId:subproject1", ALTERNATIVE_NEXT_VERSION);
        builder.addReleaseVersion("groupId:subproject2", NEXT_VERSION);
        builder.addReleaseVersion("groupId:subproject3", NEXT_VERSION);

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        for (MavenProject project : reactorProjects) {
            // skip subproject1 - we don't need to worry about its version mapping change, it has no deps of any kind
            if (!"groupId".equals(project.getGroupId()) || !"subproject1".equals(project.getArtifactId())) {
                comparePomFiles(project, "-different-version", true);
            }
        }
    }

    @Test
    void testRewriteBasicPomWithInheritedScm() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects("basic-pom-inherited-scm");
        ReleaseDescriptorBuilder builder =
                createConfigurationForWithParentNextVersion(reactorProjects, "basic-pom-inherited-scm");
        builder.addReleaseVersion("groupId:subsubproject", NEXT_VERSION);

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        assertTrue(comparePomFiles(reactorProjects));
    }

    @Test
    void testRewritePomWithParentAndProperties() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects("pom-with-parent-and-properties");

        ReleaseDescriptorBuilder builder =
                createDescriptorFromProjects(reactorProjects, "pom-with-parent-and-properties");
        builder.addReleaseVersion("groupId:artifactId", NEXT_VERSION);
        builder.addReleaseVersion("groupId:subproject1", ALTERNATIVE_NEXT_VERSION);
        builder.addReleaseVersion("groupId:subproject2", ALTERNATIVE_NEXT_VERSION);

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        assertTrue(comparePomFiles(reactorProjects));
    }

    // MRELEASE-311
    @Test
    void testRewritePomWithDependencyPropertyCoordinate() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects("pom-with-property-dependency-coordinate");

        ReleaseDescriptorBuilder builder =
                createDescriptorFromProjects(reactorProjects, "pom-with-property-dependency-coordinate");
        builder.addReleaseVersion("groupId:artifactId", NEXT_VERSION);
        builder.addReleaseVersion("groupId:subproject1-3.4", ALTERNATIVE_NEXT_VERSION);
        builder.addReleaseVersion("groupId:subproject2", ALTERNATIVE_NEXT_VERSION);

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        assertTrue(comparePomFiles(reactorProjects));
    }

    // MRELEASE-305
    @Test
    void testRewritePomWithScmOfParentEndingWithASlash() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects("pom-with-scm-of-parent-ending-with-a-slash");

        ReleaseDescriptorBuilder builder =
                createDescriptorFromProjects(reactorProjects, "pom-with-scm-of-parent-ending-with-a-slash");
        builder.addReleaseVersion("groupId:artifactId", NEXT_VERSION);
        builder.addReleaseVersion("groupId:subproject1", ALTERNATIVE_NEXT_VERSION);

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        assertTrue(comparePomFiles(reactorProjects));
    }

    @Test
    void testRewritePomWithDeepSubprojects() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects("multimodule-with-deep-subprojects");

        ReleaseDescriptorBuilder builder =
                createDescriptorFromProjects(reactorProjects, "multimodule-with-deep-subprojects");
        builder.addReleaseVersion("groupId:artifactId", NEXT_VERSION);
        builder.addReleaseVersion("groupId:subproject1", ALTERNATIVE_NEXT_VERSION);
        builder.addReleaseVersion("groupId:subproject2", ALTERNATIVE_NEXT_VERSION);

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        assertTrue(comparePomFiles(reactorProjects));
    }

    @Test
    void testRewritePomForFlatMultiModule() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects(
                "rewrite-for-branch/pom-with-parent-flat", "pom-with-parent-flat", "root-project");
        ReleaseDescriptorBuilder builder =
                createConfigurationForPomWithParentAlternateNextVersion(reactorProjects, "pom-with-parent-flat");

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        assertTrue(comparePomFiles(reactorProjects));
    }

    // MRELEASE-383
    @Test
    void testRewritePomWithCDATASectionOnWindows() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects("cdata-section");
        ReleaseDescriptorBuilder builder = createDescriptorFromProjects(reactorProjects, "cdata-section");
        mapNextVersion(builder, "groupId:artifactId");

        AbstractRewritePomsPhase phase = (AbstractRewritePomsPhase) this.phase;
        ReleaseDescriptorBuilder.BuilderReleaseDescriptor builderReleaseDescriptor =
                ReleaseUtils.buildReleaseDescriptor(builder);
        builderReleaseDescriptor.setLineSeparator("\r\n");
        phase.execute(builderReleaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects);

        // compare POMS without line ending normalization
        assertTrue(comparePomFiles(reactorProjects, false));
    }

    protected ReleaseDescriptorBuilder createDescriptorFromProjects(
            List<MavenProject> reactorProjects, String workingDirectory) {
        ReleaseDescriptorBuilder builder = super.createDescriptorFromProjects(reactorProjects, workingDirectory);
        builder.setScmReleaseLabel("release-label");
        return builder;
    }

    // MRELEASE-454
    @Test
    void testRewritePomWithImportedDependencyManagementInReactor() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects("imported-dependency-management-in-reactor");
        ReleaseDescriptorBuilder builder =
                createMappedConfiguration(reactorProjects, "imported-dependency-management-in-reactor");

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        assertTrue(comparePomFiles(reactorProjects));
    }

    @Test
    void testRewritePomWithDifferentVersionsAcrossModules() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects("modules-with-different-versions");
        ReleaseDescriptorBuilder builder =
                createMappedConfiguration(reactorProjects, "modules-with-different-versions");
        builder.addReleaseVersion("groupId:subproject2", ALTERNATIVE_NEXT_VERSION);

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        assertTrue(comparePomFiles(reactorProjects));
    }
}
