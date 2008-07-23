package org.apache.maven.shared.release.exec;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
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
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Fork Maven using the maven-invoker shared library.
 *
 * @plexus.component role="org.apache.maven.shared.release.exec.MavenExecutor" role-hint="invoker"
 */
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

    private static final String FORCE_PLUGIN_UPDATES = "cpu";

    private static final String FORCE_PLUGIN_UPDATES2 = "up";

    private static final String SUPPRESS_PLUGIN_UPDATES = "npu";

    private static final String SUPPRESS_PLUGIN_REGISTRY = "npr";

    private static final char CHECKSUM_FAILURE_POLICY = 'C';

    private static final char CHECKSUM_WARNING_POLICY = 'c';

    private static final String FAIL_FAST = "ff";

    private static final String FAIL_AT_END = "fae";

    private static final String FAIL_NEVER = "fn";

    static
    {
        OPTIONS.addOption( OptionBuilder.withLongOpt( "define" )
                                        .hasArg()
                                        .withDescription( "Define a system property" )
                                        .create( SET_SYSTEM_PROPERTY ) );

        OPTIONS.addOption( OptionBuilder.withLongOpt( "offline" )
                                        .withDescription( "Work offline" )
                                        .create( OFFLINE ) );

        OPTIONS.addOption( OptionBuilder.withLongOpt( "quiet" )
                                        .withDescription( "Quiet output - only show errors" )
                                        .create( QUIET ) );

        OPTIONS.addOption( OptionBuilder.withLongOpt( "debug" )
                                        .withDescription( "Produce execution debug output" )
                                        .create( DEBUG ) );

        OPTIONS.addOption( OptionBuilder.withLongOpt( "errors" )
                                        .withDescription( "Produce execution error messages" )
                                        .create( ERRORS ) );

        OPTIONS.addOption( OptionBuilder.withLongOpt( "reactor" )
                                        .withDescription( "Execute goals for project found in the reactor" )
                                        .create( REACTOR ) );

        OPTIONS.addOption( OptionBuilder.withLongOpt( "non-recursive" )
                                        .withDescription( "Do not recurse into sub-projects" )
                                        .create( NON_RECURSIVE ) );

        OPTIONS.addOption( OptionBuilder.withLongOpt( "update-snapshots" )
                                        .withDescription( "Forces a check for updated releases and snapshots on remote repositories" )
                                        .create( UPDATE_SNAPSHOTS ) );

        OPTIONS.addOption( OptionBuilder.withLongOpt( "activate-profiles" )
                                        .withDescription( "Comma-delimited list of profiles to activate" )
                                        .hasArg()
                                        .create( ACTIVATE_PROFILES ) );

        OPTIONS.addOption( OptionBuilder.withLongOpt( "check-plugin-updates" )
                                        .withDescription( "Force upToDate check for any relevant registered plugins" )
                                        .create( FORCE_PLUGIN_UPDATES ) );

        OPTIONS.addOption( OptionBuilder.withLongOpt( "update-plugins" )
                                        .withDescription( "Synonym for " + FORCE_PLUGIN_UPDATES )
                                        .create( FORCE_PLUGIN_UPDATES2 ) );

        OPTIONS.addOption( OptionBuilder.withLongOpt( "no-plugin-updates" )
                                        .withDescription( "Suppress upToDate check for any relevant registered plugins" )
                                        .create( SUPPRESS_PLUGIN_UPDATES ) );

        OPTIONS.addOption( OptionBuilder.withLongOpt( "no-plugin-registry" )
                                        .withDescription( "Don't use ~/.m2/plugin-registry.xml for plugin versions" )
                                        .create( SUPPRESS_PLUGIN_REGISTRY ) );

        OPTIONS.addOption( OptionBuilder.withLongOpt( "strict-checksums" )
                                        .withDescription( "Fail the build if checksums don't match" )
                                        .create( CHECKSUM_FAILURE_POLICY ) );

        OPTIONS.addOption( OptionBuilder.withLongOpt( "lax-checksums" )
                                        .withDescription( "Warn if checksums don't match" )
                                        .create( CHECKSUM_WARNING_POLICY ) );

        OPTIONS.addOption( OptionBuilder.withLongOpt( "fail-fast" )
                                        .withDescription( "Stop at first failure in reactorized builds" )
                                        .create( FAIL_FAST ) );

        OPTIONS.addOption( OptionBuilder.withLongOpt( "fail-at-end" )
                                        .withDescription( "Only fail the build afterwards; allow all non-impacted builds to continue" )
                                        .create( FAIL_AT_END ) );

        OPTIONS.addOption( OptionBuilder.withLongOpt( "fail-never" )
                                        .withDescription( "NEVER fail the build, regardless of project result" )
                                        .create( FAIL_NEVER ) );
    }

    private void setupRequest( InvocationRequest req,
                               LoggerBridge bridge,
                               String additionalArguments )
        throws MavenExecutorException
    {
        String[] args;
        if ( additionalArguments == null )
        {
            args = new String[0];
        }
        else
        {
            args = additionalArguments.split( " " );
        }
        try
        {
            CommandLine cli = new PosixParser().parse( OPTIONS, args );

            if ( cli.hasOption( SET_SYSTEM_PROPERTY ) )
            {
                String[] properties = cli.getOptionValues( SET_SYSTEM_PROPERTY );
                Properties props = new Properties();
                for ( int i = 0; i < properties.length; i++ )
                {
                    String[] parts = properties[i].split( "=" );
                    props.setProperty( parts[0], parts[1] );
                }

                req.setProperties( props );
            }

            if ( cli.hasOption( OFFLINE ) )
            {
                req.setOffline( true );
            }

            if ( cli.hasOption( QUIET ) )
            {
                bridge.setThreshold( Logger.LEVEL_ERROR );
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
                List activatedProfiles = new ArrayList();
                List deactivatedProfiles = new ArrayList();

                if ( profiles != null )
                {
                    for ( int i = 0; i < profiles.length; ++i )
                    {
                        StringTokenizer profileTokens = new StringTokenizer( profiles[i], "," );

                        while ( profileTokens.hasMoreTokens() )
                        {
                            String profileAction = profileTokens.nextToken().trim();

                            if ( profileAction.startsWith( "-" ) || profileAction.startsWith( "!" ) )
                            {
                                deactivatedProfiles.add( profileAction.substring( 1 ) );
                            }
                            else if ( profileAction.startsWith( "+" ) )
                            {
                                activatedProfiles.add( profileAction.substring( 1 ) );
                            }
                            else
                            {
                                activatedProfiles.add( profileAction );
                            }
                        }
                    }
                }

                if ( !deactivatedProfiles.isEmpty() )
                {
                    getLogger().warn(
                                      "Explicit profile deactivation is not yet supported. "
                                          + "The following profiles will NOT be deactivated: "
                                          + StringUtils.join( deactivatedProfiles.iterator(), ", " ) );
                }

                if ( !activatedProfiles.isEmpty() )
                {
                    req.setProfiles( activatedProfiles );
                }
            }

            if ( cli.hasOption( FORCE_PLUGIN_UPDATES ) || cli.hasOption( FORCE_PLUGIN_UPDATES2 ) )
            {
                getLogger().warn( "Forcing plugin updates is not supported currently." );
            }
            else if ( cli.hasOption( SUPPRESS_PLUGIN_UPDATES ) )
            {
                req.setNonPluginUpdates( true );
            }

            if ( cli.hasOption( SUPPRESS_PLUGIN_REGISTRY ) )
            {
                getLogger().warn( "Explicit suppression of the plugin registry is not supported currently." );
            }

            if ( cli.hasOption( CHECKSUM_FAILURE_POLICY ) )
            {
                req.setGlobalChecksumPolicy( InvocationRequest.CHECKSUM_POLICY_FAIL );
            }
            else if ( cli.hasOption( CHECKSUM_WARNING_POLICY ) )
            {
                req.setGlobalChecksumPolicy( InvocationRequest.CHECKSUM_POLICY_WARN );
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
        }
        catch ( ParseException e )
        {
            throw new MavenExecutorException( "Failed to re-parse additional arguments for Maven invocation.", e );
        }
    }

    public void executeGoals( File workingDirectory,
                              String goals,
                              ReleaseEnvironment releaseEnvironment,
                              boolean interactive,
                              String additionalArguments,
                              String pomFileName,
                              ReleaseResult result )
        throws MavenExecutorException
    {
        Handler handler = new Handler( getLogger() );
        LoggerBridge bridge = new LoggerBridge( getLogger() );

        Invoker invoker = new DefaultInvoker().setMavenHome( releaseEnvironment.getMavenHome() )
                                              .setLogger( bridge )
                                              .setOutputHandler( handler )
                                              .setErrorHandler( handler );

        InvocationRequest req = new DefaultInvocationRequest().setDebug( getLogger().isDebugEnabled() )
                                                              .setBaseDirectory( workingDirectory )
                                                              .setInteractive( interactive );

        if ( pomFileName != null )
        {
            req.setPomFileName( pomFileName );
        }

        if ( releaseEnvironment.getSettings() != null )
        {
            req.setUserSettingsFile( releaseEnvironment.getSettings().getRuntimeInfo().getFile() );
        }

        File localRepoDir = releaseEnvironment.getLocalRepositoryDirectory();
        if ( localRepoDir != null )
        {
            req.setLocalRepositoryDirectory( localRepoDir );
        }

        setupRequest( req, bridge, additionalArguments );

        if ( goals.trim().length() > 0 )
        {
            String[] rawGoals = goals.split( " " );
            req.setGoals( Arrays.asList( rawGoals ) );
        }

        try
        {
            InvocationResult invocationResult = invoker.execute( req );

            if ( invocationResult.getExecutionException() != null )
            {
                throw new MavenExecutorException( "Error executing Maven.", invocationResult.getExecutionException() );
            }
            if ( invocationResult.getExitCode() != 0 )
            {
                throw new MavenExecutorException( "Maven execution failed, exit code: \'"
                    + invocationResult.getExitCode() + "\'", invocationResult.getExitCode(), "", "" );
            }
        }
        catch ( MavenInvocationException e )
        {
            throw new MavenExecutorException( "Failed to invoke Maven build.", e );
        }
    }

    public void executeGoals( File workingDirectory,
                              String goals,
                              ReleaseEnvironment releaseEnvironment,
                              boolean interactive,
                              String additionalArguments,
                              ReleaseResult result )
        throws MavenExecutorException
    {
        executeGoals( workingDirectory,
                      goals,
                      releaseEnvironment,
                      interactive,
                      additionalArguments,
                      null,
                      result );
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

        public void debug( String message,
                           Throwable error )
        {
            logger.debug( message, error );
        }

        public void debug( String message )
        {
            logger.debug( message );
        }

        public void error( String message,
                           Throwable error )
        {
            logger.error( message, error );
        }

        public void error( String message )
        {
            logger.error( message );
        }

        public void fatalError( String message,
                                Throwable error )
        {
            logger.fatalError( message, error );
        }

        public void fatalError( String message )
        {
            logger.fatalError( message );
        }

        public Logger getChildLogger( String message )
        {
            return logger.getChildLogger( message );
        }

        public String getName()
        {
            return logger.getName();
        }

        public int getThreshold()
        {
            return logger.getThreshold();
        }

        public void info( String message,
                          Throwable error )
        {
            logger.info( message, error );
        }

        public void info( String message )
        {
            logger.info( message );
        }

        public boolean isDebugEnabled()
        {
            return logger.isDebugEnabled();
        }

        public boolean isErrorEnabled()
        {
            return logger.isErrorEnabled();
        }

        public boolean isFatalErrorEnabled()
        {
            return logger.isFatalErrorEnabled();
        }

        public boolean isInfoEnabled()
        {
            return logger.isInfoEnabled();
        }

        public boolean isWarnEnabled()
        {
            return logger.isWarnEnabled();
        }

        public void setThreshold( int level )
        {
            logger.setThreshold( level );
        }

        public void warn( String message,
                          Throwable error )
        {
            logger.warn( message, error );
        }

        public void warn( String message )
        {
            logger.warn( message );
        }

    }

}
