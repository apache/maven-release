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
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.scm.ReleaseScmCommandException;
import org.apache.maven.shared.release.scm.ReleaseScmRepositoryException;

/**
 * Commit the changes that were done to prepare the branch or tag to the SCM.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ScmCommitDevelopmentPhase
    extends AbstractScmCommitPhase
{

    /**
     * The format for the
     */
    private String rollbackMessageFormat;

    protected void runLogic( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                             List<MavenProject> reactorProjects, ReleaseResult result, boolean simulating )
        throws ReleaseScmCommandException, ReleaseExecutionException, ReleaseScmRepositoryException
    {
        // no rollback required
        if (
            // was there no commit that has to be rolled back by a new one
            releaseDescriptor.isSuppressCommitBeforeTagOrBranch()
                // and working copy should not be touched
                && !releaseDescriptor.isUpdateWorkingCopyVersions() )
        {
            if ( simulating )
            {
                logInfo( result, "Full run would not commit changes, because updateWorkingCopyVersions is false." );
            }
            else
            {
                logInfo( result, "Modified POMs are not committed because updateWorkingCopyVersions is set to false." );
            }
        }
        // rollback or commit development versions required
        else
        {
            String message;
            if ( !releaseDescriptor.isUpdateWorkingCopyVersions() )
            {
                // the commit is a rollback
                message = createRollbackMessage( releaseDescriptor );
            }
            else
            {
                // a normal commit
                message = createMessage( releaseDescriptor );
            }
            if ( simulating )
            {
                Collection<File> pomFiles = createPomFiles( releaseDescriptor, reactorProjects );
                logInfo( result,
                         "Full run would be commit " + pomFiles.size() + " files with message: '" + message + "'" );
            }
            else
            {
                performCheckins( releaseDescriptor, releaseEnvironment, reactorProjects, message );
            }
        }
    }

    private String createRollbackMessage( ReleaseDescriptor releaseDescriptor )
    {
        return MessageFormat.format( releaseDescriptor.getScmCommentPrefix() + rollbackMessageFormat,
                                     new Object[]{releaseDescriptor.getScmReleaseLabel()} );
    }

}