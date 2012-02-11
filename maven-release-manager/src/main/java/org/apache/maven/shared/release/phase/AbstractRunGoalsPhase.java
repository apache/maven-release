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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.exec.MavenExecutor;
import org.apache.maven.shared.release.exec.MavenExecutorException;
import org.codehaus.plexus.util.StringUtils;

/**
 * Run the integration tests for the project to verify that it builds before committing.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class AbstractRunGoalsPhase
    extends AbstractReleasePhase
{
    /**
     * Component to assist in executing Maven.
     *
     * @plexus.requirement role="org.apache.maven.shared.release.exec.MavenExecutor"
     */
    private Map<String, MavenExecutor> mavenExecutors;

    /**
     * @deprecated Use {@link AbstractRunGoalsPhase#execute(ReleaseDescriptor, ReleaseEnvironment, File, String)} instead.
     */
    public ReleaseResult execute( ReleaseDescriptor releaseDescriptor, File workingDirectory,
                                  String additionalArguments )
        throws ReleaseExecutionException
    {
        return execute( releaseDescriptor, new DefaultReleaseEnvironment(), workingDirectory, additionalArguments );
    }

    public ReleaseResult execute( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                                  File workingDirectory, String additionalArguments )
        throws ReleaseExecutionException
    {
        ReleaseResult result = new ReleaseResult();

        try
        {
            String goals = getGoals( releaseDescriptor );
            if ( !StringUtils.isEmpty( goals ) )
            {
                logInfo( result, "Executing goals '" + goals + "'..." );

                MavenExecutor mavenExecutor = mavenExecutors.get( releaseEnvironment.getMavenExecutorId() );

                if ( mavenExecutor == null )
                {
                    throw new ReleaseExecutionException(
                        "Cannot find Maven executor with id: " + releaseEnvironment.getMavenExecutorId() );
                }

                mavenExecutor.executeGoals( determineWorkingDirectory( workingDirectory,
                                                                       releaseDescriptor.getScmRelativePathProjectDirectory() ),
                                            goals, releaseEnvironment, releaseDescriptor.isInteractive(),
                                            additionalArguments, releaseDescriptor.getPomFileName(), result );
            }
        }
        catch ( MavenExecutorException e )
        {
            throw new ReleaseExecutionException( e.getMessage(), e );
        }

        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }

    /**
     * @deprecated Use {@link AbstractRunGoalsPhase#setMavenExecutor(String, MavenExecutor)} instead.
     */
    public void setMavenExecutor( MavenExecutor mavenExecutor )
    {
        setMavenExecutor( ReleaseEnvironment.DEFAULT_MAVEN_EXECUTOR_ID, mavenExecutor );
    }

    public void setMavenExecutor( String id, MavenExecutor executor )
    {
        if ( mavenExecutors == null )
        {
            mavenExecutors = new HashMap<String, MavenExecutor>();
        }

        mavenExecutors.put( id, executor );
    }

    protected abstract String getGoals( ReleaseDescriptor releaseDescriptor );

    /**
     * Determines the path of the working directory. By default, this is the
     * checkout directory. For some SCMs, the project root directory is not the
     * checkout directory itself, but a SCM-specific subdirectory.
     *
     * @param checkoutDirectory            The checkout directory as java.io.File
     * @param relativePathProjectDirectory The relative path of the project directory within the checkout
     *                                     directory or ""
     * @return The working directory
     */
    protected File determineWorkingDirectory( File checkoutDirectory, String relativePathProjectDirectory )
    {
        File workingDirectory = checkoutDirectory;

        if ( StringUtils.isNotEmpty( relativePathProjectDirectory ) )
        {
            workingDirectory = new File( checkoutDirectory, relativePathProjectDirectory );
        }

        return workingDirectory;
    }
}
