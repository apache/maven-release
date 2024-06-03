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
package org.apache.maven.shared.release.scm;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.concurrent.atomic.AtomicReference;

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
import org.apache.maven.shared.release.util.MavenCrypto;
import org.apache.maven.shared.release.util.MavenCrypto.MavenCryptoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

/**
 * Tool that gets a configured SCM repository from release configuration.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
@Singleton
@Named
public class DefaultScmRepositoryConfigurator implements ScmRepositoryConfigurator {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final AtomicReference<ScmManager> scmManager;

    private final MavenCrypto mavenCrypto;

    @Inject
    public DefaultScmRepositoryConfigurator(ScmManager scmManager, MavenCrypto mavenCrypto) {
        this.scmManager = new AtomicReference<>(requireNonNull(scmManager));
        this.mavenCrypto = requireNonNull(mavenCrypto);
    }

    /**
     * For testing purposes only!
     */
    public void setScmManager(ScmManager scmManager) {
        this.scmManager.set(scmManager);
    }

    @Override
    public ScmRepository getConfiguredRepository(ReleaseDescriptor releaseDescriptor, Settings settings)
            throws ScmRepositoryException, NoSuchScmProviderException {
        String url = releaseDescriptor.getScmSourceUrl();
        return getConfiguredRepository(url, releaseDescriptor, settings);
    }

    @Override
    public ScmRepository getConfiguredRepository(String url, ReleaseDescriptor releaseDescriptor, Settings settings)
            throws ScmRepositoryException, NoSuchScmProviderException {
        String username = releaseDescriptor.getScmUsername();
        String password = releaseDescriptor.getScmPassword();
        String privateKey = releaseDescriptor.getScmPrivateKey();
        String passphrase = releaseDescriptor.getScmPrivateKeyPassPhrase();

        ScmRepository repository = scmManager.get().makeScmRepository(url);

        ScmProviderRepository scmRepo = repository.getProviderRepository();

        // MRELEASE-76
        scmRepo.setPersistCheckout(false);

        if (settings != null) {
            Server server = null;

            if (releaseDescriptor.getScmId() != null) {
                server = settings.getServer(releaseDescriptor.getScmId());
                if (server == null) {
                    logger.warn("No server with id '{}' found in Maven settings", releaseDescriptor.getScmId());
                }
            }

            if (server == null && repository.getProviderRepository() instanceof ScmProviderRepositoryWithHost) {
                ScmProviderRepositoryWithHost repositoryWithHost =
                        (ScmProviderRepositoryWithHost) repository.getProviderRepository();
                String host = repositoryWithHost.getHost();

                int port = repositoryWithHost.getPort();

                if (port > 0) {
                    host += ":" + port;
                }

                // TODO: this is a bit dodgy - id is not host, but since we don't have a <host> field we make an
                // assumption
                server = settings.getServer(host);
            }

            if (server != null) {
                if (username == null && server.getUsername() != null) {
                    logger.debug(
                            "Using username from server id '{}' found in Maven settings", releaseDescriptor.getScmId());
                    username = server.getUsername();
                }

                if (password == null && server.getPassword() != null) {
                    logger.debug(
                            "Using password from server id '{}' found in Maven settings", releaseDescriptor.getScmId());
                    password = decrypt(server.getPassword(), server.getId());
                }

                if (privateKey == null && server.getPrivateKey() != null) {
                    logger.debug(
                            "Using private key from server id '{}' found in Maven settings",
                            releaseDescriptor.getScmId());
                    privateKey = server.getPrivateKey();
                }

                if (passphrase == null && server.getPassphrase() != null) {
                    logger.debug(
                            "Using passphrase from server id '{}' found in Maven settings",
                            releaseDescriptor.getScmId());
                    passphrase = decrypt(server.getPassphrase(), server.getId());
                }
            }
        }

        if (!(username == null || username.isEmpty())) {
            scmRepo.setUser(username);
        } else {
            logger.debug("No explicit username configured");
        }
        if (!(password == null || password.isEmpty())) {
            scmRepo.setPassword(password);
        } else {
            logger.debug("No explicit password configured");
        }

        if (scmRepo instanceof ScmProviderRepositoryWithHost) {
            ScmProviderRepositoryWithHost repositoryWithHost = (ScmProviderRepositoryWithHost) scmRepo;
            if (!(privateKey == null || privateKey.isEmpty())) {
                repositoryWithHost.setPrivateKey(privateKey);
            } else {
                logger.debug("No explicit private key configured");
            }

            if (!(passphrase == null || passphrase.isEmpty())) {
                repositoryWithHost.setPassphrase(passphrase);
            } else {
                logger.debug("No explicit passphrase configured");
            }
        }

        if ("svn".equals(repository.getProvider())) {
            SvnScmProviderRepository svnRepo = (SvnScmProviderRepository) repository.getProviderRepository();

            String tagBase = releaseDescriptor.getScmTagBase();
            if (!(tagBase == null || tagBase.isEmpty())) {
                svnRepo.setTagBase(tagBase);
            }

            String branchBase = releaseDescriptor.getScmBranchBase();
            if (!(branchBase == null || branchBase.isEmpty())) {
                svnRepo.setBranchBase(branchBase);
            }
        }
        if (!releaseDescriptor.isInteractive()) {
            scmManager.get().getProviderByRepository(repository).setInteractive(releaseDescriptor.isInteractive());
        }
        return repository;
    }

    private String decrypt(String str, String serverId) {
        try {
            return mavenCrypto.decrypt(str);
        } catch (MavenCryptoException e) {
            String msg = "Failed to decrypt password/passphrase for server with id '" + serverId
                    + "', using auth token as is: " + e.getMessage();
            if (logger.isDebugEnabled()) {
                logger.warn(msg, e);
            } else {
                logger.warn(msg);
            }
            return str;
        }
    }

    @Override
    public ScmProvider getRepositoryProvider(ScmRepository repository) throws NoSuchScmProviderException {
        return scmManager.get().getProviderByRepository(repository);
    }
}
