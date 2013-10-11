package org.apache.maven.wagon.providers.dummy;

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
import java.util.List;

import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.SessionListener;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.proxy.ProxyInfoProvider;
import org.apache.maven.wagon.repository.Repository;

/**
 * DummyWagonProvider which does absolutely nothing
 * 
 * @author Robert Scholte
 * 
 * @plexus.component role="org.apache.maven.wagon.Wagon" role-hint="dummy" instantiation-strategy="per-lookup"
 */
public class DummyWagonProvider implements Wagon
{

    public void get( String resourceName, File destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
    }

    public boolean getIfNewer( String resourceName, File destination, long timestamp )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        return false;
    }

    public void put( File source, String destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
    }

    public void putDirectory( File sourceDirectory, String destinationDirectory )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
    }

    public boolean resourceExists( String resourceName )
        throws TransferFailedException, AuthorizationException
    {
        return false;
    }

    public List<String> getFileList( String destinationDirectory )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        return null;
    }

    public boolean supportsDirectoryCopy()
    {
        return false;
    }

    public Repository getRepository()
    {
        return null;
    }

    public void connect( Repository source )
        throws ConnectionException, AuthenticationException
    {
    }

    public void connect( Repository source, ProxyInfo proxyInfo )
        throws ConnectionException, AuthenticationException
    {
    }

    public void connect( Repository source, ProxyInfoProvider proxyInfoProvider )
        throws ConnectionException, AuthenticationException
    {
    }

    public void connect( Repository source, AuthenticationInfo authenticationInfo )
        throws ConnectionException, AuthenticationException
    {
    }

    public void connect( Repository source, AuthenticationInfo authenticationInfo, ProxyInfo proxyInfo )
        throws ConnectionException, AuthenticationException
    {
    }

    public void connect( Repository source, AuthenticationInfo authenticationInfo, ProxyInfoProvider proxyInfoProvider )
        throws ConnectionException, AuthenticationException
    {
    }

    public void openConnection()
        throws ConnectionException, AuthenticationException
    {
    }

    public void disconnect()
        throws ConnectionException
    {
    }

    public void setTimeout( int timeoutValue )
    {
    }

    public int getTimeout()
    {
        return 0;
    }

    public void setReadTimeout( int timeoutValue )
    {
    }

    public int getReadTimeout()
    {
        return 0;
    }

    public void addSessionListener( SessionListener listener )
    {
    }

    public void removeSessionListener( SessionListener listener )
    {
    }

    public boolean hasSessionListener( SessionListener listener )
    {
        return false;
    }

    public void addTransferListener( TransferListener listener )
    {
    }

    public void removeTransferListener( TransferListener listener )
    {
    }

    public boolean hasTransferListener( TransferListener listener )
    {
        return false;
    }

    public boolean isInteractive()
    {
        return false;
    }

    public void setInteractive( boolean interactive )
    {
    }
}