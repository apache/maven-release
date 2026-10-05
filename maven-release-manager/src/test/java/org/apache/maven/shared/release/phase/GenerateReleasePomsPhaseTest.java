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
import java.io.Reader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.model.Model;
import org.apache.maven.model.interpolation.ModelInterpolator;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.superpom.SuperPomProvider;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmFile;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmFileStatus;
import org.apache.maven.scm.command.add.AddScmResult;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.apache.maven.shared.release.config.ReleaseUtils;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.scm.ScmRepositoryConfigurator;
import org.apache.maven.shared.release.scm.ScmTranslator;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.codehaus.plexus.testing.PlexusTest;
import org.junit.jupiter.api.Test;

import static org.codehaus.plexus.testing.PlexusExtension.getTestFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Test the generate release POMs phase.
 *
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 */
@PlexusTest
class GenerateReleasePomsPhaseTest extends AbstractRewritingReleasePhaseTestCase {
    private static final String NEXT_VERSION = "1.0";

    private static final String ALTERNATIVE_NEXT_VERSION = "2.0";

    private ScmProvider scmProviderMock;

    @Inject
    @Named("generate-release-poms")
    private ReleasePhase phase;

    @Inject
    private ScmRepositoryConfigurator scmRepositoryConfigurator;

    @Inject
    private SuperPomProvider superPomProvider;

    @Inject
    private ModelInterpolator modelInterpolator;

    @Inject
    private Map<String, ScmTranslator> scmTranslators;

    @Override
    protected ReleasePhase getTestedPhase() {
        return phase;
    }

    // TODO: MRELEASE-262
    // @Test public void testRewriteInternalRangeDependency() throws Exception
    // {
    // List reactorProjects = createReactorProjects( "internal-snapshot-range-dependency" );
    // ReleaseDescriptor config = createMappedConfiguration( reactorProjects );
    //
    // phase.execute( config, null, reactorProjects );
    //
    // compareFiles( reactorProjects );
    // }

