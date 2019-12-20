package org.apache.maven.shared.release.phase;

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

import static org.junit.Assert.assertTrue;

import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmFile;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmFileStatus;
import org.apache.maven.scm.command.add.AddScmResult;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.manager.ScmManagerStub;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.apache.maven.shared.release.config.ReleaseUtils;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.junit.Test;

/**
 * Test the generate release POMs phase.
 *
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 */
public class GenerateReleasePomsPhaseTest
    extends AbstractRewritingReleasePhaseTestCase
{
    private static final String NEXT_VERSION = "1.0";

    private static final String ALTERNATIVE_NEXT_VERSION = "2.0";

    private ScmProvider scmProviderMock;

    public GenerateReleasePomsPhaseTest( String modelETL )
    {
        super( modelETL );
    }

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        scmProviderMock = null;
    }

    @Override
    protected String getRoleHint()
    {
        return "generate-release-poms";
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
    public void testRewriteExternalRangeDependency()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "external-range-dependency" );
        ReleaseDescriptorBuilder builder = createMappedConfiguration( reactorProjects, "external-range-dependency" );

        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        comparePomFiles( reactorProjects );
    }

    // MRELEASE-787
    @Test
    public void testSuppressCommitBeforeTagOrBranch()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "basic-pom" );
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setGenerateReleasePoms( true );
        builder.setSuppressCommitBeforeTagOrBranch( true );
        builder.setRemoteTagging( false );
        builder.setPinExternals( false );
        mapNextVersion( builder, "groupId:artifactId" );

        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        verify( scmProviderMock ).add( isA( ScmRepository.class ), isA( ScmFileSet.class ) );

        verifyNoMoreInteractions( scmProviderMock );
    }

    @Test
    public void testSuppressCommitBeforeTagOrBranchAndReomoteTagging()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "basic-pom" );
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setGenerateReleasePoms( true );
        builder.setSuppressCommitBeforeTagOrBranch( true );
        builder.setRemoteTagging( true );
        builder.setPinExternals( false );
        mapNextVersion( builder, "groupId:artifactId" );

        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        verify( scmProviderMock ).add( isA( ScmRepository.class ), isA( ScmFileSet.class ) );

        verifyNoMoreInteractions( scmProviderMock );
    }

    // MRELEASE-808
    @Test
    public void testFinalName()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "pom-with-finalname" );
        ReleaseDescriptorBuilder builder = createConfigurationForWithParentNextVersion( reactorProjects, "pom-with-finalname" );
        builder.setGenerateReleasePoms( true );

        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    /*
     * @see
     * org.apache.maven.shared.release.phase.AbstractRewritingReleasePhaseTestCase#createDescriptorFromProjects(java.
     * util.List)
     */
    @Override
    protected ReleaseDescriptorBuilder createDescriptorFromProjects( List<MavenProject> reactorProjects, String workingDirectory )
    {
        ReleaseDescriptorBuilder builder = super.createDescriptorFromProjects( reactorProjects, workingDirectory );
        builder.setScmReleaseLabel( "release-label" );
        builder.setGenerateReleasePoms( true );
        return builder;
    }

    /*
     * @see org.apache.maven.shared.release.phase.AbstractRewritingReleasePhaseTestCase#createReactorProjects(java.lang.
     * String, boolean)
     */
    @Override
    protected List<MavenProject> prepareReactorProjects( String path )
        throws Exception
    {
        String dir = "generate-release-poms/" + path;
        List<MavenProject> reactorProjects = createReactorProjects( dir, dir, null );

        scmProviderMock = mock( ScmProvider.class );

        List<File> releasePoms = new ArrayList<>();

        for ( MavenProject project : reactorProjects )
        {
            releasePoms.add( ReleaseUtil.getReleasePom( project ) );
        }

        MavenProject rootProject = ReleaseUtil.getRootProject( reactorProjects );
        ScmFileSet fileSet = new ScmFileSet( rootProject.getFile().getParentFile(), releasePoms );

        when( scmProviderMock.add( isA( ScmRepository.class ),
               argThat( new IsScmFileSetEquals( fileSet ) ) ) ).thenReturn( new AddScmResult( "...",
                              Collections.singletonList( new ScmFile( "pom.xml", ScmFileStatus.ADDED ) ) ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.class );
        stub.setScmProvider( scmProviderMock );

        return reactorProjects;
    }

    @Override
    protected void verifyReactorProjects( String path, boolean copyFiles )
        throws Exception
    {
        String dir = "generate-release-poms/"+ path;
        List<MavenProject> reactorProjects = createReactorProjects( dir, dir, null );

        List<File> releasePoms = new ArrayList<>();

        for ( Iterator<MavenProject> iterator = reactorProjects.iterator(); iterator.hasNext(); )
        {
            MavenProject project = iterator.next();

            releasePoms.add( ReleaseUtil.getReleasePom( project ) );
        }

        MavenProject rootProject = ReleaseUtil.getRootProject( reactorProjects );
        ScmFileSet fileSet = new ScmFileSet( rootProject.getFile().getParentFile(), releasePoms );

        verify( scmProviderMock ).add( isA( ScmRepository.class ), argThat( new IsScmFileSetEquals( fileSet ) ) );
        verifyNoMoreInteractions( scmProviderMock );
    }

    @Override
    protected void mapNextVersion( ReleaseDescriptorBuilder config, String projectId )
    {
        config.addReleaseVersion( projectId, NEXT_VERSION );
    }

    /*
     * @see
     * org.apache.maven.shared.release.phase.AbstractRewritingReleasePhaseTestCase#mapAlternateNextVersion(org.apache.
     * maven.shared.release.config.ReleaseDescriptor, java.lang.String)
     */
    @Override
    protected void mapAlternateNextVersion( ReleaseDescriptorBuilder config, String projectId )
    {
        config.addReleaseVersion( projectId, ALTERNATIVE_NEXT_VERSION );
    }

    /*
     * @see
     * org.apache.maven.shared.release.phase.AbstractRewritingReleasePhaseTestCase#unmapNextVersion(org.apache.maven.
     * shared.release.config.ReleaseDescriptor, java.lang.String)
     */
    @Override
    protected void unmapNextVersion( ReleaseDescriptorBuilder config, String projectId )
    {
        // nothing to do
    }

    /*
     * @see org.apache.maven.shared.release.phase.AbstractRewritingReleasePhaseTestCase#
     * createConfigurationForPomWithParentAlternateNextVersion(java.util.List)
     */
    @Override
    protected ReleaseDescriptorBuilder createConfigurationForPomWithParentAlternateNextVersion( List<MavenProject> reactorProjects, String workingDirectory )
        throws Exception
    {
        ReleaseDescriptorBuilder builder = createDescriptorFromProjects( reactorProjects, workingDirectory );

        builder.addReleaseVersion( "groupId:artifactId", NEXT_VERSION );
        builder.addReleaseVersion( "groupId:subproject1", ALTERNATIVE_NEXT_VERSION );

        return builder;
    }

    /*
     * @see org.apache.maven.shared.release.phase.AbstractRewritingReleasePhaseTestCase#
     * createConfigurationForWithParentNextVersion(java.util.List)
     */
    @Override
    protected ReleaseDescriptorBuilder createConfigurationForWithParentNextVersion( List<MavenProject> reactorProjects, String workingDirectory )
        throws Exception
    {
        ReleaseDescriptorBuilder builder = createDescriptorFromProjects( reactorProjects, workingDirectory );

        builder.addReleaseVersion( "groupId:artifactId", NEXT_VERSION );
        builder.addReleaseVersion( "groupId:subproject1", NEXT_VERSION );

        return builder;
    }

    /*
     * @see
     * org.apache.maven.shared.release.phase.AbstractRewritingReleasePhaseTestCase#readTestProjectFile(java.lang.String)
     */
    @Override
    protected String readTestProjectFile( String fileName )
        throws IOException
    {
        return ReleaseUtil.readXmlFile( getTestFile( "target/test-classes/projects/generate-release-poms/"
            + fileName ) );
    }

    /*
     * @see
     * org.apache.maven.shared.release.phase.AbstractReleaseTestCase#compareFiles(org.apache.maven.project.MavenProject,
     * java.lang.String)
     */
    // @Override
    @Override
    protected void comparePomFiles( MavenProject project, String expectedFileSuffix, boolean normalizeLineEndings )
        throws IOException
    {
        File actualFile = ReleaseUtil.getReleasePom( project );
        File expectedFile =
            new File( actualFile.getParentFile(), "expected-release-pom" + expectedFileSuffix + ".xml" );

        comparePomFiles( expectedFile, actualFile, normalizeLineEndings, true );
    }
}
