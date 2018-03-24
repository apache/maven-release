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
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.scm.ScmTranslator;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.codehaus.plexus.component.annotations.Component;

/**
 * Rewrite POMs for branch.
 *
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 */
@Component( role = ReleasePhase.class, hint = "rewrite-poms-for-branch" )
public class RewritePomsForBranchPhase
    extends AbstractRewritePomsPhase
{
    @Override
    protected final String getPomSuffix()
    {
        return "branch";
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
                try
                {
                    translateScm( project, releaseDescriptor, scmRoot, scmRepository, result );
                }
                catch ( IOException e )
                {
                    throw new ReleaseExecutionException( e.getMessage(), e );
                }
            }
            else
            {
                MavenProject parent = project.getParent();
                if ( parent != null )
                {
                    // If the SCM element is not present, only add it if the parent was not mapped (ie, it's external to
                    // the release process and so has not been modified, so the values will not be correct on the tag),
                    String parentId = ArtifactUtils.versionlessKey( parent.getGroupId(), parent.getArtifactId() );
                    if ( releaseDescriptor.getOriginalScmInfo( parentId ) == null )
                    {
                        // we need to add it, since it has changed from the inherited value
                        scmRoot = new Scm();
                        // reset default value (HEAD)
                        scmRoot.setTag( null );

                        try
                        {
                            if ( translateScm( project, releaseDescriptor, scmRoot, scmRepository, result ) )
                            {
                                modelTarget.setScm( scmRoot );
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

    private boolean translateScm( MavenProject project, ReleaseDescriptor releaseDescriptor, Scm scmTarget,
                                  ScmRepository scmRepository, ReleaseResult relResult )
    throws IOException
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

            String branchName = releaseDescriptor.getScmReleaseLabel();
            String branchBase = releaseDescriptor.getScmBranchBase();

            // TODO: svn utils should take care of prepending this
            if ( branchBase != null )
            {
                branchBase = "scm:svn:" + branchBase;
            }

            Path projectBasedir = project.getBasedir().toPath().toRealPath( LinkOption.NOFOLLOW_LINKS );
            Path workingDirectory = Paths.get( releaseDescriptor.getWorkingDirectory() );

            int count = ReleaseUtil.getBaseWorkingDirectoryParentCount( workingDirectory, projectBasedir );
            
            if ( scm.getConnection() != null )
            {
                String rootUrl = ReleaseUtil.realignScmUrl( count, scm.getConnection() );

                String subDirectoryBranch = scm.getConnection().substring( rootUrl.length() );
                if ( !subDirectoryBranch.startsWith( "/" ) )
                {
                    subDirectoryBranch = "/" + subDirectoryBranch;
                }

                String scmConnectionBranch = branchBase;
                if ( scmConnectionBranch != null )
                {
                    String trunkUrl = scm.getDeveloperConnection();
                    if ( trunkUrl == null )
                    {
                        trunkUrl = scm.getConnection();
                    }
                    scmConnectionBranch = translateUrlPath( trunkUrl, branchBase, scm.getConnection() );
                }

                String value =
                    translator.translateBranchUrl( scm.getConnection(), branchName + subDirectoryBranch,
                                                   scmConnectionBranch );
                if ( !value.equals( scm.getConnection() ) )
                {
                    scmTarget.setConnection( value );
                    result = true;
                }
            }

            if ( scm.getDeveloperConnection() != null )
            {
                String rootUrl = ReleaseUtil.realignScmUrl( count, scm.getDeveloperConnection() );

                String subDirectoryBranch = scm.getDeveloperConnection().substring( rootUrl.length() );
                if ( !subDirectoryBranch.startsWith( "/" ) )
                {
                    subDirectoryBranch = "/" + subDirectoryBranch;
                }

                String value =
                    translator.translateBranchUrl( scm.getDeveloperConnection(), branchName + subDirectoryBranch,
                                                   branchBase );
                if ( !value.equals( scm.getDeveloperConnection() ) )
                {
                    scmTarget.setDeveloperConnection( value );
                    result = true;
                }
            }

            if ( scm.getUrl() != null )
            {
                String rootUrl = ReleaseUtil.realignScmUrl( count, scm.getUrl() );

                String subDirectoryBranch = scm.getUrl().substring( rootUrl.length() );
                if ( !subDirectoryBranch.startsWith( "/" ) )
                {
                    subDirectoryBranch = "/" + subDirectoryBranch;
                }

                String tagScmUrl = branchBase;
                if ( tagScmUrl != null )
                {
                    String trunkUrl = scm.getDeveloperConnection();
                    if ( trunkUrl == null )
                    {
                        trunkUrl = scm.getConnection();
                    }
                    tagScmUrl = translateUrlPath( trunkUrl, branchBase, scm.getUrl() );
                }

                // use original branch base without protocol
                String value = translator.translateBranchUrl( scm.getUrl(), branchName + subDirectoryBranch,
                                                              tagScmUrl );
                if ( !value.equals( scm.getUrl() ) )
                {
                    scmTarget.setUrl( value );
                    result = true;
                }
            }

            if ( branchName != null )
            {
                String value = translator.resolveTag( branchName );
                if ( value != null && !value.equals( scm.getTag() ) )
                {
                    scmTarget.setTag( value );
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
    protected String getOriginalVersion( ReleaseDescriptor releaseDescriptor, String projectKey, boolean simulate )
    {
        return releaseDescriptor.getProjectOriginalVersion( projectKey );
    }

    @Override
    protected String getNextVersion( ReleaseDescriptor releaseDescriptor, String key )
    {
        return releaseDescriptor.getProjectReleaseVersion( key );
    }

    @Override
    protected String getResolvedSnapshotVersion( String artifactVersionlessKey,
                                                 ReleaseDescriptor releaseDescriptor )
    {
        return releaseDescriptor.getDependencyReleaseVersion( artifactVersionlessKey );
    }
}
