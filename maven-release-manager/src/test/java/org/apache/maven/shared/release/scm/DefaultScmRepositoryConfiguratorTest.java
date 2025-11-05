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

import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.provider.ScmProviderRepositoryWithHost;
import org.apache.maven.scm.provider.svn.repository.SvnScmProviderRepository;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.apache.maven.shared.release.config.ReleaseUtils;
import org.codehaus.plexus.testing.PlexusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test the default SCM repository configurator.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
@PlexusTest
class DefaultScmRepositoryConfiguratorTest {
    @Inject
    private ScmRepositoryConfigurator scmRepositoryConfigurator;

    @Test
    void testGetConfiguredRepository() throws ScmRepositoryException, NoSuchScmProviderException {
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder();

        ScmRepository repository =
                scmRepositoryConfigurator.getConfiguredRepository(ReleaseUtils.buildReleaseDescriptor(builder), null);

        assertEquals("svn", repository.getProvider(), "check provider");
        assertNull(repository.getProviderRepository().getUser(), "check username");
        assertNull(repository.getProviderRepository().getPassword(), "check password");
    }

    @Test
    void testGetConfiguredRepositoryWithUsernameAndPassword()
            throws ScmRepositoryException, NoSuchScmProviderException {
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder("username", "password");

        ScmRepository repository =
                scmRepositoryConfigurator.getConfiguredRepository(ReleaseUtils.buildReleaseDescriptor(builder), null);

        assertEquals("username", repository.getProviderRepository().getUser(), "check username");
        assertEquals("password", repository.getProviderRepository().getPassword(), "check password");
    }

    @Test
    void testGetConfiguredRepositoryWithTagBase() throws ScmRepositoryException, NoSuchScmProviderException {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm:svn:http://localhost/home/svn/module/trunk");
        builder.setScmTagBase("http://localhost/home/svn/module/tags");

        ScmRepository repository =
                scmRepositoryConfigurator.getConfiguredRepository(ReleaseUtils.buildReleaseDescriptor(builder), null);

        SvnScmProviderRepository providerRepository = (SvnScmProviderRepository) repository.getProviderRepository();
        assertEquals("http://localhost/home/svn/module/tags", providerRepository.getTagBase(), "check tag base");
    }

    @Test
    void testGetConfiguredRepositoryWithHost() throws ScmRepositoryException, NoSuchScmProviderException {
        Settings settings = new Settings();
        Server server = new Server();
        server.setId("localhost");
        server.setUsername("settings-username");
        server.setPassword("settings-password");
        server.setPrivateKey("settings-private-key");
        server.setPassphrase("settings-passphrase");
        settings.addServer(server);

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm:svn:http://localhost/repo");

        ScmRepository repository = scmRepositoryConfigurator.getConfiguredRepository(
                ReleaseUtils.buildReleaseDescriptor(builder), settings);

        ScmProviderRepositoryWithHost providerRepository =
                (ScmProviderRepositoryWithHost) repository.getProviderRepository();
        assertEquals("localhost", providerRepository.getHost(), "check host");
        assertEquals(0, providerRepository.getPort(), "check port");
        assertEquals("settings-username", providerRepository.getUser(), "check username");
        assertEquals("settings-password", providerRepository.getPassword(), "check password");
        assertEquals("settings-private-key", providerRepository.getPrivateKey(), "check private key");
        assertEquals("settings-passphrase", providerRepository.getPassphrase(), "check passphrase");
    }

    @Test
    void testGetConfiguredRepositoryWithEncryptedPasswords() throws ScmRepositoryException, NoSuchScmProviderException {
        Settings settings = new Settings();
        Server server = new Server();
        server.setId("localhost");
        server.setUsername("testuser");
        server.setPassword("{Ael0S2tnXv8H3X+gHKpZAvAA25D8+gmU2w2RrGaf5v8=}");
        server.setPassphrase("{7zK9P8hNVeUHbTsjiA/vnOs0zUXbND+9MBNPvdvl+x4=}");
        settings.addServer(server);

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm:svn:svn://localhost/repo");

        ScmRepository repository = scmRepositoryConfigurator.getConfiguredRepository(
                ReleaseUtils.buildReleaseDescriptor(builder), settings);

        ScmProviderRepositoryWithHost providerRepository =
                (ScmProviderRepositoryWithHost) repository.getProviderRepository();
        assertEquals("localhost", providerRepository.getHost(), "check host");
        assertEquals("testuser", providerRepository.getUser(), "check username");
        assertEquals("testpass", providerRepository.getPassword(), "check password");
        assertEquals("testphrase", providerRepository.getPassphrase(), "check passphrase");
    }

    @Test
    void testGetConfiguredRepositoryInvalidScmUrl() throws Exception {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm-url");

        try {
            scmRepositoryConfigurator.getConfiguredRepository(ReleaseUtils.buildReleaseDescriptor(builder), null);

            fail("Expected failure to get a repository with an invalid SCM URL");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    void testGetConfiguredRepositoryInvalidScmProvider() throws ScmRepositoryException {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm:url:");

        try {
            scmRepositoryConfigurator.getConfiguredRepository(ReleaseUtils.buildReleaseDescriptor(builder), null);

            fail("Expected failure to get a repository with an invalid SCM URL");
        } catch (NoSuchScmProviderException e) {
            // expected
        }
    }

    @Test
    void testGetConfiguredRepositoryInvalidScmUrlParameters() throws NoSuchScmProviderException {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm:svn:");

        try {
            scmRepositoryConfigurator.getConfiguredRepository(ReleaseUtils.buildReleaseDescriptor(builder), null);

            fail("Expected failure to get a repository with an invalid SCM URL");
        } catch (ScmRepositoryException e) {
            // expected
        }
    }

    @Test
    void testGetRepositoryProvider() throws ScmRepositoryException, NoSuchScmProviderException {
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder();

        ScmRepository repository =
                scmRepositoryConfigurator.getConfiguredRepository(ReleaseUtils.buildReleaseDescriptor(builder), null);

        ScmProvider provider = scmRepositoryConfigurator.getRepositoryProvider(repository);
        assertEquals("svn", provider.getScmType(), "Check SCM provider");
    }

    private static ReleaseDescriptorBuilder createReleaseDescriptorBuilder() {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm:svn:http://localhost/repo");
        return builder;
    }

    private static ReleaseDescriptorBuilder createReleaseDescriptorBuilder(String username, String password) {
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder();
        builder.setScmUsername(username);
        builder.setScmPassword(password);
        return builder;
    }
}
