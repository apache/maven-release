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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
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
import org.apache.maven.shared.release.util.MavenCrypto;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.slf4j.Logger;

/**
 * Fork Maven using the maven-invoker shared library.
 */
@Singleton
@Named( "invoker" )
public class InvokerMavenExecutor
        extends AbstractMavenExecutor
{

    private static final Options OPTIONS = new Options();

    private static final String SET_SYSTEM_PROPERTY = "D";

    private static final String OFFLINE = "o";

    private static final String REACTOR = "r";

    private static final String QUIET = "q";

    private static final String DEBUG = "X";

    private static final String ERRORS = "e";

    private static final String NON_RECURSIVE = "N";

    private static final String UPDATE_SNAPSHOTS = "U";

    private static final String ACTIVATE_PROFILES = "P";

    private static final String CHECKSUM_FAILURE_POLICY = "C";

    private static final String CHECKSUM_WARNING_POLICY = "c";

    private static final String ALTERNATE_USER_SETTINGS = "s";

    private static final String ALTERNATE_GLOBAL_SETTINGS = "gs";

    private static final String FAIL_FAST = "ff";

    private static final String FAIL_AT_END = "fae";

    private static final String FAIL_NEVER = "fn";

    private static final String ALTERNATE_POM_FILE = "f";

    private static final String THREADS = "T";

    private static final String BATCH_MODE = "B";

    public static final String ALTERNATE_USER_TOOLCHAINS = "t";

    static
    {
        OPTIONS.addOption(
                Option.builder( SET_SYSTEM_PROPERTY )
                        .longOpt( "define" )
                        .numberOfArgs( 2 )
                        .valueSeparator( '=' )
                        .desc( "Define a system property" )
                        .build() );

        OPTIONS.addOption(
                Option.builder( OFFLINE )
                        .longOpt( "offline" )
                        .desc( "Work offline" )
                        .build() );

        OPTIONS.addOption(
                Option.builder( QUIET )
                        .longOpt( "quiet" )
                        .desc( "Quiet output - only show errors" )
                        .build() );

        OPTIONS.addOption(
                Option.builder( DEBUG )
                        .longOpt( "debug" )
                        .desc( "Produce execution debug output" )
                        .build() );

        OPTIONS.addOption(
                Option.builder( ERRORS )
                        .longOpt( "errors" )
                        .desc( "Produce execution error messages" )
                        .build() );

        OPTIONS.addOption(
                Option.builder( REACTOR )
                        .longOpt( "reactor" )
                        .desc( "Execute goals for project found in the reactor" )
                        .build() );

        OPTIONS.addOption(
                Option.builder( NON_RECURSIVE )
                        .longOpt( "non-recursive" )
                        .desc( "Do not recurse into sub-projects" )
                        .build() );

        OPTIONS.addOption(
                Option.builder( UPDATE_SNAPSHOTS )
                        .longOpt( "update-snapshots" )
                        .desc( "Forces a check for updated releases and snapshots on remote repositories" )
                        .build() );

        OPTIONS.addOption(
                Option.builder( ACTIVATE_PROFILES )
                        .longOpt( "activate-profiles" )
                        .desc( "Comma-delimited list of profiles to activate" )
                        .hasArg()
                        .build() );

        OPTIONS.addOption(
                Option.builder( CHECKSUM_FAILURE_POLICY )
                        .longOpt( "strict-checksums" )
                        .desc( "Fail the build if checksums don't match" )
                        .build() );

        OPTIONS.addOption(
                Option.builder( CHECKSUM_WARNING_POLICY )
                        .longOpt( "lax-checksums" )
                        .desc( "Warn if checksums don't match" )
                        .build() );

        OPTIONS.addOption(
                Option.builder( ALTERNATE_USER_SETTINGS )
                        .longOpt( "settings" )
                        .desc( "Alternate path for the user settings file" )
                        .hasArg()
                        .build() );

        OPTIONS.addOption(
                Option.builder( ALTERNATE_GLOBAL_SETTINGS )
                        .longOpt( "global-settings" )
                        .desc( "Alternate path for the global settings file" )
                        .hasArg()
                        .build() );

        OPTIONS.addOption(
                Option.builder( FAIL_FAST )
                        .longOpt( "fail-fast" )
                        .desc( "Stop at first failure in reactorized builds" )
                        .build() );

        OPTIONS.addOption(
                Option.builder( FAIL_AT_END )
                        .longOpt( "fail-at-end" )
                        .desc( "Only fail the build afterwards; allow all non-impacted builds to continue" )
                        .build() );

        OPTIONS.addOption(
                Option.builder( FAIL_NEVER )
                        .longOpt( "fail-never" )
                        .desc( "NEVER fail the build, regardless of project result" )
                        .build() );

        OPTIONS.addOption(
                Option.builder( ALTERNATE_POM_FILE )
                        .longOpt( "file" )
                        .desc( "Force the use of an alternate POM file." )
                        .hasArg()
                        .build() );

        OPTIONS.addOption(
                Option.builder( THREADS )
                        .longOpt( "threads" )
                        .desc( "Thread count, for instance 2.0C where C is core multiplied" )
                        .hasArg()
                        .build() );

        OPTIONS.addOption(
                Option.builder( BATCH_MODE )
                        .longOpt( "batch-mode" )
                        .desc( "Run in non-interactive (batch) mode" )
                        .build() );

        OPTIONS.addOption(
                Option.builder( ALTERNATE_USER_TOOLCHAINS )
                        .longOpt( "toolchains" )
                        .desc( "Alternate path for the user toolchains file" )
                        .hasArg()
                        .build() );
    }

    @Inject
    public InvokerMavenExecutor( MavenCrypto mavenCrypto )
    {
        super( mavenCrypto );
    }

    // TODO: Configuring an invocation request from a command line could as well be part of the Invoker API

    /**
     * <p>setupRequest.</p>
     *
     * @param req                 a {@link org.apache.maven.shared.invoker.InvocationRequest} object
     * @param bridge              a {@link org.apache.maven.shared.invoker.InvokerLogger} object
     * @param additionalArguments a {@link java.lang.String} object
     * @throws org.apache.maven.shared.release.exec.MavenExecutorException if any.
     */
    protected void setupRequest( InvocationRequest req,
                                 InvokerLogger bridge,
                                 String additionalArguments )
            throws MavenExecutorException
    {
        try
        {
            String[] args = CommandLineUtils.translateCommandline( additionalArguments );
            CommandLine cli = new DefaultParser().parse( OPTIONS, args );

            if ( cli.hasOption( SET_SYSTEM_PROPERTY ) )
            {
                String[] properties = cli.getOptionValues( SET_SYSTEM_PROPERTY );
                Properties props = new Properties();
                for ( String property : properties )
                {
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
                req.setQuiet( true );
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
                req.setGlobalChecksumPolicy( InvocationRequest.CheckSumPolicy.Fail );
            }
            else if ( cli.hasOption( CHECKSUM_WARNING_POLICY ) )
            {
                req.setGlobalChecksumPolicy( InvocationRequest.CheckSumPolicy.Warn );
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
                req.setReactorFailureBehavior( InvocationRequest.ReactorFailureBehavior.FailAtEnd );
            }
            else if ( cli.hasOption( FAIL_FAST ) )
            {
                req.setReactorFailureBehavior( InvocationRequest.ReactorFailureBehavior.FailFast );
            }
            if ( cli.hasOption( FAIL_NEVER ) )
            {
                req.setReactorFailureBehavior( InvocationRequest.ReactorFailureBehavior.FailNever );
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
                req.setBatchMode( true );
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

        File mavenPath;
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

        Invoker invoker = new DefaultInvoker()
                .setMavenHome( mavenPath )
                .setLogger( bridge );

        InvocationRequest req = new DefaultInvocationRequest()
                .setDebug( getLogger().isDebugEnabled() )
                .setBaseDirectory( workingDirectory )
                .setBatchMode( !interactive )
                .setOutputHandler( handler )
                .setErrorHandler( handler );

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
                            "Maven execution failed, exit code: '" + invocationResult.getExitCode() + "'",
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

    /**
     * <p>getInvokerLogger.</p>
     *
     * @return a {@link org.apache.maven.shared.invoker.InvokerLogger} object
     */
    protected InvokerLogger getInvokerLogger()
    {
        return new LoggerBridge( getLogger() );
    }

    /**
     * <p>getOutputHandler.</p>
     *
     * @return a {@link org.apache.maven.shared.invoker.InvocationOutputHandler} object
     */
    protected InvocationOutputHandler getOutputHandler()
    {
        return new Handler( getLogger() );
    }

    private static final class Handler
            implements InvocationOutputHandler
    {
        private final Logger logger;

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

        private final Logger logger;

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
            logger.error( message, error );
        }

        @Override
        public void fatalError( String message )
        {
            logger.error( message );
        }

        @Override
        public int getThreshold()
        {
            return InvokerLogger.DEBUG;
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
            return logger.isErrorEnabled();
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
            // logger.setThreshold( level )
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
