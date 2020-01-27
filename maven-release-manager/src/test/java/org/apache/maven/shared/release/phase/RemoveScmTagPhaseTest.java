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

import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.CommandParameters;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.untag.UntagScmResult;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.apache.maven.shared.release.config.ReleaseUtils;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.scm.ReleaseScmCommandException;
import org.apache.maven.shared.release.stubs.ScmManagerStub;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Matchers;
import org.mockito.Mockito;

/**
 * Test the remove SCM tag phase.
 */
public class RemoveScmTagPhaseTest extends AbstractReleaseTestCase
{

    @Override
    public void setUp() throws Exception
    {

        super.setUp();

        phase = ( ReleasePhase ) lookup( ReleasePhase.class, "remove-scm-tag" );

    }

    @Test
    public void testExecuteOutput() throws Exception
    {

        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmReleaseLabel( "release-label" );
        builder.setScmSourceUrl( "scm-url" );
        List<MavenProject> reactorProjects = createReactorProjects();
        MavenProject rootProject = ReleaseUtil.getRootProject( reactorProjects );
        builder.setWorkingDirectory( getPath( rootProject.getFile().getParentFile() ) );
        builder.setPomFileName( rootProject.getFile().getName() );
        ScmFileSet fileSet = new ScmFileSet( rootProject.getFile().getParentFile() );

        // mock, only real matcher is the file set
        ScmProvider scmProviderMock = Mockito.mock( ScmProvider.class );
        Mockito.when( scmProviderMock.untag( Matchers.isA( ScmRepository.class ),
                Matchers.argThat( new IsScmFileSetEquals( fileSet ) ),
                Matchers.isA( CommandParameters.class ) ) )
                .thenReturn( new UntagScmResult( "...", "...", "...", true ) );
        ScmManagerStub stub = ( ScmManagerStub ) lookup( ScmManager.class );
        stub.setScmProvider( scmProviderMock );

        // execute
        ReleaseResult actual = phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ),
                new DefaultReleaseEnvironment(), reactorProjects );

        // verify, actual contains trailing newline
        Assert.assertEquals( "[INFO] Removing tag with the label release-label ...", actual.getOutput().trim() );

    }

    @Test
    public void testExecuteResultCode() throws Exception
    {

        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmReleaseLabel( "release-label" );
        builder.setScmSourceUrl( "scm-url" );
        List<MavenProject> reactorProjects = createReactorProjects();
        MavenProject rootProject = ReleaseUtil.getRootProject( reactorProjects );
        builder.setWorkingDirectory( getPath( rootProject.getFile().getParentFile() ) );
        builder.setPomFileName( rootProject.getFile().getName() );
        ScmFileSet fileSet = new ScmFileSet( rootProject.getFile().getParentFile() );

        // mock, only real matcher is the file set
        ScmProvider scmProviderMock = Mockito.mock( ScmProvider.class );
        Mockito.when( scmProviderMock.untag( Matchers.isA( ScmRepository.class ),
                Matchers.argThat( new IsScmFileSetEquals( fileSet ) ),
                Matchers.isA( CommandParameters.class ) ) )
                .thenReturn( new UntagScmResult( "...", "...", "...", true ) );
        ScmManagerStub stub = ( ScmManagerStub ) lookup( ScmManager.class );
        stub.setScmProvider( scmProviderMock );

        // execute
        ReleaseResult actual = phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ),
                new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        Assert.assertEquals( 0, actual.getResultCode() );

    }

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    @Ignore( "We changed the behaviour to warning instead of error." )
    public void testExecuteError() throws Exception
    {

        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmReleaseLabel( "release-label" );
        builder.setScmSourceUrl( "scm-url" );
        List<MavenProject> reactorProjects = createReactorProjects();
        MavenProject rootProject = ReleaseUtil.getRootProject( reactorProjects );
        builder.setWorkingDirectory( getPath( rootProject.getFile().getParentFile() ) );
        builder.setPomFileName( rootProject.getFile().getName() );
        ScmFileSet fileSet = new ScmFileSet( rootProject.getFile().getParentFile() );

        // mock, only real matcher is the file set
        ScmProvider scmProviderMock = Mockito.mock( ScmProvider.class );
        Mockito.when( scmProviderMock.untag( Matchers.isA( ScmRepository.class ),
                Matchers.argThat( new IsScmFileSetEquals( fileSet ) ),
                Matchers.isA( CommandParameters.class ) ) )
                .thenReturn( new UntagScmResult( "command-line", "provider-message", "command-output", false ) );
        ScmManagerStub stub = ( ScmManagerStub ) lookup( ScmManager.class );
        stub.setScmProvider( scmProviderMock );

        // set up exception rule
        exceptionRule.expect( ReleaseScmCommandException.class );
        exceptionRule.expectMessage(
                "Unable to remove tag \nProvider message:\nprovider-message\nCommand output:\ncommand-output" );

        // execute
        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ),
                new DefaultReleaseEnvironment(), reactorProjects );

    }

    @Test
    public void testExecuteNoError() throws Exception
    {

        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmReleaseLabel( "release-label" );
        builder.setScmSourceUrl( "scm-url" );
        List<MavenProject> reactorProjects = createReactorProjects();
        MavenProject rootProject = ReleaseUtil.getRootProject( reactorProjects );
        builder.setWorkingDirectory( getPath( rootProject.getFile().getParentFile() ) );
        builder.setPomFileName( rootProject.getFile().getName() );
        ScmFileSet fileSet = new ScmFileSet( rootProject.getFile().getParentFile() );

        // mock, only real matcher is the file set
        ScmProvider scmProviderMock = Mockito.mock( ScmProvider.class );
        Mockito.when( scmProviderMock.untag( Matchers.isA( ScmRepository.class ),
                Matchers.argThat( new IsScmFileSetEquals( fileSet ) ),
                Matchers.isA( CommandParameters.class ) ) )
                .thenReturn( new UntagScmResult( "command-line", "provider-message", "command-output", false ) );
        ScmManagerStub stub = ( ScmManagerStub ) lookup( ScmManager.class );
        stub.setScmProvider( scmProviderMock );

        // execute
        ReleaseResult actual = phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ),
                new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        Assert.assertEquals( 0, actual.getResultCode() );


    }

    @Test
    public void testSimulateOutput() throws Exception
    {

        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmReleaseLabel( "release-label" );
        builder.setScmSourceUrl( "scm-url" );
        List<MavenProject> reactorProjects = createReactorProjects();
        MavenProject rootProject = ReleaseUtil.getRootProject( reactorProjects );
        builder.setWorkingDirectory( getPath( rootProject.getFile().getParentFile() ) );
        builder.setPomFileName( rootProject.getFile().getName() );

        // execute
        ReleaseResult actual = phase.simulate(ReleaseUtils.buildReleaseDescriptor( builder ),
                new DefaultReleaseEnvironment(), reactorProjects );

        // verify, actual contains newline
        Assert.assertEquals( "[INFO] Full run would remove tag with label: 'release-label'", 
                actual.getOutput().trim() );

    }

    @Test
    public void testSimulateResultCode() throws Exception
    {

        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmReleaseLabel( "release-label" );
        builder.setScmSourceUrl( "scm-url" );
        List<MavenProject> reactorProjects = createReactorProjects();
        MavenProject rootProject = ReleaseUtil.getRootProject( reactorProjects );
        builder.setWorkingDirectory( getPath( rootProject.getFile().getParentFile() ) );
        builder.setPomFileName( rootProject.getFile().getName() );

        // execute
        ReleaseResult actual = phase.simulate( ReleaseUtils.buildReleaseDescriptor( builder ),
                new DefaultReleaseEnvironment(), reactorProjects );

        Assert.assertEquals( 0, actual.getResultCode() );

    }

    private List<MavenProject> createReactorProjects() throws Exception
    {
        return createReactorProjects( "scm-commit/single-pom", "" );
    }

}
