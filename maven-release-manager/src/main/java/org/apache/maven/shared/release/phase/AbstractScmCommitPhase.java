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
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmVersion;
import org.apache.maven.scm.command.checkin.CheckInScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.scm.ReleaseScmCommandException;
import org.apache.maven.shared.release.scm.ReleaseScmRepositoryException;
import org.apache.maven.shared.release.scm.ScmRepositoryConfigurator;
import org.apache.maven.shared.release.util.ReleaseUtil;

import static java.util.Objects.requireNonNull;

/**
 * Holds the basic concept of committing changes to the current working copy.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author <a href="mailto:me@lcorneliussen.de">Lars Corneliussen</a>
 */
public abstract class AbstractScmCommitPhase extends AbstractReleasePhase {
    /**
     * Tool that gets a configured SCM repository from release configuration.
     */
    protected final ScmRepositoryConfigurator scmRepositoryConfigurator;

    /**
     * The getter in the descriptor for the comment.
     */
    protected final String descriptorCommentGetter;

    private final Set<String> exclusionPatterns = new HashSet<>();

    protected AbstractScmCommitPhase(
            ScmRepositoryConfigurator scmRepositoryConfigurator, String descriptorCommentGetter) {
        this.scmRepositoryConfigurator = requireNonNull(scmRepositoryConfigurator);
        this.descriptorCommentGetter = requireNonNull(descriptorCommentGetter);
    }

    @Override
    public ReleaseResult execute(
            ReleaseDescriptor releaseDescriptor,
            ReleaseEnvironment releaseEnvironment,
            List<MavenProject> reactorProjects)
            throws ReleaseExecutionException, ReleaseFailureException {
        ReleaseResult relResult = new ReleaseResult();

        validateConfiguration(releaseDescriptor);

        List<String> additionalExcludes = releaseDescriptor.getCheckModificationExcludes();

        if (additionalExcludes != null) {
            exclusionPatterns.addAll(additionalExcludes);
        }

        runLogic(releaseDescriptor, releaseEnvironment, reactorProjects, relResult, false);

        relResult.setResultCode(ReleaseResult.SUCCESS);

        return relResult;
    }

    @Override
    public ReleaseResult simulate(
            ReleaseDescriptor releaseDescriptor,
            ReleaseEnvironment releaseEnvironment,
            List<MavenProject> reactorProjects)
            throws ReleaseExecutionException, ReleaseFailureException {
        ReleaseResult result = new ReleaseResult();

        validateConfiguration(releaseDescriptor);

        runLogic(releaseDescriptor, releaseEnvironment, reactorProjects, result, true);

        result.setResultCode(ReleaseResult.SUCCESS);
        return result;
    }

    /**
     * <p>runLogic.</p>
     *
     * @param releaseDescriptor  a {@link org.apache.maven.shared.release.config.ReleaseDescriptor} object
     * @param releaseEnvironment a {@link org.apache.maven.shared.release.env.ReleaseEnvironment} object
     * @param reactorProjects    a {@link java.util.List} object
     * @param result             a {@link org.apache.maven.shared.release.ReleaseResult} object
     * @param simulating         a boolean
     * @throws org.apache.maven.shared.release.scm.ReleaseScmCommandException    if any.
     * @throws org.apache.maven.shared.release.ReleaseExecutionException         if any.
     * @throws org.apache.maven.shared.release.scm.ReleaseScmRepositoryException if any.
     */
    protected abstract void runLogic(
            ReleaseDescriptor releaseDescriptor,
            ReleaseEnvironment releaseEnvironment,
            List<MavenProject> reactorProjects,
            ReleaseResult result,
            boolean simulating)
            throws ReleaseScmCommandException, ReleaseExecutionException, ReleaseScmRepositoryException;

