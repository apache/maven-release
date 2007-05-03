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

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.scm.ScmTranslator;
import org.jdom.Element;
import org.jdom.Namespace;

import java.util.List;
import java.util.Map;

/**
 * Rewrite POMs for branch.
 *
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 */
public class RewritePomsForBranchPhase
    extends AbstractRewritePomsPhase
{
    /**
     * SCM URL translators mapped by provider name.
     */
    private Map scmTranslators;

    protected void transformScm( MavenProject project, Element rootElement, Namespace namespace,
                                 ReleaseDescriptor releaseDescriptor, String projectId, ScmRepository scmRepository,
                                 ReleaseResult result, MavenProject rootProject )
    {
        // If SCM is null in original model, it is inherited, no mods needed
        if ( project.getScm() != null )
        {
            Element scmRoot = rootElement.getChild( "scm", namespace );
            if ( scmRoot != null )
            {
                releaseDescriptor.mapOriginalScmInfo( projectId, project.getScm() );

                translateScm( project, releaseDescriptor, scmRoot, namespace, scmRepository, result, rootProject );
            }
            else
            {
                releaseDescriptor.mapOriginalScmInfo( projectId, null );

                MavenProject parent = project.getParent();
                if ( parent != null )
                {
                    // If the SCM element is not present, only add it if the parent was not mapped (ie, it's external to
                    // the release process and so has not been modified, so the values will not be correct on the tag),
                    String parentId = ArtifactUtils.versionlessKey( parent.getGroupId(), parent.getArtifactId() );
                    if ( !releaseDescriptor.getOriginalScmInfo().containsKey( parentId ) )
                    {
                        // we need to add it, since it has changed from the inherited value
                        scmRoot = new Element( "scm" );
                        scmRoot.addContent( "\n  " );

                        if ( translateScm( project, releaseDescriptor, scmRoot, namespace, scmRepository, result,
                                           rootProject ) )
                        {
                            rootElement.addContent( "\n  " ).addContent( scmRoot ).addContent( "\n" );
                        }
                    }
                }
            }
        }
    }

    private boolean translateScm( MavenProject project, ReleaseDescriptor releaseDescriptor, Element scmRoot,
                                  Namespace namespace, ScmRepository scmRepository, ReleaseResult relResult,
                                  MavenProject rootProject )
    {
        ScmTranslator translator = (ScmTranslator) scmTranslators.get( scmRepository.getProvider() );
        boolean result = false;
        if ( translator != null )
        {
            Scm scm = project.getScm();
            String branchName = releaseDescriptor.getScmReleaseLabel();
            String branchBase = releaseDescriptor.getScmTagBase();
            String subDirectoryBranch = "";

            // TODO: svn utils should take care of prepending this
            if ( branchBase != null )
            {
                branchBase = "scm:svn:" + branchBase;
            }

            Scm rootScm = rootProject.getScm();
            if ( scm.getConnection() != null )
            {
                if ( rootScm.getConnection() != null && scm.getConnection().indexOf( rootScm.getConnection() ) == 0 )
                {
                    subDirectoryBranch = scm.getConnection().substring( rootScm.getConnection().length() );
                }
                String value =
                    translator.translateBranchUrl( scm.getConnection(), branchName + subDirectoryBranch, branchBase );
                if ( !value.equals( scm.getConnection() ) )
                {
                    rewriteElement( "connection", value, scmRoot, namespace );
                    result = true;
                }
            }

            if ( scm.getDeveloperConnection() != null )
            {
                if ( rootScm.getDeveloperConnection() != null &&
                    scm.getDeveloperConnection().indexOf( rootScm.getDeveloperConnection() ) == 0 )
                {
                    subDirectoryBranch =
                        scm.getDeveloperConnection().substring( rootScm.getDeveloperConnection().length() );
                }
                String value =
                    translator.translateBranchUrl( scm.getDeveloperConnection(), branchName + subDirectoryBranch, branchBase );
                if ( !value.equals( scm.getDeveloperConnection() ) )
                {
                    rewriteElement( "developerConnection", value, scmRoot, namespace );
                    result = true;
                }
            }

            if ( scm.getUrl() != null )
            {
                if ( rootScm.getUrl() != null && scm.getUrl().indexOf( rootScm.getUrl() ) == 0 )
                {
                    subDirectoryBranch = scm.getUrl().substring( rootScm.getUrl().length() );
                }
                // use original tag base without protocol
                String value = translator.translateBranchUrl( scm.getUrl(), branchName + subDirectoryBranch,
                                                           releaseDescriptor.getScmTagBase() );
                if ( !value.equals( scm.getUrl() ) )
                {
                    rewriteElement( "url", value, scmRoot, namespace );
                    result = true;
                }
            }

            if ( branchName != null )
            {
                String value = translator.resolveTag( branchName );
                if ( value != null && !value.equals( scm.getTag() ) )
                {
                    rewriteElement( "tag", value, scmRoot, namespace );
                    result = true;
                }
            }
        }
        else
        {
            String message = "No SCM translator found - skipping rewrite";

            relResult.appendDebug( message );

            getLogger().debug( message );
        }
        return result;
    }

    protected Map getOriginalVersionMap( ReleaseDescriptor releaseDescriptor, List reactorProjects )
    {
        return releaseDescriptor.getOriginalVersions( reactorProjects );
    }

    protected Map getNextVersionMap( ReleaseDescriptor releaseDescriptor )
    {
        return releaseDescriptor.getReleaseVersions();
    }

    protected String getResolvedSnapshotVersion( String artifactVersionlessKey, Map resolvedSnapshotsMap )
    {
        Map versionsMap = (Map) resolvedSnapshotsMap.get( artifactVersionlessKey );

        if ( versionsMap != null )
        {
            return (String) ( versionsMap.get( ReleaseDescriptor.RELEASE_KEY ) );
        }
        else
        {
            return null;
        }
    }
}
