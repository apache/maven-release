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

import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.policy.PolicyException;
import org.apache.maven.shared.release.policy.version.VersionPolicy;
import org.apache.maven.shared.release.policy.version.VersionPolicyRequest;
import org.apache.maven.shared.release.scm.ScmRepositoryConfigurator;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.apache.maven.shared.release.versions.VersionParseException;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.slf4j.Logger;

import static java.util.Objects.requireNonNull;
import static org.apache.maven.shared.utils.logging.MessageUtils.buffer;

/**
 * Map projects to their new versions after release / into the next development cycle.
 * <p>
 * The map-phases per goal are:
 * <dl>
 *  <dt>release:prepare</dt><dd>map-release-versions + map-development-versions; RD.isBranchCreation() = false</dd>
 *  <dt>release:branch</dt><dd>map-branch-versions + map-development-versions; RD.isBranchCreation() = true</dd>
 *  <dt>release:update-versions</dt><dd>map-development-versions; RD.isBranchCreation() = false</dd>
 * </dl>
 *
 * <table>
 *   <caption>MapVersionsPhase</caption>
 *   <tr>
 *     <th>MapVersionsPhase field</th><th>map-release-versions</th><th>map-branch-versions</th>
 *     <th>map-development-versions</th>
 *   </tr>
 *   <tr>
 *     <td>convertToSnapshot</td>     <td>false</td>               <td>true</td>               <td>true</td>
 *   </tr>
 *   <tr>
 *     <td>convertToBranch</td>       <td>false</td>               <td>true</td>               <td>false</td>
 *   </tr>
 * </table>
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author Robert Scholte
 */
public abstract class AbstractMapVersionsPhase extends AbstractReleasePhase {
    /**
     * Tool that gets a configured SCM repository from release configuration.
     */
    private final ScmRepositoryConfigurator scmRepositoryConfigurator;

    /**
     * Component used to prompt for input.
     */
    private final Prompter prompter;

    /**
     * Component used for custom or default version policy
     */
    private final Map<String, VersionPolicy> versionPolicies;

    /**
     * Whether to convert to a snapshot or a release.
     */
    private final boolean convertToSnapshot;

    /**
     * Whether to convert to a snapshot or a release.
     */
    private final boolean convertToBranch;

    public AbstractMapVersionsPhase(
            ScmRepositoryConfigurator scmRepositoryConfigurator,
            Prompter prompter,
            Map<String, VersionPolicy> versionPolicies,
            boolean convertToSnapshot,
            boolean convertToBranch) {
        this.scmRepositoryConfigurator = requireNonNull(scmRepositoryConfigurator);
        this.prompter = requireNonNull(prompter);
        this.versionPolicies = requireNonNull(versionPolicies);
        this.convertToSnapshot = convertToSnapshot;
        this.convertToBranch = convertToBranch;
    }

