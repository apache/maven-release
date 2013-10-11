package org.apache.maven.shared.release.exec;

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
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.env.ReleaseEnvironment;

public class AbstractMavenExecutorTest
    extends TestCase
{

    public void testGoalSeparation()
        throws MavenExecutorException
    {
        AbstractMavenExecutor executor = spy( new AbstractMavenExecutorSpy() );

        executor.executeGoals( null, null, true, null, null );
        verify( executor ).executeGoals( isNull( File.class ), eq( new ArrayList<String>() ),
                                         isA( ReleaseEnvironment.class ), eq( true ), isNull( String.class ),
                                         isNull( String.class ), isNull( ReleaseResult.class ) );
        reset( executor );

        executor.executeGoals( null, " clean verify ", true, null, null );
        verify( executor ).executeGoals( isNull( File.class ),
                                         eq( Arrays.asList( "clean", "verify" ) ),
                                         isA( ReleaseEnvironment.class ), eq( true ), isNull( String.class ),
                                         isNull( String.class ), isNull( ReleaseResult.class ) );
        reset( executor );

        executor.executeGoals( null, ",clean,verify,", true, null, null );
        verify( executor ).executeGoals( isNull( File.class ),
                                         eq( Arrays.asList( "clean", "verify" ) ),
                                         isA( ReleaseEnvironment.class ), eq( true ), isNull( String.class ),
                                         isNull( String.class ), isNull( ReleaseResult.class ) );
        reset( executor );

        executor.executeGoals( null, "\nclean\nverify\n", true, null, null );
        verify( executor ).executeGoals( isNull( File.class ),
                                         eq( Arrays.asList( "clean", "verify" ) ),
                                         isA( ReleaseEnvironment.class ), eq( true ), isNull( String.class ),
                                         isNull( String.class ), isNull( ReleaseResult.class ) );
        reset( executor );

        executor.executeGoals( null, "\rclean\rverify\r", true, null, null );
        verify( executor ).executeGoals( isNull( File.class ),
                                         eq( Arrays.asList( "clean", "verify" ) ),
                                         isA( ReleaseEnvironment.class ), eq( true ), isNull( String.class ),
                                         isNull( String.class ), isNull( ReleaseResult.class ) );
        reset( executor );

        executor.executeGoals( null, "\r\nclean\r\nverify\r\n", true, null, null );
        verify( executor ).executeGoals( isNull( File.class ),
                                         eq( Arrays.asList( "clean", "verify" ) ),
                                         isA( ReleaseEnvironment.class ), eq( true ), isNull( String.class ),
                                         isNull( String.class ), isNull( ReleaseResult.class ) );
        reset( executor );

        executor.executeGoals( null, "\tclean\tverify\t", true, null, null );
        verify( executor ).executeGoals( isNull( File.class ),
                                         eq( Arrays.asList( "clean", "verify" ) ),
                                         isA( ReleaseEnvironment.class ), eq( true ), isNull( String.class ),
                                         isNull( String.class ), isNull( ReleaseResult.class ) );
        reset( executor );
    }

    protected class AbstractMavenExecutorSpy
        extends AbstractMavenExecutor
    {

        @Override
        protected void executeGoals( File workingDirectory, List<String> goals, ReleaseEnvironment releaseEnvironment,
                                     boolean interactive, String additionalArguments, String pomFileName,
                                     ReleaseResult result )
            throws MavenExecutorException
        {
        }
    }
}