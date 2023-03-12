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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.SettingsUtils;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Writer;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.util.MavenCrypto;
import org.apache.maven.shared.release.util.MavenCrypto.MavenCryptoException;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

/**
 * <p>Abstract AbstractMavenExecutor class.</p>
 */
public abstract class AbstractMavenExecutor implements MavenExecutor {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final MavenCrypto mavenCrypto;

    protected AbstractMavenExecutor(MavenCrypto mavenCrypto) {
        this.mavenCrypto = requireNonNull(mavenCrypto);
    }

    @Override
    public void executeGoals(
            File workingDirectory,
            String goals,
            ReleaseEnvironment releaseEnvironment,
            boolean interactive,
            String additionalArguments,
            String pomFileName,
            ReleaseResult result)
            throws MavenExecutorException {
        List<String> goalsList = new ArrayList<>();
        if (goals != null) {
            // accept both space and comma, so the old way still work
            // also accept line separators, so that goal lists can be spread
            // across multiple lines in the POM.
            Collections.addAll(goalsList, StringUtils.split(goals, ", \n\r\t"));
        }
        executeGoals(
                workingDirectory, goalsList, releaseEnvironment, interactive, additionalArguments, pomFileName, result);
    }

    protected abstract void executeGoals(
            File workingDirectory,
            List<String> goals,
            ReleaseEnvironment releaseEnvironment,
            boolean interactive,
            String additionalArguments,
            String pomFileName,
            ReleaseResult result)
            throws MavenExecutorException;

    /**
     * <p>Getter for the field <code>logger</code>.</p>
     *
     * @return a {@link Logger} object
     */
    protected final Logger getLogger() {
        return logger;
    }

    /**
     * <p>encryptSettings.</p>
     *
     * @param settings a {@link org.apache.maven.settings.Settings} object
     * @return a {@link org.apache.maven.settings.Settings} object
     */
    protected Settings encryptSettings(Settings settings) {
        Settings encryptedSettings = SettingsUtils.copySettings(settings);

        for (Server server : encryptedSettings.getServers()) {
            String password = server.getPassword();
            if (password != null && !mavenCrypto.isEncryptedString(password)) {
                try {
                    server.setPassword(mavenCrypto.encryptAndDecorate(password));
                } catch (MavenCryptoException e) {
                    // ignore
                }
            }

            String passphrase = server.getPassphrase();
            if (passphrase != null && !mavenCrypto.isEncryptedString(passphrase)) {
                try {
                    server.setPassphrase(mavenCrypto.encryptAndDecorate(passphrase));
                } catch (MavenCryptoException e) {
                    // ignore
                }
            }
        }

        for (Proxy proxy : encryptedSettings.getProxies()) {
            String password = proxy.getPassword();
            if (password != null && !mavenCrypto.isEncryptedString(password)) {
                try {
                    proxy.setPassword(mavenCrypto.encryptAndDecorate(password));
                } catch (MavenCryptoException e) {
                    // ignore
                }
            }
        }

        return encryptedSettings;
    }

    /**
     * <p>getSettingsWriter.</p>
     *
     * @return a {@link org.apache.maven.settings.io.xpp3.SettingsXpp3Writer} object
     */
    protected SettingsXpp3Writer getSettingsWriter() {
        return new SettingsXpp3Writer();
    }
}
