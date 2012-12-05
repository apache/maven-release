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

import org.apache.maven.Maven;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmFile;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmFileStatus;
import org.apache.maven.scm.ScmTagParameters;
import org.apache.maven.scm.command.add.AddScmResult;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.manager.ScmManagerStub;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.util.ReleaseUtil;

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

    protected void setUp()
        throws Exception
    {
        super.setUp();

        phase = (ReleasePhase) lookup( ReleasePhase.ROLE, "generate-release-poms" );
        scmProviderMock = null;
    }

    // TODO: MRELEASE-262
//    public void testRewriteInternalRangeDependency() throws Exception
//    {
//        List reactorProjects = createReactorProjects( "internal-snapshot-range-dependency" );
//        ReleaseDescriptor config = createMappedConfiguration( reactorProjects );
//
//        phase.execute( config, null, reactorProjects );
//
//        compareFiles( reactorProjects );
//    }

    public void testRewriteExternalRangeDependency() throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "external-range-dependency" );
        ReleaseDescriptor config = createMappedConfiguration( reactorProjects );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        comparePomFiles( reactorProjects );
    }

    // MRELEASE-787
    public void testSuppressCommitBeforeTagOrBranch() throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "basic-pom" );
        ReleaseDescriptor config = new ReleaseDescriptor();
        config.setGenerateReleasePoms( true );
        config.setSuppressCommitBeforeTagOrBranch( true );
        config.setRemoteTagging( false );
        mapNextVersion( config, "groupId:artifactId" );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );
        
        verify( scmProviderMock ).add( isA(ScmRepository.class), isA(ScmFileSet.class) );

        verifyNoMoreInteractions( scmProviderMock );
    }
    
    public void testSuppressCommitBeforeTagOrBranchAndReomoteTagging() throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "basic-pom" );
        ReleaseDescriptor config = new ReleaseDescriptor();
        config.setGenerateReleasePoms( true );
        config.setSuppressCommitBeforeTagOrBranch( true );
        config.setRemoteTagging( true );
        mapNextVersion( config, "groupId:artifactId" );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        verify( scmProviderMock ).add( isA(ScmRepository.class), isA(ScmFileSet.class) );

        verifyNoMoreInteractions( scmProviderMock );
    }

    // MRELEASE-808
    public void testFinalName() throws Exception 
    {
        List<MavenProject> reactorProjects = createReactorProjects( "pom-with-finalname" );
        ReleaseDescriptor config = createConfigurationForWithParentNextVersion( reactorProjects );
        config.setGenerateReleasePoms( true );
        
        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    /*
     * @see org.apache.maven.shared.release.phase.AbstractRewritingReleasePhaseTestCase#createDescriptorFromProjects(java.util.List)
     */
    protected ReleaseDescriptor createDescriptorFromProjects( List<MavenProject> reactorProjects )
    {
        ReleaseDescriptor descriptor = super.createDescriptorFromProjects( reactorProjects );
        descriptor.setScmReleaseLabel( "release-label" );
        descriptor.setGenerateReleasePoms( true );
        return descriptor;
    }

    /*
     * @see org.apache.maven.shared.release.phase.AbstractRewritingReleasePhaseTestCase#createReactorProjects(java.lang.String,
     *      boolean)
     */
    protected List<MavenProject> prepareReactorProjects( String path, boolean copyFiles ) throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "generate-release-poms/", path );

        scmProviderMock = mock( ScmProvider.class );

        List<File> releasePoms = new ArrayList<File>();

        for ( Iterator<MavenProject> iterator = reactorProjects.iterator(); iterator.hasNext(); )
        {
            MavenProject project = iterator.next();

            releasePoms.add( ReleaseUtil.getReleasePom( project ) );
        }

        MavenProject rootProject = ReleaseUtil.getRootProject( reactorProjects );
        ScmFileSet fileSet = new ScmFileSet( rootProject.getFile().getParentFile(), releasePoms );

        when( scmProviderMock.add( isA( ScmRepository.class ), 
                                   argThat( new IsScmFileSetEquals( fileSet ) ) ) ).
                                       thenReturn( new AddScmResult( "...", 
                                                                     Collections.singletonList( new ScmFile( Maven.RELEASE_POMv4, ScmFileStatus.ADDED ) ) ) );
        
        
        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( scmProviderMock );

        return reactorProjects;
    }
    
    @Override
    protected void verifyReactorProjects( String path, boolean copyFiles ) throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "generate-release-poms/", path );
        
        List<File> releasePoms = new ArrayList<File>();

        for ( Iterator<MavenProject> iterator = reactorProjects.iterator(); iterator.hasNext(); )
        {
            MavenProject project = iterator.next();

            releasePoms.add( ReleaseUtil.getReleasePom( project ) );
        }
        
        MavenProject rootProject = ReleaseUtil.getRootProject( reactorProjects );
        ScmFileSet fileSet = new ScmFileSet( rootProject.getFile().getParentFile(), releasePoms );
        
        verify( scmProviderMock ).add( isA( ScmRepository.class ), 
                                       argThat( new IsScmFileSetEquals( fileSet ) ) );
        verifyNoMoreInteractions( scmProviderMock );
    }

    /*
     * @see org.apache.maven.shared.release.phase.AbstractRewritingReleasePhaseTestCase#mapNextVersion(org.apache.maven.shared.release.config.ReleaseDescriptor,
     *      java.lang.String)
     */
    protected void mapNextVersion( ReleaseDescriptor config, String projectId )
    {
        config.mapReleaseVersion( projectId, NEXT_VERSION );
    }

    /*
     * @see org.apache.maven.shared.release.phase.AbstractRewritingReleasePhaseTestCase#mapAlternateNextVersion(org.apache.maven.shared.release.config.ReleaseDescriptor,
     *      java.lang.String)
     */
    protected void mapAlternateNextVersion( ReleaseDescriptor config, String projectId )
    {
        config.mapReleaseVersion( projectId, ALTERNATIVE_NEXT_VERSION );
    }

    /*
     * @see org.apache.maven.shared.release.phase.AbstractRewritingReleasePhaseTestCase#unmapNextVersion(org.apache.maven.shared.release.config.ReleaseDescriptor,
     *      java.lang.String)
     */
    protected void unmapNextVersion( ReleaseDescriptor config, String projectId )
    {
        // nothing to do
    }

    /*
     * @see org.apache.maven.shared.release.phase.AbstractRewritingReleasePhaseTestCase#createConfigurationForPomWithParentAlternateNextVersion(java.util.List)
     */
    protected ReleaseDescriptor createConfigurationForPomWithParentAlternateNextVersion( List<MavenProject> reactorProjects )
        throws Exception
    {
        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );

        config.mapReleaseVersion( "groupId:artifactId", NEXT_VERSION );
        config.mapReleaseVersion( "groupId:subproject1", ALTERNATIVE_NEXT_VERSION );

        return config;
    }

    /*
     * @see org.apache.maven.shared.release.phase.AbstractRewritingReleasePhaseTestCase#createConfigurationForWithParentNextVersion(java.util.List)
     */
    protected ReleaseDescriptor createConfigurationForWithParentNextVersion( List<MavenProject> reactorProjects ) throws Exception
    {
        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );

        config.mapReleaseVersion( "groupId:artifactId", NEXT_VERSION );
        config.mapReleaseVersion( "groupId:subproject1", NEXT_VERSION );

        return config;
    }

    /*
     * @see org.apache.maven.shared.release.phase.AbstractRewritingReleasePhaseTestCase#readTestProjectFile(java.lang.String)
     */
    protected String readTestProjectFile( String fileName ) throws IOException
    {
        return ReleaseUtil.readXmlFile( getTestFile( "target/test-classes/projects/generate-release-poms/" + fileName ) );
    }

    /*
     * @see org.apache.maven.shared.release.phase.AbstractReleaseTestCase#compareFiles(org.apache.maven.project.MavenProject, java.lang.String)
     */
    // @Override
    protected void comparePomFiles( MavenProject project, String expectedFileSuffix, boolean normalizeLineEndings )
        throws IOException
    {
        File actualFile = ReleaseUtil.getReleasePom( project );
        File expectedFile = new File( actualFile.getParentFile(), "expected-release-pom" + expectedFileSuffix + ".xml" );

        comparePomFiles( expectedFile, actualFile, normalizeLineEndings );
    }
}
