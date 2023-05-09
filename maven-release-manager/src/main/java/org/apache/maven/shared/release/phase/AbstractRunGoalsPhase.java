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
package org.apache.maven.shared.release.phase;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.exec.MavenExecutor;
import org.apache.maven.shared.release.exec.MavenExecutorException;
import org.codehaus.plexus.util.StringUtils;

import static java.util.Objects.requireNonNull;
import static org.apache.maven.shared.utils.logging.MessageUtils.buffer;

/**
 * Abstract phase to run a Maven invocation on the project.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class AbstractRunGoalsPhase extends AbstractReleasePhase {
    /**
     * Component to assist in executing Maven.
     */
    private final Map<String, MavenExecutor> mavenExecutors;

    protected AbstractRunGoalsPhase(Map<String, MavenExecutor> mavenExecutors) {
        this.mavenExecutors = requireNonNull(mavenExecutors);
    }

    protected ReleaseResult execute(
            ReleaseDescriptor releaseDescriptor,
            ReleaseEnvironment releaseEnvironment,
            List<MavenProject> reactorProjects,
            boolean logArguments)
            throws ReleaseExecutionException {
        return execute(
                releaseDescriptor,
                releaseEnvironment,
                new File(releaseDescriptor.getWorkingDirectory()),
                getAdditionalArguments(releaseDescriptor),
                logArguments);
    }

    protected ReleaseResult execute(
            ReleaseDescriptor releaseDescriptor,
            ReleaseEnvironment releaseEnvironment,
            File workingDirectory,
            String additionalArguments,
            boolean logArguments)
            throws ReleaseExecutionException {
        ReleaseResult result = new ReleaseResult();

        try {
            String goals = getGoals(releaseDescriptor);
            if (!(goals == null || goals.isEmpty())) {
                logInfo(result, "Executing goals '" + buffer().strong(goals) + "'...");
                if (logArguments) {
                    // logging arguments may log secrets: should be activated only on dryRun
                    logInfo(
                            result,
                            "    with additional arguments: "
                                    + (additionalArguments == null ? "(none)" : additionalArguments));
                }

                MavenExecutor mavenExecutor = mavenExecutors.get(releaseEnvironment.getMavenExecutorId());

                if (mavenExecutor == null) {
                    throw new ReleaseExecutionException(
                            "Cannot find Maven executor with id: " + releaseEnvironment.getMavenExecutorId());
                }

                File executionRoot;
                String pomFileName;
                if (releaseDescriptor.getPomFileName() != null) {
                    File rootPom = new File(workingDirectory, releaseDescriptor.getPomFileName());
                    executionRoot = rootPom.getParentFile();
                    pomFileName = rootPom.getName();
                } else {
                    executionRoot = workingDirectory;
                    pomFileName = null;
                }

                mavenExecutor.executeGoals(
                        executionRoot,
                        goals,
                        releaseEnvironment,
                        releaseDescriptor.isInteractive(),
                        additionalArguments,
                        pomFileName,
                        result);
            }
        } catch (MavenExecutorException e) {
            throw new ReleaseExecutionException(e.getMessage(), e);
        }

        result.setResultCode(ReleaseResult.SUCCESS);

        return result;
    }

    protected abstract String getGoals(ReleaseDescriptor releaseDescriptor);

    protected String getAdditionalArguments(ReleaseDescriptor releaseDescriptor) {
        StringBuilder builder = new StringBuilder();

        if (releaseDescriptor.getAdditionalArguments() != null) {
            builder.append(releaseDescriptor.getAdditionalArguments());
        }

        if (!releaseDescriptor.getActivateProfiles().isEmpty()) {
            builder.append(" -P ")
                    .append(StringUtils.join(
                            releaseDescriptor.getActivateProfiles().iterator(), ","));
        }

        return builder.length() > 0 ? builder.toString().trim() : null;
    }

    /**
     * Determines the path of the working directory. By default, this is the
     * checkout directory. For some SCMs, the project root directory is not the
     * checkout directory itself, but a SCM-specific subdirectory.
     *
     * @param checkoutDirectory            The checkout directory as java.io.File
     * @param relativePathProjectDirectory The relative path of the project directory within the checkout
     *                                     directory or ""
     * @return The working directory
     */
    protected File determineWorkingDirectory(File checkoutDirectory, String relativePathProjectDirectory) {
        File workingDirectory = checkoutDirectory;

        if (relativePathProjectDirectory != null && !relativePathProjectDirectory.isEmpty()) {
            workingDirectory = new File(checkoutDirectory, relativePathProjectDirectory);
        }

        return workingDirectory;
    }
}
