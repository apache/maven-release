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
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Scm;
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
class RewritePomsForDevelopmentPhaseTest extends AbstractEditModeRewritingReleasePhaseTestCase {
    private static final String NEXT_VERSION = "1.1-SNAPSHOT";

    private static final String ALTERNATIVE_NEXT_VERSION = "2.1-SNAPSHOT";

    private static final String RELEASE_VERSION = "1.0";

    private static final String ALTERNATIVE_RELEASE_VERSION = "2.0";

    @Inject
    @Named("rewrite-poms-for-development")
    private ReleasePhase phase;

    @Override
    protected ReleasePhase getTestedPhase() {
        return phase;
    }

    @Override
    protected Path getWorkingDirectory(String workingDir) {
        return super.getWorkingDirectory("rewrite-for-development/" + workingDir);
    }

    @Test
    void testSimulateRewrite() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjectsWhenSimulated("basic-pom");
        ReleaseDescriptorBuilder builder = createDescriptorFromBasicPom(reactorProjects, "basic-pom");
        builder.addReleaseVersion("groupId:artifactId", RELEASE_VERSION);
        builder.addDevelopmentVersion("groupId:artifactId", NEXT_VERSION);

        String expected = readTestProjectFile("basic-pom/pom.xml");

        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        String actual = readTestProjectFile("basic-pom/pom.xml");
        assertEquals(expected, actual, "Check the original POM untouched");

