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

import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.scm.ReleaseScmCommandException;
import org.apache.maven.shared.release.scm.ReleaseScmRepositoryException;

import java.text.MessageFormat;
import java.util.List;

/**
 * Commit the changes that were done to prepare the branch or tag to the SCM.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ScmCommitPreparationPhase
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
        // no prepare-commit required
        if ( releaseDescriptor.isSuppressCommitBeforeTagOrBranch() )
        {
            if ( simulating )
            {
                logInfo( result, "Full run would not commit changes, because suppressCommitBeforeTagOrBranch is set." );
            }
            else
            {
                logInfo( result,
                         "Modified POMs are not committed because suppressCommitBeforeTagOrBranch is set to false." );
            }
        }
        // commit development versions required
        else
        {
            String message = createMessage( releaseDescriptor );

            if ( simulating )
            {
                simulateCheckins( releaseDescriptor, reactorProjects, result, message );
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

    protected void validateConfiguration( ReleaseDescriptor releaseDescriptor )
        throws ReleaseFailureException
    {
        super.validateConfiguration( releaseDescriptor );

        if ( releaseDescriptor.isSuppressCommitBeforeTagOrBranch() && releaseDescriptor.isRemoteTagging() )
        {
            throw new ReleaseFailureException(
                "Cannot perform a remote tag or branch without committing the working copy first." );
        }
    }
}
