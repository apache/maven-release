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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.release.ReleaseManager;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.codehaus.plexus.util.StringUtils;

/**
 * Base class with shared configuration.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public abstract class AbstractReleaseMojo
    extends AbstractMojo
{
    /**
     * The SCM username to use.
     *
     * @parameter expression="${username}"
     */
    private String username;

    /**
     * The SCM password to use.
     *
     * @parameter expression="${password}"
     */
    private String password;

    /**
     * The SCM tag to use.
     *
     * @parameter expression="${tag}" alias="releaseLabel"
     */
    private String tag;

    /**
     * Format to use when generating the tag name if none is specified. Property interpolation is performed on the
     * tag, but in order to ensure that the interpolation occurs during release, you must use <code>@{...}</code>
     * to reference the properties rather than <code>${...}</code>. The following properties are available:
     * <ul>
     *     <li><code>groupId</code> or <code>project.groupId</code> - The groupId of the root project.
     *     <li><code>artifactId</code> or <code>project.artifactId</code> - The artifactId of the root project.
     *     <li><code>version</code> or <code>project.version</code> - The release version of the root project.
     * </ul>
     *
     * @parameter expression="${tagNameFormat}" default-value="@{project.artifactId}-@{project.version}"
     * @since 2.2.0
     */
    private String tagNameFormat;

    /**
     * The tag base directory in SVN, you must define it if you don't use the standard svn layout (trunk/tags/branches).
     * For example, <code>http://svn.apache.org/repos/asf/maven/plugins/tags</code>. The URL is an SVN URL and does not
     * include the SCM provider and protocol.
     *
     * @parameter expression="${tagBase}"
     */
    private String tagBase;

    /**
     * @parameter expression="${basedir}"
     * @required
     * @readonly
     */
    protected File basedir;

    /**
     * @parameter expression="${settings}"
     * @required
     * @readonly
     */
    protected Settings settings;

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * @component
     */
    protected ReleaseManager releaseManager;

    /**
     * Additional arguments to pass to the Maven executions, separated by spaces.
     *
     * @parameter expression="${arguments}" alias="prepareVerifyArgs"
     */
    private String arguments;

    /**
     * The file name of the POM to execute any goals against.
     *
     * @parameter expression="${pomFileName}"
     */
    private String pomFileName;

    /**
     * The message prefix to use for all SCM changes.
     *
     * @parameter expression="${scmCommentPrefix}" default-value="[maven-release-plugin] "
     * @since 2.0-beta-5
     */
    private String scmCommentPrefix;

    /**
     * @parameter expression="${reactorProjects}"
     * @required
     * @readonly
     */
    protected List reactorProjects;

    /**
     * List of provider implementations.
     *
     * @parameter
     * @since 2.0-beta-6
     */
    private Map providerImplementations;

    /**
     * The M2_HOME parameter to use for forked Maven invocations.
     *
     * @parameter default-value="${maven.home}"
     * @since 2.0-beta-8
     */
    protected File mavenHome;

    /**
     * The JAVA_HOME parameter to use for forked Maven invocations.
     *
     * @parameter default-value="${java.home}"
     * @since 2.0-beta-8
     */
    protected File javaHome;

    /**
     * The command-line local repository directory in use for this build (if specified).
     *
     * @parameter default-value="${maven.repo.local}"
     * @since 2.0-beta-8
     */
    protected File localRepoDirectory;

    /**
     * Role hint of the {@link org.apache.maven.shared.release.exec.MavenExecutor} implementation to use.
     *
     * @parameter expression="${mavenExecutorId}" default-value="invoker"
     * @since 2.0-beta-8
     */
    protected String mavenExecutorId;

    /**
     * Use a local checkout instead of doing a checkout from the upstream repository.
     * ATTENTION: This will only work with distributed SCMs which support the file:// protocol
     * like e.g. git, jgit or hg!
     *
     * TODO: we should think about having the defaults for the various SCM providers provided via modello!
     *
     * @parameter expression="${localCheckout}" default-value="false"
     * @since 2.0
     */
    private boolean localCheckout;
    
    /**
     * Implemented with git will or not push changes to the upstream repository.
     * <code>true</code> by default to preserve backward compatibility.
     * @parameter expression="${pushChanges}" default-value="true"
     * @since 2.1
     */
    private boolean pushChanges = true;

    /**
     * The SCM manager.
     *
     * @component
     */
    private ScmManager scmManager;

    /**
     * @parameter expression="${session}"
     * @readonly
     * @required
     * @since 2.0
     */
    protected MavenSession session;


    /**
     * Gets the enviroment settings configured for this release.
     *
     * @return The release environment, never <code>null</code>.
     */
    protected ReleaseEnvironment getReleaseEnvironment()
    {
        return new DefaultReleaseEnvironment().setSettings( settings )
                                              .setJavaHome( javaHome )
                                              .setMavenHome( mavenHome )
                                              .setLocalRepositoryDirectory( localRepoDirectory )
                                              .setMavenExecutorId( mavenExecutorId );
    }

    /**
     * {@inheritDoc}
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( providerImplementations != null )
        {
            for ( Iterator i = providerImplementations.keySet().iterator(); i.hasNext(); )
            {
                String providerType = (String) i.next();
                String providerImplementation = (String) providerImplementations.get( providerType );
                getLog().info( "Change the default '" + providerType + "' provider implementation to '"
                    + providerImplementation + "'." );
                scmManager.setScmProviderImplementation( providerType, providerImplementation );
            }
        }
    }

    /**
     * Creates the release descriptor from the various goal parameters.
     *
     * @return The release descriptor, never <code>null</code>.
     */
    protected ReleaseDescriptor createReleaseDescriptor()
    {
        ReleaseDescriptor descriptor = new ReleaseDescriptor();

        descriptor.setInteractive( settings.isInteractiveMode() );

        descriptor.setScmPassword( password );
        descriptor.setScmReleaseLabel( tag );
        descriptor.setScmTagNameFormat( tagNameFormat );
        descriptor.setScmTagBase( tagBase );
        descriptor.setScmUsername( username );
        descriptor.setScmCommentPrefix( scmCommentPrefix );

        descriptor.setWorkingDirectory( basedir.getAbsolutePath() );

        descriptor.setPomFileName( pomFileName );

        descriptor.setLocalCheckout( localCheckout );
        
        descriptor.setPushChanges( pushChanges );

        List profiles = project.getActiveProfiles();

        String arguments = this.arguments;
        if ( profiles != null && !profiles.isEmpty() )
        {
            if ( !StringUtils.isEmpty( arguments ) )
            {
                arguments += " -P ";
            }
            else
            {
                arguments = "-P ";
            }

            for ( Iterator it = profiles.iterator(); it.hasNext(); )
            {
                Profile profile = (Profile) it.next();

                arguments += profile.getId();
                if ( it.hasNext() )
                {
                    arguments += ",";
                }
            }

            String additionalProfiles = getAdditionalProfiles();
            if ( additionalProfiles != null )
            {
                if ( !profiles.isEmpty() )
                {
                    arguments += ",";
                }
                arguments += additionalProfiles;
            }
        }
        descriptor.setAdditionalArguments( arguments );

        return descriptor;
    }

    /**
     * Gets the comma separated list of additional profiles for the release build.
     *
     * @return additional profiles to enable during release
     */
    protected String getAdditionalProfiles()
    {
        return null;
    }

    /**
     * Sets the component used to perform release actions.
     *
     * @param releaseManager The release manager implementation to use, must not be <code>null</code>.
     */
    void setReleaseManager( ReleaseManager releaseManager )
    {
        this.releaseManager = releaseManager;
    }

    /**
     * Gets the effective settings for this build.
     *
     * @return The effective settings for this build, never <code>null</code>.
     */
    Settings getSettings()
    {
        return settings;
    }

    /**
     * Sets the base directory of the build.
     *
     * @param basedir The build's base directory, must not be <code>null</code>.
     */
    public void setBasedir( File basedir )
    {
        this.basedir = basedir;
    }

    /**
     * Gets the list of projects in the build reactor.
     *
     * @return The list of reactor project, never <code>null</code>.
     */
    public List getReactorProjects()
    {
        return reactorProjects;
    }

    /**
     * Add additional arguments.
     *
     * @param argument The argument to add, must not be <code>null</code>.
     */
    protected void addArgument( String argument )
    {
        if ( arguments != null )
        {
            arguments += " " + argument;
        }
        else
        {
            arguments = argument;
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
    protected void mergeCommandLineConfig( ReleaseDescriptor config, ReleaseDescriptor sysPropertiesConfig )
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