    @Override
    public ReleaseResult execute(
            ReleaseDescriptor releaseDescriptor,
            ReleaseEnvironment releaseEnvironment,
            List<MavenProject> reactorProjects)
            throws ReleaseExecutionException {
        ReleaseResult result = new ReleaseResult();

        MavenProject rootProject = ReleaseUtil.getRootProject(reactorProjects);

        if (releaseDescriptor.isAutoVersionSubmodules() && ArtifactUtils.isSnapshot(rootProject.getVersion())) {
            // get the root project
            MavenProject project = rootProject;

            String projectId = ArtifactUtils.versionlessKey(project.getGroupId(), project.getArtifactId());

            String nextVersion = resolveNextVersion(project, projectId, releaseDescriptor, releaseEnvironment);

            if (!convertToSnapshot) {
                releaseDescriptor.addReleaseVersion(projectId, nextVersion);
            } else if (releaseDescriptor.isBranchCreation() && convertToBranch) {
                releaseDescriptor.addReleaseVersion(projectId, nextVersion);
            } else {
                releaseDescriptor.addDevelopmentVersion(projectId, nextVersion);
            }

            for (MavenProject subProject : reactorProjects) {
                String subProjectId = ArtifactUtils.versionlessKey(subProject.getGroupId(), subProject.getArtifactId());

                if (convertToSnapshot) {
                    String subProjectNextVersion = releaseDescriptor.getProjectDevelopmentVersion(subProjectId);
                    String v;
                    if (subProjectNextVersion != null) {
                        v = subProjectNextVersion;
                    } else if (ArtifactUtils.isSnapshot(subProject.getVersion())) {
                        v = nextVersion;
                    } else {
                        v = subProject.getVersion();
                    }

                    if (releaseDescriptor.isBranchCreation() && convertToBranch) {
                        releaseDescriptor.addReleaseVersion(subProjectId, v);
                    } else {
                        releaseDescriptor.addDevelopmentVersion(subProjectId, v);
                    }
                } else {
                    String subProjectNextVersion = releaseDescriptor.getProjectReleaseVersion(subProjectId);
                    if (subProjectNextVersion != null) {
                        releaseDescriptor.addReleaseVersion(subProjectId, subProjectNextVersion);
                    } else {
                        releaseDescriptor.addReleaseVersion(subProjectId, nextVersion);
                    }
                }
            }
        } else {
            for (MavenProject project : reactorProjects) {
                String projectId = ArtifactUtils.versionlessKey(project.getGroupId(), project.getArtifactId());

                String nextVersion = resolveNextVersion(project, projectId, releaseDescriptor, releaseEnvironment);

                if (!convertToSnapshot) {
                    releaseDescriptor.addReleaseVersion(projectId, nextVersion);
                } else if (releaseDescriptor.isBranchCreation() && convertToBranch) {
                    releaseDescriptor.addReleaseVersion(projectId, nextVersion);
                } else {
                    releaseDescriptor.addDevelopmentVersion(projectId, nextVersion);
                }
            }
        }

        result.setResultCode(ReleaseResult.SUCCESS);

        return result;
    }

    private String resolveNextVersion(
            MavenProject project,
            String projectId,
            ReleaseDescriptor releaseDescriptor,
            ReleaseEnvironment releaseEnvironment)
            throws ReleaseExecutionException {
        String defaultVersion;
        if (convertToBranch) {
            // no branch modification
            if (!(releaseDescriptor.isUpdateBranchVersions()
                    && (ArtifactUtils.isSnapshot(project.getVersion())
                            || releaseDescriptor.isUpdateVersionsToSnapshot()))) {
                return project.getVersion();
            }

            defaultVersion = getReleaseVersion(projectId, releaseDescriptor);
        } else if (!convertToSnapshot) // map-release-version
        {
            defaultVersion = getReleaseVersion(projectId, releaseDescriptor);
        } else if (releaseDescriptor.isBranchCreation()) {
            // no working copy modification
            if (!(ArtifactUtils.isSnapshot(project.getVersion()) && releaseDescriptor.isUpdateWorkingCopyVersions())) {
                return project.getVersion();
            }

            defaultVersion = getDevelopmentVersion(projectId, releaseDescriptor);
        } else {
            // no working copy modification
            if (!(releaseDescriptor.isUpdateWorkingCopyVersions())) {
                return project.getVersion();
            }

            defaultVersion = getDevelopmentVersion(projectId, releaseDescriptor);
        }
        // @todo validate default version, maybe with DefaultArtifactVersion

        String suggestedVersion = null;
        String nextVersion = defaultVersion;
        String messageFormat = null;
        try {
            while (nextVersion == null || ArtifactUtils.isSnapshot(nextVersion) != convertToSnapshot) {
                if (suggestedVersion == null) {
                    String baseVersion = null;
                    if (convertToSnapshot) {
                        baseVersion = getReleaseVersion(projectId, releaseDescriptor);
                    }
                    // unspecified and unmapped version, so use project version
                    if (baseVersion == null) {
                        baseVersion = project.getVersion();
                    }

                    try {
                        try {
                            suggestedVersion =
                                    resolveSuggestedVersion(baseVersion, releaseDescriptor, releaseEnvironment);
                        } catch (VersionParseException e) {
                            if (releaseDescriptor.isInteractive()) {
                                suggestedVersion =
                                        resolveSuggestedVersion("1.0", releaseDescriptor, releaseEnvironment);
                            } else {
                                throw new ReleaseExecutionException(
                                        "Error parsing version, cannot determine next " + "version: " + e.getMessage(),
                                        e);
                            }
                        }
                    } catch (PolicyException | VersionParseException e) {
                        throw new ReleaseExecutionException(e.getMessage(), e);
                    }
                }

                if (releaseDescriptor.isInteractive()) {
                    if (messageFormat == null) {
                        messageFormat = "What is the " + getContextString(releaseDescriptor) + " version for \"%s\"? ("
                                + buffer().project("%s") + ")";
                    }
                    String message = String.format(messageFormat, project.getName(), project.getArtifactId());
                    nextVersion = prompter.prompt(message, suggestedVersion);

                    // @todo validate next version, maybe with DefaultArtifactVersion
                } else if (defaultVersion == null) {
                    nextVersion = suggestedVersion;
                } else if (convertToSnapshot) {
                    throw new ReleaseExecutionException(defaultVersion + " is invalid, expected a snapshot");
                } else {
                    throw new ReleaseExecutionException(defaultVersion + " is invalid, expected a non-snapshot");
                }
            }
        } catch (PrompterException e) {
            throw new ReleaseExecutionException("Error reading version from input handler: " + e.getMessage(), e);
        }
        return nextVersion;
    }