    /**
     * <p>performCheckins.</p>
     *
     * @param releaseDescriptor  a {@link org.apache.maven.shared.release.config.ReleaseDescriptor} object
     * @param releaseEnvironment a {@link org.apache.maven.shared.release.env.ReleaseEnvironment} object
     * @param reactorProjects    a {@link java.util.List} object
     * @param message            a {@link java.lang.String} object
     * @throws org.apache.maven.shared.release.scm.ReleaseScmRepositoryException if any.
     * @throws org.apache.maven.shared.release.ReleaseExecutionException         if any.
     * @throws org.apache.maven.shared.release.scm.ReleaseScmCommandException    if any.
     */
    protected void performCheckins(
            ReleaseDescriptor releaseDescriptor,
            ReleaseEnvironment releaseEnvironment,
            List<MavenProject> reactorProjects,
            String message)
            throws ReleaseScmRepositoryException, ReleaseExecutionException, ReleaseScmCommandException {

        getLogger().info("Checking in modified POMs...");

        ScmRepository repository;
        ScmProvider provider;
        try {
            repository = scmRepositoryConfigurator.getConfiguredRepository(
                    releaseDescriptor, releaseEnvironment.getSettings());

            repository.getProviderRepository().setPushChanges(releaseDescriptor.isPushChanges());

            repository.getProviderRepository().setWorkItem(releaseDescriptor.getWorkItem());

            provider = scmRepositoryConfigurator.getRepositoryProvider(repository);
        } catch (ScmRepositoryException e) {
            throw new ReleaseScmRepositoryException(e.getMessage(), e.getValidationMessages());
        } catch (NoSuchScmProviderException e) {
            throw new ReleaseExecutionException("Unable to configure SCM repository: " + e.getMessage(), e);
        }

        if (releaseDescriptor.isCommitByProject()) {
            for (MavenProject project : reactorProjects) {
                List<File> pomFiles = createPomFiles(releaseDescriptor, project);
                ScmFileSet fileSet = new ScmFileSet(project.getFile().getParentFile(), pomFiles);

                checkin(provider, repository, fileSet, releaseDescriptor, message);
            }
        } else {
            List<File> pomFiles = createPomFiles(releaseDescriptor, reactorProjects);

            if (!pomFiles.isEmpty()) {
                ScmFileSet fileSet = new ScmFileSet(new File(releaseDescriptor.getWorkingDirectory()), pomFiles);

                checkin(provider, repository, fileSet, releaseDescriptor, message);
            }
        }
    }

    private void checkin(
            ScmProvider provider,
            ScmRepository repository,
            ScmFileSet fileSet,
            ReleaseDescriptor releaseDescriptor,
            String message)
            throws ReleaseExecutionException, ReleaseScmCommandException {
        CheckInScmResult result;
        try {
            result = provider.checkIn(repository, fileSet, (ScmVersion) null, message);
        } catch (ScmException e) {
            throw new ReleaseExecutionException("An error is occurred in the checkin process: " + e.getMessage(), e);
        }

        if (!result.isSuccess()) {
            throw new ReleaseScmCommandException("Unable to commit files", result);
        }
        if (releaseDescriptor.isRemoteTagging()) {
            releaseDescriptor.setScmReleasedPomRevision(result.getScmRevision());
        }
    }

    /**
     * <p>simulateCheckins.</p>
     *
     * @param releaseDescriptor a {@link org.apache.maven.shared.release.config.ReleaseDescriptor} object
     * @param reactorProjects   a {@link java.util.List} object
     * @param result            a {@link org.apache.maven.shared.release.ReleaseResult} object
     * @param message           a {@link java.lang.String} object
     */
    protected void simulateCheckins(
            ReleaseDescriptor releaseDescriptor,
            List<MavenProject> reactorProjects,
            ReleaseResult result,
            String message) {
        Collection<File> pomFiles = createPomFiles(releaseDescriptor, reactorProjects);
        logInfo(result, "Full run would be commit " + pomFiles.size() + " files with message: '" + message + "'");
    }

