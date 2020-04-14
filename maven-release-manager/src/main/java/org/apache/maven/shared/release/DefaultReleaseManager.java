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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder.BuilderReleaseDescriptor;
import org.apache.maven.shared.release.config.ReleaseDescriptorStore;
import org.apache.maven.shared.release.config.ReleaseDescriptorStoreException;
import org.apache.maven.shared.release.config.ReleaseUtils;
import org.apache.maven.shared.release.phase.ReleasePhase;
import org.apache.maven.shared.release.phase.ResourceGenerator;
import org.apache.maven.shared.release.strategy.Strategy;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.StringUtils;

/**
 * Implementation of the release manager.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
@Component( role = ReleaseManager.class )
public class DefaultReleaseManager
    extends AbstractLogEnabled
    implements ReleaseManager
{
    @Requirement
    private Map<String, Strategy> strategies;

    /**
     * The available phases.
     */
    @Requirement
    private Map<String, ReleasePhase> releasePhases;

    /**
     * The configuration storage.
     */
    @Requirement( hint = "properties" )
    private ReleaseDescriptorStore configStore;

    private static final int PHASE_SKIP = 0, PHASE_START = 1, PHASE_END = 2, GOAL_END = 12, ERROR = 99;

    @Override
    public ReleaseResult prepareWithResult( ReleasePrepareRequest prepareRequest )
    {
        ReleaseResult result = new ReleaseResult();

        result.setStartTime( System.currentTimeMillis() );

        try
        {
            prepare( prepareRequest, result );

            result.setResultCode( ReleaseResult.SUCCESS );
        }
        catch ( ReleaseExecutionException | ReleaseFailureException e )
        {
            captureException( result, prepareRequest.getReleaseManagerListener(), e );
        }
        finally
        {
            result.setEndTime( System.currentTimeMillis() );
        }

        return result;
    }

    @Override
    public void prepare( ReleasePrepareRequest prepareRequest )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        prepare( prepareRequest, new ReleaseResult() );
    }

    private void prepare( ReleasePrepareRequest prepareRequest, ReleaseResult result )
        throws ReleaseExecutionException, ReleaseFailureException
    {

        final ReleaseDescriptorBuilder builder = prepareRequest.getReleaseDescriptorBuilder();

        // Create a config containing values from the session properties (ie command line properties with cli).
        ReleaseUtils.copyPropertiesToReleaseDescriptor( prepareRequest.getUserProperties(),
                new ReleaseDescriptorBuilder()
                {
                    public ReleaseDescriptorBuilder addDevelopmentVersion( String key,
                                                                           String value )
                    {
                        builder.addDevelopmentVersion( key, value );
                        return this;
                    }

                    public ReleaseDescriptorBuilder addReleaseVersion( String key,
                                                                       String value )
                    {
                        builder.addReleaseVersion( key, value );
                        return this;
                    }

                    public ReleaseDescriptorBuilder addDependencyReleaseVersion( String dependencyKey,
                                                                                String version )
                    {
                        builder.addDependencyReleaseVersion( dependencyKey, version );
                        return this;
                    }

                    public ReleaseDescriptorBuilder addDependencyDevelopmentVersion( String dependencyKey,
                                                                                    String version )
                    {
                        builder.addDependencyDevelopmentVersion( dependencyKey, version );
                        return this;
                    }
                } );

        BuilderReleaseDescriptor config;
        if ( BooleanUtils.isNotFalse( prepareRequest.getResume() ) )
        {
            config = loadReleaseDescriptor( builder, prepareRequest.getReleaseManagerListener() );
        }
        else
        {
            config = ReleaseUtils.buildReleaseDescriptor( builder );
        }

        Strategy releaseStrategy = getStrategy( config.getReleaseStrategyId() );

        List<String> preparePhases = getGoalPhases( releaseStrategy, "prepare" );

        goalStart( prepareRequest.getReleaseManagerListener(), "prepare", preparePhases );

        // Later, it would be a good idea to introduce a proper workflow tool so that the release can be made up of a
        // more flexible set of steps.

        String completedPhase = config.getCompletedPhase();
        int index = preparePhases.indexOf( completedPhase );

        for ( int idx = 0; idx <= index; idx++ )
        {
            updateListener( prepareRequest.getReleaseManagerListener(), preparePhases.get( idx ), PHASE_SKIP );
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

            updateListener( prepareRequest.getReleaseManagerListener(), name, PHASE_START );

            ReleaseResult phaseResult = null;
            try
            {
                if ( BooleanUtils.isTrue( prepareRequest.getDryRun() ) )
                {
                    phaseResult = phase.simulate( config,
                                                  prepareRequest.getReleaseEnvironment(),
                                                  prepareRequest.getReactorProjects() );
                }
                else
                {
                    phaseResult = phase.execute( config,
                                                 prepareRequest.getReleaseEnvironment(),
                                                 prepareRequest.getReactorProjects() );
                }
            }
            finally
            {
                if ( result != null && phaseResult != null )
                {
                    result.appendOutput(  phaseResult.getOutput() );
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

            updateListener( prepareRequest.getReleaseManagerListener(), name, PHASE_END );
        }

        updateListener( prepareRequest.getReleaseManagerListener(), "prepare", GOAL_END );
    }

    @Override
    public void rollback( ReleaseRollbackRequest rollbackRequest )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        ReleaseDescriptor releaseDescriptor =
            loadReleaseDescriptor( rollbackRequest.getReleaseDescriptorBuilder(), null );

        Strategy releaseStrategy = getStrategy( releaseDescriptor.getReleaseStrategyId() );

        List<String> rollbackPhases = getGoalPhases( releaseStrategy, "rollback" );

        goalStart( rollbackRequest.getReleaseManagerListener(), "rollback", rollbackPhases );

        for ( String name : rollbackPhases )
        {
            ReleasePhase phase = releasePhases.get( name );

            if ( phase == null )
            {
                throw new ReleaseExecutionException( "Unable to find phase '" + name + "' to execute" );
            }

            updateListener( rollbackRequest.getReleaseManagerListener(), name, PHASE_START );
            phase.execute( releaseDescriptor,
                           rollbackRequest.getReleaseEnvironment(),
                           rollbackRequest.getReactorProjects() );
            updateListener( rollbackRequest.getReleaseManagerListener(), name, PHASE_END );
        }

        //call release:clean so that resume will not be possible anymore after a rollback
        clean( rollbackRequest );
        updateListener( rollbackRequest.getReleaseManagerListener(), "rollback", GOAL_END );
    }

    @Override
    public ReleaseResult performWithResult( ReleasePerformRequest performRequest )
    {
        ReleaseResult result = new ReleaseResult();

        try
        {
            result.setStartTime( System.currentTimeMillis() );

            perform( performRequest, result );

            result.setResultCode( ReleaseResult.SUCCESS );
        }
        catch ( ReleaseExecutionException | ReleaseFailureException e )
        {
            captureException( result, performRequest.getReleaseManagerListener(), e );
        }
        finally
        {
            result.setEndTime( System.currentTimeMillis() );
        }

        return result;
    }

    @Override
    public void perform( ReleasePerformRequest performRequest )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        perform( performRequest, new ReleaseResult() );
    }

    private void perform( ReleasePerformRequest performRequest, ReleaseResult result )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        List<String> specificProfiles =
            ReleaseUtils.buildReleaseDescriptor( performRequest.getReleaseDescriptorBuilder() )
            .getActivateProfiles();

        ReleaseDescriptor releaseDescriptor =
            loadReleaseDescriptor( performRequest.getReleaseDescriptorBuilder(),
                                   performRequest.getReleaseManagerListener() );

        if ( specificProfiles != null && !specificProfiles.isEmpty() )
        {
            for ( String specificProfile : specificProfiles )
            {
                if ( !releaseDescriptor.getActivateProfiles().contains( specificProfile ) )
                {
                    releaseDescriptor.getActivateProfiles().add( specificProfile );
                }
            }
        }

        Strategy releaseStrategy = getStrategy( releaseDescriptor.getReleaseStrategyId() );

        List<String> performPhases = getGoalPhases( releaseStrategy, "perform" );

        goalStart( performRequest.getReleaseManagerListener(), "perform", performPhases );

        for ( String name : performPhases )
        {
            ReleasePhase phase = releasePhases.get( name );

            if ( phase == null )
            {
                throw new ReleaseExecutionException( "Unable to find phase '" + name + "' to execute" );
            }

            updateListener( performRequest.getReleaseManagerListener(), name, PHASE_START );

            ReleaseResult phaseResult = null;
            try
            {
                if ( BooleanUtils.isTrue( performRequest.getDryRun() ) )
                {
                    phaseResult = phase.simulate( releaseDescriptor,
                                                 performRequest.getReleaseEnvironment(),
                                                 performRequest.getReactorProjects() );
                }
                else
                {
                    phaseResult = phase.execute( releaseDescriptor,
                                                 performRequest.getReleaseEnvironment(),
                                                 performRequest.getReactorProjects() );
                }
            }
            finally
            {
                if ( result != null && phaseResult != null )
                {
                    result.appendOutput( phaseResult.getOutput() );
                }
            }

            updateListener( performRequest.getReleaseManagerListener(), name, PHASE_END );
        }

        if ( BooleanUtils.isNotFalse( performRequest.getClean() ) )
        {
            // call release:clean so that resume will not be possible anymore after a perform
            clean( performRequest );
        }

        updateListener( performRequest.getReleaseManagerListener(), "perform", GOAL_END );
    }

    @Override
    public void branch( ReleaseBranchRequest branchRequest )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        final ReleaseDescriptorBuilder builder = branchRequest.getReleaseDescriptorBuilder();
        
        ReleaseUtils.copyPropertiesToReleaseDescriptor( branchRequest.getUserProperties(),
                    new ReleaseDescriptorBuilder()
                    {
                        public ReleaseDescriptorBuilder addDevelopmentVersion( String key,
                                                                               String value )
                        {
                            builder.addDevelopmentVersion( key, value );
                            return this;
                        }

                        public ReleaseDescriptorBuilder addReleaseVersion( String key,
                                                                           String value )
                        {
                            builder.addReleaseVersion( key, value );
                            return this;
                        }
                    } );
        
        ReleaseDescriptor releaseDescriptor =
            loadReleaseDescriptor( builder, branchRequest.getReleaseManagerListener() );

        boolean dryRun = BooleanUtils.isTrue( branchRequest.getDryRun() );

        Strategy releaseStrategy = getStrategy( releaseDescriptor.getReleaseStrategyId() );

        List<String> branchPhases = getGoalPhases( releaseStrategy, "branch" );

        goalStart( branchRequest.getReleaseManagerListener(), "branch", branchPhases );

        for ( String name : branchPhases )
        {
            ReleasePhase phase = releasePhases.get( name );

            if ( phase == null )
            {
                throw new ReleaseExecutionException( "Unable to find phase '" + name + "' to execute" );
            }

            updateListener( branchRequest.getReleaseManagerListener(), name, PHASE_START );

            if ( dryRun )
            {
                phase.simulate( releaseDescriptor,
                                branchRequest.getReleaseEnvironment(),
                                branchRequest.getReactorProjects() );
            }
            else // getDryRun is null or FALSE
            {
                phase.execute( releaseDescriptor,
                               branchRequest.getReleaseEnvironment(),
                               branchRequest.getReactorProjects() );
            }
            updateListener( branchRequest.getReleaseManagerListener(), name, PHASE_END );
        }

        if ( !dryRun )
        {
            clean( branchRequest );
        }

        updateListener( branchRequest.getReleaseManagerListener(), "branch", GOAL_END );
    }

    @Override
    public void updateVersions( ReleaseUpdateVersionsRequest updateVersionsRequest )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        final ReleaseDescriptorBuilder builder = updateVersionsRequest.getReleaseDescriptorBuilder();
        
        // Create a config containing values from the session properties (ie command line properties with cli).
        ReleaseUtils.copyPropertiesToReleaseDescriptor( updateVersionsRequest.getUserProperties(),
                                    new ReleaseDescriptorBuilder()
                                    {
                                        public ReleaseDescriptorBuilder addDevelopmentVersion( String key,
                                                                                               String value )
                                        {
                                            builder.addDevelopmentVersion( key, value );
                                            return this;
                                        }

                                        public ReleaseDescriptorBuilder addReleaseVersion( String key,
                                                                                           String value )
                                        {
                                            builder.addReleaseVersion( key, value );
                                            return this;
                                        }
                                    } );

        ReleaseDescriptor releaseDescriptor =
            loadReleaseDescriptor( builder, updateVersionsRequest.getReleaseManagerListener() );

        Strategy releaseStrategy = getStrategy( releaseDescriptor.getReleaseStrategyId() );

        List<String> updateVersionsPhases = getGoalPhases( releaseStrategy, "updateVersions" );

        goalStart( updateVersionsRequest.getReleaseManagerListener(), "updateVersions", updateVersionsPhases );

        for ( String name : updateVersionsPhases )
        {
            ReleasePhase phase = releasePhases.get( name );

            if ( phase == null )
            {
                throw new ReleaseExecutionException( "Unable to find phase '" + name + "' to execute" );
            }

            updateListener( updateVersionsRequest.getReleaseManagerListener(), name, PHASE_START );
            phase.execute( releaseDescriptor,
                           updateVersionsRequest.getReleaseEnvironment(),
                           updateVersionsRequest.getReactorProjects() );
            updateListener( updateVersionsRequest.getReleaseManagerListener(), name, PHASE_END );
        }

        clean( updateVersionsRequest );

        updateListener( updateVersionsRequest.getReleaseManagerListener(), "updateVersions", GOAL_END );
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

    private BuilderReleaseDescriptor loadReleaseDescriptor( ReleaseDescriptorBuilder builder,
                                                     ReleaseManagerListener listener )
        throws ReleaseExecutionException
    {
        try
        {
            updateListener( listener, "verify-release-configuration", PHASE_START );
            BuilderReleaseDescriptor descriptor = ReleaseUtils.buildReleaseDescriptor( configStore.read( builder ) );
            updateListener( listener, "verify-release-configuration", PHASE_END );
            return descriptor;
        }
        catch ( ReleaseDescriptorStoreException e )
        {
            updateListener( listener, e.getMessage(), ERROR );

            throw new ReleaseExecutionException( "Error reading stored configuration: " + e.getMessage(), e );
        }
    }

    
    protected void clean( AbstractReleaseRequest releaseRequest  ) throws ReleaseFailureException
    {
        ReleaseCleanRequest cleanRequest = new ReleaseCleanRequest();
        cleanRequest.setReleaseDescriptorBuilder( releaseRequest.getReleaseDescriptorBuilder() );
        cleanRequest.setReleaseManagerListener( releaseRequest.getReleaseManagerListener() );
        cleanRequest.setReactorProjects( releaseRequest.getReactorProjects() );

        clean( cleanRequest );
    }

    @Override
    public void clean( ReleaseCleanRequest cleanRequest ) throws ReleaseFailureException
    {
        updateListener( cleanRequest.getReleaseManagerListener(), "cleanup", PHASE_START );

        getLogger().info( "Cleaning up after release..." );

        ReleaseDescriptor releaseDescriptor =
            ReleaseUtils.buildReleaseDescriptor( cleanRequest.getReleaseDescriptorBuilder() );

        configStore.delete( releaseDescriptor );

        Strategy releaseStrategy = getStrategy( releaseDescriptor.getReleaseStrategyId() );

        Set<String> phases = new LinkedHashSet<>();
        phases.addAll( getGoalPhases( releaseStrategy, "prepare" ) );
        phases.addAll( getGoalPhases( releaseStrategy, "branch" ) );

        for ( String name : phases )
        {
            ReleasePhase phase = releasePhases.get( name );
            
            if ( phase instanceof ResourceGenerator )
            {
                ( (ResourceGenerator) phase ).clean( cleanRequest.getReactorProjects() );
            }
        }

        updateListener( cleanRequest.getReleaseManagerListener(), "cleanup", PHASE_END );
    }

    void setConfigStore( ReleaseDescriptorStore configStore )
    {
        this.configStore = configStore;
    }

    void goalStart( ReleaseManagerListener listener, String goal, List<String> phases )
    {
        if ( listener != null )
        {
            listener.goalStart( goal, phases );
        }
    }

    void updateListener( ReleaseManagerListener listener, String name, int state )
    {
        if ( listener != null )
        {
            switch ( state )
            {
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

    private Strategy getStrategy( String strategyId ) throws ReleaseFailureException
    {
        Strategy strategy = strategies.get( strategyId );
        if ( strategy == null )
        {
            throw new ReleaseFailureException( "Unknown strategy: " + strategyId );
        }
        return strategy;
    }

    private List<String> getGoalPhases( Strategy strategy, String goal )
    {
        List<String> phases;

        if ( "prepare".equals( goal ) )
        {
            phases = strategy.getPreparePhases();
            if ( phases  == null )
            {
                phases = strategies.get( "default" ).getPreparePhases();
            }
        }
        else if ( "perform".equals( goal ) )
        {
            phases = strategy.getPerformPhases();
            if ( phases  == null )
            {
                phases = strategies.get( "default" ).getPerformPhases();
            }
        }
        else if ( "rollback".equals( goal ) )
        {
            phases = strategy.getRollbackPhases();
            if ( phases  == null )
            {
                phases = strategies.get( "default" ).getRollbackPhases();
            }
        }
        else if ( "branch".equals( goal ) )
        {
            phases = strategy.getBranchPhases();
            if ( phases  == null )
            {
                phases = strategies.get( "default" ).getBranchPhases();
            }
        }
        else if ( "updateVersions".equals( goal ) )
        {
            phases = strategy.getUpdateVersionsPhases();
            if ( phases  == null )
            {
                phases = strategies.get( "default" ).getUpdateVersionsPhases();
            }
        }
        else
        {
            phases = null;
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
}
