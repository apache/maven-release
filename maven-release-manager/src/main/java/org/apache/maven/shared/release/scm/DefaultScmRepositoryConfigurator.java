package org.apache.maven.shared.release.scm;

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

import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.provider.ScmProviderRepository;
import org.apache.maven.scm.provider.ScmProviderRepositoryWithHost;
import org.apache.maven.scm.provider.svn.repository.SvnScmProviderRepository;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

/**
 * Tool that gets a configured SCM repository from release configuration.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @plexus.component role="org.apache.maven.shared.release.scm.ScmRepositoryConfigurator"
 */
public class DefaultScmRepositoryConfigurator
    extends AbstractLogEnabled
    implements ScmRepositoryConfigurator
{
    /**
     * The SCM manager.
     *
     * @plexus.requirement
     */
    private ScmManager scmManager;

    /**
     * When this plugin requires Maven 3.0 as minimum, this component can be removed and o.a.m.s.c.SettingsDecrypter be
     * used instead.
     * 
     * @plexus.requirement role-hint="mng-4384"
     */
    private SecDispatcher secDispatcher;

    public ScmRepository getConfiguredRepository( ReleaseDescriptor releaseDescriptor, Settings settings )
        throws ScmRepositoryException, NoSuchScmProviderException
    {
        String url = releaseDescriptor.getScmSourceUrl();
        return getConfiguredRepository( url, releaseDescriptor, settings );
    }

    public ScmRepository getConfiguredRepository( String url, ReleaseDescriptor releaseDescriptor, Settings settings )
        throws ScmRepositoryException, NoSuchScmProviderException
    {
        String username = releaseDescriptor.getScmUsername();
        String password = releaseDescriptor.getScmPassword();
        String privateKey = releaseDescriptor.getScmPrivateKey();
        String passphrase = releaseDescriptor.getScmPrivateKeyPassPhrase();

        ScmRepository repository = scmManager.makeScmRepository( url );

        ScmProviderRepository scmRepo = repository.getProviderRepository();

        //MRELEASE-76
        scmRepo.setPersistCheckout( false );

        if ( settings != null )
        {
            Server server = null;

            if ( releaseDescriptor.getScmId() != null )
            {
                server = settings.getServer( releaseDescriptor.getScmId() );
            }

            if ( server == null && repository.getProviderRepository() instanceof ScmProviderRepositoryWithHost )
            {
                ScmProviderRepositoryWithHost repositoryWithHost =
                    (ScmProviderRepositoryWithHost) repository.getProviderRepository();
                String host = repositoryWithHost.getHost();

                int port = repositoryWithHost.getPort();

                if ( port > 0 )
                {
                    host += ":" + port;
                }

                // TODO: this is a bit dodgy - id is not host, but since we don't have a <host> field we make an assumption
                server = settings.getServer( host );
            }

            if ( server != null )
            {
                if ( username == null )
                {
                    username = server.getUsername();
                }

                if ( password == null )
                {
                    password = decrypt( server.getPassword(), server.getId() );
                }

                if ( privateKey == null )
                {
                    privateKey = server.getPrivateKey();
                }

                if ( passphrase == null )
                {
                    passphrase = decrypt( server.getPassphrase(), server.getId() );
                }
            }
        }

        if ( !StringUtils.isEmpty( username ) )
        {
            scmRepo.setUser( username );
        }
        if ( !StringUtils.isEmpty( password ) )
        {
            scmRepo.setPassword( password );
        }

        if ( scmRepo instanceof ScmProviderRepositoryWithHost )
        {
            ScmProviderRepositoryWithHost repositoryWithHost = (ScmProviderRepositoryWithHost) scmRepo;
            if ( !StringUtils.isEmpty( privateKey ) )
            {
                repositoryWithHost.setPrivateKey( privateKey );
            }

            if ( !StringUtils.isEmpty( passphrase ) )
            {
                repositoryWithHost.setPassphrase( passphrase );
            }
        }

        if ( "svn".equals( repository.getProvider() ) )
        {
            SvnScmProviderRepository svnRepo = (SvnScmProviderRepository) repository.getProviderRepository();

            String tagBase = releaseDescriptor.getScmTagBase();
            if ( !StringUtils.isEmpty( tagBase ) )
            {
                svnRepo.setTagBase( tagBase );
            }

            String branchBase = releaseDescriptor.getScmBranchBase();
            if ( !StringUtils.isEmpty( branchBase ) )
            {
                svnRepo.setBranchBase( branchBase );
            }
        }

        return repository;
    }

    private String decrypt( String str, String server )
    {
        try
        {
            return secDispatcher.decrypt( str );
        }
        catch ( SecDispatcherException e )
        {
            String msg =
                "Failed to decrypt password/passphrase for server " + server + ", using auth token as is: "
                    + e.getMessage();
            if ( getLogger().isDebugEnabled() )
            {
                getLogger().warn( msg, e );
            }
            else
            {
                getLogger().warn( msg );
            }
            return str;
        }
    }

    public ScmProvider getRepositoryProvider( ScmRepository repository )
        throws NoSuchScmProviderException
    {
        return scmManager.getProviderByRepository( repository );
    }

    public void setScmManager( ScmManager scmManager )
    {
        this.scmManager = scmManager;
    }
}
