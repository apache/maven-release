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

import static org.mockito.Matchers.endsWith;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;

import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Writer;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.cli.Arg;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.Commandline;
import org.mockito.ArgumentCaptor;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;

/**
 * Test the forked Maven executor.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ForkedMavenExecutorTest
    extends PlexusTestCase
{
    private ForkedMavenExecutor executor;
    
    private SecDispatcher secDispatcher;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        executor = (ForkedMavenExecutor) lookup( MavenExecutor.ROLE, "forked-path" );
        
        secDispatcher = (SecDispatcher) lookup( SecDispatcher.ROLE, "mng-4384" );
    }

    public void testExecution()
        throws Exception
    {
        // prepare
        File workingDirectory = getTestFile( "target/working-directory" );
        Process mockProcess = mock( Process.class );
        when( mockProcess.getInputStream() ).thenReturn( mock( InputStream.class ) );
        when( mockProcess.getErrorStream() ).thenReturn( mock( InputStream.class ) );
        when( mockProcess.getOutputStream() ).thenReturn( mock( OutputStream.class ) );
        when( mockProcess.waitFor() ).thenReturn( 0 );

        Commandline commandLineMock = mock( Commandline.class );
        when( commandLineMock.execute() ).thenReturn( mockProcess );

        Arg valueArgument = mock( Arg.class );
        when( commandLineMock.createArg() ).thenReturn( valueArgument );
        
        CommandLineFactory commandLineFactoryMock = mock( CommandLineFactory.class );
        when( commandLineFactoryMock.createCommandLine( isA( String.class ) /*"mvn"*/ ) ).thenReturn( commandLineMock );

        executor.setCommandLineFactory( commandLineFactoryMock );

        // execute
        executor.executeGoals( workingDirectory, "clean integration-test", false, null, new ReleaseResult() );

        // verify
        verify( mockProcess ).getInputStream();
        verify( mockProcess ).getErrorStream();
        verify( mockProcess ).getOutputStream();
        verify( mockProcess ).waitFor();
        verify( commandLineMock ).setWorkingDirectory( workingDirectory.getAbsolutePath() );
        verify( commandLineMock ).addEnvironment( "MAVEN_TERMINATE_CMD", "on" );
        verify( commandLineMock ).addEnvironment( eq( "M2_HOME" ), isNull( String.class ) );
        verify( commandLineMock ).execute();
        verify( commandLineMock, times( 4 ) ).createArg();
        verify( valueArgument ).setValue( "clean" );
        verify( valueArgument ).setValue( "integration-test" );
        verify( valueArgument ).setValue( "--no-plugin-updates" );
        verify( valueArgument ).setValue( "--batch-mode" );
        verify( commandLineFactoryMock ).createCommandLine( endsWith( "mvn" ) );
        
        verifyNoMoreInteractions( mockProcess, commandLineFactoryMock, commandLineMock, valueArgument );
    }

    public void testExecutionWithCustomPomFile()
        throws Exception
    {
        File workingDirectory = getTestFile( "target/working-directory" );
        Process mockProcess = mock( Process.class );
        when( mockProcess.getInputStream() ).thenReturn( mock( InputStream.class ) );
        when( mockProcess.getErrorStream() ).thenReturn( mock( InputStream.class ) );
        when( mockProcess.getOutputStream() ).thenReturn( mock( OutputStream.class ) );
        when( mockProcess.waitFor() ).thenReturn( 0 );
        
        Commandline commandLineMock = mock( Commandline.class );
        when( commandLineMock.execute() ).thenReturn( mockProcess );
        
        Arg argMock = mock( Arg.class );
        when( commandLineMock.createArg() ).thenReturn( argMock );
        
        CommandLineFactory commandLineFactoryMock = mock( CommandLineFactory.class );
        when( commandLineFactoryMock.createCommandLine( isA( String.class ) /* "mvn" */ ) ).thenReturn( commandLineMock );

        executor.setCommandLineFactory( commandLineFactoryMock );

        // execute
        executor.executeGoals( workingDirectory, "clean integration-test", false, null, "my-pom.xml",
                               new ReleaseResult() );
        // verify
        verify( mockProcess ).getInputStream();
        verify( mockProcess ).getErrorStream();
        verify( mockProcess ).getOutputStream();
        verify( mockProcess ).waitFor();
        verify( commandLineMock ).setWorkingDirectory( workingDirectory.getAbsolutePath() );
        verify( commandLineMock ).addEnvironment( "MAVEN_TERMINATE_CMD", "on" );
        verify( commandLineMock ).addEnvironment( eq( "M2_HOME" ), isNull( String.class ) );
        verify( commandLineMock ).execute();
        verify( commandLineMock, times( 6 ) ).createArg();
        verify( argMock ).setValue( "clean" );
        verify( argMock ).setValue( "integration-test" );
        verify( argMock ).setValue( "-f" );
        verify( argMock ).setValue( "my-pom.xml" );
        verify( argMock ).setValue( "--no-plugin-updates" );
        verify( argMock ).setValue( "--batch-mode" );
        verify( commandLineFactoryMock ).createCommandLine( endsWith( "mvn" ) );
        
        verifyNoMoreInteractions( mockProcess, commandLineMock, argMock, commandLineFactoryMock );
    }

    public void testExecutionWithArguments()
        throws Exception
    {
        File workingDirectory = getTestFile( "target/working-directory" );
        Process mockProcess = mock( Process.class );
        when( mockProcess.getInputStream() ).thenReturn( mock( InputStream.class ) );
        when( mockProcess.getErrorStream() ).thenReturn( mock( InputStream.class ) );
        when( mockProcess.getOutputStream() ).thenReturn( mock( OutputStream.class ) );
        when( mockProcess.waitFor() ).thenReturn( 0 );

        Commandline commandLineMock = mock( Commandline.class );
        when( commandLineMock.execute() ).thenReturn( mockProcess );
        
        Arg argMock = mock( Arg.class );
        when( commandLineMock.createArg() ).thenReturn( argMock );

        CommandLineFactory commandLineFactoryMock = mock( CommandLineFactory.class );
        when( commandLineFactoryMock.createCommandLine( endsWith( "mvn" ) ) ).thenReturn( commandLineMock );

        executor.setCommandLineFactory( commandLineFactoryMock );

        // execute
        String arguments = "-DperformRelease=true -Dmaven.test.skip=true";
        executor.executeGoals( workingDirectory, "clean integration-test", false, arguments, new ReleaseResult() );

        // verify
        verify( mockProcess ).getInputStream();
        verify( mockProcess ).getErrorStream();
        verify( mockProcess ).getOutputStream();
        verify( mockProcess ).waitFor();
        verify( commandLineMock ).setWorkingDirectory( workingDirectory.getAbsolutePath() );
        verify( commandLineMock ).addEnvironment( "MAVEN_TERMINATE_CMD", "on" );
        verify( commandLineMock ).addEnvironment( eq( "M2_HOME" ), isNull( String.class ) );
        verify( commandLineMock ).execute();
        verify( commandLineMock, times( 5 ) ).createArg();
        verify( argMock ).setValue( "clean" );
        verify( argMock ).setValue( "integration-test" );
        verify( argMock ).setValue( "--no-plugin-updates" );
        verify( argMock ).setValue( "--batch-mode" );
        verify( argMock ).setLine( "-DperformRelease=true -Dmaven.test.skip=true" );
        verify( commandLineFactoryMock ).createCommandLine( endsWith( "mvn" ) );
        
        verifyNoMoreInteractions( mockProcess, commandLineMock, argMock, commandLineFactoryMock );
    }

    public void testExecutionWithNonZeroExitCode()
        throws Exception
    {
        // prepare
        File workingDirectory = getTestFile( "target/working-directory" );
        Process mockProcess = mock( Process.class );
        when( mockProcess.getInputStream() ).thenReturn( mock( InputStream.class ) );
        when( mockProcess.getErrorStream() ).thenReturn( mock( InputStream.class ) );
        when( mockProcess.getOutputStream() ).thenReturn( mock( OutputStream.class ) );
        when( mockProcess.waitFor() ).thenReturn( 1 );
        when( mockProcess.exitValue() ).thenReturn( 1 ); // why was this here in the original test?

        Commandline commandLineMock = mock( Commandline.class );
        when( commandLineMock.execute() ).thenReturn( mockProcess );
        
        Arg argMock = mock( Arg.class );
        when( commandLineMock.createArg() ).thenReturn( argMock );
        
        CommandLineFactory commandLineFactoryMock = mock( CommandLineFactory.class );
        when( commandLineFactoryMock.createCommandLine( endsWith( "mvn" ) ) ).thenReturn( commandLineMock );

        executor.setCommandLineFactory( commandLineFactoryMock );

        // execute
        try
        {
            executor.executeGoals( workingDirectory, "clean integration-test", false, null, new ReleaseResult() );

            fail( "Should have thrown an exception" );
        }
        catch ( MavenExecutorException e )
        {
            assertEquals( "Check exit code", 1, e.getExitCode() );
        }
        
        // verify
        verify( mockProcess ).getInputStream();
        verify( mockProcess ).getErrorStream();
        verify( mockProcess ).getOutputStream();
        verify( mockProcess ).waitFor();
//        verify( mockProcess ).exitValue();
        verify( commandLineMock ).setWorkingDirectory( workingDirectory.getAbsolutePath() );
        verify( commandLineMock ).addEnvironment( "MAVEN_TERMINATE_CMD", "on" );
        verify( commandLineMock ).addEnvironment( eq( "M2_HOME" ), isNull( String.class ) );
        verify( commandLineMock ).execute();
        verify( commandLineMock, times( 4 ) ).createArg();
        verify( argMock ).setValue( "clean" );
        verify( argMock ).setValue( "integration-test" );
        verify( argMock ).setValue( "--no-plugin-updates" );
        verify( argMock ).setValue( "--batch-mode" );
        verify( commandLineFactoryMock ).createCommandLine( endsWith( "mvn" ) );
        
        verifyNoMoreInteractions( mockProcess, commandLineMock, argMock, commandLineFactoryMock );
    }

    public void testExecutionWithCommandLineException()
        throws Exception
    {
        // prepare
        File workingDirectory = getTestFile( "target/working-directory" );

        Commandline commandLineMock = mock( Commandline.class );
        when( commandLineMock.execute() ).thenThrow( new CommandLineException( "..." ) );

        Arg argMock = mock( Arg.class );
        when ( commandLineMock.createArg() ).thenReturn( argMock );
        
        CommandLineFactory commandLineFactoryMock = mock( CommandLineFactory.class );
        when( commandLineFactoryMock.createCommandLine( endsWith( "mvn" ) ) ).thenReturn( commandLineMock );
        
        executor.setCommandLineFactory( commandLineFactoryMock );

        // execute
        try
        {
            executor.executeGoals( workingDirectory, "clean integration-test", false, null, new ReleaseResult() );

            fail( "Should have thrown an exception" );
        }
        catch ( MavenExecutorException e )
        {
            assertEquals( "Check cause", CommandLineException.class, e.getCause().getClass() );
        }

        // verify
        verify( commandLineMock ).setWorkingDirectory( workingDirectory.getAbsolutePath() );
        verify( commandLineMock ).addEnvironment( "MAVEN_TERMINATE_CMD", "on" );
        verify( commandLineMock ).addEnvironment( eq( "M2_HOME" ), isNull( String.class ) );
        verify( commandLineMock ).execute();
        verify( commandLineMock, times( 4 ) ).createArg();
        verify( argMock ).setValue( "clean" );
        verify( argMock ).setValue( "integration-test" );
        verify( argMock ).setValue( "--no-plugin-updates" );
        verify( argMock ).setValue( "--batch-mode" );
        verify( commandLineFactoryMock ).createCommandLine( endsWith( "mvn" ) );
        
        verifyNoMoreInteractions( commandLineMock, argMock, commandLineFactoryMock );
    }
    
    public void testEncryptSettings()
        throws Exception
    {
        // prepare
        File workingDirectory = getTestFile( "target/working-directory" );
        Process mockProcess = mock( Process.class );
        when( mockProcess.getInputStream() ).thenReturn( mock( InputStream.class ) );
        when( mockProcess.getErrorStream() ).thenReturn( mock( InputStream.class ) );
        when( mockProcess.getOutputStream() ).thenReturn( mock( OutputStream.class ) );
        when( mockProcess.waitFor() ).thenReturn( 0 );

        Commandline commandLineMock = mock( Commandline.class );
        when( commandLineMock.execute() ).thenReturn( mockProcess );

        Arg valueArgument = mock( Arg.class );
        when( commandLineMock.createArg() ).thenReturn( valueArgument );

        CommandLineFactory commandLineFactoryMock = mock( CommandLineFactory.class );
        when( commandLineFactoryMock.createCommandLine( isA( String.class ) /* "mvn" */) ).thenReturn( commandLineMock );

        executor.setCommandLineFactory( commandLineFactoryMock );

        Settings settings = new Settings();
        Server server = new Server();
        server.setPassphrase( "server_passphrase" );
        server.setPassword( "server_password" );
        settings.addServer( server );
        Proxy proxy = new Proxy();
        proxy.setPassword( "proxy_password" );
        settings.addProxy( proxy );

        ReleaseEnvironment releaseEnvironment = new DefaultReleaseEnvironment();
        releaseEnvironment.setSettings( settings );

        AbstractMavenExecutor executorSpy = spy( executor );
        SettingsXpp3Writer settingsWriter = mock( SettingsXpp3Writer.class );

        ArgumentCaptor<Settings> encryptedSettings = ArgumentCaptor.forClass( Settings.class );

        when( executorSpy.getSettingsWriter() ).thenReturn( settingsWriter );

        executorSpy.executeGoals( workingDirectory, "validate", releaseEnvironment, false, null, new ReleaseResult() );

        verify( settingsWriter ).write( isA( Writer.class ), encryptedSettings.capture() );

        assertNotSame( settings, encryptedSettings.getValue() );

        Server encryptedServer = encryptedSettings.getValue().getServers().get( 0 );
        assertEquals( "server_passphrase", secDispatcher.decrypt( encryptedServer.getPassphrase() ) );
        assertEquals( "server_password", secDispatcher.decrypt( encryptedServer.getPassword() ) );

        Proxy encryptedProxy = encryptedSettings.getValue().getProxies().get( 0 );
        assertEquals( "proxy_password", secDispatcher.decrypt( encryptedProxy.getPassword() ) );

        File settingsSecurity = new File( System.getProperty( "user.home" ), ".m2/settings-security.xml" );
        if ( settingsSecurity.exists() )
        {
            assertFalse( "server_passphrase".equals( encryptedServer.getPassphrase() ) );
            assertFalse( "server_password".equals( encryptedServer.getPassword() ) );
            assertFalse( "proxy_password".equals( encryptedProxy.getPassword() ) );
        }
    }
}