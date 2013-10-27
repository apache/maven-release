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

import java.util.List;
import java.util.Map;

import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.scm.ScmTranslator;
import org.jdom.Element;
import org.jdom.Namespace;

/**
 * Rewrite POMs for future development
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class RewritePomsForDevelopmentPhase
    extends AbstractRewritePomsPhase
{
    @Override
    protected void transformScm( MavenProject project, Element rootElement, Namespace namespace,
                                 ReleaseDescriptor releaseDescriptor, String projectId, ScmRepository scmRepository,
                                 ReleaseResult result, String commonBasedir )
        throws ReleaseExecutionException
    {
        // If SCM is null in original model, it is inherited, no mods needed
        if ( project.getScm() != null )
        {
            Element scmRoot = rootElement.getChild( "scm", namespace );
            if ( scmRoot != null )
            {
                @SuppressWarnings( "unchecked" )
                Map<String, Scm> originalScmInfo = releaseDescriptor.getOriginalScmInfo();
                // check containsKey, not == null, as we store null as a value
                if ( !originalScmInfo.containsKey( projectId ) )
                {
                    throw new ReleaseExecutionException(
                        "Unable to find original SCM info for '" + project.getName() + "'" );
                }

                ScmTranslator translator = getScmTranslators().get( scmRepository.getProvider() );
                if ( translator != null )
                {
                    Scm scm = originalScmInfo.get( projectId );

                    if ( scm != null )
                    {
                        rewriteElement( "connection", scm.getConnection(), scmRoot, namespace );
                        rewriteElement( "developerConnection", scm.getDeveloperConnection(), scmRoot, namespace );
                        rewriteElement( "url", scm.getUrl(), scmRoot, namespace );
                        rewriteElement( "tag", translator.resolveTag( scm.getTag() ), scmRoot, namespace );
                    }
                    else
                    {
                        // cleanly remove the SCM element
                        rewriteElement( "scm", null, rootElement, namespace );
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

    @SuppressWarnings( "unchecked" )
    @Override
    protected Map<String, String> getOriginalVersionMap( ReleaseDescriptor releaseDescriptor,
                                                         List<MavenProject> reactorProjects, boolean simulate )
    {
        return simulate
            ? releaseDescriptor.getOriginalVersions( reactorProjects )
            : releaseDescriptor.getReleaseVersions();
    }

    @SuppressWarnings( "unchecked" )
    @Override
    protected Map<String, String> getNextVersionMap( ReleaseDescriptor releaseDescriptor )
    {
        return releaseDescriptor.getDevelopmentVersions();
    }

    @Override
    protected String getResolvedSnapshotVersion( String artifactVersionlessKey,
                                                 Map<String, Map<String, String>> resolvedSnapshotsMap )
    {
        Map<String, String> versionsMap = resolvedSnapshotsMap.get( artifactVersionlessKey );

        if ( versionsMap != null )
        {
            return versionsMap.get( ReleaseDescriptor.DEVELOPMENT_KEY );
        }
        else
        {
            return null;
        }
    }
}