        expected = readTestProjectFile("basic-pom/expected-pom.xml");
        actual = readTestProjectFile("basic-pom/pom.xml.next");
        assertEquals(expected, actual, "Check the transformed POM");
    }

    private List<MavenProject> createReactorProjectsWhenSimulated(String name) throws Exception {
        return createReactorProjects("rewrite-for-release/" + name, name, null);
    }

    @Test
    void testSimulateRewriteEjbClientDeps() throws Exception {
        List<MavenProject> reactorProjects =
                new LinkedList<>(createReactorProjects("basic-pom-ejb-client-dep/project"));
        reactorProjects.addAll(createReactorProjects("basic-pom-ejb-client-dep/ejb"));
        ReleaseDescriptorBuilder builder = createDescriptorFromBasicPom(reactorProjects, "basic-pom-ejb-client-dep");
        builder.addReleaseVersion("groupId:artifactId", RELEASE_VERSION);
        builder.addDevelopmentVersion("groupId:artifactId", NEXT_VERSION);
        builder.addDevelopmentVersion(ArtifactUtils.versionlessKey("groupId", "artifactId1"), NEXT_VERSION);
        builder.addReleaseVersion(ArtifactUtils.versionlessKey("groupId", "artifactId1"), RELEASE_VERSION);

        String expected = readTestProjectFile("basic-pom-ejb-client-dep/project/pom.xml");

        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        String actual = readTestProjectFile("basic-pom-ejb-client-dep/project/pom.xml");
        assertEquals(expected, actual, "Check the original POM untouched");

        expected = readTestProjectFile("basic-pom-ejb-client-dep/project/expected-pom.xml");
        actual = readTestProjectFile("basic-pom-ejb-client-dep/project/pom.xml.next");
        assertEquals(expected, actual, "Check the transformed POM");
    }

    @Test
    void testClean() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjectsWhenSimulated("basic-pom");
        ReleaseDescriptorBuilder builder = createDescriptorFromBasicPom(reactorProjects, "basic-pom");
        builder.addReleaseVersion("groupId:artifactId", RELEASE_VERSION);
        builder.addDevelopmentVersion("groupId:artifactId", NEXT_VERSION);

        File testFile = getTestFile("target/test-classes/projects/rewrite-for-development/basic-pom/pom.xml.next");
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

        File testFile = getTestFile("target/test-classes/projects/rewrite-for-development/basic-pom/pom.xml.next");
        testFile.delete();
        assertFalse(testFile.exists());

        ((ResourceGenerator) phase).clean(reactorProjects);

        assertFalse(testFile.exists());
    }

    @Override
    protected String readTestProjectFile(String fileName) throws IOException {
        return readTestProjectFile(fileName, "rewrite-for-development/");
    }

    protected String readTestProjectFile(String fileName, String subpath) throws IOException {
        return ReleaseUtil.readXmlFile(getTestFile("target/test-classes/projects/" + subpath + fileName));
    }

    @Override
    protected List<MavenProject> prepareReactorProjects(String path) throws Exception {
        String dir = "rewrite-for-development/" + path;
        return createReactorProjects(dir, path, null);
    }

    @Override
    protected ReleaseDescriptorBuilder createDescriptorFromBasicPom(
            List<MavenProject> reactorProjects, String workingDirectory) throws Exception {
        ReleaseDescriptorBuilder builder = super.createDescriptorFromProjects(reactorProjects, workingDirectory);

        mapScm(builder);

        return builder;
    }

    private void mapScm(ReleaseDescriptorBuilder builder) {
        Scm scm = new Scm();
        scm.setConnection("scm:svn:file://localhost/tmp/scm-repo/trunk");
        scm.setDeveloperConnection("scm:svn:file://localhost/tmp/scm-repo/trunk");
        scm.setUrl("file://localhost/tmp/scm-repo/trunk");
        builder.addOriginalScmInfo("groupId:artifactId", scm);
    }

    @Override
    protected void mapAlternateNextVersion(ReleaseDescriptorBuilder config, String projectId) {
        config.addReleaseVersion(projectId, ALTERNATIVE_RELEASE_VERSION);
        config.addDevelopmentVersion(projectId, ALTERNATIVE_NEXT_VERSION);
    }

    @Override
    protected void mapNextVersion(ReleaseDescriptorBuilder config, String projectId) {
        config.addReleaseVersion(projectId, RELEASE_VERSION);
        config.addDevelopmentVersion(projectId, NEXT_VERSION);
    }

    @Override
    protected void unmapNextVersion(ReleaseDescriptorBuilder builder, String projectId) {
        builder.addReleaseVersion(projectId, RELEASE_VERSION);
    }

    @Override
    protected ReleaseDescriptorBuilder createConfigurationForPomWithParentAlternateNextVersion(
            List<MavenProject> reactorProjects, String workingDirectory) throws Exception {
        ReleaseDescriptorBuilder builder = createDescriptorFromProjects(reactorProjects, workingDirectory);

        builder.addReleaseVersion("groupId:artifactId", RELEASE_VERSION);
        builder.addDevelopmentVersion("groupId:artifactId", NEXT_VERSION);
        builder.addReleaseVersion("groupId:subproject1", ALTERNATIVE_RELEASE_VERSION);
        builder.addDevelopmentVersion("groupId:subproject1", ALTERNATIVE_NEXT_VERSION);
        mapScm(builder);

        return builder;
    }

    @Override
    protected ReleaseDescriptorBuilder createConfigurationForWithParentNextVersion(
            List<MavenProject> reactorProjects, String workingDirectory) throws Exception {
        ReleaseDescriptorBuilder builder = createDescriptorFromProjects(reactorProjects, workingDirectory);

        builder.addReleaseVersion("groupId:artifactId", RELEASE_VERSION);
        builder.addDevelopmentVersion("groupId:artifactId", NEXT_VERSION);
        builder.addReleaseVersion("groupId:subproject1", RELEASE_VERSION);
        builder.addDevelopmentVersion("groupId:subproject1", NEXT_VERSION);
        mapScm(builder);

        return builder;
    }

    @Test
    void testRewriteBasicPomWithGit() throws Exception {

        List<MavenProject> reactorProjects = createReactorProjects("basic-pom-with-git");
        ReleaseDescriptorBuilder builder = createDescriptorFromProjects(reactorProjects, "basic-pom-with-git");
        mapNextVersion(builder, "groupId:artifactId");

        Scm scm = new Scm();
        scm.setConnection("${scm.base}:git://localhost/repo");
        scm.setDeveloperConnection("${scm.base}:git+ssh://${username}@localhost/tmp/repo");
        scm.setUrl("${baseUrl}/repo");
        builder.addOriginalScmInfo("groupId:artifactId", scm);

        String sourceUrl = "scm:git:git://localhost/repo";
        SvnScmProviderRepository scmProviderRepository = new SvnScmProviderRepository(sourceUrl);
        ScmRepository repository = new ScmRepository("git", scmProviderRepository);
        scmManager.addScmRepositoryForUrl("scm:git:git://localhost/repo", repository);

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        assertTrue(comparePomFiles(reactorProjects));
    }

    @Test
    void testRewriteBasicPomWithGitFromTag() throws Exception {

        List<MavenProject> reactorProjects = createReactorProjects("basic-pom-with-git-from-tag");
        ReleaseDescriptorBuilder builder = createDescriptorFromProjects(reactorProjects, "basic-pom-with-git-from-tag");
        mapNextVersion(builder, "groupId:artifactId");

        Scm scm = new Scm();
        scm.setConnection("scm:git:git://localhost/repo");
        scm.setDeveloperConnection("scm:git:git+ssh://${username}@localhost/tmp/repo");
        scm.setUrl("http://localhost/viewgit.cgi/repo");
        scm.setTag("original-label");
        builder.addOriginalScmInfo("groupId:artifactId", scm);

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        assertTrue(comparePomFiles(reactorProjects));
    }

    @Test
    void testRewriteBasicPomWithSvnFromTag() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects("basic-pom-with-svn-from-tag");
        ReleaseDescriptorBuilder builder = createDescriptorFromProjects(reactorProjects, "basic-pom-with-svn-from-tag");
        mapNextVersion(builder, "groupId:artifactId");

        Scm scm = new Scm();
        scm.setConnection("scm:svn:file://localhost/svnroot/trunk/");
        scm.setDeveloperConnection("scm:svn:file://localhost/svnroot/trunk/");
        scm.setUrl("http://localhost/svn");
        scm.setTag("trunk");
        builder.addOriginalScmInfo("groupId:artifactId", scm);

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        assertTrue(comparePomFiles(reactorProjects));
    }

    @Test
    void testRewriteBasicPomWithInheritedScm() throws Exception {

        List<MavenProject> reactorProjects = createReactorProjects("basic-pom-inherited-scm");
        ReleaseDescriptorBuilder builder = createDescriptorFromProjects(reactorProjects, "basic-pom-inherited-scm");

        builder.addReleaseVersion("groupId:artifactId", RELEASE_VERSION);
        builder.addDevelopmentVersion("groupId:artifactId", NEXT_VERSION);
        builder.addReleaseVersion("groupId:subproject1", RELEASE_VERSION);
        builder.addDevelopmentVersion("groupId:subproject1", NEXT_VERSION);
        builder.addReleaseVersion("groupId:subsubproject", RELEASE_VERSION);
        builder.addDevelopmentVersion("groupId:subsubproject", NEXT_VERSION);
        Scm scm = new Scm();
        scm.setConnection("scm:svn:file://localhost/tmp/scm-repo/trunk/subproject1");
        scm.setDeveloperConnection("scm:svn:file://localhost/tmp/scm-repo/trunk/subproject1");
        // MRELEASE-107
        scm.setUrl("http://localhost/viewvc/mypath/trunk/subproject1");
        builder.addOriginalScmInfo("groupId:subproject1", scm);
        builder.addOriginalScmInfo("groupId:subsubproject", null);

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        assertTrue(comparePomFiles(reactorProjects));
    }

    @Test
    void testRewritePomWithParentAndProperties() throws Exception {
        performTestRewritePomWithParentAndProperties("pom-with-parent-and-properties");
    }

    // MRELEASE-454
    @Test
    void testRewritePomWithParentAndPropertiesInDependencyManagement() throws Exception {
        performTestRewritePomWithParentAndProperties("pom-with-parent-and-properties-in-dependency-management");
    }

    // MRELEASE-454
    @Test
    void testRewritePomWithParentAndPropertiesInDependencyManagementImport() throws Exception {
        performTestRewritePomWithParentAndProperties("pom-with-parent-and-properties-in-dependency-management-import");
    }

    private void performTestRewritePomWithParentAndProperties(String path) throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects(path);

        ReleaseDescriptorBuilder builder = createDescriptorFromProjects(reactorProjects, path);
        builder.addReleaseVersion("groupId:artifactId", RELEASE_VERSION);
        builder.addDevelopmentVersion("groupId:artifactId", NEXT_VERSION);
        builder.addReleaseVersion("groupId:subproject1", ALTERNATIVE_RELEASE_VERSION);
        builder.addDevelopmentVersion("groupId:subproject1", ALTERNATIVE_NEXT_VERSION);
        builder.addReleaseVersion("groupId:subproject2", ALTERNATIVE_RELEASE_VERSION);
        builder.addDevelopmentVersion("groupId:subproject2", ALTERNATIVE_NEXT_VERSION);

        mapScm(builder);

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        assertTrue(comparePomFiles(reactorProjects));
    }

    @Test
    void testSimulateRewritePomWithParentAndProperties() throws Exception {
        // use the original ones since simulation didn't modify them
        List<MavenProject> reactorProjects = createReactorProjects("pom-with-parent-and-properties-sim");

        ReleaseDescriptorBuilder builder =
                createDescriptorFromProjects(reactorProjects, "pom-with-parent-and-properties-sim");
        builder.addReleaseVersion("groupId:artifactId", RELEASE_VERSION);
        builder.addDevelopmentVersion("groupId:artifactId", NEXT_VERSION);
        builder.addReleaseVersion("groupId:subproject1", ALTERNATIVE_RELEASE_VERSION);
        builder.addDevelopmentVersion("groupId:subproject1", ALTERNATIVE_NEXT_VERSION);
        builder.addReleaseVersion("groupId:subproject2", ALTERNATIVE_RELEASE_VERSION);
        builder.addDevelopmentVersion("groupId:subproject2", ALTERNATIVE_NEXT_VERSION);

        mapScm(builder);

        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        for (MavenProject project : reactorProjects) {
            File pomFile = project.getFile();
            File actualFile = new File(pomFile.getParentFile(), pomFile.getName() + ".next");
            File expectedFile = new File(actualFile.getParentFile(), "expected-pom.xml");

            comparePomFiles(expectedFile, actualFile, true, false);
        }
    }

    // MRELEASE-311
    @Test
    void testRewritePomWithDependencyPropertyCoordinate() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects("pom-with-property-dependency-coordinate");

        ReleaseDescriptorBuilder builder =
                createDescriptorFromProjects(reactorProjects, "pom-with-property-dependency-coordinate");
        builder.addReleaseVersion("groupId:artifactId", RELEASE_VERSION);
        builder.addDevelopmentVersion("groupId:artifactId", NEXT_VERSION);
        builder.addReleaseVersion("groupId:subproject1-3.4", ALTERNATIVE_RELEASE_VERSION);
        builder.addDevelopmentVersion("groupId:subproject1-3.4", ALTERNATIVE_NEXT_VERSION);
        builder.addReleaseVersion("groupId:subproject2", ALTERNATIVE_RELEASE_VERSION);
        builder.addDevelopmentVersion("groupId:subproject2", ALTERNATIVE_NEXT_VERSION);

        mapScm(builder);

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        assertTrue(comparePomFiles(reactorProjects));
    }

    @Test
    void testRewritePomDependenciesWithoutDependenciesVersionUpdate() throws Exception {
        List<MavenProject> reactorProjects =
                createReactorProjects("internal-snapshot-dependencies-without-dependencies-version-update");
        ReleaseDescriptorBuilder builder = createDefaultConfiguration(
                reactorProjects, "internal-snapshot-dependencies-without-dependencies-version-update");
        builder.setUpdateDependencies(false);
        mapNextVersion(builder, "groupId:subsubproject");

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        assertTrue(comparePomFiles(reactorProjects));
    }

    @Test
    void testRewritePomWithCiFriendlyReactor() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects("pom-with-parent-and-cifriendly-expressions");

        ReleaseDescriptorBuilder builder =
                createDescriptorFromProjects(reactorProjects, "pom-with-parent-and-cifriendly-expressions");
        builder.addReleaseVersion("groupId:artifactId", RELEASE_VERSION);
        builder.addDevelopmentVersion("groupId:artifactId", NEXT_VERSION);
        builder.addReleaseVersion("groupId:subproject1", RELEASE_VERSION);
        builder.addDevelopmentVersion("groupId:subproject1", NEXT_VERSION);

        mapScm(builder);

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        assertTrue(comparePomFiles(reactorProjects));
    }

    @Test
    void testRewritePomWithCiFriendlyReactorWithOnlyRevision() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects("pom-with-parent-and-cifriendly-revision");

        ReleaseDescriptorBuilder builder =
                createDescriptorFromProjects(reactorProjects, "pom-with-parent-and-cifriendly-revision");
        builder.addReleaseVersion("groupId:artifactId", RELEASE_VERSION);
        builder.addDevelopmentVersion("groupId:artifactId", NEXT_VERSION);
        builder.addReleaseVersion("groupId:subproject1", RELEASE_VERSION);
        builder.addDevelopmentVersion("groupId:subproject1", NEXT_VERSION);
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        assertTrue(comparePomFiles(reactorProjects));
    }
}
