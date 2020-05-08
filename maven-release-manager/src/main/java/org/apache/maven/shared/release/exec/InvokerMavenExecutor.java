package org.apache.maven.shared.release.exec;

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
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Writer;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.InvokerLogger;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.cli.CommandLineUtils;

/**
 * Fork Maven using the maven-invoker shared library.
 */
@Component( role = MavenExecutor.class, hint = "invoker" )
@SuppressWarnings( "static-access" )
public class InvokerMavenExecutor
    extends AbstractMavenExecutor
{

    private static final Options OPTIONS = new Options();

    private static final char SET_SYSTEM_PROPERTY = 'D';

    private static final char OFFLINE = 'o';

    private static final char REACTOR = 'r';

    private static final char QUIET = 'q';

    private static final char DEBUG = 'X';

    private static final char ERRORS = 'e';

    private static final char NON_RECURSIVE = 'N';

    private static final char UPDATE_SNAPSHOTS = 'U';

    private static final char ACTIVATE_PROFILES = 'P';

    private static final char CHECKSUM_FAILURE_POLICY = 'C';

    private static final char CHECKSUM_WARNING_POLICY = 'c';

    private static final char ALTERNATE_USER_SETTINGS = 's';

    private static final String ALTERNATE_GLOBAL_SETTINGS = "gs";

    private static final String FAIL_FAST = "ff";

    private static final String FAIL_AT_END = "fae";

    private static final String FAIL_NEVER = "fn";
    
    private static final String ALTERNATE_POM_FILE = "f";
    
    private static final String THREADS = "T";

    private static final String BATCH_MODE = "B";
    
    public static final char ALTERNATE_USER_TOOLCHAINS = 't';
    
    static
    {
        OPTIONS.addOption(
            OptionBuilder.withLongOpt( "define" ).hasArg().withDescription( "Define a system property" ).create(
                SET_SYSTEM_PROPERTY ) );

        OPTIONS.addOption( OptionBuilder.withLongOpt( "offline" ).withDescription( "Work offline" ).create( OFFLINE ) );

        OPTIONS.addOption(
            OptionBuilder.withLongOpt( "quiet" ).withDescription( "Quiet output - only show errors" ).create( QUIET ) );

        OPTIONS.addOption(
            OptionBuilder.withLongOpt( "debug" ).withDescription( "Produce execution debug output" ).create( DEBUG ) );

        OPTIONS.addOption(
            OptionBuilder.withLongOpt( "errors" ).withDescription( "Produce execution error messages" ).create(
                ERRORS ) );

        OPTIONS.addOption( OptionBuilder.withLongOpt( "reactor" ).withDescription(
            "Execute goals for project found in the reactor" ).create( REACTOR ) );

        OPTIONS.addOption(
            OptionBuilder.withLongOpt( "non-recursive" ).withDescription( "Do not recurse into sub-projects" ).create(
                NON_RECURSIVE ) );

        OPTIONS.addOption( OptionBuilder.withLongOpt( "update-snapshots" ).withDescription(
            "Forces a check for updated releases and snapshots on remote repositories" ).create( UPDATE_SNAPSHOTS ) );

        OPTIONS.addOption( OptionBuilder.withLongOpt( "activate-profiles" ).withDescription(
            "Comma-delimited list of profiles to activate" ).hasArg().create( ACTIVATE_PROFILES ) );

        OPTIONS.addOption( OptionBuilder.withLongOpt( "strict-checksums" ).withDescription(
            "Fail the build if checksums don't match" ).create( CHECKSUM_FAILURE_POLICY ) );

        OPTIONS.addOption(
            OptionBuilder.withLongOpt( "lax-checksums" ).withDescription( "Warn if checksums don't match" ).create(
                CHECKSUM_WARNING_POLICY ) );

        OPTIONS.addOption( OptionBuilder.withLongOpt( "settings" ).withDescription(
            "Alternate path for the user settings file" ).hasArg().create( ALTERNATE_USER_SETTINGS ) );

        OPTIONS.addOption( OptionBuilder.withLongOpt( "global-settings" ).withDescription(
            " Alternate path for the global settings file" ).hasArg().create( ALTERNATE_GLOBAL_SETTINGS ) );

        OPTIONS.addOption( OptionBuilder.withLongOpt( "fail-fast" ).withDescription(
            "Stop at first failure in reactorized builds" ).create( FAIL_FAST ) );

        OPTIONS.addOption( OptionBuilder.withLongOpt( "fail-at-end" ).withDescription(
            "Only fail the build afterwards; allow all non-impacted builds to continue" ).create( FAIL_AT_END ) );

        OPTIONS.addOption( OptionBuilder.withLongOpt( "fail-never" ).withDescription(
            "NEVER fail the build, regardless of project result" ).create( FAIL_NEVER ) );
        
        OPTIONS.addOption( OptionBuilder.withLongOpt( "file" ).withDescription( 
            "Force the use of an alternate POM file." ).hasArg().create( ALTERNATE_POM_FILE ) );
        
        OPTIONS.addOption( OptionBuilder.withLongOpt( "threads" ).withDescription( 
            "Thread count, for instance 2.0C where C is core multiplied" ).hasArg().create( THREADS ) );
        
        OPTIONS.addOption( OptionBuilder.withLongOpt( "batch-mode" ).withDescription( 
            "Run in non-interactive (batch) mode" ).create( BATCH_MODE ) );
        
        OPTIONS.addOption( OptionBuilder.withLongOpt( "toolchains" ).withDescription( 
            "Alternate path for the user toolchains file" ).hasArg().create( ALTERNATE_USER_TOOLCHAINS ) );
    }

    // TODO: Configuring an invocation request from a command line could as well be part of the Invoker API
    protected void setupRequest( InvocationRequest req,
                                 InvokerLogger bridge,
                               String additionalArguments )
        throws MavenExecutorException
    {
        try
        {
            String[] args = CommandLineUtils.translateCommandline( additionalArguments );
            CommandLine cli = new PosixParser().parse( OPTIONS, args );

            if ( cli.hasOption( SET_SYSTEM_PROPERTY ) )
            {
                String[] properties = cli.getOptionValues( SET_SYSTEM_PROPERTY );
                Properties props = new Properties();
                for ( int i = 0; i < properties.length; i++ )
                {
                    String property = properties[i];
                    String name, value;
                    int sep = property.indexOf( "=" );
                    if ( sep <= 0 )
                    {
                        name = property.trim();
                        value = "true";
                    }
                    else
                    {
                        name = property.substring( 0, sep ).trim();
                        value = property.substring( sep + 1 ).trim();
                    }
                    props.setProperty( name, value );
                }

                req.setProperties( props );
            }

            if ( cli.hasOption( OFFLINE ) )
            {
                req.setOffline( true );
            }

            if ( cli.hasOption( QUIET ) )
            {
                // TODO: setQuiet() currently not supported by InvocationRequest
                req.setDebug( false );
            }
            else if ( cli.hasOption( DEBUG ) )
            {
                req.setDebug( true );
            }
            else if ( cli.hasOption( ERRORS ) )
            {
                req.setShowErrors( true );
            }

            if ( cli.hasOption( REACTOR ) )
            {
                req.setRecursive( true );
            }
            else if ( cli.hasOption( NON_RECURSIVE ) )
            {
                req.setRecursive( false );
            }

            if ( cli.hasOption( UPDATE_SNAPSHOTS ) )
            {
                req.setUpdateSnapshots( true );
            }

            if ( cli.hasOption( ACTIVATE_PROFILES ) )
            {
                String[] profiles = cli.getOptionValues( ACTIVATE_PROFILES );
                
                if ( profiles != null )
                {
                    req.setProfiles( Arrays.asList( profiles ) );
                }
            }

            if ( cli.hasOption( CHECKSUM_FAILURE_POLICY ) )
            {
                req.setGlobalChecksumPolicy( InvocationRequest.CHECKSUM_POLICY_FAIL );
            }
            else if ( cli.hasOption( CHECKSUM_WARNING_POLICY ) )
            {
                req.setGlobalChecksumPolicy( InvocationRequest.CHECKSUM_POLICY_WARN );
            }

            if ( cli.hasOption( ALTERNATE_USER_SETTINGS ) )
            {
                req.setUserSettingsFile( new File( cli.getOptionValue( ALTERNATE_USER_SETTINGS ) ) );
            }
            
            if ( cli.hasOption( ALTERNATE_GLOBAL_SETTINGS ) )
            {
                req.setGlobalSettingsFile( new File( cli.getOptionValue( ALTERNATE_GLOBAL_SETTINGS ) ) );
            }

            if ( cli.hasOption( FAIL_AT_END ) )
            {
                req.setFailureBehavior( InvocationRequest.REACTOR_FAIL_AT_END );
            }
            else if ( cli.hasOption( FAIL_FAST ) )
            {
                req.setFailureBehavior( InvocationRequest.REACTOR_FAIL_FAST );
            }
            if ( cli.hasOption( FAIL_NEVER ) )
            {
                req.setFailureBehavior( InvocationRequest.REACTOR_FAIL_NEVER );
            }
            if ( cli.hasOption( ALTERNATE_POM_FILE ) )
            {
                if ( req.getPomFileName() != null )
                {
                    getLogger().info( "pomFileName is already set, ignoring the -f argument" );
                }
                else
                {
                    req.setPomFileName( cli.getOptionValue( ALTERNATE_POM_FILE ) );
                }
            }
            
            if ( cli.hasOption( THREADS ) )
            {
                req.setThreads( cli.getOptionValue( THREADS ) );
            }
            
            if ( cli.hasOption( BATCH_MODE ) )
            {
                req.setInteractive( false );
            }
            
            if ( cli.hasOption( ALTERNATE_USER_TOOLCHAINS ) )
            {
                req.setToolchainsFile( new File( cli.getOptionValue( ALTERNATE_USER_TOOLCHAINS ) ) );
            }
            
        }
        catch ( Exception e )
        {
            throw new MavenExecutorException( "Failed to re-parse additional arguments for Maven invocation.", e );
        }
    }

    @Override
    public void executeGoals( File workingDirectory, List<String> goals, ReleaseEnvironment releaseEnvironment,
                              boolean interactive, String additionalArguments, String pomFileName,
                              ReleaseResult result )
        throws MavenExecutorException
    {
        InvocationOutputHandler handler = getOutputHandler();
        InvokerLogger bridge = getInvokerLogger();

        File mavenPath = null;
        // if null we use the current one
        if ( releaseEnvironment.getMavenHome() != null )
        {
            mavenPath = releaseEnvironment.getMavenHome();
        }
        else
        {
            String mavenHome = System.getProperty( "maven.home" );
            if ( mavenHome == null )
            {
                mavenHome = System.getenv( "MAVEN_HOME" );
            }
            if ( mavenHome == null )
            {
                mavenHome = System.getenv( "M2_HOME" );
            }
            mavenPath = mavenHome == null ? null : new File( mavenHome );
        }
        Invoker invoker =
            new DefaultInvoker().setMavenHome( mavenPath ).setLogger( bridge )
                .setOutputHandler( handler ).setErrorHandler( handler );

        InvocationRequest req =
            new DefaultInvocationRequest().setDebug( getLogger().isDebugEnabled() )
                .setBaseDirectory( workingDirectory ).setInteractive( interactive );

        if ( pomFileName != null )
        {
            req.setPomFileName( pomFileName );
        }

        File settingsFile = null;
        if ( releaseEnvironment.getSettings() != null )
        {
            // Have to serialize to a file as if Maven is embedded, there may not actually be a settings.xml on disk
            try
            {
                settingsFile = File.createTempFile( "release-settings", ".xml" );
                SettingsXpp3Writer writer = getSettingsWriter();
                
                try ( FileWriter fileWriter = new FileWriter( settingsFile ) )
                {
                    writer.write( fileWriter, encryptSettings( releaseEnvironment.getSettings() ) );
                }
                req.setUserSettingsFile( settingsFile );
            }
            catch ( IOException e )
            {
                throw new MavenExecutorException( "Could not create temporary file for release settings.xml", e );
            }
        }
        try
        {
            File localRepoDir = releaseEnvironment.getLocalRepositoryDirectory();
            if ( localRepoDir != null )
            {
                req.setLocalRepositoryDirectory( localRepoDir );
            }

            setupRequest( req, bridge, additionalArguments );

            req.setGoals( goals );

            try
            {
                InvocationResult invocationResult = invoker.execute( req );

                if ( invocationResult.getExecutionException() != null )
                {
                    throw new MavenExecutorException( "Error executing Maven.",
                                                      invocationResult.getExecutionException() );
                }
                if ( invocationResult.getExitCode() != 0 )
                {
                    throw new MavenExecutorException(
                        "Maven execution failed, exit code: \'" + invocationResult.getExitCode() + "\'",
                        invocationResult.getExitCode() );
                }
            }
            catch ( MavenInvocationException e )
            {
                throw new MavenExecutorException( "Failed to invoke Maven build.", e );
            }
        }
        finally
        {
            if ( settingsFile != null && settingsFile.exists() && !settingsFile.delete() )
            {
                settingsFile.deleteOnExit();
            }
        }
    }

    protected InvokerLogger getInvokerLogger()
    {
        return new LoggerBridge( getLogger() );
    }

    protected InvocationOutputHandler getOutputHandler()
    {
        return new Handler( getLogger() );
    }

    private static final class Handler
        implements InvocationOutputHandler
    {
        private Logger logger;

        Handler( Logger logger )
        {
            this.logger = logger;
        }

        public void consumeLine( String line )
        {
            logger.info( line );
        }
    }

    private static final class LoggerBridge
        implements InvokerLogger
    {

        private Logger logger;

        LoggerBridge( Logger logger )
        {
            this.logger = logger;
        }

        @Override
        public void debug( String message, Throwable error )
        {
            logger.debug( message, error );
        }

        @Override
        public void debug( String message )
        {
            logger.debug( message );
        }

        @Override
        public void error( String message, Throwable error )
        {
            logger.error( message, error );
        }

        @Override
        public void error( String message )
        {
            logger.error( message );
        }

        @Override
        public void fatalError( String message, Throwable error )
        {
            logger.fatalError( message, error );
        }

        @Override
        public void fatalError( String message )
        {
            logger.fatalError( message );
        }

        @Override
        public int getThreshold()
        {
            return logger.getThreshold();
        }

        @Override
        public void info( String message, Throwable error )
        {
            logger.info( message, error );
        }

        @Override
        public void info( String message )
        {
            logger.info( message );
        }

        @Override
        public boolean isDebugEnabled()
        {
            return logger.isDebugEnabled();
        }

        @Override
        public boolean isErrorEnabled()
        {
            return logger.isErrorEnabled();
        }

        @Override
        public boolean isFatalErrorEnabled()
        {
            return logger.isFatalErrorEnabled();
        }

        @Override
        public boolean isInfoEnabled()
        {
            return logger.isInfoEnabled();
        }

        @Override
        public boolean isWarnEnabled()
        {
            return logger.isWarnEnabled();
        }

        @Override
        public void setThreshold( int level )
        {
            // NOTE:
            // logger.setThreadhold( level )
            // is not supported in plexus-container-default:1.0-alpha-9 as used in Maven 2.x
        }

        @Override
        public void warn( String message, Throwable error )
        {
            logger.warn( message, error );
        }

        @Override
        public void warn( String message )
        {
            logger.warn( message );
        }
    }

}
