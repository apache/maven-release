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
import static org.junit.Assert.fail;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.CommandParameters;
import org.apache.maven.scm.ScmFile;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmTag;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.manager.ScmManagerStub;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.shared.release.PlexusJUnit4TestCase;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.apache.maven.shared.release.config.ReleaseUtils;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.exec.MavenExecutor;
import org.apache.maven.shared.release.exec.MavenExecutorException;
import org.apache.maven.shared.release.stubs.MavenExecutorWrapper;
import org.junit.Test;

/**
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 */
public class RunPerformGoalsPhaseTest
    extends PlexusJUnit4TestCase
{
    private RunPerformGoalsPhase phase;

    private MavenExecutorWrapper mavenExecutorWrapper;

    private DefaultReleaseEnvironment releaseEnvironment;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        phase = (RunPerformGoalsPhase) lookup( ReleasePhase.class, "run-perform-goals" );

        mavenExecutorWrapper = (MavenExecutorWrapper) lookup( "org.apache.maven.shared.release.exec.MavenExecutor", "wrapper" );

        releaseEnvironment = new DefaultReleaseEnvironment();
        releaseEnvironment.setMavenExecutorId( "wrapper" );
    }

    @Test
    public void testExecuteException()
        throws Exception
    {
        // prepare
        File testFile = getTestFile( "target/checkout-directory" );

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setPerformGoals( "goal1 goal2" );
        builder.setCheckoutDirectory( testFile.getAbsolutePath() );

        MavenExecutor mock = mock( MavenExecutor.class );

        mavenExecutorWrapper.setMavenExecutor( mock );

        doThrow( new MavenExecutorException( "...", new Exception() ) ).when( mock ).executeGoals( eq( testFile ),
                                                                                                   eq( "goal1 goal2" ),
                                                                                                   isA( ReleaseEnvironment.class ),
                                                                                                   eq( true ),
                                                                                                   eq( "-DperformRelease=true -f pom.xml" ),
                                                                                                   isNull( String.class ),
                                                                                                   isA( ReleaseResult.class ) );

        mavenExecutorWrapper.setMavenExecutor( mock );

        // execute
        try
        {
            phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), releaseEnvironment, (List<MavenProject>) null );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertEquals( "Check cause", MavenExecutorException.class, e.getCause().getClass() );
        }

        //verify
        verify( mock ).executeGoals( eq( testFile ),
                                     eq( "goal1 goal2" ),
                                     isA( ReleaseEnvironment.class ),
                                     eq( true ),
                                     eq( "-DperformRelease=true -f pom.xml" ),
                                     isNull( String.class ),
                                     isA( ReleaseResult.class ) );
        verifyNoMoreInteractions( mock );
    }

    @Test
    public void testCustomPomFile() throws Exception
    {
        //prepare
        File testFile = getTestFile( "target/checkout-directory" );
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setPerformGoals( "goal1 goal2" );
        builder.setPomFileName( "pom1.xml" );
        builder.setCheckoutDirectory( testFile.getAbsolutePath() );

        MavenExecutor mock = mock( MavenExecutor.class );

        mavenExecutorWrapper.setMavenExecutor( mock );

        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), releaseEnvironment, (List<MavenProject>) null );

        verify( mock ).executeGoals( eq( testFile ),
                                     eq( "goal1 goal2" ),
                                     isA( ReleaseEnvironment.class ),
                                     eq( true ),
                                     eq( "-DperformRelease=true -f pom1.xml" ),
                                     eq( "pom1.xml" ),
                                     isA( ReleaseResult.class ) );

        verifyNoMoreInteractions( mock );
    }

    public void testReleasePerformWithArgumentsNoReleaseProfile()
                    throws Exception
    {
        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl( "scm-url" );
        builder.setAdditionalArguments( "-Dmaven.test.skip=true" );
        builder.setPerformGoals( "goal1 goal2" );
        File checkoutDirectory = getTestFile( "target/checkout-directory" );
        builder.setCheckoutDirectory( checkoutDirectory.getAbsolutePath() );

        MavenExecutor mock = mock( MavenExecutor.class );
        mavenExecutorWrapper.setMavenExecutor( mock );

        ScmProvider scmProviderMock = mock( ScmProvider.class );
        when( scmProviderMock.checkOut( isA( ScmRepository.class ),
                                        argThat( new IsScmFileSetEquals( new ScmFileSet( checkoutDirectory ) ) ),
                                        isA( ScmTag.class ),
                                        isA( CommandParameters.class )))
                                .thenReturn( new CheckOutScmResult( "...", Collections.<ScmFile>emptyList() ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.class );
        stub.setScmProvider( scmProviderMock );

        builder.setUseReleaseProfile( false );

        // execute
        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), releaseEnvironment, createReactorProjects() );

        // verify
        verify( mock ).executeGoals( eq( checkoutDirectory ),
                                     eq( "goal1 goal2" ),
                                     isA( ReleaseEnvironment.class ),
                                     eq( true ),
                                     eq( "-Dmaven.test.skip=true -f pom.xml" ),
                                     isNull( String.class ),
                                     isA( ReleaseResult.class ) );
        verify( scmProviderMock ).checkOut( isA( ScmRepository.class ),
                                            argThat( new IsScmFileSetEquals( new ScmFileSet( checkoutDirectory ) ) ),
                                            isA( ScmTag.class ),
                                            isA( CommandParameters.class ));
        verifyNoMoreInteractions( mock, scmProviderMock );
    }

    public void testReleasePerform()
                    throws Exception
    {
        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl( "scm-url" );
        builder.setPerformGoals( "goal1 goal2" );
        File checkoutDirectory = getTestFile( "target/checkout-directory" );
        builder.setCheckoutDirectory( checkoutDirectory.getAbsolutePath() );

        MavenExecutor mock = mock( MavenExecutor.class );
        mavenExecutorWrapper.setMavenExecutor( mock );

        ScmProvider scmProviderMock = mock( ScmProvider.class );
        when( scmProviderMock.checkOut( isA( ScmRepository.class ),
                                        argThat( new IsScmFileSetEquals( new ScmFileSet( checkoutDirectory ) ) ),
                                        isA( ScmTag.class ),
                                        isA( CommandParameters.class )) )
            .thenReturn( new CheckOutScmResult( "...", Collections.<ScmFile>emptyList() ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.class );
        stub.setScmProvider( scmProviderMock );

        // execute
        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), releaseEnvironment, createReactorProjects() );

        // verify
        verify( mock ).executeGoals( eq( checkoutDirectory ),
                                     eq( "goal1 goal2" ),
                                     isA( ReleaseEnvironment.class ),
                                     eq( true ),
                                     eq( "-DperformRelease=true -f pom.xml" ),
                                     isNull( String.class ),
                                     isA( ReleaseResult.class ) );
        verify( scmProviderMock ).checkOut( isA( ScmRepository.class ),
                                            argThat( new IsScmFileSetEquals( new ScmFileSet( checkoutDirectory ) ) ),
                                            isA( ScmTag.class ),
                                            isA( CommandParameters.class ));
        verifyNoMoreInteractions( mock, scmProviderMock );
    }

    public void testReleasePerformNoReleaseProfile()
                    throws Exception
    {
        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl( "scm-url" );
        builder.setPerformGoals( "goal1 goal2" );
        File checkoutDirectory = getTestFile( "target/checkout-directory" );
        builder.setCheckoutDirectory( checkoutDirectory.getAbsolutePath() );

        MavenExecutor mock = mock( MavenExecutor.class );
        mavenExecutorWrapper.setMavenExecutor( mock );

        ScmProvider scmProviderMock = mock( ScmProvider.class );
        when( scmProviderMock.checkOut( isA( ScmRepository.class ),
                                        argThat( new IsScmFileSetEquals( new ScmFileSet( checkoutDirectory ) ) ),
                                        isA( ScmTag.class ),
                                        isA( CommandParameters.class )) )
            .thenReturn( new CheckOutScmResult( "...", Collections.<ScmFile>emptyList() ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.class );
        stub.setScmProvider( scmProviderMock );

        builder.setUseReleaseProfile( false );

        // execute
        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), releaseEnvironment, createReactorProjects() );

        // verify
        verify( mock ).executeGoals( eq( checkoutDirectory ),
                                     eq( "goal1 goal2" ),
                                     isA( ReleaseEnvironment.class ),
                                     eq( true ),
                                     eq( "-f pom.xml" ),
                                     isNull( String.class ),
                                     isA( ReleaseResult.class ) );
        verify( scmProviderMock ).checkOut( isA( ScmRepository.class ),
                                            argThat( new IsScmFileSetEquals( new ScmFileSet( checkoutDirectory ) ) ),
                                            isA( ScmTag.class ),
                                            isA( CommandParameters.class ));
        verifyNoMoreInteractions( mock, scmProviderMock );
    }

    public void testReleasePerformWithArguments()
                    throws Exception
    {
        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl( "scm-url" );
        builder.setAdditionalArguments( "-Dmaven.test.skip=true" );
        builder.setPerformGoals( "goal1 goal2" );
        File checkoutDirectory = getTestFile( "target/checkout-directory" );
        builder.setCheckoutDirectory( checkoutDirectory.getAbsolutePath() );

        MavenExecutor mock = mock( MavenExecutor.class );
        mavenExecutorWrapper.setMavenExecutor( mock );

        ScmProvider scmProviderMock = mock( ScmProvider.class );
        when( scmProviderMock.checkOut( isA( ScmRepository.class ),
                                        argThat( new IsScmFileSetEquals( new ScmFileSet( checkoutDirectory ) ) ),
                                        isA( ScmTag.class ),
                                        isA( CommandParameters.class )) )
            .thenReturn( new CheckOutScmResult( "...", Collections.<ScmFile>emptyList() ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.class );
        stub.setScmProvider( scmProviderMock );

        // execute
        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), releaseEnvironment, createReactorProjects() );

        // verify
        verify( mock ).executeGoals( eq( checkoutDirectory ),
                                     eq( "goal1 goal2" ),
                                     isA( ReleaseEnvironment.class),
                                     eq( true),
                                     eq( "-Dmaven.test.skip=true -DperformRelease=true -f pom.xml" ),
                                     isNull( String.class ),
                                     isA( ReleaseResult.class ) );
        verify( scmProviderMock ).checkOut( isA( ScmRepository.class ),
                                            argThat( new IsScmFileSetEquals( new ScmFileSet( checkoutDirectory ) ) ),
                                            isA( ScmTag.class ),
                                            isA( CommandParameters.class ));
        verifyNoMoreInteractions( mock, scmProviderMock );
    }

    public void testReleasePerformWithReleasePropertiesCompleted()
                    throws Exception
    {
        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl( "scm-url" );
        builder.setPerformGoals( "goal1 goal2" );
        File checkoutDirectory = getTestFile( "target/checkout-directory" );
        builder.setCheckoutDirectory( checkoutDirectory.getAbsolutePath() );

        MavenExecutor mock = mock( MavenExecutor.class );
        mavenExecutorWrapper.setMavenExecutor( mock );

        ScmProvider scmProviderMock = mock( ScmProvider.class );
        when( scmProviderMock.checkOut( isA( ScmRepository.class ),
                                        argThat( new IsScmFileSetEquals( new ScmFileSet( checkoutDirectory ) ) ),
                                        isA( ScmTag.class ),
                                        isA( CommandParameters.class )) )
            .thenReturn( new CheckOutScmResult( "...", Collections.<ScmFile>emptyList() ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.class );
        stub.setScmProvider( scmProviderMock );

        builder.setCompletedPhase( "end-release" );

        // execute
        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), releaseEnvironment, createReactorProjects() );

        // verify
        verify( mock ).executeGoals( eq( checkoutDirectory ),
                                     eq( "goal1 goal2" ),
                                     isA( ReleaseEnvironment.class ),
                                     eq( true ),
                                     eq( "-DperformRelease=true -f pom.xml" ),
                                     isNull( String.class ),
                                     isA( ReleaseResult.class ) );
        verify( scmProviderMock ).checkOut( isA( ScmRepository.class ),
                                            argThat( new IsScmFileSetEquals( new ScmFileSet( checkoutDirectory ) ) ),
                                            isA( ScmTag.class ),
                                            isA( CommandParameters.class ));
        verifyNoMoreInteractions( mock, scmProviderMock );
    }

    private static List<MavenProject> createReactorProjects()
    {
        MavenProject project = new MavenProject();
        project.setFile( getTestFile( "target/dummy-project/pom.xml" ) );
        return Collections.singletonList( project );
    }
}