    @Test
    void testRewriteExternalRangeDependency() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects("external-range-dependency");
        ReleaseDescriptorBuilder builder = createMappedConfiguration(reactorProjects, "external-range-dependency");

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        comparePomFiles(reactorProjects);
    }

    // MRELEASE-787
    @Test
    void testSuppressCommitBeforeTagOrBranch() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects("basic-pom");
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setGenerateReleasePoms(true);
        builder.setSuppressCommitBeforeTagOrBranch(true);
        builder.setRemoteTagging(false);
        builder.setPinExternals(false);
        mapNextVersion(builder, "groupId:artifactId");

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        verify(scmProviderMock).add(isA(ScmRepository.class), isA(ScmFileSet.class));

        verifyNoMoreInteractions(scmProviderMock);
    }

    @Test
    void testSuppressCommitBeforeTagOrBranchAndReomoteTagging() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects("basic-pom");
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setGenerateReleasePoms(true);
        builder.setSuppressCommitBeforeTagOrBranch(true);
        builder.setRemoteTagging(true);
        builder.setPinExternals(false);
        mapNextVersion(builder, "groupId:artifactId");

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        verify(scmProviderMock).add(isA(ScmRepository.class), isA(ScmFileSet.class));

        verifyNoMoreInteractions(scmProviderMock);
    }

    // MRELEASE-808
    @Test
    void testFinalName() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects("pom-with-finalname");
        ReleaseDescriptorBuilder builder =
                createConfigurationForWithParentNextVersion(reactorProjects, "pom-with-finalname");
        builder.setGenerateReleasePoms(true);

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        comparePomFiles(reactorProjects);
    }

    // Maven 3.10 and Maven 4 declare these in the super POM (apache/maven#12032, MNG-8258)
    private static final String OUTPUT_TIMESTAMP = "project.build.outputTimestamp";

    private static final String SOURCE_ENCODING = "project.build.sourceEncoding";

    private static final String OUTPUT_ENCODING = "project.reporting.outputEncoding";

    private static final String SUPER_POM_TIMESTAMP = "1980-02-01T00:00:00Z";

    @Test
    void testSuperPomPropertiesAreNotWrittenToReleasePom() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects("pom-with-parent");
        addSuperPomPropertiesToEffectiveModels(reactorProjects);

        executePhaseWithSuperPomProperties(reactorProjects);

        for (MavenProject project : reactorProjects) {
            Properties properties = readReleasePom(project).getProperties();
            assertFalse(properties.containsKey(SOURCE_ENCODING), project.getArtifactId());
            assertFalse(properties.containsKey(OUTPUT_ENCODING), project.getArtifactId());
            assertFalse(properties.containsKey(OUTPUT_TIMESTAMP), project.getArtifactId());
        }
    }

    @Test
    void testPropertyDeclaredByProjectIsKeptInReleasePom() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects("pom-with-parent");
        addSuperPomPropertiesToEffectiveModels(reactorProjects);
        MavenProject sub = reactorProjects.get(1);
        // declared by the project itself, with the same value as the super POM default
        sub.getOriginalModel().addProperty(OUTPUT_TIMESTAMP, SUPER_POM_TIMESTAMP);

        executePhaseWithSuperPomProperties(reactorProjects);

        Properties subProperties = readReleasePom(sub).getProperties();
        assertEquals(SUPER_POM_TIMESTAMP, subProperties.getProperty(OUTPUT_TIMESTAMP));
        assertFalse(subProperties.containsKey(SOURCE_ENCODING));
        assertFalse(readReleasePom(reactorProjects.get(0)).getProperties().containsKey(OUTPUT_TIMESTAMP));
    }

    @Test
    void testPropertyDeclaredByParentIsKeptInReleasePom() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects("pom-with-parent");
        addSuperPomPropertiesToEffectiveModels(reactorProjects);
        MavenProject root = reactorProjects.get(0);
        MavenProject sub = reactorProjects.get(1);
        root.getOriginalModel().addProperty(OUTPUT_TIMESTAMP, SUPER_POM_TIMESTAMP);
        sub.setParent(root);

        executePhaseWithSuperPomProperties(reactorProjects);

        assertEquals(SUPER_POM_TIMESTAMP, readReleasePom(root).getProperties().getProperty(OUTPUT_TIMESTAMP));
        assertEquals(SUPER_POM_TIMESTAMP, readReleasePom(sub).getProperties().getProperty(OUTPUT_TIMESTAMP));
        assertFalse(readReleasePom(sub).getProperties().containsKey(SOURCE_ENCODING));
    }

    private static Properties superPomProperties() {
        Properties properties = new Properties();
        properties.setProperty(SOURCE_ENCODING, "UTF-8");
        properties.setProperty(OUTPUT_ENCODING, "UTF-8");
        properties.setProperty(OUTPUT_TIMESTAMP, SUPER_POM_TIMESTAMP);
        return properties;
    }

    /** Mimics Maven 3.10, where the effective model inherits the super POM properties. */
    private static void addSuperPomPropertiesToEffectiveModels(List<MavenProject> reactorProjects) {
        for (MavenProject project : reactorProjects) {
            project.getModel().getProperties().putAll(superPomProperties());
        }
    }

    private void executePhaseWithSuperPomProperties(List<MavenProject> reactorProjects) throws Exception {
        // Maven 3.9's super POM has no properties, so substitute one that looks like Maven 3.10's
        SuperPomProvider provider = modelVersion -> {
            Model superModel = superPomProvider.getSuperModel(modelVersion).clone();
            superModel.getProperties().putAll(superPomProperties());
            return superModel;
        };
        ReleasePhase superPomPhase =
                new GenerateReleasePomsPhase(scmRepositoryConfigurator, provider, modelInterpolator, scmTranslators);

        ReleaseDescriptorBuilder builder =
                createConfigurationForWithParentNextVersion(reactorProjects, "pom-with-parent");
        builder.setGenerateReleasePoms(true);

        superPomPhase.execute(
                ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);
    }

    private static Model readReleasePom(MavenProject project) throws Exception {
        try (Reader reader =
                Files.newBufferedReader(ReleaseUtil.getReleasePom(project).toPath())) {
            return new MavenXpp3Reader().read(reader);
        }
    }

    /*
     * @see
     * org.apache.maven.shared.release.phase.AbstractRewritingReleasePhaseTestCase#createDescriptorFromProjects(java.
     * util.List)
     */
    @Override
    protected ReleaseDescriptorBuilder createDescriptorFromProjects(
            List<MavenProject> reactorProjects, String workingDirectory) {
        ReleaseDescriptorBuilder builder = super.createDescriptorFromProjects(reactorProjects, workingDirectory);
        builder.setScmReleaseLabel("release-label");
        builder.setGenerateReleasePoms(true);
        return builder;
    }

    /*
     * @see org.apache.maven.shared.release.phase.AbstractRewritingReleasePhaseTestCase#createReactorProjects(java.lang.
     * String, boolean)
     */
    @Override
    protected List<MavenProject> prepareReactorProjects(String path) throws Exception {
        String dir = "generate-release-poms/" + path;
        List<MavenProject> reactorProjects = createReactorProjects(dir, dir, null);

        scmProviderMock = mock(ScmProvider.class);

        List<File> releasePoms = new ArrayList<>();

        for (MavenProject project : reactorProjects) {
            releasePoms.add(ReleaseUtil.getReleasePom(project));
        }

        MavenProject rootProject = ReleaseUtil.getRootProject(reactorProjects);
        ScmFileSet fileSet = new ScmFileSet(rootProject.getFile().getParentFile(), releasePoms);

        when(scmProviderMock.add(isA(ScmRepository.class), argThat(new IsScmFileSetEquals(fileSet))))
                .thenReturn(new AddScmResult(
                        "...", Collections.singletonList(new ScmFile("pom.xml", ScmFileStatus.ADDED))));

        scmManager.setScmProvider(scmProviderMock);

        return reactorProjects;
    }

    @Override
    protected void verifyReactorProjects(String path, boolean copyFiles) throws Exception {
        String dir = "generate-release-poms/" + path;
        List<MavenProject> reactorProjects = createReactorProjects(dir, dir, null);

        List<File> releasePoms = new ArrayList<>();

        for (Iterator<MavenProject> iterator = reactorProjects.iterator(); iterator.hasNext(); ) {
            MavenProject project = iterator.next();

            releasePoms.add(ReleaseUtil.getReleasePom(project));
        }

        MavenProject rootProject = ReleaseUtil.getRootProject(reactorProjects);
        ScmFileSet fileSet = new ScmFileSet(rootProject.getFile().getParentFile(), releasePoms);

        verify(scmProviderMock).add(isA(ScmRepository.class), argThat(new IsScmFileSetEquals(fileSet)));
        verifyNoMoreInteractions(scmProviderMock);
    }

    @Override
    protected void mapNextVersion(ReleaseDescriptorBuilder config, String projectId) {
        config.addReleaseVersion(projectId, NEXT_VERSION);
    }

    /*
     * @see
     * org.apache.maven.shared.release.phase.AbstractRewritingReleasePhaseTestCase#mapAlternateNextVersion(org.apache.
     * maven.shared.release.config.ReleaseDescriptor, java.lang.String)
     */
    @Override
    protected void mapAlternateNextVersion(ReleaseDescriptorBuilder config, String projectId) {
        config.addReleaseVersion(projectId, ALTERNATIVE_NEXT_VERSION);
    }

    /*
     * @see
     * org.apache.maven.shared.release.phase.AbstractRewritingReleasePhaseTestCase#unmapNextVersion(org.apache.maven.
     * shared.release.config.ReleaseDescriptor, java.lang.String)
     */
    @Override
    protected void unmapNextVersion(ReleaseDescriptorBuilder config, String projectId) {
        // nothing to do
    }

    /*
     * @see org.apache.maven.shared.release.phase.AbstractRewritingReleasePhaseTestCase#
     * createConfigurationForPomWithParentAlternateNextVersion(java.util.List)
     */
    @Override
    protected ReleaseDescriptorBuilder createConfigurationForPomWithParentAlternateNextVersion(
            List<MavenProject> reactorProjects, String workingDirectory) throws Exception {
        ReleaseDescriptorBuilder builder = createDescriptorFromProjects(reactorProjects, workingDirectory);

        builder.addReleaseVersion("groupId:artifactId", NEXT_VERSION);
        builder.addReleaseVersion("groupId:subproject1", ALTERNATIVE_NEXT_VERSION);

        return builder;
    }

    /*
     * @see org.apache.maven.shared.release.phase.AbstractRewritingReleasePhaseTestCase#
     * createConfigurationForWithParentNextVersion(java.util.List)
     */
    @Override
    protected ReleaseDescriptorBuilder createConfigurationForWithParentNextVersion(
            List<MavenProject> reactorProjects, String workingDirectory) throws Exception {
        ReleaseDescriptorBuilder builder = createDescriptorFromProjects(reactorProjects, workingDirectory);

        builder.addReleaseVersion("groupId:artifactId", NEXT_VERSION);
        builder.addReleaseVersion("groupId:subproject1", NEXT_VERSION);

        return builder;
    }

    /*
     * @see
     * org.apache.maven.shared.release.phase.AbstractRewritingReleasePhaseTestCase#readTestProjectFile(java.lang.String)
     */
    @Override
    protected String readTestProjectFile(String fileName) throws IOException {
        return ReleaseUtil.readXmlFile(getTestFile("target/test-classes/projects/generate-release-poms/" + fileName));
    }

    /*
     * @see
     * org.apache.maven.shared.release.phase.AbstractReleaseTestCase#compareFiles(org.apache.maven.project.MavenProject,
     * java.lang.String)
     */
    // @Override
    @Override
    protected void comparePomFiles(MavenProject project, String expectedFileSuffix, boolean normalizeLineEndings)
            throws IOException {
        File actualFile = ReleaseUtil.getReleasePom(project);
        File expectedFile = new File(actualFile.getParentFile(), "expected-release-pom" + expectedFileSuffix + ".xml");

        comparePomFiles(expectedFile, actualFile, normalizeLineEndings, true);
    }
}
