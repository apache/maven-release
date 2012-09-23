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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.codehaus.plexus.util.StringUtils;

/**
 * Perform a release from SCM to a staging repository.
 *
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 * @version $Id$
 * @since 2.0-beta-8
 */
@Mojo( name = "stage", aggregator = true, requiresProject = false )
public class StageReleaseMojo
    extends AbstractScmReleaseMojo
{
    /**
     * A comma or space separated list of goals to execute on deployment. Default value is either <code>deploy</code> or
     * <code>deploy site-deploy</code>, if the project has a &lt;distributionManagement&gt;/&lt;site&gt; element.
     *
     * @since 2.0-beta-8
     */
    @Parameter( property="goals" )
    private String goals;

    /**
     * Comma separated profiles to enable on deployment, in addition to active profiles for project execution.
     *
     * @since 2.0-beta-8
     */
    @Parameter( property="releaseProfiles" )
    private String releaseProfiles;

    /**
     * The checkout directory.
     *
     * @since 2.0-beta-8
     */
    @Parameter( defaultValue="${project.build.directory}/checkout", property="workingDirectory", required = true )
    private File workingDirectory;

    /**
     * The SCM URL to checkout from. If omitted, the one from the <code>release.properties</code> file is used, followed
     * by the URL from the current POM.
     *
     * @since 2.0-beta-8
     */
    @Parameter( property="connectionUrl" )
    private String connectionUrl;

    /**
     * Whether to use the release profile that adds sources and javadocs to the released artifact, if appropriate.
     * 
     * @since 2.0-beta-8
     */
    @Parameter( defaultValue="true", property="useReleaseProfile" )
    private boolean useReleaseProfile;

    /**
     * URL of the staging repository to use.
     *
     * @since 2.0-beta-8
     */
    @Parameter( property="stagingRepository", required = true )
    private String stagingRepository;

    /**
     * {@inheritDoc}
     */
    protected String getAdditionalProfiles()
    {
        return releaseProfiles;
    }

    /**
     * {@inheritDoc}
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        super.execute();

        // goals may be splitted into multiple line in configuration.
        // Let's build a single line command
        if ( goals != null )
        {
            goals = StringUtils.join( StringUtils.split( goals ), " " );
        }

        try
        {
            addArgument( "-DaltDeploymentRepository=\"" + stagingRepository + "\"" );

            // Note that the working directory here is not the same as in the release configuration, so don't reuse that
            ReleaseDescriptor releaseDescriptor = createReleaseDescriptor();
            if ( connectionUrl != null )
            {
                releaseDescriptor.setScmSourceUrl( connectionUrl );
            }

            releaseDescriptor.setCheckoutDirectory( workingDirectory.getAbsolutePath() );
            releaseDescriptor.setUseReleaseProfile( useReleaseProfile );

            if ( goals == null )
            {
                // set default
                goals = "deploy";
                if ( project.getDistributionManagement() != null
                    && project.getDistributionManagement().getSite() != null )
                {
                    goals += " site:stage-deploy";
                }
            }

            goals = StringUtils.replace( goals, "site-deploy", "site:stage-deploy" );
            goals = StringUtils.replace( goals, "site:deploy", "site:stage-deploy" );

            releaseDescriptor.setPerformGoals( goals );

            releaseManager.perform( releaseDescriptor, getReleaseEnvironment(), getReactorProjects(), false );
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
