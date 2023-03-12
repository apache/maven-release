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
package org.apache.maven.shared.release.exec;

import java.io.File;
import java.io.Writer;

import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Writer;
import org.apache.maven.shared.release.PlexusJUnit4TestCase;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.util.MavenCrypto;
import org.mockito.ArgumentCaptor;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class InvokerMavenExecutorTest extends PlexusJUnit4TestCase {

    private MavenCrypto mavenCrypto;

    private SecDispatcher secDispatcher;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        mavenCrypto = lookup(MavenCrypto.class);
        secDispatcher = lookup(SecDispatcher.class);
    }

    public void testEncryptSettings() throws Exception {
        InvokerMavenExecutor executor = new InvokerMavenExecutor(mavenCrypto);

        // prepare
        File workingDirectory = getTestFile("target/working-directory");
        workingDirectory.mkdirs();

        Settings settings = new Settings();
        Server server = new Server();
        server.setPassphrase("server_passphrase");
        server.setPassword("server_password");
        settings.addServer(server);
        Proxy proxy = new Proxy();
        proxy.setPassword("proxy_password");
        settings.addProxy(proxy);

        DefaultReleaseEnvironment releaseEnvironment = new DefaultReleaseEnvironment();
        releaseEnvironment.setSettings(settings);
        releaseEnvironment.setMavenHome(new File(System.getProperty("injectedMavenHome")));

        InvokerMavenExecutor executorSpy = spy(executor);
        SettingsXpp3Writer settingsWriter = mock(SettingsXpp3Writer.class);

        ArgumentCaptor<Settings> encryptedSettings = ArgumentCaptor.forClass(Settings.class);

        when(executorSpy.getSettingsWriter()).thenReturn(settingsWriter);

        try {
            executorSpy.executeGoals(
                    workingDirectory, "validate", releaseEnvironment, false, null, null, new ReleaseResult());
        } catch (MavenExecutorException e) {
        }

        verify(settingsWriter).write(isA(Writer.class), encryptedSettings.capture());

        assertNotSame(settings, encryptedSettings.getValue());

        Server encryptedServer = encryptedSettings.getValue().getServers().get(0);
        assertEquals("server_passphrase", secDispatcher.decrypt(encryptedServer.getPassphrase()));
        assertEquals("server_password", secDispatcher.decrypt(encryptedServer.getPassword()));

        Proxy encryptedProxy = encryptedSettings.getValue().getProxies().get(0);
        assertEquals("proxy_password", secDispatcher.decrypt(encryptedProxy.getPassword()));

        File settingsSecurity = new File(System.getProperty("user.home"), ".m2/settings-security.xml");
        if (settingsSecurity.exists()) {
            assertNotEquals("server_passphrase", encryptedServer.getPassphrase());
            assertNotEquals("server_password", encryptedServer.getPassword());
            assertNotEquals("proxy_password", encryptedProxy.getPassword());
        }
    }
}
