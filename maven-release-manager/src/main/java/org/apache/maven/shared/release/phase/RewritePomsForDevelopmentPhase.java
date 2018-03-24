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

import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.scm.ScmTranslator;
import org.codehaus.plexus.component.annotations.Component;

/**
 * Rewrite POMs for future development
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
@Component( role = ReleasePhase.class , hint = "rewrite-poms-for-development" )
public class RewritePomsForDevelopmentPhase
    extends AbstractRewritePomsPhase
{
    @Override
    protected final String getPomSuffix()
    {
        return "next";
    }

    @Override
    protected void transformScm( MavenProject project, Model modelTarget, ReleaseDescriptor releaseDescriptor,
                                 String projectId, ScmRepository scmRepository, ReleaseResult result )
        throws ReleaseExecutionException
    {
        // If SCM is null in original model, it is inherited, no mods needed
        if ( project.getScm() != null )
        {
            Scm scmRoot = modelTarget.getScm();
            if ( scmRoot != null )
            {
                ScmTranslator translator = getScmTranslators().get( scmRepository.getProvider() );
                if ( translator != null )
                {
                    Scm scm = releaseDescriptor.getOriginalScmInfo( projectId );

                    if ( scm != null )
                    {
                        scmRoot.setConnection( scm.getConnection() );
                        scmRoot.setDeveloperConnection( scm.getDeveloperConnection() );
                        scmRoot.setUrl( scm.getUrl() );
                        scmRoot.setTag( translator.resolveTag( scm.getTag() ) );
                    }
                    else
                    {
                        // cleanly remove the SCM element
                        modelTarget.setScm( null );
                    }
                }
                else
                {
                    String message = "No SCM translator found - skipping rewrite";
                    result.appendDebug( message );
                    getLogger().debug( message );
                }
            }
        }
    }

    @Override
    protected String getOriginalVersion( ReleaseDescriptor releaseDescriptor, String projectKey, boolean simulate )
    {
        return simulate
                        ? releaseDescriptor.getProjectOriginalVersion( projectKey )
                        : releaseDescriptor.getProjectReleaseVersion( projectKey );
    }

    @Override
    protected String getNextVersion( ReleaseDescriptor releaseDescriptor, String key )
    {
        return releaseDescriptor.getProjectDevelopmentVersion( key );
    }

    @Override
    protected String getResolvedSnapshotVersion( String artifactVersionlessKey,
                                                 ReleaseDescriptor releaseDescriptor )
    {
        return releaseDescriptor.getDependencyDevelopmentVersion( artifactVersionlessKey );
    }
}
