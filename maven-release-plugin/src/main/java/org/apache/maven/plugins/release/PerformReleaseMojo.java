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
import org.apache.maven.shared.release.ReleasePerformRequest;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.codehaus.plexus.util.StringUtils;

/**
 * Perform a release from SCM, either from a specified tag, or the tag representing the previous release in
 * the working copy created by <tt>release:prepare</tt>.
 * For more info see <a href="http://maven.apache.org/plugins/maven-release-plugin/examples/perform-release.html">http://maven.apache.org/plugins/maven-release-plugin/examples/perform-release.html</a>.
 *
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
@Mojo( name = "perform", aggregator = true, requiresProject = false )
public class PerformReleaseMojo
    extends AbstractReleaseMojo
{
    /**
     * A space separated list of goals to execute on deployment. Default value is either <code>deploy</code> or
     * <code>deploy site-deploy</code>, if the project has a &lt;distributionManagement&gt;/&lt;site&gt; element.
     */
    @Parameter( property = "goals" )
    private String goals;

    /**
     * Comma separated profiles to enable on deployment, in addition to active profiles for project execution.
     *
     * @since 2.0-beta-8
     */
    @Parameter( property = "releaseProfiles" )
    private String releaseProfiles;

    /**
     * The checkout directory.
     */
    @Parameter( defaultValue = "${project.build.directory}/checkout", property = "workingDirectory", required = true )
    private File workingDirectory;

    /**
     * The SCM URL to checkout from. If omitted, the one from the <code>release.properties</code> file is used, followed
     * by the URL from the current POM.
     */
    @Parameter( property = "connectionUrl" )
    private String connectionUrl;

    /**
     * Whether to use the release profile that adds sources and javadocs to the released artifact, if appropriate.
     * If set to true, the release plugin sets the property "performRelease" to true, which activates the profile
     * "release-profile", which is inherited from the super pom.
     */
    @Parameter( defaultValue = "true", property = "useReleaseProfile" )
    private boolean useReleaseProfile;

    /**
     * Dry run: don't checkout anything from the scm repository, or modify the checkout.
     * The goals (by default at least {@code deploy}) will <strong>not</strong> be executed.
     */
    @Parameter( defaultValue = "false", property = "dryRun" )
    private boolean dryRun;

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
        // goals may be splitted into multiple line in configuration.
        // Let's build a single line command
        if ( goals != null )
        {
            goals = StringUtils.join( StringUtils.split( goals ), " " );
        }

        try
        {
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
                    goals += " site-deploy";
                }
            }
            releaseDescriptor.setPerformGoals( goals );
            
            ReleasePerformRequest performRequest  = new ReleasePerformRequest();
            performRequest.setReleaseDescriptor( releaseDescriptor );
            performRequest.setReleaseEnvironment( getReleaseEnvironment() );
            performRequest.setReactorProjects( getReactorProjects() );
            performRequest.setDryRun( dryRun );

            releaseManager.perform( performRequest );
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
