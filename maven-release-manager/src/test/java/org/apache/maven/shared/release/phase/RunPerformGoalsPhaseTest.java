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
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.File;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.exec.MavenExecutor;
import org.apache.maven.shared.release.exec.MavenExecutorException;
import org.codehaus.plexus.PlexusTestCase;

/**
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 */
public class RunPerformGoalsPhaseTest
    extends PlexusTestCase
{
    private RunPerformGoalsPhase phase;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        phase = (RunPerformGoalsPhase) lookup( ReleasePhase.ROLE, "run-perform-goals" );
    }

    public void testExecuteException()
        throws Exception
    {
        // prepare
        File testFile = getTestFile( "target/checkout-directory" );

        ReleaseDescriptor config = new ReleaseDescriptor();
        config.setPerformGoals( "goal1 goal2" );
        config.setCheckoutDirectory( testFile.getAbsolutePath() );

        MavenExecutor mock = mock( MavenExecutor.class );

        doThrow( new MavenExecutorException( "...", new Exception() ) ).when( mock ).executeGoals( eq( testFile ),
                                                                                                   eq( "goal1 goal2" ),
                                                                                                   isA( ReleaseEnvironment.class ),
                                                                                                   eq( true ),
                                                                                                   eq( "-DperformRelease=true -f pom.xml" ),
                                                                                                   isNull( String.class ),
                                                                                                   isA( ReleaseResult.class ) );

        phase.setMavenExecutor(ReleaseEnvironment.DEFAULT_MAVEN_EXECUTOR_ID, mock );

        // execute
        try
        {
            phase.execute( config, (Settings) null, (List<MavenProject>) null );

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

    public void testCustomPomFile() throws Exception
    {
        //prepare
        File testFile = getTestFile( "target/checkout-directory" );
        ReleaseDescriptor config = new ReleaseDescriptor();
        config.setPerformGoals( "goal1 goal2" );
        config.setPomFileName( "pom1.xml" );
        config.setCheckoutDirectory( testFile.getAbsolutePath() );
        
        MavenExecutor mock = mock( MavenExecutor.class );
        
        phase.setMavenExecutor(ReleaseEnvironment.DEFAULT_MAVEN_EXECUTOR_ID, mock );
        
        phase.execute( config, (Settings) null, (List<MavenProject>) null );
        
        verify( mock ).executeGoals( eq( testFile ),
                                     eq( "goal1 goal2" ),
                                     isA( ReleaseEnvironment.class ),
                                     eq( true ),
                                     eq( "-DperformRelease=true -f pom1.xml" ),
                                     eq( "pom1.xml" ),
                                     isA( ReleaseResult.class ) );
        
        verifyNoMoreInteractions( mock );
    }
}
