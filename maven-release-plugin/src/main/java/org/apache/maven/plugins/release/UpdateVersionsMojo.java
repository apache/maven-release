package org.apache.maven.plugins.release;

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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.release.DefaultReleaseManagerListener;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.ReleaseUpdateVersionsRequest;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;

/**
 * Update the POM versions for a project. This performs the normal version updates of the <tt>release:prepare</tt> goal
 * without making other modifications to the SCM such as tagging. For more info see <a
 * href="https://maven.apache.org/plugins/maven-release-plugin/examples/update-versions.html"
 * >https://maven.apache.org/plugins/maven-release-plugin/examples/update-versions.html</a>.
 *
 * @author Paul Gier
 * @version $Id$
 * @since 2.0
 */
@Mojo( name = "update-versions", aggregator = true )
public class UpdateVersionsMojo
    extends AbstractReleaseMojo
{

    /**
     * Whether to automatically assign submodules the parent version. If set to false, the user will be prompted for the
     * version of each submodules.
     *
     * @since 2.0
     */
    @Parameter( defaultValue = "false", property = "autoVersionSubmodules" )
    private boolean autoVersionSubmodules;

    /**
     * Whether to add a schema to the POM if it was previously missing on release.
     *
     * @since 2.0
     */
    @Parameter( defaultValue = "true", property = "addSchema" )
    private boolean addSchema;

    /**
     * Default version to use for new local working copy.
     *
     * @since 2.0
     */
    @Parameter( property = "developmentVersion" )
    private String developmentVersion;

    /**
     * Whether to update dependencies version to the next development version.
     *
     * @since 2.5.2
     */
    @Parameter( defaultValue = "true", property = "updateDependencies" )
    private boolean updateDependencies;

    /**
     * Whether to use "edit" mode on the SCM, to lock the file for editing during SCM operations.
     *
     * @since 2.5.2
     */
    @Parameter( defaultValue = "false", property = "useEditMode" )
    private boolean useEditMode;

    /**
     * The role-hint for the VersionPolicy implementation used to calculate the project versions.
     *
     * @since 3.0.0
     */
    @Parameter( defaultValue = "default", property = "projectVersionPolicyId" )
    private String projectVersionPolicyId;

    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        final ReleaseDescriptorBuilder config = createReleaseDescriptor();
        config.setAddSchema( addSchema );
        config.setAutoVersionSubmodules( autoVersionSubmodules );
        config.setDefaultDevelopmentVersion( developmentVersion );
        config.setScmUseEditMode( useEditMode );
        config.setUpdateDependencies( updateDependencies );
        config.setProjectVersionPolicyId( projectVersionPolicyId );

        config.addOriginalScmInfo( ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() ),
                                   project.getScm() );

        try
        {
            ReleaseUpdateVersionsRequest updateVersionsRequest = new ReleaseUpdateVersionsRequest();
            updateVersionsRequest.setReleaseDescriptorBuilder( config );
            updateVersionsRequest.setReleaseEnvironment( getReleaseEnvironment() );
            updateVersionsRequest.setReactorProjects( getReactorProjects() );
            updateVersionsRequest.setReleaseManagerListener( new DefaultReleaseManagerListener( getLog() ) );
            updateVersionsRequest.setUserProperties( session.getUserProperties() );

            releaseManager.updateVersions( updateVersionsRequest );
        }
        catch ( ReleaseExecutionException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        catch ( ReleaseFailureException e )
        {
            throw new MojoFailureException( e.getMessage(), e );
        }
    }
}
