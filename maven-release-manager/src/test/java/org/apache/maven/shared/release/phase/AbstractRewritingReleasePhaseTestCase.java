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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.apache.maven.shared.release.config.ReleaseUtils;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.scm.DefaultScmRepositoryConfigurator;
import org.apache.maven.shared.release.scm.ReleaseScmRepositoryException;
import org.apache.maven.shared.release.scm.ScmRepositoryConfigurator;
import org.apache.maven.shared.release.stubs.ScmManagerStub;
import org.apache.maven.shared.release.transform.jdom2.JDomModelETLFactory;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Base class with tests for rewriting POMs.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
@RunWith(Parameterized.class)
public abstract class AbstractRewritingReleasePhaseTestCase
    extends AbstractReleaseTestCase
{
    private String modelETL;

    @Parameters
    public static Collection<Object[]> data()
    {
        return Arrays.asList( new Object[][] { { JDomModelETLFactory.ROLE_HINT } } );
    }

    public AbstractRewritingReleasePhaseTestCase( String modelETL )
    {
        this.modelETL = modelETL;
    }

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        phase = lookup( ReleasePhase.class, getRoleHint() );

        if( phase instanceof AbstractRewritePomsPhase)
        {
            ((AbstractRewritePomsPhase) phase).setModelETL( modelETL );
            ((AbstractRewritePomsPhase) phase).setStartTime( 0 );
        }
    }

    protected abstract String getRoleHint();

    @Test
    public void testRewriteBasicPom()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "basic-pom" );
        ReleaseDescriptorBuilder builder = createDescriptorFromBasicPom( reactorProjects, "basic-pom" );
        mapNextVersion( builder, "groupId:artifactId" );

        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    @Test
    public void testRewriteBasicPomEntities()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "basic-pom-entities" );
        ReleaseDescriptorBuilder builder = createDescriptorFromBasicPom( reactorProjects, "basic-pom-entities" );
        mapNextVersion( builder, "groupId:artifactId" );

        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    @Test
    public void testRewriteBasicPomNamespace()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "basic-pom-namespace" );
        ReleaseDescriptorBuilder builder = createDescriptorFromBasicPom( reactorProjects, "basic-pom-namespace" );
        mapNextVersion( builder, "groupId:artifactId" );

        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    @Test
    public void testRewriteBasicPomWithEncoding()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "basic-pom-with-encoding" );
        ReleaseDescriptorBuilder builder = createDescriptorFromBasicPom( reactorProjects, "basic-pom-with-encoding" );
        mapNextVersion( builder, "groupId:artifactId" );

        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    @Test
    public void testRewritePomWithParent()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "pom-with-parent" );
        ReleaseDescriptorBuilder builder = createConfigurationForPomWithParentAlternateNextVersion( reactorProjects, "pom-with-parent" );

        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    @Test
    public void testRewritePomWithUnmappedParent()
        throws Exception
    {

        List<MavenProject> reactorProjects = createReactorProjects( "pom-with-parent" );
        ReleaseDescriptorBuilder builder = createDescriptorFromProjects( reactorProjects, "pom-with-parent" );

        // Process the child first
        reactorProjects = new ArrayList<>( reactorProjects );
        Collections.reverse( reactorProjects );

        mapAlternateNextVersion( builder, "groupId:subproject1" );

        try
        {
            phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    @Test
    public void testRewritePomWithReleasedParent()
        throws Exception
    {

        List<MavenProject> reactorProjects = createReactorProjects( "pom-with-released-parent" );
        ReleaseDescriptorBuilder builder = createDescriptorFromProjects( reactorProjects, "pom-with-released-parent" );

        mapAlternateNextVersion( builder, "groupId:subproject1" );
        builder.addReleaseVersion( "groupId:artifactId", "1" );
        builder.addDevelopmentVersion( "groupId:artifactId", "1" );

        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    protected abstract void mapAlternateNextVersion( ReleaseDescriptorBuilder config, String projectId );

    @Test
    public void testRewritePomWithInheritedVersion()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "pom-with-inherited-version" );
        ReleaseDescriptorBuilder builder = createConfigurationForWithParentNextVersion( reactorProjects, "pom-with-inherited-version" );

        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    @Test
    public void testRewritePomWithChangedInheritedVersion()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "pom-with-inherited-version" );
        ReleaseDescriptorBuilder builder = createConfigurationForPomWithParentAlternateNextVersion( reactorProjects, "pom-with-inherited-version" );

        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        MavenProject project = getProjectsAsMap( reactorProjects ).get( "groupId:subproject1" );
        comparePomFiles( project, "-version-changed" );
    }

    protected abstract ReleaseDescriptorBuilder createConfigurationForPomWithParentAlternateNextVersion( List<MavenProject> reactorProjects, String workingDirectory )
        throws Exception;

    @Test
    public void testRewritePomDependencies()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-snapshot-dependencies" );
        ReleaseDescriptorBuilder builder = createDefaultConfiguration( reactorProjects, "internal-snapshot-dependencies" );
        mapNextVersion( builder, "groupId:subsubproject" );

        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    @Test
    public void testRewritePomUnmappedDependencies()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-snapshot-dependencies" );
        ReleaseDescriptorBuilder builder = createUnmappedConfiguration( reactorProjects, "internal-snapshot-dependencies" );

        try
        {
            phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    @Test
    public void testRewritePomDependenciesDifferentVersion()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-differing-snapshot-dependencies" );
        ReleaseDescriptorBuilder builder = createDifferingVersionConfiguration( reactorProjects, "internal-differing-snapshot-dependencies" );

        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    @Test
    public void testRewriteManagedPomDependencies()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-managed-snapshot-dependency" );
        ReleaseDescriptorBuilder builder = createMappedConfiguration( reactorProjects, "internal-managed-snapshot-dependency" );

        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    @Test
    public void testRewriteManagedPomUnmappedDependencies()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-managed-snapshot-dependency" );
        ReleaseDescriptorBuilder builder = createUnmappedConfiguration( reactorProjects, "internal-managed-snapshot-dependency" );

        try
        {
            phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    @Test
    public void testRewritePomPlugins()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-snapshot-plugins" );
        ReleaseDescriptorBuilder builder = createDefaultConfiguration( reactorProjects, "internal-snapshot-plugins" );

        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    @Test
    public void testRewritePomUnmappedPlugins()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-snapshot-plugins" );
        ReleaseDescriptorBuilder builder = createUnmappedConfiguration( reactorProjects, "internal-snapshot-plugins" );

        try
        {
            phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    @Test
    public void testRewritePomPluginsDifferentVersion()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-differing-snapshot-plugins" );
        ReleaseDescriptorBuilder builder = createDifferingVersionConfiguration( reactorProjects, "internal-differing-snapshot-plugins" );

        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    @Test
    public void testRewriteManagedPomPlugins()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-managed-snapshot-plugin" );
        ReleaseDescriptorBuilder builder = createMappedConfiguration( reactorProjects, "internal-managed-snapshot-plugin" );

        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    @Test
    public void testRewriteManagedPomUnmappedPlugins()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-managed-snapshot-plugin" );
        ReleaseDescriptorBuilder builder = createUnmappedConfiguration( reactorProjects, "internal-managed-snapshot-plugin" );

        try
        {
            phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    @Test
    public void testRewritePomReportPlugins()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-snapshot-report-plugins" );
        ReleaseDescriptorBuilder builder = createDefaultConfiguration( reactorProjects, "internal-snapshot-report-plugins" );

        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    @Test
    public void testRewritePomUnmappedReportPlugins()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-snapshot-report-plugins" );
        ReleaseDescriptorBuilder builder = createUnmappedConfiguration( reactorProjects, "internal-snapshot-report-plugins" );

        try
        {
            phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    @Test
    public void testRewritePomReportPluginsDifferentVersion()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-differing-snapshot-report-plugins" );
        ReleaseDescriptorBuilder builder = createDifferingVersionConfiguration( reactorProjects, "internal-differing-snapshot-report-plugins" );

        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    @Test
    @Ignore( "Extensions being part of reactor is not supported anymore" )
    public void testRewritePomExtension()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-snapshot-extension" );
        ReleaseDescriptorBuilder builder = createDefaultConfiguration( reactorProjects, "internal-snapshot-extension" );

        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    @Test
    @Ignore( "Extensions being part of reactor is not supported anymore" )
    public void testRewritePomUnmappedExtension()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-snapshot-extension" );
        ReleaseDescriptorBuilder builder = createUnmappedConfiguration( reactorProjects, "internal-snapshot-extension" );

        try
        {
            phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    @Test
    @Ignore( "Extensions being part of reactor is not supported anymore" )
    public void testRewritePomExtensionDifferentVersion()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-differing-snapshot-extension" );
        ReleaseDescriptorBuilder builder = createDifferingVersionConfiguration( reactorProjects, "internal-differing-snapshot-extension" );

        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    @Test
    @Ignore( "Extensions being part of reactor is not supported anymore" )
    public void testRewritePomExtensionUndefinedVersion()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "pom-without-extension-version" );
        ReleaseDescriptorBuilder builder = createDefaultConfiguration( reactorProjects, "pom-without-extension-version" );

        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    @Test
    public void testRewriteAddSchema()
        throws Exception
    {
        boolean copyFiles = true;

        // Run a second time to check they are not duplicated
        for ( int i = 0; i < 2; i++ )
        {
            String path = "basic-pom";
            List<MavenProject> reactorProjects = prepareReactorProjects( path );
            ReleaseDescriptorBuilder builder = createDescriptorFromBasicPom( reactorProjects, "basic-pom" );
            mapNextVersion( builder, "groupId:artifactId" );
            builder.setAddSchema( true );

            phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

            comparePomFiles( reactorProjects, "-with-schema" );

            copyFiles = false;

            verifyReactorProjects( path, copyFiles );
        }
    }

    @Test
    public void testSimulateRewriteEditModeSkipped()
        throws Exception
    {
        // prepare
        List<MavenProject> reactorProjects = createReactorProjects( "basic-pom" );
        ReleaseDescriptorBuilder builder = createDescriptorFromBasicPom( reactorProjects, "basic-pom" );
        builder.setScmUseEditMode( true );
        mapNextVersion( builder, "groupId:artifactId" );

        ScmProvider scmProviderMock = mock( ScmProvider.class );

        ScmManagerStub scmManager = new ScmManagerStub();
        DefaultScmRepositoryConfigurator configurator =
            (DefaultScmRepositoryConfigurator) lookup( ScmRepositoryConfigurator.class );
        configurator.setScmManager( scmManager );
        scmManager.setScmProvider( scmProviderMock );

        // execute
        phase.simulate( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        verifyNoMoreInteractions( scmProviderMock );
    }

    @Test
    public void testRewriteUnmappedPom()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "basic-pom" );
        ReleaseDescriptorBuilder builder = createDescriptorFromBasicPom( reactorProjects, "basic-pom" );

        try
        {
            phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    @Test
    public void testRewriteBasicPomWithScmRepoException()
        throws Exception
    {
        // prepare
        List<MavenProject> reactorProjects = createReactorProjects( "basic-pom" );
        ReleaseDescriptorBuilder builder = createDescriptorFromBasicPom( reactorProjects, "basic-pom" );
        builder.setScmUseEditMode( true );
        builder.setScmSourceUrl( "scm:svn:fail" );
        mapNextVersion( builder, "groupId:artifactId" );

        ScmManager scmManager = lookup( ScmManager.class );
        if ( scmManager instanceof ScmManagerStub )
        {
            ((ScmManagerStub) scmManager ).setException( new ScmRepositoryException( "..." ) );
        }

        try
        {
            phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseScmRepositoryException e )
        {
            assertNull( "Check no additional cause", e.getCause() );
        }
    }

    @Test
    public void testRewriteBasicPomWithNoSuchProviderException()
        throws Exception
    {
        // prepare
        List<MavenProject> reactorProjects = createReactorProjects( "basic-pom" );
        ReleaseDescriptorBuilder builder = createDescriptorFromBasicPom( reactorProjects, "basic-pom" );
        builder.setScmUseEditMode( true );
        builder.setScmSourceUrl( "scm:fail:path" );
        mapNextVersion( builder, "groupId:artifactId" );

        ScmManager scmManager = (ScmManager) lookup( ScmManager.class );
        if ( scmManager instanceof ScmManagerStub )
        {
            ((ScmManagerStub) scmManager ).setException( new NoSuchScmProviderException( "..." ) );;
        }

        // execute
        try
        {
            phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseExecutionException e )
        {
            // verify
            assertEquals( "Check cause", NoSuchScmProviderException.class, e.getCause().getClass() );
        }
    }

    @Test
    public void testRewriteWhitespaceAroundValues()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "whitespace-around-values" );
        ReleaseDescriptorBuilder builder = createConfigurationForPomWithParentAlternateNextVersion( reactorProjects, "whitespace-around-values" );
        mapNextVersion( builder, "groupId:subproject2" );

        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    @Test
    public void testRewriteCommentsAroundValues()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "comments-around-values" );
        ReleaseDescriptorBuilder builder = createConfigurationForPomWithParentAlternateNextVersion( reactorProjects, "comments-around-values" );
        mapNextVersion( builder, "groupId:subproject2" );

        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    @Test
    public void testRewriteCDataAroundValues()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "cdata-around-values" );
        ReleaseDescriptorBuilder builder = createConfigurationForPomWithParentAlternateNextVersion( reactorProjects, "cdata-around-values" );
        mapNextVersion( builder, "groupId:subproject2" );

        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    @Test
    public void testCleanNoProjects()
        throws Exception
    {
        // This occurs when it is release:perform run standalone. Just check there are no errors.
        ( (ResourceGenerator) phase ).clean( Collections.<MavenProject>emptyList() );
    }

    protected ReleaseDescriptorBuilder createUnmappedConfiguration( List<MavenProject> reactorProjects, String workingDirectory )
        throws Exception
    {
        ReleaseDescriptorBuilder builder = createDescriptorFromProjects( reactorProjects, workingDirectory );

        unmapNextVersion( builder, "groupId:subproject1" );
        mapNextVersion( builder, "groupId:subproject2" );
        mapNextVersion( builder, "groupId:subproject3" );
        mapNextVersion( builder, "groupId:artifactId" );
        return builder;
    }

    protected List<MavenProject> createReactorProjects( String path )
        throws Exception
    {
        return prepareReactorProjects( path );
    }

    protected ReleaseDescriptorBuilder createDefaultConfiguration( List<MavenProject> reactorProjects, String workingDirectory )
        throws Exception
    {
        ReleaseDescriptorBuilder builder = createMappedConfiguration( reactorProjects, workingDirectory );

        mapNextVersion( builder, "groupId:subproject4" );
        return builder;
    }

    protected ReleaseDescriptorBuilder createMappedConfiguration( List<MavenProject> reactorProjects, String workingDirectory )
        throws Exception
    {
        ReleaseDescriptorBuilder builder = createDifferingVersionConfiguration( reactorProjects, workingDirectory );

        mapNextVersion( builder, "groupId:subproject3" );
        return builder;
    }

    private ReleaseDescriptorBuilder createDifferingVersionConfiguration( List<MavenProject> reactorProjects, String workingDirectory )
        throws Exception
    {
        ReleaseDescriptorBuilder builder = createConfigurationForWithParentNextVersion( reactorProjects, workingDirectory );

        mapNextVersion( builder, "groupId:subproject2" );
        return builder;
    }

    protected abstract ReleaseDescriptorBuilder createConfigurationForWithParentNextVersion( List<MavenProject> reactorProjects, String workingDirectory )
        throws Exception;

    protected abstract void unmapNextVersion( ReleaseDescriptorBuilder config, String projectId );

    protected abstract void mapNextVersion( ReleaseDescriptorBuilder config, String projectId );

    protected ReleaseDescriptorBuilder createDescriptorFromBasicPom( List<MavenProject> reactorProjects, String workingDirectory )
        throws Exception
    {
        return createDescriptorFromProjects( reactorProjects, workingDirectory );
    }

    protected abstract String readTestProjectFile( String fileName )
        throws IOException;

    @Test
    public void testRewritePomDependenciesWithNamespace()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "pom-with-namespace" );
        ReleaseDescriptorBuilder builder = createDefaultConfiguration( reactorProjects, "pom-with-namespace" );

        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    protected abstract List<MavenProject> prepareReactorProjects( String path )
        throws Exception;

    protected void verifyReactorProjects( String path, boolean copyFiles )
        throws Exception
    {
    }

    protected ReleaseDescriptorBuilder createDescriptorFromProjects( List<MavenProject> reactorProjects, String workingDirectory )
    {
        ReleaseDescriptorBuilder builder = createDescriptorFromProjects( new ReleaseDescriptorBuilder(), reactorProjects );
        builder.setWorkingDirectory( getWorkingDirectory( workingDirectory ).toString() );
        return builder;
    }
    
    private ReleaseDescriptorBuilder createDescriptorFromProjects( ReleaseDescriptorBuilder builder, List<MavenProject> reactorProjects )
    {
        MavenProject rootProject = ReleaseUtil.getRootProject( reactorProjects );
        
        for ( MavenProject project : reactorProjects )
        {
            String key = project.getGroupId() + ':' + project.getArtifactId();
            builder.putOriginalVersion( key, project.getVersion() );
            builder.addOriginalScmInfo( key, project.getScm() );
        }
        
        if ( rootProject.getScm() == null )
        {
            builder.setScmSourceUrl( "scm:svn:file://localhost/tmp/scm-repo/trunk" );
        }
        else
        {
            builder.setScmSourceUrl( rootProject.getScm().getConnection() );
        }

        return builder;
    }
}
