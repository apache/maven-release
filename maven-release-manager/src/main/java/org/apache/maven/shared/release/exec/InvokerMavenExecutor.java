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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.settings.io.xpp3.SettingsXpp3Writer;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.InvokerLogger;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.util.MavenCrypto;
import org.slf4j.Logger;

/**
 * Fork Maven using the maven-invoker shared library.
 */
@Singleton
@Named("invoker")
public class InvokerMavenExecutor extends AbstractMavenExecutor {
    @Inject
    public InvokerMavenExecutor(MavenCrypto mavenCrypto) {
        super(mavenCrypto);
    }

    @Override
    public void executeGoals(
            File workingDirectory,
            List<String> goals,
            ReleaseEnvironment releaseEnvironment,
            boolean interactive,
            String additionalArguments,
            String pomFileName,
            ReleaseResult result)
            throws MavenExecutorException {
        InvokerLogger bridge = getInvokerLogger();

        Invoker invoker = new DefaultInvoker()
                .setMavenHome(releaseEnvironment.getMavenHome())
                .setLocalRepositoryDirectory(releaseEnvironment.getLocalRepositoryDirectory())
                .setLogger(bridge);

        InvocationRequest req = new DefaultInvocationRequest()
                .setDebug(getLogger().isDebugEnabled())
                .setBaseDirectory(workingDirectory)
                // fix for MRELEASE-1105
                // .addShellEnvironment( "MAVEN_DEBUG_OPTS", "" )
                .setBatchMode(!interactive)
                .setJavaHome(releaseEnvironment.getJavaHome())
                .setOutputHandler(getLogger()::info)
                .setErrorHandler(getLogger()::error);

        // for interactive mode we need some inputs stream
        if (interactive) {
            req.setInputStream(System.in);
        }

        if (pomFileName != null) {
            req.setPomFileName(pomFileName);
        }

        File settingsFile = null;
        if (releaseEnvironment.getSettings() != null) {
            // Have to serialize to a file as if Maven is embedded, there may not actually be a settings.xml on disk
            try {
                settingsFile = Files.createTempFile("release-settings", ".xml").toFile();
                SettingsXpp3Writer writer = getSettingsWriter();

                try (FileWriter fileWriter = new FileWriter(settingsFile)) {
                    writer.write(fileWriter, encryptSettings(releaseEnvironment.getSettings()));
                }
                req.setUserSettingsFile(settingsFile);
            } catch (IOException e) {
                throw new MavenExecutorException("Could not create temporary file for release settings.xml", e);
            }
        }

        try {
            List<String> targetGoals = new ArrayList<>(goals);

            if (additionalArguments != null && !additionalArguments.isEmpty()) {
                // additionalArguments will be parsed be MavenInvoker
                targetGoals.add(additionalArguments);
            }

            req.setGoals(targetGoals);

            try {
                InvocationResult invocationResult = invoker.execute(req);

                if (invocationResult.getExecutionException() != null) {
                    throw new MavenExecutorException(
                            "Error executing Maven.", invocationResult.getExecutionException());
                }

                if (invocationResult.getExitCode() != 0) {
                    throw new MavenExecutorException(
                            "Maven execution failed, exit code: " + invocationResult.getExitCode(),
                            invocationResult.getExitCode());
                }
            } catch (MavenInvocationException e) {
                throw new MavenExecutorException("Failed to invoke Maven build.", e);
            }
        } finally {
            if (settingsFile != null && settingsFile.exists() && !settingsFile.delete()) {
                settingsFile.deleteOnExit();
            }
        }
    }

    /**
     * <p>getInvokerLogger.</p>
     *
     * @return a {@link org.apache.maven.shared.invoker.InvokerLogger} object
     */
    protected InvokerLogger getInvokerLogger() {
        return new LoggerBridge(getLogger());
    }

    private static final class LoggerBridge implements InvokerLogger {

        private final Logger logger;

        LoggerBridge(Logger logger) {
            this.logger = logger;
        }

        @Override
        public void debug(String message, Throwable error) {
            logger.debug(message, error);
        }

        @Override
        public void debug(String message) {
            logger.debug(message);
        }

        @Override
        public void error(String message, Throwable error) {
            logger.error(message, error);
        }

        @Override
        public void error(String message) {
            logger.error(message);
        }

        @Override
        public void fatalError(String message, Throwable error) {
            logger.error(message, error);
        }

        @Override
        public void fatalError(String message) {
            logger.error(message);
        }

        @Override
        public int getThreshold() {
            return InvokerLogger.DEBUG;
        }

        @Override
        public void info(String message, Throwable error) {
            logger.info(message, error);
        }

        @Override
        public void info(String message) {
            logger.info(message);
        }

        @Override
        public boolean isDebugEnabled() {
            return logger.isDebugEnabled();
        }

        @Override
        public boolean isErrorEnabled() {
            return logger.isErrorEnabled();
        }

        @Override
        public boolean isFatalErrorEnabled() {
            return logger.isErrorEnabled();
        }

        @Override
        public boolean isInfoEnabled() {
            return logger.isInfoEnabled();
        }

        @Override
        public boolean isWarnEnabled() {
            return logger.isWarnEnabled();
        }

        @Override
        public void setThreshold(int level) {
            // NOTE:
            // logger.setThreshold( level )
            // is not supported in plexus-container-default:1.0-alpha-9 as used in Maven 2.x
        }

        @Override
        public void warn(String message, Throwable error) {
            logger.warn(message, error);
        }

        @Override
        public void warn(String message) {
            logger.warn(message);
        }
    }
}
