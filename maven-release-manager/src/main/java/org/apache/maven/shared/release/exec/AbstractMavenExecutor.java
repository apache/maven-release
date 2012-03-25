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

import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractMavenExecutor
    implements MavenExecutor, LogEnabled
{

    private Logger logger;

    protected AbstractMavenExecutor()
    {
    }

    /** {@inheritDoc} */
    public void executeGoals( File workingDirectory, String goals, boolean interactive, String additionalArguments,
                              String pomFileName, ReleaseResult result )
        throws MavenExecutorException
    {
        executeGoals( workingDirectory, goals, new DefaultReleaseEnvironment(), interactive, additionalArguments, pomFileName, result );
    }

    /** {@inheritDoc} */
    public void executeGoals( File workingDirectory, String goals, boolean interactive, String additionalArguments,
                              ReleaseResult result )
        throws MavenExecutorException
    {
        executeGoals( workingDirectory, goals, new DefaultReleaseEnvironment(), interactive, additionalArguments, result );
    }

    /** {@inheritDoc} */
    public void executeGoals( File workingDirectory, String goals, ReleaseEnvironment releaseEnvironment,
                              boolean interactive, String arguments, ReleaseResult result )
        throws MavenExecutorException
    {
        executeGoals( workingDirectory, goals, releaseEnvironment, interactive, arguments, null, result );
    }

    /** {@inheritDoc} */
    public void executeGoals( File workingDirectory, String goals, ReleaseEnvironment releaseEnvironment,
                              boolean interactive, String additionalArguments, String pomFileName, ReleaseResult result )
        throws MavenExecutorException
    {
        List<String> goalsList = new ArrayList<String>();
        if ( goals != null )
        {
            // accept both space and comma, so the old way still work
            // also accept line separators, so that goal lists can be spread
            // across multiple lines in the POM.
            String[] tokens = StringUtils.split( goals, ", \n\r\t" );

            for ( int i = 0; i < tokens.length; ++i )
            {
                goalsList.add( tokens[i] );
            }
        }
        executeGoals( workingDirectory, goalsList, releaseEnvironment, interactive, additionalArguments, pomFileName, result );
    }

    protected abstract void executeGoals( File workingDirectory, List<String> goals, ReleaseEnvironment releaseEnvironment,
                              boolean interactive, String additionalArguments, String pomFileName, ReleaseResult result )
        throws MavenExecutorException;


    protected final Logger getLogger()
    {
        return logger;
    }

    /** {@inheritDoc} */
    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }
}
