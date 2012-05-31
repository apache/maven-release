package org.apache.maven.shared.release;

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
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFile;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmTag;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.manager.ScmManagerStub;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.provider.ScmProviderStub;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.config.ReleaseDescriptorStore;
import org.apache.maven.shared.release.config.ReleaseDescriptorStoreException;
import org.apache.maven.shared.release.config.ReleaseDescriptorStoreStub;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.exec.MavenExecutor;
import org.apache.maven.shared.release.phase.IsScmFileSetEquals;
import org.apache.maven.shared.release.phase.ReleasePhase;
import org.apache.maven.shared.release.phase.ReleasePhaseStub;
import org.apache.maven.shared.release.phase.RunPerformGoalsPhase;
import org.apache.maven.shared.release.scm.DefaultScmRepositoryConfigurator;
import org.apache.maven.shared.release.scm.ReleaseScmCommandException;
import org.apache.maven.shared.release.scm.ReleaseScmRepositoryException;
import org.apache.maven.shared.release.scm.ScmRepositoryConfigurator;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.ReflectionUtils;

/**
 * Test the default release manager.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class DefaultReleaseManagerTest
    extends PlexusTestCase
{
    private ReleaseDescriptorStoreStub configStore;


    protected void setUp()
        throws Exception
    {
        super.setUp();

        configStore = (ReleaseDescriptorStoreStub) lookup( ReleaseDescriptorStore.ROLE, "stub" );
    }

    public void testPrepareNoCompletedPhase()
        throws Exception
    {
        ReleaseManager releaseManager = (ReleaseManager) lookup( ReleaseManager.ROLE, "test" );

        ReleaseDescriptor releaseDescriptor = configStore.getReleaseConfiguration();
        releaseDescriptor.setCompletedPhase( null );

        releaseManager.prepare( new ReleaseDescriptor(), new DefaultReleaseEnvironment(), null );

        @SuppressWarnings("unchecked")
        Map<String,ReleasePhaseStub> phases = container.lookupMap( ReleasePhase.ROLE );

        ReleasePhaseStub phase = (ReleasePhaseStub) phases.get( "step1" );
        assertTrue( "step1 executed", phase.isExecuted() );
        assertFalse( "step1 not simulated", phase.isSimulated() );
        phase = (ReleasePhaseStub) phases.get( "step2" );
        assertTrue( "step2 executed", phase.isExecuted() );
        assertFalse( "step2 not simulated", phase.isSimulated() );
        phase = (ReleasePhaseStub) phases.get( "step3" );
        assertTrue( "step3 executed", phase.isExecuted() );
        assertFalse( "step3 not simulated", phase.isSimulated() );
    }

    public void testPrepareCompletedPhase()
        throws Exception
    {
        ReleaseManager releaseManager = (ReleaseManager) lookup( ReleaseManager.ROLE, "test" );

        ReleaseDescriptor releaseDescriptor = configStore.getReleaseConfiguration();
        releaseDescriptor.setCompletedPhase( "step1" );

        releaseManager.prepare( new ReleaseDescriptor(), new DefaultReleaseEnvironment(), null );

        @SuppressWarnings("unchecked")
        Map<String,ReleasePhaseStub> phases = container.lookupMap( ReleasePhase.ROLE );

        ReleasePhaseStub phase = (ReleasePhaseStub) phases.get( "step1" );
        assertFalse( "step1 not executed", phase.isExecuted() );
        assertFalse( "step1 not simulated", phase.isSimulated() );
        phase = (ReleasePhaseStub) phases.get( "step2" );
        assertTrue( "step2 executed", phase.isExecuted() );
        assertFalse( "step2 not simulated", phase.isSimulated() );
        phase = (ReleasePhaseStub) phases.get( "step3" );
        assertTrue( "step3 executed", phase.isExecuted() );
        assertFalse( "step3 not simulated", phase.isSimulated() );
    }

    public void testPrepareCompletedPhaseNoResume()
        throws Exception
    {
        ReleaseManager releaseManager = (ReleaseManager) lookup( ReleaseManager.ROLE, "test" );

        ReleaseDescriptor releaseDescriptor = configStore.getReleaseConfiguration();
        releaseDescriptor.setCompletedPhase( "step1" );

        releaseManager.prepare( new ReleaseDescriptor(), new DefaultReleaseEnvironment(), null, false, false );

        @SuppressWarnings("unchecked")
        Map<String,ReleasePhaseStub> phases = container.lookupMap( ReleasePhase.ROLE );

        ReleasePhaseStub phase = (ReleasePhaseStub) phases.get( "step1" );
        assertTrue( "step1 executed", phase.isExecuted() );
        assertFalse( "step1 not simulated", phase.isSimulated() );
        phase = (ReleasePhaseStub) phases.get( "step2" );
        assertTrue( "step2 executed", phase.isExecuted() );
        assertFalse( "step2 not simulated", phase.isSimulated() );
        phase = (ReleasePhaseStub) phases.get( "step3" );
        assertTrue( "step3 executed", phase.isExecuted() );
        assertFalse( "step3 not simulated", phase.isSimulated() );
    }

    public void testPrepareCompletedAllPhases()
        throws Exception
    {
        ReleaseManager releaseManager = (ReleaseManager) lookup( ReleaseManager.ROLE, "test" );

        ReleaseDescriptor releaseDescriptor = configStore.getReleaseConfiguration();
        releaseDescriptor.setCompletedPhase( "step3" );

        releaseManager.prepare( new ReleaseDescriptor(), new DefaultReleaseEnvironment(), null );

        @SuppressWarnings("unchecked")
        Map<String,ReleasePhaseStub> phases = container.lookupMap( ReleasePhase.ROLE );

        ReleasePhaseStub phase = (ReleasePhaseStub) phases.get( "step1" );
        assertFalse( "step1 not executed", phase.isExecuted() );
        assertFalse( "step1 not simulated", phase.isSimulated() );
        phase = (ReleasePhaseStub) phases.get( "step2" );
        assertFalse( "step2 not executed", phase.isExecuted() );
        assertFalse( "step2 not simulated", phase.isSimulated() );
        phase = (ReleasePhaseStub) phases.get( "step3" );
        assertFalse( "step3 not executed", phase.isExecuted() );
        assertFalse( "step3 not simulated", phase.isSimulated() );
    }

    public void testPrepareInvalidCompletedPhase()
        throws Exception
    {
        ReleaseManager releaseManager = (ReleaseManager) lookup( ReleaseManager.ROLE, "test" );

        ReleaseDescriptor releaseDescriptor = configStore.getReleaseConfiguration();
        releaseDescriptor.setCompletedPhase( "foo" );

        releaseManager.prepare( new ReleaseDescriptor(), new DefaultReleaseEnvironment(), null );

        @SuppressWarnings("unchecked")
        Map<String,ReleasePhaseStub> phases = container.lookupMap( ReleasePhase.ROLE );

        ReleasePhaseStub phase = phases.get( "step1" );
        assertTrue( "step1 executed", phase.isExecuted() );
        assertFalse( "step1 not simulated", phase.isSimulated() );
        phase = phases.get( "step2" );
        assertTrue( "step2 executed", phase.isExecuted() );
        assertFalse( "step2 not simulated", phase.isSimulated() );
        phase = phases.get( "step3" );
        assertTrue( "step3 executed", phase.isExecuted() );
        assertFalse( "step3 not simulated", phase.isSimulated() );
    }

    public void testPrepareSimulateNoCompletedPhase()
        throws Exception
    {
        ReleaseManager releaseManager = (ReleaseManager) lookup( ReleaseManager.ROLE, "test" );

        ReleaseDescriptor releaseDescriptor = configStore.getReleaseConfiguration();
        releaseDescriptor.setCompletedPhase( null );

        releaseManager.prepare( new ReleaseDescriptor(), new DefaultReleaseEnvironment(), null, true, true );

        @SuppressWarnings("unchecked")
        Map<String,ReleasePhaseStub> phases = container.lookupMap( ReleasePhase.ROLE );

        ReleasePhaseStub phase = (ReleasePhaseStub) phases.get( "step1" );
        assertTrue( "step1 simulated", phase.isSimulated() );
        assertFalse( "step1 not executed", phase.isExecuted() );
        phase = (ReleasePhaseStub) phases.get( "step2" );
        assertTrue( "step2 simulated", phase.isSimulated() );
        assertFalse( "step2 not executed", phase.isExecuted() );
        phase = (ReleasePhaseStub) phases.get( "step3" );
        assertTrue( "step3 simulated", phase.isSimulated() );
        assertFalse( "step3 not executed", phase.isExecuted() );
    }

    public void testPrepareSimulateCompletedPhase()
        throws Exception
    {
        ReleaseManager releaseManager = (ReleaseManager) lookup( ReleaseManager.ROLE, "test" );

        ReleaseDescriptor releaseDescriptor = configStore.getReleaseConfiguration();
        releaseDescriptor.setCompletedPhase( "step1" );

        releaseManager.prepare( new ReleaseDescriptor(), new DefaultReleaseEnvironment(), null, true, true );

        @SuppressWarnings("unchecked")
        Map<String,ReleasePhaseStub> phases = container.lookupMap( ReleasePhase.ROLE );

        ReleasePhaseStub phase = (ReleasePhaseStub) phases.get( "step1" );
        assertFalse( "step1 not simulated", phase.isSimulated() );
        assertFalse( "step1 not executed", phase.isExecuted() );
        phase = (ReleasePhaseStub) phases.get( "step2" );
        assertTrue( "step2 simulated", phase.isSimulated() );
        assertFalse( "step2 not executed", phase.isExecuted() );
        phase = (ReleasePhaseStub) phases.get( "step3" );
        assertTrue( "step3 simulated", phase.isSimulated() );
        assertFalse( "step3 not executed", phase.isExecuted() );
    }

    public void testPrepareSimulateCompletedAllPhases()
        throws Exception
    {
        ReleaseManager releaseManager = (ReleaseManager) lookup( ReleaseManager.ROLE, "test" );

        ReleaseDescriptor releaseDescriptor = configStore.getReleaseConfiguration();
        releaseDescriptor.setCompletedPhase( "step3" );

        releaseManager.prepare( new ReleaseDescriptor(), new DefaultReleaseEnvironment(), null, true, true );

        @SuppressWarnings("unchecked")
        Map<String,ReleasePhaseStub> phases = container.lookupMap( ReleasePhase.ROLE );

        ReleasePhaseStub phase = (ReleasePhaseStub) phases.get( "step1" );
        assertFalse( "step1 not simulated", phase.isSimulated() );
        assertFalse( "step1 not executed", phase.isExecuted() );
        phase = (ReleasePhaseStub) phases.get( "step2" );
        assertFalse( "step2 not simulated", phase.isSimulated() );
        assertFalse( "step2 not executed", phase.isExecuted() );
        phase = (ReleasePhaseStub) phases.get( "step3" );
        assertFalse( "step3 not simulated", phase.isSimulated() );
        assertFalse( "step3 not executed", phase.isExecuted() );
    }

    public void testPrepareSimulateInvalidCompletedPhase()
        throws Exception
    {
        ReleaseManager releaseManager = (ReleaseManager) lookup( ReleaseManager.ROLE, "test" );

        ReleaseDescriptor releaseDescriptor = configStore.getReleaseConfiguration();
        releaseDescriptor.setCompletedPhase( "foo" );

        releaseManager.prepare( new ReleaseDescriptor(), new DefaultReleaseEnvironment(), null, true, true );

        @SuppressWarnings("unchecked")
        Map<String,ReleasePhaseStub> phases = container.lookupMap( ReleasePhase.ROLE );

        ReleasePhaseStub phase = (ReleasePhaseStub) phases.get( "step1" );
        assertTrue( "step1 simulated", phase.isSimulated() );
        assertFalse( "step1 not executed", phase.isExecuted() );
        phase = (ReleasePhaseStub) phases.get( "step2" );
        assertTrue( "step2 simulated", phase.isSimulated() );
        assertFalse( "step2 not executed", phase.isExecuted() );
        phase = (ReleasePhaseStub) phases.get( "step3" );
        assertTrue( "step3 simulated", phase.isSimulated() );
        assertFalse( "step3 not executed", phase.isExecuted() );
    }

    public void testPrepareUnknownPhaseConfigured()
        throws Exception
    {
        ReleaseManager releaseManager = (ReleaseManager) lookup( ReleaseManager.ROLE, "bad-phase-configured" );

        try
        {
            releaseManager.prepare( new ReleaseDescriptor(), new DefaultReleaseEnvironment(), null );
            fail( "Should have failed to find a phase" );
        }
        catch ( ReleaseExecutionException e )
        {
            // good
        }
    }

    public void testReleaseConfigurationStoreReadFailure()
        throws Exception
    {
        // prepare
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setScmSourceUrl( "scm-url" );
        releaseDescriptor.setWorkingDirectory( getTestFile( "target/working-directory" ).getAbsolutePath() );

        DefaultReleaseManager releaseManager = (DefaultReleaseManager) lookup( ReleaseManager.ROLE, "test" );

        ReleaseDescriptorStore configStoreMock = mock( ReleaseDescriptorStore.class );
        when( configStoreMock.read( releaseDescriptor ) ).thenThrow( new ReleaseDescriptorStoreException( "message", new IOException( "ioExceptionMsg" ) ) );

        releaseManager.setConfigStore( configStoreMock );

        // execute
        try
        {
            releaseManager.prepare( releaseDescriptor, new DefaultReleaseEnvironment(), null );
            fail( "Should have failed to read configuration" );
        }
        catch ( ReleaseExecutionException e )
        {
            // good
            assertEquals( "check cause", ReleaseDescriptorStoreException.class, e.getCause().getClass() );
        }
        
        // verify
        verify( configStoreMock ).read( releaseDescriptor );
        verifyNoMoreInteractions( configStoreMock );
    }

    public void testReleaseConfigurationStoreWriteFailure()
        throws Exception
    {
        // prepare
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setScmSourceUrl( "scm-url" );
        releaseDescriptor.setWorkingDirectory( getTestFile( "target/working-directory" ).getAbsolutePath() );

        DefaultReleaseManager releaseManager = (DefaultReleaseManager) lookup( ReleaseManager.ROLE, "test" );

        ReleaseDescriptorStore configStoreMock = mock( ReleaseDescriptorStore.class );
        doThrow( new ReleaseDescriptorStoreException( "message", new IOException( "ioExceptionMsg" ) ) ).when( configStoreMock ).write( releaseDescriptor );

        releaseManager.setConfigStore( configStoreMock );

        // execute
        try
        {
            releaseManager.prepare( releaseDescriptor, new DefaultReleaseEnvironment(), null, false, false );
            fail( "Should have failed to read configuration" );
        }
        catch ( ReleaseExecutionException e )
        {
            // good
            assertEquals( "check cause", ReleaseDescriptorStoreException.class, e.getCause().getClass() );
        }
        
        // verify
        verify( configStoreMock ).write( releaseDescriptor ) ;
        verifyNoMoreInteractions( configStoreMock );
    }

    public void testReleaseConfigurationStoreClean()
        throws Exception
    {
        // prepare
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setScmSourceUrl( "scm-url" );
        releaseDescriptor.setWorkingDirectory( getTestFile( "target/working-directory" ).getAbsolutePath() );

        DefaultReleaseManager releaseManager = (DefaultReleaseManager) lookup( ReleaseManager.ROLE, "test" );

        ReleaseDescriptorStore configStoreMock = mock( ReleaseDescriptorStore.class );

        releaseManager.setConfigStore( configStoreMock );

        // execute
        releaseManager.clean( releaseDescriptor, null, null );

        // verify
        @SuppressWarnings("unchecked")
        Map<String,ReleasePhaseStub> phases = container.lookupMap( ReleasePhase.ROLE );

        ReleasePhaseStub phase = (ReleasePhaseStub) phases.get( "step1" );
        assertTrue( "step1 not cleaned", phase.isCleaned() );

        phase = (ReleasePhaseStub) phases.get( "step2" );
        assertTrue( "step2 not cleaned", phase.isCleaned() );

        phase = (ReleasePhaseStub) phases.get( "step3" );
        assertTrue( "step3 not cleaned", phase.isCleaned() );

        phase = (ReleasePhaseStub) phases.get( "branch1" );
        assertTrue( "branch1 not cleaned", phase.isCleaned() );
        
        verify( configStoreMock ).delete( releaseDescriptor );
        verifyNoMoreInteractions( configStoreMock );
    }

    public void testReleasePerform()
        throws Exception
    {
        // prepare
        DefaultReleaseManager releaseManager = (DefaultReleaseManager) lookup( ReleaseManager.ROLE, "test" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setScmSourceUrl( "scm-url" );
        releaseDescriptor.setPerformGoals( "goal1 goal2" );
        File checkoutDirectory = getTestFile( "target/checkout-directory" );
        releaseDescriptor.setCheckoutDirectory( checkoutDirectory.getAbsolutePath() );

        MavenExecutor mock = mock( MavenExecutor.class );
        RunPerformGoalsPhase phase = (RunPerformGoalsPhase) lookup( ReleasePhase.ROLE, "run-perform-goals" );
        phase.setMavenExecutor( mock );

        ScmProvider scmProviderMock = mock( ScmProvider.class );
        when( scmProviderMock.checkOut( isA( ScmRepository.class ), 
                                        argThat( new IsScmFileSetEquals( new ScmFileSet( checkoutDirectory ) ) ),
                                        isA( ScmTag.class ) ) ).thenReturn( new CheckOutScmResult( "...", Collections.<ScmFile>emptyList() ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( scmProviderMock );

        // execute
        releaseManager.perform( releaseDescriptor, new DefaultReleaseEnvironment(), createReactorProjects() );

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
                                            isA( ScmTag.class ) );
        verifyNoMoreInteractions( mock, scmProviderMock );
    }

    private static List<MavenProject> createReactorProjects()
    {
        MavenProject project = new MavenProject();
        project.setFile( getTestFile( "target/dummy-project/pom.xml" ) );
        return Collections.singletonList( project );
    }

    public void testReleasePerformWithResult() 
        throws Exception
    {
        DefaultReleaseManager releaseManager = (DefaultReleaseManager) lookup( ReleaseManager.ROLE, "test" );
        
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setScmSourceUrl( "scm-url" );
        File checkoutDirectory = getTestFile( "target/checkout-directory" );
        releaseDescriptor.setCheckoutDirectory( checkoutDirectory.getAbsolutePath() );

        ReleaseResult result = releaseManager.performWithResult( releaseDescriptor, new DefaultReleaseEnvironment(),
                                                                 createReactorProjects(), null );
        assertTrue( result.getOutput().length() > 0 );
    }  

    public void testReleasePerformNoReleaseProfile()
        throws Exception
    {
        // prepare
        DefaultReleaseManager releaseManager = (DefaultReleaseManager) lookup( ReleaseManager.ROLE, "test" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setScmSourceUrl( "scm-url" );
        releaseDescriptor.setPerformGoals( "goal1 goal2" );
        File checkoutDirectory = getTestFile( "target/checkout-directory" );
        releaseDescriptor.setCheckoutDirectory( checkoutDirectory.getAbsolutePath() );

        MavenExecutor mock = mock( MavenExecutor.class );
        RunPerformGoalsPhase phase = (RunPerformGoalsPhase) lookup( ReleasePhase.ROLE, "run-perform-goals" );
        phase.setMavenExecutor( mock );

        ScmProvider scmProviderMock = mock( ScmProvider.class );
        when( scmProviderMock.checkOut( isA( ScmRepository.class ), 
                                        argThat( new IsScmFileSetEquals( new ScmFileSet( checkoutDirectory ) ) ), 
                                        isA( ScmTag.class ) ) ).thenReturn( new CheckOutScmResult( "...", Collections.<ScmFile>emptyList() ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( scmProviderMock );

        releaseDescriptor.setUseReleaseProfile( false );

        // execute
        releaseManager.perform( releaseDescriptor, new DefaultReleaseEnvironment(), createReactorProjects() );

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
                                            isA( ScmTag.class ) );
        verifyNoMoreInteractions( mock, scmProviderMock );
    }

    public void testReleasePerformWithArguments()
        throws Exception
    {
        // prepare
        DefaultReleaseManager releaseManager = (DefaultReleaseManager) lookup( ReleaseManager.ROLE, "test" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setScmSourceUrl( "scm-url" );
        releaseDescriptor.setAdditionalArguments( "-Dmaven.test.skip=true" );
        releaseDescriptor.setPerformGoals( "goal1 goal2" );
        File checkoutDirectory = getTestFile( "target/checkout-directory" );
        releaseDescriptor.setCheckoutDirectory( checkoutDirectory.getAbsolutePath() );

        MavenExecutor mock = mock( MavenExecutor.class );
        RunPerformGoalsPhase phase = (RunPerformGoalsPhase) lookup( ReleasePhase.ROLE, "run-perform-goals" );
        phase.setMavenExecutor( mock );

        ScmProvider scmProviderMock = mock( ScmProvider.class );
        when( scmProviderMock.checkOut( isA( ScmRepository.class ),
                                        argThat( new IsScmFileSetEquals( new ScmFileSet( checkoutDirectory ) ) ),
                                        isA( ScmTag.class ) ) ).thenReturn( new CheckOutScmResult( "...", Collections.<ScmFile>emptyList() ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( scmProviderMock );

        // execute
        releaseManager.perform( releaseDescriptor, new DefaultReleaseEnvironment(), createReactorProjects() );

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
                                            isA( ScmTag.class ) );
        verifyNoMoreInteractions( mock, scmProviderMock );
    }

    public void testReleasePerformWithArgumentsNoReleaseProfile()
        throws Exception
    {
        // prepare
        DefaultReleaseManager releaseManager = (DefaultReleaseManager) lookup( ReleaseManager.ROLE, "test" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setScmSourceUrl( "scm-url" );
        releaseDescriptor.setAdditionalArguments( "-Dmaven.test.skip=true" );
        releaseDescriptor.setPerformGoals( "goal1 goal2" );
        File checkoutDirectory = getTestFile( "target/checkout-directory" );
        releaseDescriptor.setCheckoutDirectory( checkoutDirectory.getAbsolutePath() );

        MavenExecutor mock = mock( MavenExecutor.class );
        RunPerformGoalsPhase phase = (RunPerformGoalsPhase) lookup( ReleasePhase.ROLE, "run-perform-goals" );
        phase.setMavenExecutor( mock );

        ScmProvider scmProviderMock = mock( ScmProvider.class );
        when( scmProviderMock.checkOut( isA( ScmRepository.class ),
                                        argThat( new IsScmFileSetEquals( new ScmFileSet( checkoutDirectory ) ) ),
                                        isA( ScmTag.class ) ) ).thenReturn( new CheckOutScmResult( "...", Collections.<ScmFile>emptyList() ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( scmProviderMock );

        releaseDescriptor.setUseReleaseProfile( false );

        // execute
        releaseManager.perform( releaseDescriptor, new DefaultReleaseEnvironment(), createReactorProjects() );

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
                                            isA( ScmTag.class ) );
        verifyNoMoreInteractions( mock, scmProviderMock );
    }

    public void testReleasePerformWithReleasePropertiesCompleted()
        throws Exception
    {
        // prepare
        DefaultReleaseManager releaseManager = (DefaultReleaseManager) lookup( ReleaseManager.ROLE, "test" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setScmSourceUrl( "scm-url" );
        releaseDescriptor.setPerformGoals( "goal1 goal2" );
        File checkoutDirectory = getTestFile( "target/checkout-directory" );
        releaseDescriptor.setCheckoutDirectory( checkoutDirectory.getAbsolutePath() );

        MavenExecutor mock = mock( MavenExecutor.class );
        RunPerformGoalsPhase phase = (RunPerformGoalsPhase) lookup( ReleasePhase.ROLE, "run-perform-goals" );
        phase.setMavenExecutor( mock );

        ScmProvider scmProviderMock = mock( ScmProvider.class );
        when( scmProviderMock.checkOut( isA( ScmRepository.class ),
                                        argThat( new IsScmFileSetEquals( new ScmFileSet( checkoutDirectory ) ) ),
                                        isA( ScmTag.class ) ) ).thenReturn( new CheckOutScmResult( "...", Collections.<ScmFile>emptyList() ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( scmProviderMock );

        ReleaseDescriptorStoreStub configStore = new ReleaseDescriptorStoreStub();
        configStore.getReleaseConfiguration().setCompletedPhase( "end-release" );
        releaseManager.setConfigStore( configStore );

        // execute
        releaseManager.perform( releaseDescriptor, new DefaultReleaseEnvironment(), createReactorProjects() );

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
                                            isA( ScmTag.class ) );
        verifyNoMoreInteractions( mock, scmProviderMock );
    }

    public void testReleaseConfigurationStoreReadFailureOnPerform()
        throws Exception
    {
        // prepare
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setScmSourceUrl( "scm-url" );
        releaseDescriptor.setWorkingDirectory( getTestFile( "target/working-directory" ).getAbsolutePath() );

        DefaultReleaseManager releaseManager = (DefaultReleaseManager) lookup( ReleaseManager.ROLE, "test" );

        ReleaseDescriptorStore configStoreMock = mock( ReleaseDescriptorStore.class );
        when( configStoreMock.read( releaseDescriptor ) ).thenThrow( new ReleaseDescriptorStoreException( "message", new IOException( "ioExceptionMsg" ) ) );

        releaseManager.setConfigStore( configStoreMock );

        // execute
        try
        {
            releaseDescriptor.setUseReleaseProfile( false );

            releaseManager.perform( releaseDescriptor, new DefaultReleaseEnvironment(), null );
            fail( "Should have failed to read configuration" );
        }
        catch ( ReleaseExecutionException e )
        {
            // good
            assertEquals( "check cause", ReleaseDescriptorStoreException.class, e.getCause().getClass() );
        }
        
        // verify
        verify( configStoreMock ).read( releaseDescriptor );
        verifyNoMoreInteractions( configStoreMock );
    }

    public void testReleasePerformWithIncompletePrepare()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setScmSourceUrl( "scm-url" );
        releaseDescriptor.setWorkingDirectory( getTestFile( "target/working-directory" ).getAbsolutePath() );

        DefaultReleaseManager releaseManager = (DefaultReleaseManager) lookup( ReleaseManager.ROLE, "test" );

        ReleaseDescriptorStoreStub configStore = new ReleaseDescriptorStoreStub();
        releaseDescriptor.setCompletedPhase( "scm-tag" );
        releaseManager.setConfigStore( configStore );

        try
        {
            releaseDescriptor.setUseReleaseProfile( false );

            releaseManager.perform( releaseDescriptor, new DefaultReleaseEnvironment(), null );
            fail( "Should have failed to perform" );
        }
        catch ( ReleaseFailureException e )
        {
            // good
            assertTrue( true );
        }
    }
    
    // MRELEASE-758: release:perform no longer removes release.properties
    @SuppressWarnings( "unchecked" )
    public void testPerformWithDefaultClean()
        throws Exception
    {
        // prepare
        ReleasePerformRequest performRequest = new ReleasePerformRequest();
        performRequest.setDryRun( true );
        
        ReleaseManagerListener managerListener = mock( ReleaseManagerListener.class );
        performRequest.setReleaseManagerListener( managerListener );
        
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setScmSourceUrl( "scm-url" );
        releaseDescriptor.setWorkingDirectory( getTestFile( "target/working-directory" ).getAbsolutePath() );
        performRequest.setReleaseDescriptor( releaseDescriptor );
        
        DefaultReleaseManager releaseManager = (DefaultReleaseManager) lookup( ReleaseManager.ROLE, "test" );
        
        // test
        releaseManager.perform( performRequest );

        // verify
        verify( managerListener ).phaseStart( "verify-release-configuration" );
        verify( managerListener ).phaseStart( "verify-completed-prepare-phases" );
        verify( managerListener ).phaseStart( "checkout-project-from-scm" );
        verify( managerListener ).phaseStart( "run-perform-goals" );
        verify( managerListener ).phaseStart( "cleanup" );
        verify( managerListener, times( 5 ) ).phaseEnd();
        
        // not part of actual test, but required to confirm 'no more interactions'
        verify( managerListener ).goalStart( anyString(), any( List.class ) );
        verify( managerListener ).goalEnd();
        
        verifyNoMoreInteractions( managerListener );
    }

    public void testNoScmUrlPerform()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setWorkingDirectory( getTestFile( "target/test/checkout" ).getAbsolutePath() );

        DefaultReleaseManager releaseManager = (DefaultReleaseManager) lookup( ReleaseManager.ROLE, "test" );

        try
        {
            releaseDescriptor.setUseReleaseProfile( false );

            releaseManager.perform( releaseDescriptor, new DefaultReleaseEnvironment(), null );

            fail( "perform should have failed" );
        }
        catch ( ReleaseFailureException e )
        {
            assertNull( "check no cause", e.getCause() );
        }
    }

    public void testNoSuchScmProviderExceptionThrown()
        throws Exception
    {
        // prepare
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setScmSourceUrl( "scm-url" );
        releaseDescriptor.setWorkingDirectory( getTestFile( "target/test/checkout" ).getAbsolutePath() );

        ScmManager scmManagerMock = mock( ScmManager.class );
        when( scmManagerMock.makeScmRepository( "scm-url" ) ).thenThrow( new NoSuchScmProviderException( "..." ) );

        DefaultScmRepositoryConfigurator configurator =
            (DefaultScmRepositoryConfigurator) lookup( ScmRepositoryConfigurator.ROLE );
        configurator.setScmManager( scmManagerMock );

        DefaultReleaseManager releaseManager = (DefaultReleaseManager) lookup( ReleaseManager.ROLE, "test" );

        // execute
        try
        {
            releaseDescriptor.setUseReleaseProfile( false );

            releaseManager.perform( releaseDescriptor, new DefaultReleaseEnvironment(), null );

            fail( "commit should have failed" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertEquals( "check cause", NoSuchScmProviderException.class, e.getCause().getClass() );
        }
        
        // verify
        verify( scmManagerMock ).makeScmRepository( "scm-url" );
        verifyNoMoreInteractions( scmManagerMock );
    }

    public void testScmRepositoryExceptionThrown()
        throws Exception
    {
        // prepare
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setScmSourceUrl( "scm-url" );
        releaseDescriptor.setWorkingDirectory( getTestFile( "target/test/checkout" ).getAbsolutePath() );

        ScmManager scmManagerMock = mock( ScmManager.class );
        when( scmManagerMock.makeScmRepository( "scm-url" ) ).thenThrow( new ScmRepositoryException( "..." ) );

        DefaultScmRepositoryConfigurator configurator =
            (DefaultScmRepositoryConfigurator) lookup( ScmRepositoryConfigurator.ROLE );
        configurator.setScmManager( scmManagerMock );

        DefaultReleaseManager releaseManager = (DefaultReleaseManager) lookup( ReleaseManager.ROLE, "test" );

        // execute
        try
        {
            releaseDescriptor.setUseReleaseProfile( false );

            releaseManager.perform( releaseDescriptor, new DefaultReleaseEnvironment(), null );

            fail( "commit should have failed" );
        }
        catch ( ReleaseScmRepositoryException e )
        {
            assertNull( "Check no additional cause", e.getCause() );
        }
        
        // verify
        verify( scmManagerMock ).makeScmRepository( "scm-url" );
        verifyNoMoreInteractions( scmManagerMock );
    }

    public void testScmExceptionThrown()
        throws Exception
    {
        // prepare
        DefaultReleaseManager releaseManager = (DefaultReleaseManager) lookup( ReleaseManager.ROLE, "test" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setScmSourceUrl( "scm-url" );
        File checkoutDirectory = getTestFile( "target/checkout-directory" );
        releaseDescriptor.setCheckoutDirectory( checkoutDirectory.getAbsolutePath() );

        ScmProvider scmProviderMock = mock( ScmProvider.class );
        when( scmProviderMock.checkOut( any( ScmRepository.class ), any( ScmFileSet.class ), any ( ScmTag.class ) ) ).thenThrow( new ScmException( "..." ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( scmProviderMock );

        // execute
        try
        {
            releaseManager.perform( releaseDescriptor, new DefaultReleaseEnvironment(), createReactorProjects() );

            fail( "commit should have failed" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertEquals( "check cause", ScmException.class, e.getCause().getClass() );
        }
        
        // verify
        verify(  scmProviderMock ).checkOut( any( ScmRepository.class ), any( ScmFileSet.class ), any ( ScmTag.class ) );
        verifyNoMoreInteractions( scmProviderMock );
    }

    public void testScmResultFailure()
        throws Exception
    {
        DefaultReleaseManager releaseManager = (DefaultReleaseManager) lookup( ReleaseManager.ROLE, "test" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setScmSourceUrl( "scm-url" );
        File checkoutDirectory = getTestFile( "target/checkout-directory" );
        releaseDescriptor.setCheckoutDirectory( checkoutDirectory.getAbsolutePath() );

        ScmManager scmManager = (ScmManager) lookup( ScmManager.ROLE );
        ScmProviderStub providerStub =
            (ScmProviderStub) scmManager.getProviderByUrl( releaseDescriptor.getScmSourceUrl() );

        providerStub.setCheckOutScmResult( new CheckOutScmResult( "", "", "", false ) );

        try
        {
            releaseManager.perform( releaseDescriptor, new DefaultReleaseEnvironment(), createReactorProjects() );

            fail( "commit should have failed" );
        }
        catch ( ReleaseScmCommandException e )
        {
            assertNull( "check no other cause", e.getCause() );
        }
    }

    public void testDetermineWorkingDirectory()
        throws Exception
    {
        DefaultReleaseManager defaultReleaseManager = new DefaultReleaseManager();

        File checkoutDir = getTestFile( "target/checkout" );
        FileUtils.forceDelete( checkoutDir );
        checkoutDir.mkdirs();

        File projectDir = getTestFile( "target/checkout/my/project" );
        projectDir.mkdirs();

        // only checkout dir
        assertEquals( checkoutDir, defaultReleaseManager.determineWorkingDirectory( checkoutDir, "" ) );
        assertEquals( checkoutDir, defaultReleaseManager.determineWorkingDirectory( checkoutDir, null ) );

        // checkout dir and relative path project dir
        assertEquals( projectDir, defaultReleaseManager.determineWorkingDirectory( checkoutDir, "my/project" ) );
        assertEquals( projectDir, defaultReleaseManager.determineWorkingDirectory( checkoutDir, "my/project/" ) );
        assertEquals( projectDir, defaultReleaseManager.determineWorkingDirectory( checkoutDir, "my" + File.separator +
            "project" ) );

        FileUtils.forceDelete( checkoutDir );
    }

    // MRELEASE-761
    @SuppressWarnings( "unchecked" )
    public void testRollbackCall()
        throws Exception
    {
        DefaultReleaseManager defaultReleaseManager = (DefaultReleaseManager) lookup( ReleaseManager.ROLE, "test" );

        ReleasePhase rollbackPhase1 = mock( ReleasePhase.class );
        ReflectionUtils.setVariableValueInObject( defaultReleaseManager, "rollbackPhases",
                                                  Collections.singletonList( "rollbackPhase1" ) );
        Map<String, ReleasePhase> releasePhases =
            (Map<String, ReleasePhase>) ReflectionUtils.getValueIncludingSuperclasses( "releasePhases",
                                                                                       defaultReleaseManager );
        releasePhases.put( "rollbackPhase1", rollbackPhase1 );

        defaultReleaseManager.rollback( configStore.getReleaseConfiguration(), (ReleaseEnvironment) null, null );

        verify( rollbackPhase1 ).execute( any( ReleaseDescriptor.class ), any( ReleaseEnvironment.class ),
                                                any( List.class ) );
        verifyNoMoreInteractions( rollbackPhase1 );
    }


    // MRELEASE-765
    @SuppressWarnings( "unchecked" )
    public void testUpdateVersionsCall()
        throws Exception
    {
        DefaultReleaseManager defaultReleaseManager = (DefaultReleaseManager) lookup( ReleaseManager.ROLE, "test" );

        ReleasePhase updateVersionsPhase1 = mock( ReleasePhase.class );
        ReflectionUtils.setVariableValueInObject( defaultReleaseManager, "updateVersionsPhases",
                                                  Collections.singletonList( "updateVersionsPhase1" ) );
        Map<String, ReleasePhase> releasePhases =
            (Map<String, ReleasePhase>) ReflectionUtils.getValueIncludingSuperclasses( "releasePhases",
                                                                                       defaultReleaseManager );
        releasePhases.put( "updateVersionsPhase1", updateVersionsPhase1 );

        defaultReleaseManager.updateVersions( configStore.getReleaseConfiguration(), null, null );

        verify( updateVersionsPhase1 ).execute( any( ReleaseDescriptor.class ), any( ReleaseEnvironment.class ),
                                                any( List.class ) );
        verifyNoMoreInteractions( updateVersionsPhase1 );
    }
}