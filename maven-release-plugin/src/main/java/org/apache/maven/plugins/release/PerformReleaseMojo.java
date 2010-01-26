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
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.codehaus.plexus.util.StringUtils;

/**
 * Perform a release from SCM.
 * For more info see <a href="/plugins/maven-release-plugin/examples/perform-release.html">this example</a>.
 *
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 * @aggregator
 * @requiresProject false
 * @goal perform
 */
public class PerformReleaseMojo
    extends AbstractReleaseMojo
{
    /**
     * A comma or space separated list of goals to execute on deployment. Default value is either <code>deploy</code> or
     * <code>deploy site-deploy</code>, if the project has a &lt;distributionManagement&gt;/&lt;site&gt; element.
     *
     * @parameter expression="${goals}"
     */
    private String goals;

    /**
     * Comma separated profiles to enable on deployment, in addition to active profiles for project execution.
     *
     * @parameter expression="${releaseProfiles}"
     * @since 2.0-beta-8
     */
    private String releaseProfiles;

    /**
     * The checkout directory.
     *
     * @parameter expression="${workingDirectory}" default-value="${project.build.directory}/checkout"
     * @required
     */
    private File workingDirectory;

    /**
     * The SCM URL to checkout from. If omitted, the one from the <code>release.properties</code> file is used, followed
     * by the URL from the current POM.
     *
     * @parameter expression="${connectionUrl}"
     */
    private String connectionUrl;

    /**
     * Whether to use the release profile that adds sources and javadocs to the released artifact, if appropriate.
     *
     * @parameter expression="${useReleaseProfile}" default-value="true"
     */
    private boolean useReleaseProfile;

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

            releaseManager.perform( releaseDescriptor, getReleaseEnvironment(), reactorProjects );
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
}
