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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.manager.ScmManagerStub;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.scm.DefaultScmRepositoryConfigurator;
import org.apache.maven.shared.release.scm.ReleaseScmRepositoryException;
import org.apache.maven.shared.release.scm.ScmRepositoryConfigurator;
import org.apache.maven.shared.release.util.ReleaseUtil;

/**
 * Base class with tests for rewriting POMs.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class AbstractRewritingReleasePhaseTestCase
    extends AbstractReleaseTestCase
{
    public void testRewriteBasicPom()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjectsFromBasicPom();
        ReleaseDescriptor config = createDescriptorFromBasicPom( reactorProjects );
        mapNextVersion( config, "groupId:artifactId" );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    public void testRewriteBasicPomEntities()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "basic-pom-entities" );
        ReleaseDescriptor config = createDescriptorFromBasicPom( reactorProjects );
        mapNextVersion( config, "groupId:artifactId" );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    public void testRewriteBasicPomNamespace()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "basic-pom-namespace" );
        ReleaseDescriptor config = createDescriptorFromBasicPom( reactorProjects );
        mapNextVersion( config, "groupId:artifactId" );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    public void testRewriteBasicPomWithEncoding()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "basic-pom-with-encoding" );
        ReleaseDescriptor config = createDescriptorFromBasicPom( reactorProjects );
        mapNextVersion( config, "groupId:artifactId" );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    public void testRewritePomWithParent()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "pom-with-parent" );
        ReleaseDescriptor config = createConfigurationForPomWithParentAlternateNextVersion( reactorProjects );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    public void testRewritePomWithUnmappedParent()
        throws Exception
    {

        List<MavenProject> reactorProjects = createReactorProjects( "pom-with-parent" );
        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );

        // Process the child first
        reactorProjects = new ArrayList<MavenProject>( reactorProjects );
        Collections.reverse( reactorProjects );

        mapAlternateNextVersion( config, "groupId:subproject1" );

        try
        {
            phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testRewritePomWithReleasedParent()
        throws Exception
    {

        List<MavenProject> reactorProjects = createReactorProjects( "pom-with-released-parent" );
        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );

        mapAlternateNextVersion( config, "groupId:subproject1" );
        config.mapReleaseVersion( "groupId:artifactId", "1" );
        config.mapDevelopmentVersion( "groupId:artifactId", "1" );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    protected abstract void mapAlternateNextVersion( ReleaseDescriptor config, String projectId );

    public void testRewritePomWithInheritedVersion()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "pom-with-inherited-version" );
        ReleaseDescriptor config = createConfigurationForWithParentNextVersion( reactorProjects );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    public void testRewritePomWithChangedInheritedVersion()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "pom-with-inherited-version" );
        ReleaseDescriptor config = createConfigurationForPomWithParentAlternateNextVersion( reactorProjects );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        MavenProject project = (MavenProject) getProjectsAsMap( reactorProjects ).get( "groupId:subproject1" );
        comparePomFiles( project, "-version-changed" );
    }

    protected abstract ReleaseDescriptor createConfigurationForPomWithParentAlternateNextVersion( List<MavenProject> reactorProjects )
        throws Exception;

    public void testRewritePomDependencies()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-snapshot-dependencies" );
        ReleaseDescriptor config = createDefaultConfiguration( reactorProjects );
        mapNextVersion( config, "groupId:subsubproject" );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    public void testRewritePomUnmappedDependencies()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-snapshot-dependencies" );
        ReleaseDescriptor config = createUnmappedConfiguration( reactorProjects );

        try
        {
            phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testRewritePomDependenciesDifferentVersion()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-differing-snapshot-dependencies" );
        ReleaseDescriptor config = createDifferingVersionConfiguration( reactorProjects );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    public void testRewriteManagedPomDependencies()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-managed-snapshot-dependency" );
        ReleaseDescriptor config = createMappedConfiguration( reactorProjects );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    public void testRewriteManagedPomUnmappedDependencies()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-managed-snapshot-dependency" );
        ReleaseDescriptor config = createUnmappedConfiguration( reactorProjects );

        try
        {
            phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testRewritePomPlugins()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-snapshot-plugins" );
        ReleaseDescriptor config = createDefaultConfiguration( reactorProjects );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    public void testRewritePomUnmappedPlugins()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-snapshot-plugins" );
        ReleaseDescriptor config = createUnmappedConfiguration( reactorProjects );

        try
        {
            phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testRewritePomPluginsDifferentVersion()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-differing-snapshot-plugins" );
        ReleaseDescriptor config = createDifferingVersionConfiguration( reactorProjects );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    public void testRewriteManagedPomPlugins()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-managed-snapshot-plugin" );
        ReleaseDescriptor config = createMappedConfiguration( reactorProjects );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    public void testRewriteManagedPomUnmappedPlugins()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-managed-snapshot-plugin" );
        ReleaseDescriptor config = createUnmappedConfiguration( reactorProjects );

        try
        {
            phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testRewritePomReportPlugins()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-snapshot-report-plugins" );
        ReleaseDescriptor config = createDefaultConfiguration( reactorProjects );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    public void testRewritePomUnmappedReportPlugins()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-snapshot-report-plugins" );
        ReleaseDescriptor config = createUnmappedConfiguration( reactorProjects );

        try
        {
            phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testRewritePomReportPluginsDifferentVersion()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-differing-snapshot-report-plugins" );
        ReleaseDescriptor config = createDifferingVersionConfiguration( reactorProjects );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    public void testRewritePomExtension()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-snapshot-extension" );
        ReleaseDescriptor config = createDefaultConfiguration( reactorProjects );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    public void testRewritePomUnmappedExtension()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-snapshot-extension" );
        ReleaseDescriptor config = createUnmappedConfiguration( reactorProjects );

        try
        {
            phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testRewritePomExtensionDifferentVersion()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-differing-snapshot-extension" );
        ReleaseDescriptor config = createDifferingVersionConfiguration( reactorProjects );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    public void testRewritePomExtensionUndefinedVersion()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "pom-without-extension-version" );
        ReleaseDescriptor config = createDefaultConfiguration( reactorProjects );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    public void testRewriteAddSchema()
        throws Exception
    {
        boolean copyFiles = true;

        // Run a second time to check they are not duplicated
        for ( int i = 0; i < 2; i++ )
        {
            String path = "basic-pom";
            List<MavenProject> reactorProjects = prepareReactorProjects( path, copyFiles );
            ReleaseDescriptor config = createDescriptorFromBasicPom( reactorProjects );
            mapNextVersion( config, "groupId:artifactId" );
            config.setAddSchema( true );

            phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

            comparePomFiles( reactorProjects, "-with-schema" );

            copyFiles = false;
            
            verifyReactorProjects( path, copyFiles );
        }
    }

    public void testSimulateRewriteEditModeSkipped()
        throws Exception
    {
        // prepare
        List<MavenProject> reactorProjects = createReactorProjectsFromBasicPom();
        ReleaseDescriptor config = createDescriptorFromBasicPom( reactorProjects );
        config.setScmUseEditMode( true );
        mapNextVersion( config, "groupId:artifactId" );

        ScmProvider scmProviderMock = mock( ScmProvider.class );

        ScmManagerStub scmManager = new ScmManagerStub();
        DefaultScmRepositoryConfigurator configurator =
            (DefaultScmRepositoryConfigurator) lookup( ScmRepositoryConfigurator.ROLE );
        configurator.setScmManager( scmManager );
        scmManager.setScmProvider( scmProviderMock );

        // execute
        phase.simulate( config,  new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        verifyNoMoreInteractions( scmProviderMock );
    }

    public void testRewriteUnmappedPom()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjectsFromBasicPom();
        ReleaseDescriptor config = createDescriptorFromBasicPom( reactorProjects );

        try
        {
            phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testRewriteBasicPomWithScmRepoException()
        throws Exception
    {
        // prepare
        List<MavenProject> reactorProjects = createReactorProjectsFromBasicPom();
        ReleaseDescriptor config = createDescriptorFromBasicPom( reactorProjects );
        config.setScmUseEditMode( true );
        mapNextVersion( config, "groupId:artifactId" );

        ScmManager scmManagerMock = mock( ScmManager.class );
        when( scmManagerMock.makeScmRepository( config.getScmSourceUrl() ) ).thenThrow( new ScmRepositoryException( "..." ) );

        DefaultScmRepositoryConfigurator configurator =
            (DefaultScmRepositoryConfigurator) lookup( ScmRepositoryConfigurator.ROLE );
        configurator.setScmManager( scmManagerMock );

        try
        {
            phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseScmRepositoryException e )
        {
            assertNull( "Check no additional cause", e.getCause() );
        }
        
        verify( scmManagerMock ).makeScmRepository( config.getScmSourceUrl() );
        verifyNoMoreInteractions( scmManagerMock );
    }

    public void testRewriteBasicPomWithNoSuchProviderException()
        throws Exception
    {
        // prepare
        List<MavenProject> reactorProjects = createReactorProjectsFromBasicPom();
        ReleaseDescriptor config = createDescriptorFromBasicPom( reactorProjects );
        config.setScmUseEditMode( true );
        mapNextVersion( config, "groupId:artifactId" );

        ScmManager scmManagerMock = mock( ScmManager.class );
        when( scmManagerMock.makeScmRepository( config.getScmSourceUrl() ) ).thenThrow( new NoSuchScmProviderException( "..." ) );

        DefaultScmRepositoryConfigurator configurator =
            (DefaultScmRepositoryConfigurator) lookup( ScmRepositoryConfigurator.ROLE );
        configurator.setScmManager( scmManagerMock );

        // execute
        try
        {
            phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertEquals( "Check cause", NoSuchScmProviderException.class, e.getCause().getClass() );
        }
        
        // verify
        verify( scmManagerMock ).makeScmRepository( config.getScmSourceUrl() );
        verifyNoMoreInteractions( scmManagerMock );
    }

    public void testRewriteWhitespaceAroundValues()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "whitespace-around-values" );
        ReleaseDescriptor config = createConfigurationForPomWithParentAlternateNextVersion( reactorProjects );
        mapNextVersion( config, "groupId:subproject2" );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    public void testRewriteCommentsAroundValues()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "comments-around-values" );
        ReleaseDescriptor config = createConfigurationForPomWithParentAlternateNextVersion( reactorProjects );
        mapNextVersion( config, "groupId:subproject2" );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    public void testRewriteCDataAroundValues()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "cdata-around-values" );
        ReleaseDescriptor config = createConfigurationForPomWithParentAlternateNextVersion( reactorProjects );
        mapNextVersion( config, "groupId:subproject2" );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    public void testCleanNoProjects()
        throws Exception
    {
        // This occurs when it is release:perform run standalone. Just check there are no errors.
        ReleaseDescriptor config = new ReleaseDescriptor();
        config.setScmSourceUrl( "scm:svn:file://localhost/tmp/scm-repo" );
        config.setWorkingDirectory( getTestFile( "target/test/checkout" ).getAbsolutePath() );

        phase.clean( Collections.<MavenProject>emptyList() );

        assertTrue( true );
    }

    protected ReleaseDescriptor createUnmappedConfiguration( List<MavenProject> reactorProjects )
        throws Exception
    {
        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );

        unmapNextVersion( config, "groupId:subproject1" );
        mapNextVersion( config, "groupId:subproject2" );
        mapNextVersion( config, "groupId:subproject3" );
        mapNextVersion( config, "groupId:artifactId" );
        return config;
    }

    protected List<MavenProject> createReactorProjects( String path )
        throws Exception
    {
        return prepareReactorProjects( path, true );
    }

    protected ReleaseDescriptor createDefaultConfiguration( List<MavenProject> reactorProjects )
        throws Exception
    {
        ReleaseDescriptor config = createMappedConfiguration( reactorProjects );

        mapNextVersion( config, "groupId:subproject4" );
        return config;
    }

    protected ReleaseDescriptor createMappedConfiguration( List<MavenProject> reactorProjects )
        throws Exception
    {
        ReleaseDescriptor config = createDifferingVersionConfiguration( reactorProjects );

        mapNextVersion( config, "groupId:subproject3" );
        return config;
    }

    private ReleaseDescriptor createDifferingVersionConfiguration( List<MavenProject> reactorProjects )
        throws Exception
    {
        ReleaseDescriptor config = createConfigurationForWithParentNextVersion( reactorProjects );

        mapNextVersion( config, "groupId:subproject2" );
        return config;
    }

    protected List<MavenProject> createReactorProjectsFromBasicPom()
        throws Exception
    {
        return createReactorProjects( "basic-pom" );
    }

    protected abstract ReleaseDescriptor createConfigurationForWithParentNextVersion( List<MavenProject> reactorProjects )
        throws Exception;

    protected abstract void unmapNextVersion( ReleaseDescriptor config, String projectId );

    protected abstract void mapNextVersion( ReleaseDescriptor config, String projectId );

    protected ReleaseDescriptor createDescriptorFromBasicPom( List<MavenProject> reactorProjects )
        throws Exception
    {
        return createDescriptorFromProjects( reactorProjects );
    }

    protected abstract String readTestProjectFile( String fileName )
        throws IOException;

    public void testRewritePomDependenciesWithNamespace()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "pom-with-namespace" );
        ReleaseDescriptor config = createDefaultConfiguration( reactorProjects );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    protected abstract List<MavenProject> prepareReactorProjects( String path, boolean copyFiles )
        throws Exception;
    
    protected void verifyReactorProjects( String path, boolean copyFiles ) throws Exception
    {
    }

    protected ReleaseDescriptor createDescriptorFromProjects( List<MavenProject> reactorProjects )
    {
        ReleaseDescriptor descriptor = new ReleaseDescriptor();
        MavenProject rootProject = ReleaseUtil.getRootProject( reactorProjects );
        if ( rootProject.getScm() == null )
        {
            descriptor.setScmSourceUrl( "scm:svn:file://localhost/tmp/scm-repo/trunk" );
        }
        else
        {
            descriptor.setScmSourceUrl( rootProject.getScm().getConnection() );
        }

        descriptor.setWorkingDirectory( getTestFile( "target/test/checkout" ).getAbsolutePath() );

        return descriptor;
    }
}
