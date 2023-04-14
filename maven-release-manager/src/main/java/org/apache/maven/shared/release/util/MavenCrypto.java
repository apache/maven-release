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
package org.apache.maven.shared.release.util;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.Properties;

import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;
import org.sonatype.plexus.components.sec.dispatcher.SecUtil;
import org.sonatype.plexus.components.sec.dispatcher.model.SettingsSecurity;

/**
 * A shared utility to access {@link DefaultSecDispatcher} service.
 *
 * @since TBD
 */
@Singleton
@Named
public class MavenCrypto {
    /**
     * Exception thrown when "something" of crypto operation did not succeed. All the code all over the place
     * was catching sec dispatcher and plexus cipher exceptions just to neglect it (maybe log in DEBUG), so
     * this is one single exception here.
     */
    public static class MavenCryptoException extends Exception {
        private MavenCryptoException(String message) {
            super(message);
        }

        private MavenCryptoException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private final DefaultSecDispatcher secDispatcher;

    private final PlexusCipher plexusCipher;

    @Inject
    public MavenCrypto(DefaultSecDispatcher secDispatcher, PlexusCipher plexusCipher) {
        this.secDispatcher = secDispatcher;
        this.plexusCipher = plexusCipher;

        // Adjust the default path (def path != maven path)
        this.secDispatcher.setConfigurationFile("~/.m2/settings-security.xml");
    }

    public String decrypt(String value) throws MavenCryptoException {
        try {
            return secDispatcher.decrypt(value);
        } catch (SecDispatcherException e) {
            throw new MavenCryptoException("decrypt failed", e);
        }
    }

    public void decryptProperties(Properties properties) throws MavenCryptoException {
        String[] keys = new String[] {"scm.password", "scm.passphrase"};

        for (String key : keys) {
            String value = properties.getProperty(key);
            if (value != null) {
                properties.put(key, decryptDecorated(value));
            }
        }
    }

    public String encryptAndDecorate(String passwd) throws MavenCryptoException {
        try {
            final String master = getMaster();

            DefaultPlexusCipher cipher = new DefaultPlexusCipher();
            String masterPasswd = cipher.decryptDecorated(master, DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION);
            return cipher.encryptAndDecorate(passwd, masterPasswd);
        } catch (PlexusCipherException e) {
            throw new MavenCryptoException("encrypt failed", e);
        }
    }

    public boolean isEncryptedString(String str) {
        return plexusCipher.isEncryptedString(str);
    }

    private String decryptDecorated(String value) throws MavenCryptoException {
        try {
            final String master = getMaster();

            DefaultPlexusCipher cipher = new DefaultPlexusCipher();
            String masterPasswd = cipher.decryptDecorated(master, DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION);
            return cipher.decryptDecorated(value, masterPasswd);
        } catch (PlexusCipherException e) {
            throw new MavenCryptoException("decrypt failed", e);
        }
    }

    private String getMaster() throws MavenCryptoException {
        String configurationFile = secDispatcher.getConfigurationFile();

        if (configurationFile.startsWith("~")) {
            configurationFile = System.getProperty("user.home") + configurationFile.substring(1);
        }

        String file = System.getProperty(DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION, configurationFile);

        String master = null;

        try {
            SettingsSecurity sec = SecUtil.read(file, true);
            if (sec != null) {
                master = sec.getMaster();
            }
        } catch (SecDispatcherException e) {
            throw new MavenCryptoException("config file read failed", e);
        }

        if (master == null) {
            throw new MavenCryptoException("Master password is not set in the setting security file: " + file);
        }

        return master;
    }
}
