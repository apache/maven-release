package org.apache.maven.shared.release;

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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.config.ReleaseDescriptorStore;
import org.apache.maven.shared.release.config.ReleaseDescriptorStoreException;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.phase.ReleasePhase;
import org.apache.maven.shared.release.scm.ScmRepositoryConfigurator;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.StringUtils;

/**
 * Implementation of the release manager.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class DefaultReleaseManager
    extends AbstractLogEnabled
    implements ReleaseManager
{
    /**
     * The phases of release to run, and in what order.
     */
    private List<String> preparePhases;

    /**
     * The phases of release to run to perform.
     */
    private List<String> performPhases;

    /**
     * The phases of release to run to rollback changes
     */
    private List<String> rollbackPhases;

    /**
     * The phases to create a branch.
     */
    private List<String> branchPhases;

    /**
     * The phases to create update versions.
     */
    private List<String> updateVersionsPhases;

    /**
     * The available phases.
     */
    private Map<String, ReleasePhase> releasePhases;

    /**
     * The configuration storage.
     */
    private ReleaseDescriptorStore configStore;

    /**
     * Tool for configuring SCM repositories from release configuration.
     */
    private ScmRepositoryConfigurator scmRepositoryConfigurator;

    private static final int PHASE_SKIP = 0, PHASE_START = 1, PHASE_END = 2, GOAL_START = 11, GOAL_END = 12, ERROR = 99;

    public void prepare( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                         List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        prepare( releaseDescriptor, releaseEnvironment, reactorProjects, true, false, null );
    }

    public void prepare( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                         List<MavenProject> reactorProjects, boolean resume, boolean dryRun )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        prepare( releaseDescriptor, releaseEnvironment, reactorProjects, resume, dryRun, null );
    }

    public ReleaseResult prepareWithResult( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                                            List<MavenProject> reactorProjects, boolean resume, boolean dryRun,
                                            ReleaseManagerListener listener )
    {
        ReleaseResult result = new ReleaseResult();

        result.setStartTime( System.currentTimeMillis() );

        try
        {
            prepare( releaseDescriptor, releaseEnvironment, reactorProjects, resume, dryRun, listener, result );

            result.setResultCode( ReleaseResult.SUCCESS );
        }
        catch ( ReleaseExecutionException e )
        {
            captureException( result, listener, e );
        }
        catch ( ReleaseFailureException e )
        {
            captureException( result, listener, e );
        }
        finally
        {
            result.setEndTime( System.currentTimeMillis() );
        }

        return result;
    }

    public void prepare( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                         List<MavenProject> reactorProjects, boolean resume, boolean dryRun,
                         ReleaseManagerListener listener )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        prepare( releaseDescriptor, releaseEnvironment, reactorProjects, resume, dryRun, listener, null );
    }

    private void prepare( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                          List<MavenProject> reactorProjects, boolean resume, boolean dryRun,
                          ReleaseManagerListener listener, ReleaseResult result )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        updateListener( listener, "prepare", GOAL_START );

        ReleaseDescriptor config;
        if ( resume )
        {
            config = loadReleaseDescriptor( releaseDescriptor, listener );
        }
        else
        {
            config = releaseDescriptor;
        }

        // Later, it would be a good idea to introduce a proper workflow tool so that the release can be made up of a
        // more flexible set of steps.

        String completedPhase = config.getCompletedPhase();
        int index = preparePhases.indexOf( completedPhase );

        for ( int idx = 0; idx <= index; idx++ )
        {
            updateListener( listener, preparePhases.get( idx ), PHASE_SKIP );
        }

        if ( index == preparePhases.size() - 1 )
        {
            logInfo( result, "Release preparation already completed. You can now continue with release:perform, "
                + "or start again using the -Dresume=false flag" );
        }
        else if ( index >= 0 )
        {
            logInfo( result, "Resuming release from phase '" + preparePhases.get( index + 1 ) + "'" );
        }

        // start from next phase
        for ( int i = index + 1; i < preparePhases.size(); i++ )
        {
            String name = preparePhases.get( i );

            ReleasePhase phase = releasePhases.get( name );

            if ( phase == null )
            {
                throw new ReleaseExecutionException( "Unable to find phase '" + name + "' to execute" );
            }

            updateListener( listener, name, PHASE_START );

            ReleaseResult phaseResult = null;
            try
            {
                if ( dryRun )
                {
                    phaseResult = phase.simulate( config, releaseEnvironment, reactorProjects );
                }
                else
                {
                    phaseResult = phase.execute( config, releaseEnvironment, reactorProjects );
                }
            }
            finally
            {
                if ( result != null && phaseResult != null )
                {
                    result.getOutputBuffer().append( phaseResult.getOutput() );
                }
            }

            config.setCompletedPhase( name );
            try
            {
                configStore.write( config );
            }
            catch ( ReleaseDescriptorStoreException e )
            {
                // TODO: rollback?
                throw new ReleaseExecutionException( "Error writing release properties after completing phase", e );
            }

            updateListener( listener, name, PHASE_END );
        }

        updateListener( listener, "prepare", GOAL_END );
    }

    public void rollback( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                          List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        rollback( releaseDescriptor, releaseEnvironment, reactorProjects, null );
    }

    public void rollback( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                          List<MavenProject> reactorProjects, ReleaseManagerListener listener )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        updateListener( listener, "rollback", GOAL_START );

        releaseDescriptor = loadReleaseDescriptor( releaseDescriptor, null );

        for ( String name : rollbackPhases )
        {
            ReleasePhase phase = releasePhases.get( name );

            if ( phase == null )
            {
                throw new ReleaseExecutionException( "Unable to find phase '" + name + "' to execute" );
            }

            updateListener( listener, name, PHASE_START );
            phase.execute( releaseDescriptor, releaseEnvironment, reactorProjects );
            updateListener( listener, name, PHASE_END );
        }

        //call release:clean so that resume will not be possible anymore after a rollback
        clean( releaseDescriptor, listener, reactorProjects );
        updateListener( listener, "prepare", GOAL_END );
    }

    public void perform( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                         List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        perform( releaseDescriptor, releaseEnvironment, reactorProjects, null, true );
    }

    public void perform( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                         List<MavenProject> reactorProjects, boolean clean )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        perform( releaseDescriptor, releaseEnvironment, reactorProjects, null, clean );
    }

    public void perform( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                         List<MavenProject> reactorProjects, ReleaseManagerListener listener )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        perform( releaseDescriptor, releaseEnvironment, reactorProjects, listener, true );
    }

    public void perform( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                         List<MavenProject> reactorProjects, ReleaseManagerListener listener, boolean clean )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        perform( releaseDescriptor, releaseEnvironment, reactorProjects, listener, new ReleaseResult(), clean );
    }

    public ReleaseResult performWithResult( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                                            List<MavenProject> reactorProjects, ReleaseManagerListener listener )
    {
        ReleaseResult result = new ReleaseResult();

        try
        {
            result.setStartTime( System.currentTimeMillis() );

            perform( releaseDescriptor, releaseEnvironment, reactorProjects, listener, result, true );

            result.setResultCode( ReleaseResult.SUCCESS );
        }
        catch ( ReleaseExecutionException e )
        {
            captureException( result, listener, e );
        }
        catch ( ReleaseFailureException e )
        {
            captureException( result, listener, e );
        }
        finally
        {
            result.setEndTime( System.currentTimeMillis() );
        }

        return result;
    }

    private void perform( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                          List<MavenProject> reactorProjects, ReleaseManagerListener listener, ReleaseResult result,
                          boolean clean )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        updateListener( listener, "perform", GOAL_START );

        releaseDescriptor = loadReleaseDescriptor( releaseDescriptor, listener );
        
        ReleaseResult phaseResult = null;

        for ( String name : performPhases )
        {
            ReleasePhase phase = releasePhases.get( name );

            if ( phase == null )
            {
                throw new ReleaseExecutionException( "Unable to find phase '" + name + "' to execute" );
            }

            updateListener( listener, name, PHASE_START );
            phaseResult = phase.execute( releaseDescriptor, releaseEnvironment, reactorProjects );
            result.getOutputBuffer().append( phaseResult.getOutput() );
            updateListener( listener, name, PHASE_END );
        }

        if ( clean )
        {
            // call release:clean so that resume will not be possible anymore after a perform
            clean( releaseDescriptor, listener, reactorProjects );
        }

        updateListener( listener, "perform", GOAL_END );
    }

    public void branch( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                        List<MavenProject> reactorProjects, boolean dryRun )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        branch( releaseDescriptor, releaseEnvironment, reactorProjects, dryRun, null );
    }

    public void branch( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                        List<MavenProject> reactorProjects, boolean dryRun, ReleaseManagerListener listener )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        updateListener( listener, "branch", GOAL_START );

        releaseDescriptor = loadReleaseDescriptor( releaseDescriptor, listener );

        for ( String name : branchPhases )
        {
            ReleasePhase phase = releasePhases.get( name );

            if ( phase == null )
            {
                throw new ReleaseExecutionException( "Unable to find phase '" + name + "' to execute" );
            }

            updateListener( listener, name, PHASE_START );
            if ( dryRun )
            {
                phase.simulate( releaseDescriptor, releaseEnvironment, reactorProjects );
            }
            else
            {
                phase.execute( releaseDescriptor, releaseEnvironment, reactorProjects );
            }
            updateListener( listener, name, PHASE_END );
        }

        if ( !dryRun )
        {
            clean( releaseDescriptor, listener, reactorProjects );
        }

        updateListener( listener, "branch", GOAL_END );
    }

    public void updateVersions( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                                List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        ReleaseManagerListener listener = null;
        
        updateListener( listener, "updateVersions", GOAL_START );

        releaseDescriptor = loadReleaseDescriptor( releaseDescriptor, listener );

        for ( String name : updateVersionsPhases )
        {
            ReleasePhase phase = releasePhases.get( name );

            if ( phase == null )
            {
                throw new ReleaseExecutionException( "Unable to find phase '" + name + "' to execute" );
            }

            updateListener( listener, name, PHASE_START );
            phase.execute( releaseDescriptor, releaseEnvironment, reactorProjects );
            updateListener( listener, name, PHASE_END );
        }

        clean( releaseDescriptor, listener, reactorProjects );

        updateListener( listener, "updateVersions", GOAL_END );
    }

    /**
     * Determines the path of the working directory. By default, this is the
     * checkout directory. For some SCMs, the project root directory is not the
     * checkout directory itself, but a SCM-specific subdirectory.
     *
     * @param checkoutDirectory            The checkout directory as java.io.File
     * @param relativePathProjectDirectory The relative path of the project directory within the checkout
     *                                     directory or ""
     * @return The working directory
     */
    protected File determineWorkingDirectory( File checkoutDirectory, String relativePathProjectDirectory )
    {
        if ( StringUtils.isNotEmpty( relativePathProjectDirectory ) )
        {
            return new File( checkoutDirectory, relativePathProjectDirectory );
        }
        else
        {
            return checkoutDirectory;
        }
    }

    private ReleaseDescriptor loadReleaseDescriptor( ReleaseDescriptor releaseDescriptor,
                                                     ReleaseManagerListener listener )
        throws ReleaseExecutionException
    {
        try
        {
            updateListener( listener, "verify-release-configuration", PHASE_START );
            ReleaseDescriptor descriptor = configStore.read( releaseDescriptor );
            updateListener( listener, "verify-release-configuration", PHASE_END );
            return descriptor;
        }
        catch ( ReleaseDescriptorStoreException e )
        {
            updateListener( listener, e.getMessage(), ERROR );

            throw new ReleaseExecutionException( "Error reading stored configuration: " + e.getMessage(), e );
        }
    }

    public void clean( ReleaseDescriptor releaseDescriptor, ReleaseManagerListener listener,
                       List<MavenProject> reactorProjects )
    {
        updateListener( listener, "cleanup", PHASE_START );

        getLogger().info( "Cleaning up after release..." );

        configStore.delete( releaseDescriptor );
        Set<String> phases = new LinkedHashSet<String>( preparePhases );
        phases.addAll( branchPhases );

        for ( String name : phases )
        {
            ReleasePhase phase = releasePhases.get( name );

            phase.clean( reactorProjects );
        }

        updateListener( listener, "cleanup", PHASE_END );
    }

    void setConfigStore( ReleaseDescriptorStore configStore )
    {
        this.configStore = configStore;
    }

    void updateListener( ReleaseManagerListener listener, String name, int state )
    {
        if ( listener != null )
        {
            switch ( state )
            {
                case GOAL_START:
                    listener.goalStart( name, getGoalPhases( name ) );
                    break;
                case GOAL_END:
                    listener.goalEnd();
                    break;
                case PHASE_SKIP:
                    listener.phaseSkip( name );
                    break;
                case PHASE_START:
                    listener.phaseStart( name );
                    break;
                case PHASE_END:
                    listener.phaseEnd();
                    break;
                default:
                    listener.error( name );
            }
        }
    }

    private List<String> getGoalPhases( String name )
    {
        List<String> phases = new ArrayList<String>();

        if ( "prepare".equals( name ) )
        {
            phases.addAll( preparePhases );
        }
        else if ( "perform".equals( name ) )
        {
            phases.addAll( performPhases );
        }
        else if ( "rollback".equals( name ) )
        {
            phases.addAll( rollbackPhases );
        }
        else if ( "branch".equals( name ) )
        {
            phases.addAll( branchPhases );
        }
        else if ( "updateVersions".equals( name ) )
        {
            phases.addAll( updateVersionsPhases );
        }

        return Collections.unmodifiableList( phases );
    }

    private void logInfo( ReleaseResult result, String message )
    {
        if ( result != null )
        {
            result.appendInfo( message );
        }

        getLogger().info( message );
    }

    private void captureException( ReleaseResult result, ReleaseManagerListener listener, Exception e )
    {
        updateListener( listener, e.getMessage(), ERROR );

        result.appendError( e );

        result.setResultCode( ReleaseResult.ERROR );
    }

    public void branch( ReleaseDescriptor releaseDescriptor, Settings settings, List<MavenProject> reactorProjects,
                        boolean dryRun )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        branch( releaseDescriptor, new DefaultReleaseEnvironment().setSettings( settings ), reactorProjects, dryRun );
    }

    public void branch( ReleaseDescriptor releaseDescriptor, Settings settings, List<MavenProject> reactorProjects,
                        boolean dryRun, ReleaseManagerListener listener )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        branch( releaseDescriptor, new DefaultReleaseEnvironment().setSettings( settings ), reactorProjects, dryRun,
                listener );
    }

    public void perform( ReleaseDescriptor releaseDescriptor, Settings settings, List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        perform( releaseDescriptor, new DefaultReleaseEnvironment().setSettings( settings ), reactorProjects );
    }

    public void perform( ReleaseDescriptor releaseDescriptor, Settings settings, List<MavenProject> reactorProjects,
                         ReleaseManagerListener listener )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        perform( releaseDescriptor, new DefaultReleaseEnvironment().setSettings( settings ), reactorProjects, listener );
    }

    public void perform( ReleaseDescriptor releaseDescriptor, Settings settings, List<MavenProject> reactorProjects,
                         boolean clean )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        perform( releaseDescriptor, new DefaultReleaseEnvironment().setSettings( settings ), reactorProjects, clean );
    }

    public ReleaseResult performWithResult( ReleaseDescriptor releaseDescriptor, Settings settings,
                                            List<MavenProject> reactorProjects, ReleaseManagerListener listener )
    {
        return performWithResult( releaseDescriptor, new DefaultReleaseEnvironment().setSettings( settings ),
                                  reactorProjects, listener );
    }

    public void prepare( ReleaseDescriptor releaseDescriptor, Settings settings, List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        prepare( releaseDescriptor, new DefaultReleaseEnvironment().setSettings( settings ), reactorProjects );
    }

    public void prepare( ReleaseDescriptor releaseDescriptor, Settings settings, List<MavenProject> reactorProjects,
                         boolean resume, boolean dryRun )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        prepare( releaseDescriptor, new DefaultReleaseEnvironment().setSettings( settings ), reactorProjects, resume,
                 dryRun );
    }

    public void prepare( ReleaseDescriptor releaseDescriptor, Settings settings, List<MavenProject> reactorProjects,
                         boolean resume, boolean dryRun, ReleaseManagerListener listener )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        prepare( releaseDescriptor, new DefaultReleaseEnvironment().setSettings( settings ), reactorProjects, resume,
                 dryRun, listener );
    }

    public ReleaseResult prepareWithResult( ReleaseDescriptor releaseDescriptor, Settings settings,
                                            List<MavenProject> reactorProjects, boolean resume, boolean dryRun,
                                            ReleaseManagerListener listener )
    {
        return prepareWithResult( releaseDescriptor, new DefaultReleaseEnvironment().setSettings( settings ),
                                  reactorProjects, resume, dryRun, listener );
    }

    public void rollback( ReleaseDescriptor releaseDescriptor, Settings settings, List<MavenProject> reactorProjects,
                          ReleaseManagerListener listener )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        rollback( releaseDescriptor, new DefaultReleaseEnvironment().setSettings( settings ), reactorProjects, listener );
    }

    public void rollback( ReleaseDescriptor releaseDescriptor, Settings settings, List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        rollback( releaseDescriptor, new DefaultReleaseEnvironment().setSettings( settings ), reactorProjects, null );
    }
}
