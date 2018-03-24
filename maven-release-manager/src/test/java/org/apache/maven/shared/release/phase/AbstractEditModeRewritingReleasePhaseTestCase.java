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
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.edit.EditScmResult;
import org.apache.maven.scm.manager.ScmManagerStub;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.provider.ScmProviderStub;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.apache.maven.shared.release.config.ReleaseUtils;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.scm.DefaultScmRepositoryConfigurator;
import org.apache.maven.shared.release.scm.ReleaseScmCommandException;
import org.apache.maven.shared.release.scm.ScmRepositoryConfigurator;
import org.junit.Test;

/**
 * Base class with tests for rewriting POMs with edit mode.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class AbstractEditModeRewritingReleasePhaseTestCase
    extends AbstractRewritingReleasePhaseTestCase
{
    public AbstractEditModeRewritingReleasePhaseTestCase( String modelETL )
    {
        super( modelETL );
    }

    @Test
    public void testRewriteBasicPomWithEditMode()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "basic-pom" );
        ReleaseDescriptorBuilder builder = createDescriptorFromBasicPom( reactorProjects, "basic-pom" );
        builder.setScmUseEditMode( true );
        mapNextVersion( builder, "groupId:artifactId" );

        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    @Test
    public void testRewriteBasicPomWithEditModeFailure()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "basic-pom" );
        ReleaseDescriptorBuilder builder = createDescriptorFromProjects( reactorProjects, "basic-pom" );
        builder.setScmUseEditMode( true );
        mapNextVersion( builder, "groupId:artifactId" );

        ScmManagerStub scmManager = new ScmManagerStub();
        DefaultScmRepositoryConfigurator configurator =
            (DefaultScmRepositoryConfigurator) lookup( ScmRepositoryConfigurator.class, "default" );
        configurator.setScmManager( scmManager );

        ScmProviderStub providerStub = (ScmProviderStub) scmManager.getProviderByUrl( ReleaseUtils.buildReleaseDescriptor( builder ).getScmSourceUrl() );
        providerStub.setEditScmResult( new EditScmResult( "", "", "", false ) );

        try
        {
            phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseScmCommandException e )
        {
            assertNull( "Check no other cause", e.getCause() );
        }
    }

    @Test
    public void testRewriteBasicPomWithEditModeException()
        throws Exception
    {
        // prepare
        List<MavenProject> reactorProjects = createReactorProjects( "basic-pom" );
        ReleaseDescriptorBuilder builder = createDescriptorFromProjects( reactorProjects, "basic-pom" );
        builder.setScmUseEditMode( true );
        mapNextVersion( builder, "groupId:artifactId" );

        ScmProvider scmProviderMock = mock( ScmProvider.class );
        when( scmProviderMock.edit( isA( ScmRepository.class ),
                                    isA( ScmFileSet.class ) ) ).thenThrow( new ScmException( "..." ) );

        ScmManagerStub scmManager = new ScmManagerStub();
        DefaultScmRepositoryConfigurator configurator =
            (DefaultScmRepositoryConfigurator) lookup( ScmRepositoryConfigurator.class, "default" );
        configurator.setScmManager( scmManager );
        scmManager.setScmProvider( scmProviderMock );

        // execute
        try
        {
            phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertEquals( "Check cause", ScmException.class, e.getCause().getClass() );
        }
        // verify
        verify( scmProviderMock ).edit( isA( ScmRepository.class ), isA( ScmFileSet.class ) );
        verifyNoMoreInteractions( scmProviderMock );
    }

    @Test
    public void testRewritePomPluginDependencies()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-snapshot-plugin-deps" );
        ReleaseDescriptorBuilder builder = createDefaultConfiguration( reactorProjects, "internal-snapshot-plugin-deps" );

        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    @Test
    public void testRewritePomUnmappedPluginDependencies()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-snapshot-plugin-deps" );
        ReleaseDescriptorBuilder builder = createUnmappedConfiguration( reactorProjects, "internal-snapshot-plugin-deps" );

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
    public void testRewritePomProfile()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-snapshot-profile" );
        ReleaseDescriptorBuilder builder = createDefaultConfiguration( reactorProjects, "internal-snapshot-profile" );

        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    @Test
    public void testRewritePomUnmappedProfile()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-snapshot-profile" );
        ReleaseDescriptorBuilder builder = createUnmappedConfiguration( reactorProjects, "internal-snapshot-profile" );

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

}
