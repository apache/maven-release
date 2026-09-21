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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.apache.maven.executor.ExecutorException;
import org.apache.maven.executor.ExecutorRequest;
import org.apache.maven.executor.ExecutorResult;
import org.apache.maven.executor.forked.ForkedMavenExecutor;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Writer;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.util.MavenCrypto;
import org.codehaus.plexus.util.cli.CommandLineUtils;

/**
 * Fork Maven through maven-executor, on the Maven installation of the release environment.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
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
        if (releaseEnvironment.getMavenHome() == null) {
            throw new MavenExecutorException(
                    "The release environment does not name a Maven home.", new IllegalStateException("mavenHome"));
        }

        List<String> arguments = new ArrayList<>();
        if (getLogger().isDebugEnabled()) {
            arguments.add("-X");
        }
        if (!interactive) {
            arguments.add("-B");
        }
        if (releaseEnvironment.getLocalRepositoryDirectory() != null) {
            arguments.add("-Dmaven.repo.local="
                    + releaseEnvironment.getLocalRepositoryDirectory().getAbsolutePath());
        }
        if (pomFileName != null) {
            getLogger()
                    .debug("Specified POM file is not named 'pom.xml'. "
                            + "Using the '-f' command-line option to accommodate non-standard filename...");
            arguments.add("-f");
            arguments.add(pomFileName);
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
                arguments.add("-s");
                arguments.add(settingsFile.getAbsolutePath());
            } catch (IOException e) {
                throw new MavenExecutorException("Could not create temporary file for release settings.xml", e);
            }
        }

        try {
            arguments.addAll(goals);
            if (additionalArguments != null && !additionalArguments.isEmpty()) {
                try {
                    arguments.addAll(Arrays.asList(CommandLineUtils.translateCommandline(additionalArguments)));
                } catch (Exception e) {
                    throw new MavenExecutorException("Cannot parse the additional arguments.", e);
                }
            }

            ExecutorRequest.Builder request = ExecutorRequest.mavenBuilder()
                    .cwd(workingDirectory.toPath())
                    .arguments(arguments)
                    .stdOut(new LineOutputStream(getLogger()::info))
                    .stdErr(new LineOutputStream(getLogger()::error));
            if (releaseEnvironment.getJavaHome() != null) {
                request.environmentVariable(
                        "JAVA_HOME", releaseEnvironment.getJavaHome().getAbsolutePath());
            }
            // for interactive mode we need some inputs stream
            if (interactive) {
                request.stdIn(System.in);
            }

            getLogger().debug("Executing: mvn " + String.join(" ", arguments));
            try (ForkedMavenExecutor executor =
                    new ForkedMavenExecutor(releaseEnvironment.getMavenHome().toPath())) {
                ExecutorResult executorResult = executor.execute(request.build());
                if (!executorResult.success()) {
                    int exitCode = executorResult.exitCode().orElse(-1);
                    throw new MavenExecutorException("Maven execution failed, exit code: " + exitCode, exitCode);
                }
            } catch (ExecutorException e) {
                throw new MavenExecutorException("Failed to invoke Maven build.", e);
            }
        } finally {
            if (settingsFile != null && settingsFile.exists() && !settingsFile.delete()) {
                settingsFile.deleteOnExit();
            }
        }
    }

    /**
     * Hands the build's output to the logger one line at a time.
     */
    private static final class LineOutputStream extends OutputStream {
        private final Consumer<String> lines;

        private final ByteArrayOutputStream line = new ByteArrayOutputStream();

        LineOutputStream(Consumer<String> lines) {
            this.lines = lines;
        }

        @Override
        public void write(int b) {
            if (b == '\n') {
                flushLine();
            } else if (b != '\r') {
                line.write(b);
            }
        }

        @Override
        public void write(byte[] b, int off, int len) {
            for (int i = off; i < off + len; i++) {
                write(b[i]);
            }
        }

        @Override
        public void close() {
            if (line.size() > 0) {
                flushLine();
            }
        }

        private void flushLine() {
            lines.accept(new String(line.toByteArray(), StandardCharsets.UTF_8));
            line.reset();
        }
    }
}
