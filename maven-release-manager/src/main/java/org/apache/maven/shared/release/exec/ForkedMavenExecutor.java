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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;

import org.apache.maven.settings.io.xpp3.SettingsXpp3Writer;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.util.MavenCrypto;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.Commandline;

import static java.util.Objects.requireNonNull;

/**
 * Fork Maven to execute a series of goals.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
@Singleton
@Named("forked-path")
public class ForkedMavenExecutor extends AbstractMavenExecutor {
    /**
     * Command line factory.
     */
    private final CommandLineFactory commandLineFactory;

    @Inject
    public ForkedMavenExecutor(MavenCrypto mavenCrypto, CommandLineFactory commandLineFactory) {
        super(mavenCrypto);
        this.commandLineFactory = requireNonNull(commandLineFactory);
    }

    /*
     * @noinspection UseOfSystemOutOrSystemErr
     */
    @Override
    public void executeGoals(
            File workingDirectory,
            List<String> goals,
            ReleaseEnvironment releaseEnvironment,
            boolean interactive,
            String additionalArguments,
            String pomFileName,
            ReleaseResult relResult)
            throws MavenExecutorException {
        String mavenPath;
        // if null we use the current one
        if (releaseEnvironment.getMavenHome() != null) {
            mavenPath = releaseEnvironment.getMavenHome().getAbsolutePath();
        } else {
            mavenPath = System.getProperty("maven.home");
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
            } catch (IOException e) {
                throw new MavenExecutorException("Could not create temporary file for release settings.xml", e);
            }
        }
        try {

            Commandline cl =
                    commandLineFactory.createCommandLine(mavenPath + File.separator + "bin" + File.separator + "mvn");

            cl.setWorkingDirectory(workingDirectory.getAbsolutePath());

            // FIX for MRELEASE-1105
            // cl.addEnvironment( "MAVEN_DEBUG_OPTS", "" );

            cl.addEnvironment("MAVEN_TERMINATE_CMD", "on");

            if (releaseEnvironment.getJavaHome() != null) {
                cl.addEnvironment("JAVA_HOME", releaseEnvironment.getJavaHome().getAbsolutePath());
            }

            if (settingsFile != null) {
                cl.createArg().setValue("-s");
                cl.createArg().setFile(settingsFile);
            }

            if (pomFileName != null) {
                cl.createArg().setValue("-f");
                cl.createArg().setValue(pomFileName);
            }

            for (String goal : goals) {
                cl.createArg().setValue(goal);
            }

            if (!interactive) {
                cl.createArg().setValue("--batch-mode");
            }

            if (!(additionalArguments == null || additionalArguments.isEmpty())) {
                cl.createArg().setLine(additionalArguments);
            }

            TeeOutputStream stdOut = new TeeOutputStream(System.out);

            TeeOutputStream stdErr = new TeeOutputStream(System.err);

            try {
                relResult.appendInfo("Executing: " + cl);
                getLogger().info("Executing: " + cl);

                int result = executeCommandLine(cl, System.in, stdOut, stdErr);

                if (result != 0) {
                    throw new MavenExecutorException("Maven execution failed, exit code: '" + result + "'", result);
                }
            } catch (CommandLineException e) {
                throw new MavenExecutorException("Can't run goal " + goals, e);
            } finally {
                relResult.appendOutput(stdOut.toString());
            }
        } finally {
            if (settingsFile != null && settingsFile.exists() && !settingsFile.delete()) {
                settingsFile.deleteOnExit();
            }
        }
    }

    /**
     * <p>executeCommandLine.</p>
     *
     * @param cl        a {@link org.codehaus.plexus.util.cli.Commandline} object
     * @param systemIn  a {@link java.io.InputStream} object
     * @param systemOut a {@link java.io.OutputStream} object
     * @param systemErr a {@link java.io.OutputStream} object
     * @return a int
     * @throws org.codehaus.plexus.util.cli.CommandLineException if any.
     */
    public static int executeCommandLine(
            Commandline cl, InputStream systemIn, OutputStream systemOut, OutputStream systemErr)
            throws CommandLineException {
        if (cl == null) {
            throw new IllegalArgumentException("cl cannot be null.");
        }

        Process p = cl.execute();

        // processes.put( new Long( cl.getPid() ), p );

        RawStreamPumper inputFeeder = null;

        if (systemIn != null) {
            inputFeeder = new RawStreamPumper(systemIn, p.getOutputStream(), true);
        }

        RawStreamPumper outputPumper = new RawStreamPumper(p.getInputStream(), systemOut);
        RawStreamPumper errorPumper = new RawStreamPumper(p.getErrorStream(), systemErr);

        if (inputFeeder != null) {
            inputFeeder.start();
        }

        outputPumper.start();

        errorPumper.start();

        try {
            int returnValue = p.waitFor();

            if (inputFeeder != null) {
                inputFeeder.setDone();
            }
            outputPumper.setDone();
            errorPumper.setDone();

            // processes.remove( new Long( cl.getPid() ) );

            return returnValue;
        } catch (InterruptedException ex) {
            // killProcess( cl.getPid() );
            throw new CommandLineException("Error while executing external command, process killed.", ex);
        } finally {
            try {
                errorPumper.closeInput();
            } catch (IOException e) {
                // ignore
            }
            try {
                outputPumper.closeInput();
            } catch (IOException e) {
                // ignore
            }
            if (inputFeeder != null) {
                try {
                    inputFeeder.closeOutput();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }
}
