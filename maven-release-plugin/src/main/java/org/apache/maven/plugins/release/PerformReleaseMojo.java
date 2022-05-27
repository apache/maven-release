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
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.shared.release.DefaultReleaseManagerListener;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.ReleasePerformRequest;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.codehaus.plexus.util.StringUtils;

/**
 * Perform a release from SCM, either from a specified tag, or the tag representing the previous release in
 * the working copy created by <code>release:prepare</code>.
 * For more info see <a href="https://maven.apache.org/plugins/maven-release-plugin/examples/perform-release.html"
 * >https://maven.apache.org/plugins/maven-release-plugin/examples/perform-release.html</a>.
 *
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
@Mojo( name = "perform", aggregator = true, requiresProject = false )
public class PerformReleaseMojo
    extends AbstractReleaseMojo
{
    /**
     * A space separated list of goals to execute on release perform. Default value is either <code>deploy</code> or
     * <code>deploy site-deploy</code>, if the project has a &lt;distributionManagement&gt;/&lt;site&gt; element.
     */
    @Parameter( property = "goals" )
    String goals;

    /**
     * Comma separated profiles to enable on release perform, in addition to active profiles for project execution.
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
     * Use a local checkout instead of doing a checkout from the upstream repository.
     * ATTENTION: This will only work with distributed SCMs which support the file:// protocol
     * like e.g. git, jgit or hg!
     *
     * TODO: we should think about having the defaults for the various SCM providers provided via modello!
     *
     * @since 2.0 for release:perform and 2.5.2 for release:stage
     */
    @Parameter( defaultValue = "false", property = "localCheckout" )
    private boolean localCheckout;

    /**
     * The SCM private key to use.
     */
    @Parameter( property = "privateKey" )
    private String privateKey;

    /**
     * The SCM Private Key Pass Phrase to use.
     */
    @Parameter( property = "passphrase" )
    private String passphrase;

    /**
     * The SCM username to use.
     */
    @Parameter( property = "username" )
    private String username;

    /**
     * The SCM password to use.
     */
    @Parameter( property = "password" )
    private String password;

    /**
     * Whether to use the default release profile (Maven 2 and 3) that adds sources and javadocs to the released
     * artifact, if appropriate. If set to true, the release plugin sets the property "<code>performRelease</code>" to
     * true, which activates the profile "<code>release-profile</code>" as inherited from
     * <a href="/ref/3.8.5/maven-model-builder/super-pom.html">the super pom</a>.
     *
     * @deprecated The <code>release-profile</code> profile will be removed from future versions of the super POM
     */
    @Parameter( defaultValue = "false", property = "useReleaseProfile" )
    @Deprecated
    private boolean useReleaseProfile;

    /**
     * Dry run: don't checkout anything from the scm repository, or modify the checkout.
     * The goals (by default at least {@code deploy}) will <strong>not</strong> be executed.
     */
    @Parameter( defaultValue = "false", property = "dryRun" )
    private boolean dryRun;

    /**
     * Add a new or overwrite the default implementation per provider.
     * The key is the scm prefix and the value is the role hint of the
     * {@link org.apache.maven.scm.provider.ScmProvider}.
     *
     * @since 2.5.3
     * @see ScmManager#setScmProviderImplementation(String, String)
     */
    @Parameter
    private Map<String, String> providerImplementations;

    /**
     * The SCM manager.
     */
    @Component
    private ScmManager scmManager;

    @Override
    protected String getAdditionalProfiles()
    {
        return releaseProfiles;
    }

    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( providerImplementations != null )
        {
            for ( Map.Entry<String, String> providerEntry : providerImplementations.entrySet() )
            {
                getLog().info( "Change the default '" + providerEntry.getKey() + "' provider implementation to '"
                    + providerEntry.getValue() + "'." );
                scmManager.setScmProviderImplementation( providerEntry.getKey(), providerEntry.getValue() );
            }
        }

        // goals may be splitted into multiple line in configuration.
        // Let's build a single line command
        if ( goals != null )
        {
            goals = StringUtils.join( StringUtils.split( goals ), " " );
        }

        try
        {
            setDeploymentRepository();
            // Note that the working directory here is not the same as in the release configuration, so don't reuse that
            ReleaseDescriptorBuilder releaseDescriptor = createReleaseDescriptor();
            if ( connectionUrl != null )
            {
                releaseDescriptor.setScmSourceUrl( connectionUrl );
            }

            if ( username != null )
            {
                releaseDescriptor.setScmUsername( username );
            }

            if ( password != null )
            {
                releaseDescriptor.setScmPassword( password );
            }

            releaseDescriptor.setLocalCheckout( localCheckout );

            releaseDescriptor.setCheckoutDirectory( workingDirectory.getAbsolutePath() );
            releaseDescriptor.setUseReleaseProfile( useReleaseProfile );

            createGoals();
            releaseDescriptor.setPerformGoals( goals );

            ReleasePerformRequest performRequest  = new ReleasePerformRequest();
            performRequest.setReleaseDescriptorBuilder( releaseDescriptor );
            performRequest.setReleaseEnvironment( getReleaseEnvironment() );
            performRequest.setReactorProjects( getReactorProjects() );
            performRequest.setReleaseManagerListener( new DefaultReleaseManagerListener( getLog(), dryRun ) );
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

    /** Just here so it may be overridden by StageReleaseMojo */
    void setDeploymentRepository()
    {
    }

    /** Just here so it may be overridden by StageReleaseMojo */
    void createGoals()
    {
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
    }
}
