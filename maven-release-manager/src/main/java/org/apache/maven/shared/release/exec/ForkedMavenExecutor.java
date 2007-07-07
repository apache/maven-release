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

import org.apache.maven.shared.release.ReleaseResult;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.util.cli.StreamFeeder;
import org.codehaus.plexus.util.cli.StreamPumper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Fork Maven to executed a series of goals.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @plexus.component role="org.apache.maven.shared.release.exec.MavenExecutor"
 */
public class ForkedMavenExecutor
    extends AbstractLogEnabled
    implements MavenExecutor
{
    /**
     * Command line factory.
     *
     * @plexus.requirement
     */
    private CommandLineFactory commandLineFactory;

    /**
     * @noinspection UseOfSystemOutOrSystemErr
     */
    public void executeGoals( File workingDirectory, String goals, boolean interactive, String additionalArguments,
                              String pomFileName, ReleaseResult relResult )
        throws MavenExecutorException
    {
        Commandline cl = commandLineFactory.createCommandLine( "mvn" );

        cl.setWorkingDirectory( workingDirectory.getAbsolutePath() );

        cl.addEnvironment( "MAVEN_TERMINATE_CMD", "on" );

        if ( pomFileName != null )
        {
            cl.createArgument().setLine( "-f " + pomFileName );
        }

        if ( goals != null )
        {
            // accept both space and comma, so the old way still work
            String[] tokens = StringUtils.split( goals, ", " );

            for ( int i = 0; i < tokens.length; ++i )
            {
                cl.createArgument().setValue( tokens[i] );
            }
        }

        cl.createArgument().setValue( "--no-plugin-updates" );

        if ( !interactive )
        {
            cl.createArgument().setValue( "--batch-mode" );
        }

        if ( !StringUtils.isEmpty( additionalArguments ) )
        {
            cl.createArgument().setLine( additionalArguments );
        }

        TeeOutputStream stdOut = new TeeOutputStream( System.out );

        TeeOutputStream stdErr = new TeeOutputStream( System.err );

        try
        {
            relResult.appendInfo( "Executing: " + cl.toString() );
            getLogger().info( "Executing: " + cl.toString() );
            
            int result = executeCommandLine( cl, System.in, stdOut, stdErr );

            if ( result != 0 )
            {
                throw new MavenExecutorException( "Maven execution failed, exit code: \'" + result + "\'", result,
                                                  stdOut.toString(), stdErr.toString() );
            }
        }
        catch ( CommandLineException e )
        {
            throw new MavenExecutorException( "Can't run goal " + goals, stdOut.toString(), stdErr.toString(), e );
        }
        finally
        {
            relResult.appendOutput( stdOut.toString() );
        }
    }

    public void executeGoals( File workingDirectory, String goals, boolean interactive, String arguments,
                              ReleaseResult result )
        throws MavenExecutorException
    {
        executeGoals( workingDirectory, goals, interactive, arguments, null, result );
    }

    public void setCommandLineFactory( CommandLineFactory commandLineFactory )
    {
        this.commandLineFactory = commandLineFactory;
    }
    
    
    
    
    public static int executeCommandLine( Commandline cl, InputStream systemIn, 
                                          OutputStream systemOut, OutputStream systemErr )
        throws CommandLineException
    {
        if ( cl == null )
        {
            throw new IllegalArgumentException( "cl cannot be null." );
        }
    
        Process p = cl.execute();
    
        //processes.put( new Long( cl.getPid() ), p );
    
        RawStreamPumper inputFeeder = null;
    
        if ( systemIn != null )
        {
            inputFeeder = new RawStreamPumper( systemIn, p.getOutputStream(), true );
        }
    
        RawStreamPumper outputPumper = new RawStreamPumper( p.getInputStream(), systemOut );
        RawStreamPumper errorPumper = new RawStreamPumper( p.getErrorStream(), systemErr );
    
        if ( inputFeeder != null )
        {
            inputFeeder.start();
        }
    
        outputPumper.start();
    
        errorPumper.start();
    
        try
        {
            int returnValue = p.waitFor();
    
            if ( inputFeeder != null )
            {
                inputFeeder.setDone();
            }
            outputPumper.setDone();
            errorPumper.setDone();
    
            //processes.remove( new Long( cl.getPid() ) );
    
            return returnValue;
        }
        catch ( InterruptedException ex )
        {
            //killProcess( cl.getPid() );
            throw new CommandLineException( "Error while executing external command, process killed.", ex );
        } 
        finally
        {
            try
            {
                errorPumper.closeInput();
            } 
            catch ( IOException e )
            {
                //ignore
            }
            try
            {
                outputPumper.closeInput();
            } 
            catch ( IOException e )
            {
                //ignore
            }
            if ( inputFeeder != null )
            {
                try
                {
                    inputFeeder.closeOutput();
                }
                catch ( IOException e )
                {
                    //ignore
                }
            }
        }
    }


}
