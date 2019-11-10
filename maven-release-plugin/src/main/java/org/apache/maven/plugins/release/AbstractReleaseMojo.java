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
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.release.ReleaseManager;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
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
     */
    @Parameter( defaultValue = "${basedir}", readonly = true, required = true )
    private File basedir;

    /**
     */
    @Parameter( defaultValue = "${settings}", readonly = true, required = true )
    private Settings settings;

    /**
     */
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    protected MavenProject project;

    /**
     */
    @Component
    protected ReleaseManager releaseManager;

    /**
     * Additional arguments to pass to the Maven executions, separated by spaces.
     */
    @Parameter( alias = "prepareVerifyArgs", property = "arguments" )
    private String arguments;

    /**
     * The file name of the POM to execute any goals against. As of version 3.0.0, this defaults to the name of
     * POM file of the project being built.
     */
    @Parameter( property = "pomFileName", defaultValue = "${project.file.name}" )
    private String pomFileName;

    /**
     */
    @Parameter( defaultValue = "${reactorProjects}", readonly = true, required = true )
    private List<MavenProject> reactorProjects;

    /**
     * The {@code M2_HOME} parameter to use for forked Maven invocations.
     *
     * @since 2.0-beta-8
     */
    @Parameter( defaultValue = "${maven.home}" )
    private File mavenHome;

    /**
     * The {@code JAVA_HOME} parameter to use for forked Maven invocations.
     *
     * @since 2.0-beta-8
     */
    @Parameter( defaultValue = "${java.home}" )
    private File javaHome;

    /**
     * The command-line local repository directory in use for this build (if specified).
     *
     * @since 2.0-beta-8
     */
    @Parameter ( defaultValue = "${maven.repo.local}" )
    private File localRepoDirectory;

    /**
     * Role hint of the {@link org.apache.maven.shared.release.exec.MavenExecutor} implementation to use.
     *
     * @since 2.0-beta-8
     */
    @Parameter( defaultValue = "invoker", property = "mavenExecutorId" )
    private String mavenExecutorId;

    /**
     * @since 2.0
     */
    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    protected MavenSession session;

    /**
     * The role-hint for the {@link org.apache.maven.shared.release.strategy.Strategy}
     * implementation used to specify the phases per goal.
     * 
     * @since 3.0.0
     * @see org.apache.maven.shared.release.strategies.DefaultStrategy
     */
    @Parameter( defaultValue = "default", property = "releaseStrategyId" )
    private String releaseStrategyId;
    
    /**
     * Gets the environment settings configured for this release.
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
     * Creates the release descriptor from the various goal parameters.
     *
     * @return The release descriptor, never <code>null</code>.
     */
    protected ReleaseDescriptorBuilder createReleaseDescriptor()
    {
        ReleaseDescriptorBuilder descriptor = new ReleaseDescriptorBuilder();
        
        descriptor.setInteractive( settings.isInteractiveMode() );

        Path workingDirectory;
        try
        {
            workingDirectory = getCommonBasedir( reactorProjects );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e.getMessage() );
        }
        descriptor.setWorkingDirectory( workingDirectory.toFile().getAbsolutePath() );

        Path rootBasedir = basedir.toPath();
        if ( rootBasedir.equals( workingDirectory ) )
        {
            descriptor.setPomFileName( pomFileName );
        }
        else
        {
            descriptor.setPomFileName( workingDirectory.relativize( rootBasedir ).resolve( pomFileName ).toString() );
        }

        for ( MavenProject project : reactorProjects )
        {
            String versionlessKey = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );
            descriptor.putOriginalVersion( versionlessKey, project.getVersion() );
        }

        descriptor.setAdditionalArguments( this.arguments );

        List<String> profileIds = session.getRequest().getActiveProfiles();
        String additionalProfiles = getAdditionalProfiles();

        if ( !profileIds.isEmpty() || StringUtils.isNotBlank( additionalProfiles ) )
        {
            List<String> profiles = new ArrayList<>( profileIds );
            
            if ( additionalProfiles != null )
            {
                profiles.addAll( Arrays.asList( additionalProfiles.split( "," ) ) );
            }

            descriptor.setActivateProfiles( profiles );
        }
        
        descriptor.setReleaseStrategyId( releaseStrategyId );

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

    protected final File getBasedir()
    {
        return basedir;
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

    public void setPomFileName( String pomFileName )
    {
        this.pomFileName = pomFileName;
    }
    
    /**
     * Gets the list of projects in the build reactor.
     *
     * @return The list of reactor project, never <code>null</code>.
     */
    public List<MavenProject> getReactorProjects()
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
    
    static Path getCommonBasedir( List<MavenProject> reactorProjects )
                    throws IOException
    {
        Path basePath = reactorProjects.get( 0 ).getBasedir().toPath();
        
        for ( MavenProject reactorProject : reactorProjects )
        {
            Path matchPath = reactorProject.getBasedir().toPath();
            while ( !basePath.startsWith( matchPath ) )
            {
                matchPath = matchPath.getParent();
            }
            basePath = matchPath;
        }
        
        return basePath;
    }
}
