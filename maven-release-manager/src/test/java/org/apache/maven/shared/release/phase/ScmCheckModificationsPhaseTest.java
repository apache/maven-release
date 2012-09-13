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

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFile;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmFileStatus;
import org.apache.maven.scm.command.status.StatusScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.manager.ScmManagerStub;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.provider.ScmProviderStub;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.scm.DefaultScmRepositoryConfigurator;
import org.apache.maven.shared.release.scm.ReleaseScmCommandException;
import org.apache.maven.shared.release.scm.ReleaseScmRepositoryException;
import org.apache.maven.shared.release.scm.ScmRepositoryConfigurator;
import org.codehaus.plexus.PlexusTestCase;

/**
 * Test the SCM modification check phase.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ScmCheckModificationsPhaseTest
    extends PlexusTestCase
{
    private ReleasePhase phase;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        phase = (ReleasePhase) lookup( ReleasePhase.ROLE, "scm-check-modifications" );
    }

    public void testNoSuchScmProviderExceptionThrown()
        throws Exception
    {
        // prepare
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setScmSourceUrl( "scm-url" );
        releaseDescriptor.setWorkingDirectory( getTestFile( "target/test/checkout" ).getAbsolutePath() );

        ScmManager scmManagerMock = mock( ScmManager.class );
        when( scmManagerMock.makeScmRepository( eq( "scm-url" ) ) ).thenThrow( new NoSuchScmProviderException( "..." ) );

        DefaultScmRepositoryConfigurator configurator =
            (DefaultScmRepositoryConfigurator) lookup( ScmRepositoryConfigurator.ROLE );
        configurator.setScmManager( scmManagerMock );

        // execute
        try
        {
            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), null );

            fail( "Status check should have failed" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertEquals( "check cause", NoSuchScmProviderException.class, e.getCause().getClass() );
        }

        try
        {
            phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), null );

            fail( "Status check should have failed" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertEquals( "check cause", NoSuchScmProviderException.class, e.getCause().getClass() );
        }
        
        // verify
        verify( scmManagerMock, times( 2 ) ).makeScmRepository( eq( "scm-url" ) );
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
        when( scmManagerMock.makeScmRepository( eq( "scm-url" ) ) ).thenThrow( new ScmRepositoryException( "..." ) );

        DefaultScmRepositoryConfigurator configurator =
            (DefaultScmRepositoryConfigurator) lookup( ScmRepositoryConfigurator.ROLE );
        configurator.setScmManager( scmManagerMock );

        // execute
        try
        {
            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), null );

            fail( "Status check should have failed" );
        }
        catch ( ReleaseScmRepositoryException e )
        {
            assertNull( "Check no additional cause", e.getCause() );
        }

        try
        {
            phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), null );

            fail( "Status check should have failed" );
        }
        catch ( ReleaseScmRepositoryException e )
        {
            assertNull( "Check no additional cause", e.getCause() );
        }
        
        // verify
        verify( scmManagerMock, times( 2 ) ).makeScmRepository( eq( "scm-url" ) );
        verifyNoMoreInteractions( scmManagerMock );
    }

    public void testScmExceptionThrown()
        throws Exception
    {
        // prepare
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setScmSourceUrl( "scm-url" );
        releaseDescriptor.setWorkingDirectory( getTestFile( "target/test/checkout" ).getAbsolutePath() );

        ScmProvider scmProviderMock = mock( ScmProvider.class );
        when( scmProviderMock.status( isA( ScmRepository.class ), isA( ScmFileSet.class ) ) ).thenThrow( new ScmException( "..." ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( scmProviderMock );

        // execute
        try
        {
            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), null );

            fail( "Status check should have failed" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertEquals( "check cause", ScmException.class, e.getCause().getClass() );
        }

        try
        {
            phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), null );

            fail( "Status check should have failed" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertEquals( "check cause", ScmException.class, e.getCause().getClass() );
        }
        
        // verify
        verify( scmProviderMock, times( 2 ) ).status( isA( ScmRepository.class ), isA( ScmFileSet.class ) );
        verifyNoMoreInteractions( scmProviderMock );
    }

    public void testScmResultFailure()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = createReleaseDescriptor();

        ScmManager scmManager = (ScmManager) lookup( ScmManager.ROLE );
        ScmProviderStub providerStub =
            (ScmProviderStub) scmManager.getProviderByUrl( releaseDescriptor.getScmSourceUrl() );

        providerStub.setStatusScmResult( new StatusScmResult( "", "", "", false ) );

        try
        {
            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), null );

            fail( "Status check should have failed" );
        }
        catch ( ReleaseScmCommandException e )
        {
            assertNull( "check no other cause", e.getCause() );
        }

        try
        {
            phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), null );

            fail( "Status check should have failed" );
        }
        catch ( ReleaseScmCommandException e )
        {
            assertNull( "check no other cause", e.getCause() );
        }
    }

    public void testNoModifications()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = createReleaseDescriptor();

        setChangedFiles( releaseDescriptor, Collections.<String>emptyList() );

        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), null );

        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), null );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testModificationsToExcludedFilesOnly()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = createReleaseDescriptor();

        setChangedFiles( releaseDescriptor, Arrays.asList( "release.properties", "pom.xml.backup",
            "pom.xml.tag", "pom.xml.next" ) );

        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), null );

        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), null );

        // successful execution is verification enough
        assertTrue( true );
    }
    
    // MRELEASE-645: Allow File/Directory Patterns for the checkModificationExcludes Option
    public void testModificationsToCustomExcludedFilesOnly()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = createReleaseDescriptor();
        
        releaseDescriptor.setCheckModificationExcludes( Collections.singletonList( "**/keep.me" ) );
    
        setChangedFiles( releaseDescriptor, Arrays.asList( "release.properties", "pom.xml.backup",
            "pom.xml.tag", "pom.xml.next", "keep.me", "src/app/keep.me", "config\\keep.me" ) );
    
        assertEquals( ReleaseResult.SUCCESS, phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), null ).getResultCode() );
    
        assertEquals( ReleaseResult.SUCCESS, phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), null ).getResultCode() );
    }

    public void testModificationsToPoms()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = createReleaseDescriptor();

        setChangedFiles( releaseDescriptor, Arrays.asList( "pom.xml", "module/pom.xml" ) );

        try
        {
            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), null );

            fail( "Status check should have failed" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        try
        {
            phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), null );

            fail( "Status check should have failed" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testModificationsToIncludedFilesOnly()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = createReleaseDescriptor();

        setChangedFiles( releaseDescriptor, Collections.singletonList( "something.txt" ) );

        try
        {
            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), null );

            fail( "Status check should have failed" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        try
        {
            phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), null );

            fail( "Status check should have failed" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testModificationsToIncludedAndExcludedFiles()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = createReleaseDescriptor();

        setChangedFiles( releaseDescriptor, Arrays.asList( "release.properties", "pom.xml.backup",
            "pom.xml.tag", "pom.xml.release", "something.txt" ) );

        try
        {
            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), null );

            fail( "Status check should have failed" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        try
        {
            phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), null );

            fail( "Status check should have failed" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }
    
    public void testModificationsToAdditionalExcludedFiles()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = createReleaseDescriptor();
        releaseDescriptor.setCheckModificationExcludes( Collections.singletonList( "something.*" ) );

        setChangedFiles( releaseDescriptor, Collections.singletonList( "something.txt" ) );

        assertEquals( ReleaseResult.SUCCESS,  phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), null ).getResultCode() );
        
        assertEquals( ReleaseResult.SUCCESS,  phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), null ).getResultCode() );
    }

    // MRELEASE-775
    public void testMultipleExclusionPatternMatch() throws Exception
    {
        ReleaseDescriptor releaseDescriptor = createReleaseDescriptor();
        
        releaseDescriptor.setCheckModificationExcludes( Collections.singletonList( "release.properties" ) );
    
        setChangedFiles( releaseDescriptor, Arrays.asList( "release.properties" ) );
    
        assertEquals( ReleaseResult.SUCCESS, phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), null ).getResultCode() );
    
        assertEquals( ReleaseResult.SUCCESS, phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), null ).getResultCode() );
    }

    private void setChangedFiles( ReleaseDescriptor releaseDescriptor, List<String> changedFiles )
        throws Exception
    {
        ScmManager scmManager = (ScmManager) lookup( ScmManager.ROLE );
        ScmProviderStub providerStub =
            (ScmProviderStub) scmManager.getProviderByUrl( releaseDescriptor.getScmSourceUrl() );

        providerStub.setStatusScmResult( new StatusScmResult( "", createScmFiles( changedFiles ) ) );
    }

    private static List<ScmFile> createScmFiles( List<String> changedFiles )
    {
        List<ScmFile> files = new ArrayList<ScmFile>( changedFiles.size() );
        for ( Iterator<String> i = changedFiles.iterator(); i.hasNext(); )
        {
            String fileName = i.next();
            files.add( new ScmFile( fileName, ScmFileStatus.MODIFIED ) );
        }
        return files;
    }

    private static ReleaseDescriptor createReleaseDescriptor()
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setScmSourceUrl( "scm:svn:file://localhost/tmp/scm-repo" );
        releaseDescriptor.setWorkingDirectory( getTestFile( "target/test/checkout" ).getAbsolutePath() );
        return releaseDescriptor;
    }
}
