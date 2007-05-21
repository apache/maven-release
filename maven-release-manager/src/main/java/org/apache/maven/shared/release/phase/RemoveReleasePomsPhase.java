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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.remove.RemoveScmResult;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.scm.ReleaseScmCommandException;
import org.apache.maven.shared.release.util.ReleaseUtil;

/**
 * Remove release POMs.
 * 
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 * @plexus.component role="org.apache.maven.shared.release.phase.ReleasePhase" role-hint="remove-release-poms"
 */
public class RemoveReleasePomsPhase
    extends AbstractReleasePomsPhase
{
    public ReleaseResult execute( ReleaseDescriptor releaseDescriptor, Settings settings, List reactorProjects )
        throws ReleaseFailureException, ReleaseExecutionException
    {
        return execute( releaseDescriptor, settings, reactorProjects, false );
    }

    public ReleaseResult simulate( ReleaseDescriptor releaseDescriptor, Settings settings, List reactorProjects )
        throws ReleaseFailureException, ReleaseExecutionException
    {
        return execute( releaseDescriptor, settings, reactorProjects, true );
    }

    private ReleaseResult execute( ReleaseDescriptor releaseDescriptor, Settings settings, List reactorProjects,
                                   boolean simulate )
        throws ReleaseFailureException, ReleaseExecutionException
    {
        ReleaseResult result = new ReleaseResult();

        if ( releaseDescriptor.isGenerateReleasePoms() )
        {
            logInfo( result, "Removing release POMs..." );

            removeReleasePoms( releaseDescriptor, settings, simulate, result, reactorProjects );
        }
        else
        {
            logInfo( result, "Not removing release POMs" );
        }

        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }

    private void removeReleasePoms( ReleaseDescriptor releaseDescriptor, Settings settings, boolean simulate,
                                    ReleaseResult result, List projects )
        throws ReleaseFailureException, ReleaseExecutionException
    {
        List releasePoms = new ArrayList();

        for ( Iterator iterator = projects.iterator(); iterator.hasNext(); )
        {
            MavenProject project = (MavenProject) iterator.next();

            logInfo( result, "Removing release POM for '" + project.getName() + "'..." );

            releasePoms.add( ReleaseUtil.getReleasePom( project ) );
        }

        removeReleasePomsFromScm( releaseDescriptor, settings, simulate, result, releasePoms );
    }
    
    private void removeReleasePomsFromScm( ReleaseDescriptor releaseDescriptor, Settings settings, boolean simulate,
                                           ReleaseResult result, List releasePoms )
        throws ReleaseFailureException, ReleaseExecutionException
    {
        if ( simulate )
        {
            logInfo( result, "Full run would be removing " + releasePoms );
        }
        else
        {
            ScmRepository scmRepository = getScmRepository( releaseDescriptor, settings );
            ScmProvider scmProvider = getScmProvider( scmRepository );

            ScmFileSet scmFileSet = new ScmFileSet( new File( releaseDescriptor.getWorkingDirectory() ), releasePoms );

            try
            {
                RemoveScmResult scmResult =
                    scmProvider.remove( scmRepository, scmFileSet, "Removing for next development iteration." );

                if ( !scmResult.isSuccess() )
                {
                    throw new ReleaseScmCommandException( "Cannot remove release POMs from SCM", scmResult );
                }
            }
            catch ( ScmException exception )
            {
                throw new ReleaseExecutionException( "Cannot remove release POMs from SCM: " + exception.getMessage(),
                                                     exception );
            }
        }
    }
}