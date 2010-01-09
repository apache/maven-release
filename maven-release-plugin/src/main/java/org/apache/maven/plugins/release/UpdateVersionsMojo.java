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

import java.util.HashMap;
import java.util.Map;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.config.ReleaseUtils;

/**
 * Update the POM versions for a project.
 *
 * @author Paul Gier
 * @version $Id$
 * @aggregator
 * @goal update-versions
 */
public class UpdateVersionsMojo
    extends AbstractReleaseMojo
{

    /**
     * Whether to automatically assign submodules the parent version. If set to false, the user will be prompted for the
     * version of each submodules.
     *
     * @parameter expression="${autoVersionSubmodules}" default-value="false"
     */
    private boolean autoVersionSubmodules;

    /**
     * Whether to add a schema to the POM if it was previously missing on release.
     *
     * @parameter expression="${addSchema}" default-value="true"
     */
    private boolean addSchema;

    /**
     * Default version to use for new local working copy.
     *
     * @parameter expression="${developmentVersion}"
     */
    private String developmentVersion;

    /**
     * @parameter expression="${session}"
     * @readonly
     * @required
     * @since 2.0
     */
    protected MavenSession session;

    /**
     * {@inheritDoc}
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        super.execute();

        ReleaseDescriptor config = createReleaseDescriptor();
        config.setAddSchema( addSchema );
        config.setAutoVersionSubmodules( autoVersionSubmodules );
        config.setDefaultDevelopmentVersion( developmentVersion );

        Map originalScmInfo = new HashMap();
        originalScmInfo.put( ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() ), project.getScm() );
        config.setOriginalScmInfo( originalScmInfo );

        // Create a config containing values from the session properties (ie command line properties with cli).
        ReleaseDescriptor sysPropertiesConfig
                = ReleaseUtils.copyPropertiesToReleaseDescriptor( session.getExecutionProperties() );
        mergeCommandLineConfig( config, sysPropertiesConfig );

        try
        {
            releaseManager.updateVersions( config, getReleaseEnvironment(), reactorProjects );
        }
        catch ( ReleaseExecutionException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        catch ( ReleaseFailureException e )
        {
            throw new MojoFailureException( e.getMessage() );
        }
    }

    /**
     * This method takes some of the release configuration picked up from the command line system properties and copies
     * it into the release config object.
     *
     * @param config The release configuration to merge the system properties into, must not be <code>null</code>.
     * @param sysPropertiesConfig The configuration from the system properties to merge in, must not be
     *            <code>null</code>.
     */
    private void mergeCommandLineConfig( ReleaseDescriptor config, ReleaseDescriptor sysPropertiesConfig )
    {
        // If the user specifies versions, these should override the existing versions
        if ( sysPropertiesConfig.getReleaseVersions() != null )
        {
            config.getReleaseVersions().putAll( sysPropertiesConfig.getReleaseVersions() );
        }
        if ( sysPropertiesConfig.getDevelopmentVersions() != null )
        {
            config.getDevelopmentVersions().putAll( sysPropertiesConfig.getDevelopmentVersions() );
        }
    }

}
