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

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.Writer;

import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Writer;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.logging.Logger;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;

public class InvokerMavenExecutorTest
    extends PlexusTestCase
{

    private InvokerMavenExecutor executor;

    private SecDispatcher secDispatcher;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        executor = (InvokerMavenExecutor) lookup( MavenExecutor.ROLE, "invoker" );

        secDispatcher = (SecDispatcher) lookup( SecDispatcher.ROLE, "mng-4384" );
    }

    @Test
    public void testThreads()
        throws Exception
    {
        Logger logger = mock( Logger.class );
        executor.enableLogging( logger );

        InvocationRequest req = new DefaultInvocationRequest();
        executor.setupRequest( req, null, "-T 3" );
        assertEquals( "3", req.getThreads() );

        req = new DefaultInvocationRequest();
        executor.setupRequest( req, null, "-T4" );
        assertEquals( "4", req.getThreads() );

        req = new DefaultInvocationRequest();
        executor.setupRequest( req, null, "\"-T5\"" );
        assertEquals( "5", req.getThreads() );
    }

    @Test
    public void testGlobalSettings()
        throws Exception
    {
        Logger logger = mock( Logger.class );
        executor.enableLogging( logger );

        InvocationRequest req = new DefaultInvocationRequest();
        executor.setupRequest( req, null, "-gs custom-settings.xml" );
        assertEquals( "custom-settings.xml", req.getGlobalSettingsFile().getPath() );

        req = new DefaultInvocationRequest();
        executor.setupRequest( req, null, "--global-settings other-settings.xml" );
        assertEquals( "other-settings.xml", req.getGlobalSettingsFile().getPath() );
    }

    public void testEncryptSettings()
        throws Exception
    {
        // prepare
        File workingDirectory = getTestFile( "target/working-directory" );
        workingDirectory.mkdirs();
        
        
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

        InvokerMavenExecutor executorSpy = spy( executor );
        SettingsXpp3Writer settingsWriter = mock( SettingsXpp3Writer.class );

        ArgumentCaptor<Settings> encryptedSettings = ArgumentCaptor.forClass( Settings.class );

        when( executorSpy.getSettingsWriter() ).thenReturn( settingsWriter );
        when( executorSpy.getOutputHandler() ).thenReturn( null );
        when( executorSpy.getInvokerLogger() ).thenReturn( null );

        try
        {
            executorSpy.executeGoals( workingDirectory, "validate", releaseEnvironment, false, null, new ReleaseResult() );
        }
        catch ( MavenExecutorException e )
        {
        }

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