    private String getContextString(ReleaseDescriptor releaseDescriptor) {
        if (convertToBranch) {
            return "branch";
        }
        if (!convertToSnapshot) {
            return "release";
        }
        if (releaseDescriptor.isBranchCreation()) {
            return "new working copy";
        }
        return "new development";
    }

    private String resolveSuggestedVersion(
            String baseVersion, ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment)
            throws PolicyException, VersionParseException {
        String policyId = releaseDescriptor.getProjectVersionPolicyId();
        VersionPolicy policy = versionPolicies.get(policyId);
        if (policy == null) {
            throw new PolicyException("Policy '" + policyId + "' is unknown, available: " + versionPolicies.keySet());
        }

        VersionPolicyRequest request = new VersionPolicyRequest().setVersion(baseVersion);

        if (releaseDescriptor.getProjectVersionPolicyConfig() != null) {
            request.setConfig(releaseDescriptor.getProjectVersionPolicyConfig().toString());
        }
        request.setWorkingDirectory(releaseDescriptor.getWorkingDirectory());

        if (scmRepositoryConfigurator != null && releaseDescriptor.getScmSourceUrl() != null) {
            try {
                ScmRepository repository = scmRepositoryConfigurator.getConfiguredRepository(
                        releaseDescriptor, releaseEnvironment.getSettings());

                ScmProvider provider = scmRepositoryConfigurator.getRepositoryProvider(repository);

                request.setScmRepository(repository);
                request.setScmProvider(provider);
            } catch (ScmRepositoryException | NoSuchScmProviderException e) {
                Logger logger = getLogger();
                if (logger.isWarnEnabled()) {
                    logger.warn("Next Version will NOT be based on the version control: {}", e.getMessage());
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.warn("Next Version will NOT be based on the version control", e);
                    }
                }
            }
        }
        return convertToSnapshot
                ? policy.getDevelopmentVersion(request).getVersion()
                : policy.getReleaseVersion(request).getVersion();
    }

    private String getDevelopmentVersion(String projectId, ReleaseDescriptor releaseDescriptor) {
        String projectVersion = releaseDescriptor.getProjectDevelopmentVersion(projectId);

        if (projectVersion == null || projectVersion.isEmpty()) {
            projectVersion = releaseDescriptor.getDefaultDevelopmentVersion();
        }

        if (projectVersion == null || projectVersion.isEmpty()) {
            return null;
        }

        return projectVersion;
    }

    private String getReleaseVersion(String projectId, ReleaseDescriptor releaseDescriptor) {
        String projectVersion = releaseDescriptor.getProjectReleaseVersion(projectId);

        if (projectVersion == null || projectVersion.isEmpty()) {
            projectVersion = releaseDescriptor.getDefaultReleaseVersion();
        }

        if (projectVersion == null || projectVersion.isEmpty()) {
            return null;
        }

        return projectVersion;
    }

    @Override
    public ReleaseResult simulate(
            ReleaseDescriptor releaseDescriptor,
            ReleaseEnvironment releaseEnvironment,
            List<MavenProject> reactorProjects)
            throws ReleaseExecutionException {
        ReleaseResult result = new ReleaseResult();

        // It makes no modifications, so simulate is the same as execute
        execute(releaseDescriptor, releaseEnvironment, reactorProjects);

        result.setResultCode(ReleaseResult.SUCCESS);

        return result;
    }
}
