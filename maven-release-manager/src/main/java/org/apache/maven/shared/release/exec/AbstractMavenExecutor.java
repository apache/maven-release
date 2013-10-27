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
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.SettingsUtils;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Writer;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;
import org.sonatype.plexus.components.sec.dispatcher.SecUtil;
import org.sonatype.plexus.components.sec.dispatcher.model.SettingsSecurity;

public abstract class AbstractMavenExecutor
    implements MavenExecutor, LogEnabled
{

    private Logger logger;

    /**
     * When this plugin requires Maven 3.0 as minimum, this component can be removed and o.a.m.s.c.SettingsDecrypter be
     * used instead.
     * 
     * @plexus.requirement role="org.sonatype.plexus.components.sec.dispatcher.SecDispatcher" role-hint="mng-4384"
     */
    private DefaultSecDispatcher secDispatcher;

    /**
     * @plexus.requirement
     */
    private PlexusCipher cipher;
    
    protected AbstractMavenExecutor()
    {
    }

    /** {@inheritDoc} */
    public void executeGoals( File workingDirectory, String goals, boolean interactive, String additionalArguments,
                              String pomFileName, ReleaseResult result )
        throws MavenExecutorException
    {
        executeGoals( workingDirectory, goals, new DefaultReleaseEnvironment(), interactive, additionalArguments, pomFileName, result );
    }

    /** {@inheritDoc} */
    public void executeGoals( File workingDirectory, String goals, boolean interactive, String additionalArguments,
                              ReleaseResult result )
        throws MavenExecutorException
    {
        executeGoals( workingDirectory, goals, new DefaultReleaseEnvironment(), interactive, additionalArguments, result );
    }

    /** {@inheritDoc} */
    public void executeGoals( File workingDirectory, String goals, ReleaseEnvironment releaseEnvironment,
                              boolean interactive, String arguments, ReleaseResult result )
        throws MavenExecutorException
    {
        executeGoals( workingDirectory, goals, releaseEnvironment, interactive, arguments, null, result );
    }

    /** {@inheritDoc} */
    public void executeGoals( File workingDirectory, String goals, ReleaseEnvironment releaseEnvironment,
                              boolean interactive, String additionalArguments, String pomFileName, ReleaseResult result )
        throws MavenExecutorException
    {
        List<String> goalsList = new ArrayList<String>();
        if ( goals != null )
        {
            // accept both space and comma, so the old way still work
            // also accept line separators, so that goal lists can be spread
            // across multiple lines in the POM.
            String[] tokens = StringUtils.split( goals, ", \n\r\t" );

            for ( int i = 0; i < tokens.length; ++i )
            {
                goalsList.add( tokens[i] );
            }
        }
        executeGoals( workingDirectory, goalsList, releaseEnvironment, interactive, additionalArguments, pomFileName, result );
    }

    protected abstract void executeGoals( File workingDirectory, List<String> goals, ReleaseEnvironment releaseEnvironment,
                              boolean interactive, String additionalArguments, String pomFileName, ReleaseResult result )
        throws MavenExecutorException;


    protected final Logger getLogger()
    {
        return logger;
    }

    /** {@inheritDoc} */
    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }

    
    protected Settings encryptSettings( Settings settings )
    {
        Settings encryptedSettings = SettingsUtils.copySettings( settings );
        
        for ( Server server : encryptedSettings.getServers() )
        {
            String password = server.getPassword(); 
            if ( password != null && !isEncryptedString( password ) )
            {
                try
                {
                    server.setPassword( encryptAndDecorate( password ) );
                }
                catch ( IllegalStateException e )
                {
                    // ignore
                }
                catch ( SecDispatcherException e )
                {
                    // ignore
                }
                catch ( PlexusCipherException e )
                {
                    // ignore
                }
            }

            String passphrase = server.getPassphrase(); 
            if ( passphrase != null && !isEncryptedString( passphrase ) )
            {
                try
                {
                    server.setPassphrase( encryptAndDecorate( passphrase ) );
                }
                catch ( IllegalStateException e )
                {
                    // ignore
                }
                catch ( SecDispatcherException e )
                {
                    // ignore
                }
                catch ( PlexusCipherException e )
                {
                    // ignore
                }
            }
        }
        
        for ( Proxy proxy : encryptedSettings.getProxies() )
        {
            String password = proxy.getPassword();
            if ( password != null && !isEncryptedString( password ) )
            {
                try
                {
                    proxy.setPassword( encryptAndDecorate( password ) );
                }
                catch ( IllegalStateException e )
                {
                    // ignore
                }
                catch ( SecDispatcherException e )
                {
                    // ignore
                }
                catch ( PlexusCipherException e )
                {
                    // ignore
                }
            }
        }
        
        return encryptedSettings;
    }
    
    // From org.apache.maven.cli.MavenCli.encryption(CliRequest)
    private final String encryptAndDecorate( String passwd ) throws IllegalStateException, SecDispatcherException, PlexusCipherException
    {
        String configurationFile = secDispatcher.getConfigurationFile();

        if ( configurationFile.startsWith( "~" ) )
        {
            configurationFile = System.getProperty( "user.home" ) + configurationFile.substring( 1 );
        }

        String file = System.getProperty( DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION, configurationFile );

        String master = null;

        SettingsSecurity sec = SecUtil.read( file, true );
        if ( sec != null )
        {
            master = sec.getMaster();
        }

        if ( master == null )
        {
            throw new IllegalStateException( "Master password is not set in the setting security file: " + file );
        }

        DefaultPlexusCipher cipher = new DefaultPlexusCipher();
        String masterPasswd = cipher.decryptDecorated( master, DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION );
        return cipher.encryptAndDecorate( passwd, masterPasswd );
    }
    
    private boolean isEncryptedString( String str )
    {
        return cipher.isEncryptedString( str );
    }

    protected SettingsXpp3Writer getSettingsWriter()
    {
        return new SettingsXpp3Writer();
    }
}
