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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.scm.ScmTranslator;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.jdom.Element;
import org.jdom.Namespace;

/**
 * Rewrite POMs for release.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class RewritePomsForReleasePhase
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
                Scm scm = buildScm( project );
                releaseDescriptor.mapOriginalScmInfo( projectId, scm );

                try
                {
                    translateScm( project, releaseDescriptor, scmRoot, namespace, scmRepository, result, commonBasedir );
                }
                catch ( IOException e )
                {
                    throw new ReleaseExecutionException( e.getMessage(), e );
                }
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

                        try
                        {
                            if ( translateScm( project, releaseDescriptor, scmRoot, namespace, scmRepository, result,
                                               commonBasedir ) )
                            {
                                rootElement.addContent( "\n  " ).addContent( scmRoot ).addContent( "\n" );
                            }
                        }
                        catch ( IOException e )
                        {
                            throw new ReleaseExecutionException( e.getMessage(), e );
                        }
                    }
                }
            }
        }
    }

    private boolean translateScm( MavenProject project, ReleaseDescriptor releaseDescriptor, Element scmRoot,
                                  Namespace namespace, ScmRepository scmRepository, ReleaseResult relResult,
                                  String commonBasedir ) throws IOException
    {
        ScmTranslator translator = getScmTranslators().get( scmRepository.getProvider() );
        boolean result = false;
        if ( translator != null )
        {
            Scm scm = project.getOriginalModel().getScm();
            if ( scm == null )
            {
                scm = project.getScm();
            }

            String tag = releaseDescriptor.getScmReleaseLabel();
            String tagBase = releaseDescriptor.getScmTagBase();

            // TODO: svn utils should take care of prepending this
            if ( tagBase != null )
            {
                tagBase = "scm:svn:" + tagBase;
            }

            String workingDirectory =
                ReleaseUtil.isSymlink( project.getBasedir() ) ? project.getBasedir().getCanonicalPath()
                                : project.getBasedir().getAbsolutePath();
            int count =
                ReleaseUtil.getBaseWorkingDirectoryParentCount( commonBasedir, workingDirectory );
            if ( scm.getConnection() != null )
            {
                String rootUrl = ReleaseUtil.realignScmUrl( count, scm.getConnection() );

                String subDirectoryTag = scm.getConnection().substring( rootUrl.length() );
                if ( !subDirectoryTag.startsWith( "/" ) )
                {
                    subDirectoryTag = "/" + subDirectoryTag;
                }

                String scmConnectionTag = tagBase;
                if ( scmConnectionTag != null )
                {
                    String trunkUrl = scm.getDeveloperConnection();
                    if ( trunkUrl == null )
                    {
                        trunkUrl = scm.getConnection();
                    }
                    scmConnectionTag = translateUrlPath( trunkUrl, tagBase, scm.getConnection() );
                }
                String value =
                    translator.translateTagUrl( scm.getConnection(), tag + subDirectoryTag, scmConnectionTag );

                if ( !value.equals( scm.getConnection() ) )
                {
                    rewriteElement( "connection", value, scmRoot, namespace );
                    result = true;
                }
            }

            if ( scm.getDeveloperConnection() != null )
            {
                String rootUrl = ReleaseUtil.realignScmUrl( count, scm.getDeveloperConnection() );

                String subDirectoryTag = scm.getDeveloperConnection().substring( rootUrl.length() );
                if ( !subDirectoryTag.startsWith( "/" ) )
                {
                    subDirectoryTag = "/" + subDirectoryTag;
                }

                String value =
                    translator.translateTagUrl( scm.getDeveloperConnection(), tag + subDirectoryTag, tagBase );

                if ( !value.equals( scm.getDeveloperConnection() ) )
                {
                    rewriteElement( "developerConnection", value, scmRoot, namespace );
                    result = true;
                }
            }

            if ( scm.getUrl() != null )
            {
                String rootUrl = ReleaseUtil.realignScmUrl( count, scm.getUrl() );

                String subDirectoryTag = scm.getUrl().substring( rootUrl.length() );
                if ( !subDirectoryTag.startsWith( "/" ) )
                {
                    subDirectoryTag = "/" + subDirectoryTag;
                }

                String tagScmUrl = tagBase;
                if ( tagScmUrl != null )
                {
                    String trunkUrl = scm.getDeveloperConnection();
                    if ( trunkUrl == null )
                    {
                        trunkUrl = scm.getConnection();
                    }
                    tagScmUrl = translateUrlPath( trunkUrl, tagBase, scm.getUrl() );
                }
                // use original tag base without protocol
                String value = translator.translateTagUrl( scm.getUrl(), tag + subDirectoryTag, tagScmUrl );
                if ( !value.equals( scm.getUrl() ) )
                {
                    rewriteElement( "url", value, scmRoot, namespace );
                    result = true;
                }
            }

            if ( tag != null )
            {
                String value = translator.resolveTag( tag );
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

    @Override
    protected Map<String, String> getOriginalVersionMap( ReleaseDescriptor releaseDescriptor,
                                                         List<MavenProject> reactorProjects, boolean simulate )
    {
        return releaseDescriptor.getOriginalVersions( reactorProjects );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    protected Map<String, String> getNextVersionMap( ReleaseDescriptor releaseDescriptor )
    {
        return releaseDescriptor.getReleaseVersions();
    }

    @Override
    protected String getResolvedSnapshotVersion( String artifactVersionlessKey,
                                                 Map<String, Map<String, String>> resolvedSnapshotsMap )
    {
        Map<String, String> versionsMap = resolvedSnapshotsMap.get( artifactVersionlessKey );

        if ( versionsMap != null )
        {
            return versionsMap.get( ReleaseDescriptor.RELEASE_KEY );
        }
        else
        {
            return null;
        }
    }
}
