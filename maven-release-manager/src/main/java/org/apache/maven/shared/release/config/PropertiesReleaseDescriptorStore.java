package org.apache.maven.shared.release.config;

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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.model.Scm;
import org.apache.maven.shared.release.scm.IdentifiedScm;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.IOUtil;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;
import org.sonatype.plexus.components.sec.dispatcher.SecUtil;
import org.sonatype.plexus.components.sec.dispatcher.model.SettingsSecurity;

/**
 * Read and write release configuration and state from a properties file.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @plexus.component role="org.apache.maven.shared.release.config.ReleaseDescriptorStore" role-hint="properties"
 */
public class PropertiesReleaseDescriptorStore
    extends AbstractLogEnabled
    implements ReleaseDescriptorStore
{
    
    /**
     * When this plugin requires Maven 3.0 as minimum, this component can be removed and o.a.m.s.c.SettingsDecrypter be
     * used instead.
     * 
     * @plexus.requirement role="org.sonatype.plexus.components.sec.dispatcher.SecDispatcher" role-hint="mng-4384"
     */
    
    private DefaultSecDispatcher secDispatcher;
    
    public ReleaseDescriptor read( ReleaseDescriptor mergeDescriptor )
        throws ReleaseDescriptorStoreException
    {
        return read( mergeDescriptor, getDefaultReleasePropertiesFile( mergeDescriptor ) );
    }

    public ReleaseDescriptor read( File file )
        throws ReleaseDescriptorStoreException
    {
        return read( null, file );
    }

    public ReleaseDescriptor read( ReleaseDescriptor mergeDescriptor, File file )
        throws ReleaseDescriptorStoreException
    {
        Properties properties = new Properties();

        InputStream inStream = null;
        try
        {
            inStream = new FileInputStream( file );

            properties.load( inStream );
        }
        catch ( FileNotFoundException e )
        {
            getLogger().debug( file.getName() + " not found - using empty properties" );
        }
        catch ( IOException e )
        {
            throw new ReleaseDescriptorStoreException(
                "Error reading properties file '" + file.getName() + "': " + e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( inStream );
        }

        ReleaseDescriptor releaseDescriptor = ReleaseUtils.copyPropertiesToReleaseDescriptor( properties );

        if ( mergeDescriptor != null )
        {
            releaseDescriptor = ReleaseUtils.merge( releaseDescriptor, mergeDescriptor );
        }

        return releaseDescriptor;
    }

    public void write( ReleaseDescriptor config )
        throws ReleaseDescriptorStoreException
    {
        write( config, getDefaultReleasePropertiesFile( config ) );
    }

    public void delete( ReleaseDescriptor config )
    {
        File file = getDefaultReleasePropertiesFile( config );
        if ( file.exists() )
        {
            file.delete();
        }
    }

    public void write( ReleaseDescriptor config, File file )
        throws ReleaseDescriptorStoreException
    {
        Properties properties = new Properties();
        properties.setProperty( "completedPhase", config.getCompletedPhase() );
        if ( config.isCommitByProject() ) //default is false
        {
            properties.setProperty( "commitByProject", "true" );
        }
        properties.setProperty( "scm.url", config.getScmSourceUrl() );
        if ( config.getScmId() != null )
        {
            properties.setProperty( "scm.id", config.getScmId() );
        }
        if ( config.getScmUsername() != null )
        {
            properties.setProperty( "scm.username", config.getScmUsername() );
        }
        if ( config.getScmPassword() != null )
        {
            String password = config.getScmPassword();
            try
            {
                password = encryptAndDecorate( password );
            }
            catch ( IllegalStateException e )
            {
                getLogger().debug( e.getMessage() );
            }
            catch ( SecDispatcherException e )
            {
                getLogger().debug( e.getMessage() );
            }
            catch ( PlexusCipherException e )
            {
                getLogger().debug( e.getMessage() );
            }
            properties.setProperty( "scm.password", password );
        }
        if ( config.getScmPrivateKey() != null )
        {
            properties.setProperty( "scm.privateKey", config.getScmPrivateKey() );
        }
        if ( config.getScmPrivateKeyPassPhrase() != null )
        {
            String passPhrase = config.getScmPrivateKeyPassPhrase();
            try
            {
                passPhrase = encryptAndDecorate( passPhrase );
            }
            catch ( IllegalStateException e )
            {
                getLogger().debug( e.getMessage() );
            }
            catch ( SecDispatcherException e )
            {
                getLogger().debug( e.getMessage() );
            }
            catch ( PlexusCipherException e )
            {
                getLogger().debug( e.getMessage() );
            }
            properties.setProperty( "scm.passphrase", passPhrase  );
        }
        if ( config.getScmTagBase() != null )
        {
            properties.setProperty( "scm.tagBase", config.getScmTagBase() );
        }
        if ( config.getScmBranchBase() != null )
        {
            properties.setProperty( "scm.branchBase", config.getScmBranchBase() );
        }
        if ( config.getScmReleaseLabel() != null )
        {
            properties.setProperty( "scm.tag", config.getScmReleaseLabel() );
        }
        if ( config.getScmTagNameFormat() != null )
        {
            properties.setProperty( "scm.tagNameFormat", config.getScmTagNameFormat() );
        }
        if ( config.getScmCommentPrefix() != null )
        {
            properties.setProperty( "scm.commentPrefix", config.getScmCommentPrefix() );
        }
        if ( config.getAdditionalArguments() != null )
        {
            properties.setProperty( "exec.additionalArguments", config.getAdditionalArguments() );
        }
        if ( config.getPomFileName() != null )
        {
            properties.setProperty( "exec.pomFileName", config.getPomFileName() );
        }
        if ( config.getPreparationGoals() != null )
        {
            properties.setProperty( "preparationGoals", config.getPreparationGoals() );
        }
        if ( config.getCompletionGoals() != null )
        {
            properties.setProperty( "completionGoals", config.getCompletionGoals() );
        }

        properties.setProperty( "exec.snapshotReleasePluginAllowed",
                                Boolean.toString( config.isSnapshotReleasePluginAllowed() ) );

        properties.setProperty( "remoteTagging", Boolean.toString( config.isRemoteTagging() ) );

        properties.setProperty( "pushChanges", Boolean.toString( config.isPushChanges() ) );

        // others boolean properties are not written to the properties file because the value from the caller is always
        // used

        for ( Iterator<?> i = config.getReleaseVersions().entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) i.next();
            properties.setProperty( "project.rel." + entry.getKey(), (String) entry.getValue() );
        }

        for ( Iterator<?> i = config.getDevelopmentVersions().entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) i.next();
            properties.setProperty( "project.dev." + entry.getKey(), (String) entry.getValue() );
        }

        for ( Iterator<?> i = config.getOriginalScmInfo().entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) i.next();
            Scm scm = (Scm) entry.getValue();
            String prefix = "project.scm." + entry.getKey();
            if ( scm != null )
            {
                if ( scm.getConnection() != null )
                {
                    properties.setProperty( prefix + ".connection", scm.getConnection() );
                }
                if ( scm.getDeveloperConnection() != null )
                {
                    properties.setProperty( prefix + ".developerConnection", scm.getDeveloperConnection() );
                }
                if ( scm.getUrl() != null )
                {
                    properties.setProperty( prefix + ".url", scm.getUrl() );
                }
                if ( scm.getTag() != null )
                {
                    properties.setProperty( prefix + ".tag", scm.getTag() );
                }
                if ( scm instanceof IdentifiedScm )
                {
                    IdentifiedScm identifiedScm = (IdentifiedScm) scm;
                    if ( identifiedScm.getId() != null )
                    {
                        properties.setProperty( prefix + ".id", identifiedScm.getId() );
                    }
                }
            }
            else
            {
                properties.setProperty( prefix + ".empty", "true" );
            }
        }

        if ( ( config.getResolvedSnapshotDependencies() != null )
            && ( config.getResolvedSnapshotDependencies().size() > 0 ) )
        {
            processResolvedDependencies( properties, config.getResolvedSnapshotDependencies() );
        }

        OutputStream outStream = null;
        //noinspection OverlyBroadCatchBlock
        try
        {
            outStream = new FileOutputStream( file );

            properties.store( outStream, "release configuration" );
        }
        catch ( IOException e )
        {
            throw new ReleaseDescriptorStoreException(
                "Error writing properties file '" + file.getName() + "': " + e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( outStream );
        }

    }

    private void processResolvedDependencies( Properties prop, Map<?, ?> resolvedDependencies )
    {
        Set<?> entries = resolvedDependencies.entrySet();
        Iterator<?> iterator = entries.iterator();
        Entry<?, ?> currentEntry;

        while ( iterator.hasNext() )
        {
            currentEntry = (Entry<?, ?>) iterator.next();

            Map<?, ?> versionMap = (Map<?, ?>) currentEntry.getValue();

            prop.setProperty( "dependency." + currentEntry.getKey() + ".release",
                              (String) versionMap.get( ReleaseDescriptor.RELEASE_KEY ) );
            prop.setProperty( "dependency." + currentEntry.getKey() + ".development",
                              (String) versionMap.get( ReleaseDescriptor.DEVELOPMENT_KEY ) );
        }
    }

    private static File getDefaultReleasePropertiesFile( ReleaseDescriptor mergeDescriptor )
    {
        return new File( mergeDescriptor.getWorkingDirectory(), "release.properties" );
    }
    
    // From org.apache.maven.cli.MavenCli.encryption(CliRequest)
    private String encryptAndDecorate( String passwd ) throws IllegalStateException, SecDispatcherException, PlexusCipherException
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

}