    /**
     * <p>validateConfiguration.</p>
     *
     * @param releaseDescriptor a {@link org.apache.maven.shared.release.config.ReleaseDescriptor} object
     * @throws org.apache.maven.shared.release.ReleaseFailureException if any.
     */
    protected void validateConfiguration(ReleaseDescriptor releaseDescriptor) throws ReleaseFailureException {
        if (releaseDescriptor.getScmReleaseLabel() == null) {
            throw new ReleaseFailureException("A release label is required for committing");
        }
    }

    /**
     * <p>createMessage.</p>
     *
     * @param reactorProjects   a {@link java.util.List} object
     * @param releaseDescriptor a {@link org.apache.maven.shared.release.config.ReleaseDescriptor} object
     * @return a {@link java.lang.String} object
     * @throws org.apache.maven.shared.release.ReleaseExecutionException if any.
     */
    protected String createMessage(List<MavenProject> reactorProjects, ReleaseDescriptor releaseDescriptor)
            throws ReleaseExecutionException {
        String comment;
        boolean branch = false;
        if ("getScmReleaseCommitComment".equals(descriptorCommentGetter)) {
            comment = releaseDescriptor.getScmReleaseCommitComment();
        } else if ("getScmDevelopmentCommitComment".equals(descriptorCommentGetter)) {
            comment = releaseDescriptor.getScmDevelopmentCommitComment();
        } else if ("getScmBranchCommitComment".equals(descriptorCommentGetter)) {
            comment = releaseDescriptor.getScmBranchCommitComment();
            branch = true;
        } else if ("getScmRollbackCommitComment".equals(descriptorCommentGetter)) {
            comment = releaseDescriptor.getScmRollbackCommitComment();
        } else {
            throw new ReleaseExecutionException(
                    "Invalid configuration of descriptorCommentGetter='" + descriptorCommentGetter + "'");
        }

        MavenProject project = ReleaseUtil.getRootProject(reactorProjects);
        comment = comment.replace(
                "@{prefix}", releaseDescriptor.getScmCommentPrefix().trim());
        comment = comment.replace("@{groupId}", project.getGroupId());
        comment = comment.replace("@{artifactId}", project.getArtifactId());
        if (branch) {
            comment = comment.replace("@{branchName}", releaseDescriptor.getScmReleaseLabel());
        } else {
            comment = comment.replace("@{releaseLabel}", releaseDescriptor.getScmReleaseLabel());
        }
        return comment;
    }

    /**
     * <p>createPomFiles.</p>
     *
     * @param releaseDescriptor a {@link org.apache.maven.shared.release.config.ReleaseDescriptor} object
     * @param project           a {@link org.apache.maven.project.MavenProject} object
     * @return a {@link java.util.List} object
     */
    protected static List<File> createPomFiles(ReleaseDescriptor releaseDescriptor, MavenProject project) {
        List<File> pomFiles = new ArrayList<>();

        pomFiles.add(ReleaseUtil.getStandardPom(project));

        if (releaseDescriptor.isGenerateReleasePoms() && !releaseDescriptor.isSuppressCommitBeforeTagOrBranch()) {
            pomFiles.add(ReleaseUtil.getReleasePom(project));
        }

        return pomFiles;
    }

    /**
     * <p>createPomFiles.</p>
     *
     * @param releaseDescriptor a {@link org.apache.maven.shared.release.config.ReleaseDescriptor} object
     * @param reactorProjects   a {@link java.util.List} object
     * @return a {@link java.util.List} object
     */
    protected List<File> createPomFiles(ReleaseDescriptor releaseDescriptor, List<MavenProject> reactorProjects) {

        List<File> pomFiles = new ArrayList<>();
        for (MavenProject project : reactorProjects) {

            final String path = project.getFile().getPath();

            boolean isExcludedPathFound = exclusionPatterns.stream()
                    .anyMatch(exclusionPattern -> FileSystems.getDefault()
                            .getPathMatcher("glob:" + exclusionPattern)
                            .matches(Paths.get(path)));
            if (!isExcludedPathFound) {
                pomFiles.addAll(createPomFiles(releaseDescriptor, project));
            }
        }
        return pomFiles;
    }
}
