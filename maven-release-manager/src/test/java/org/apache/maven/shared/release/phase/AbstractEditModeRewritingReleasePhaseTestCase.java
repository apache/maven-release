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

import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.command.edit.EditScmResult;
import org.apache.maven.scm.manager.ScmManagerStub;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.provider.ScmProviderStub;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.scm.DefaultScmRepositoryConfigurator;
import org.apache.maven.shared.release.scm.ReleaseScmCommandException;
import org.apache.maven.shared.release.scm.ScmRepositoryConfigurator;
import org.jmock.Mock;
import org.jmock.core.matcher.InvokeAtLeastOnceMatcher;
import org.jmock.core.stub.ThrowStub;

import java.util.List;

/**
 * Base class with tests for rewriting POMs with edit mode.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class AbstractEditModeRewritingReleasePhaseTestCase
    extends AbstractRewritingReleasePhaseTestCase
{
    public void testRewriteBasicPomWithEditMode()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjectsFromBasicPom();
        ReleaseDescriptor config = createDescriptorFromBasicPom( reactorProjects );
        config.setScmUseEditMode( true );
        mapNextVersion( config, "groupId:artifactId" );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    public void testRewriteBasicPomWithEditModeFailure()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjectsFromBasicPom();
        ReleaseDescriptor config = createDescriptorFromBasicPom( reactorProjects );
        config.setScmUseEditMode( true );
        mapNextVersion( config, "groupId:artifactId" );

        ScmManagerStub scmManager = new ScmManagerStub();
        DefaultScmRepositoryConfigurator configurator =
            (DefaultScmRepositoryConfigurator) lookup( ScmRepositoryConfigurator.ROLE );
        configurator.setScmManager( scmManager );

        ScmProviderStub providerStub = (ScmProviderStub) scmManager.getProviderByUrl( config.getScmSourceUrl() );
        providerStub.setEditScmResult( new EditScmResult( "", "", "", false ) );

        try
        {
            phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseScmCommandException e )
        {
            assertNull( "Check no other cause", e.getCause() );
        }
    }

    public void testRewriteBasicPomWithEditModeException()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjectsFromBasicPom();
        ReleaseDescriptor config = createDescriptorFromBasicPom( reactorProjects );
        config.setScmUseEditMode( true );
        mapNextVersion( config, "groupId:artifactId" );

        Mock scmProviderMock = new Mock( ScmProvider.class );
        scmProviderMock.expects( new InvokeAtLeastOnceMatcher() ).method( "edit" ).will(
            new ThrowStub( new ScmException( "..." ) ) );

        ScmManagerStub scmManager = new ScmManagerStub();
        DefaultScmRepositoryConfigurator configurator =
            (DefaultScmRepositoryConfigurator) lookup( ScmRepositoryConfigurator.ROLE );
        configurator.setScmManager( scmManager );
        scmManager.setScmProvider( (ScmProvider) scmProviderMock.proxy() );

        try
        {
            phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertEquals( "Check cause", ScmException.class, e.getCause().getClass() );
        }
    }

    public void testRewritePomPluginDependencies()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-snapshot-plugin-deps" );
        ReleaseDescriptor config = createDefaultConfiguration( reactorProjects );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    public void testRewritePomUnmappedPluginDependencies()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-snapshot-plugin-deps" );
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

    public void testRewritePomProfile()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-snapshot-profile" );
        ReleaseDescriptor config = createDefaultConfiguration( reactorProjects );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    public void testRewritePomUnmappedProfile()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "internal-snapshot-profile" );
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

}
