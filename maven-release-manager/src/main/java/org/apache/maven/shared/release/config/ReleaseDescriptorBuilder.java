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
package org.apache.maven.shared.release.config;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.model.Scm;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>ReleaseDescriptorBuilder class.</p>
 *
 * @author Robert Scholte
 * @since 3.0.0-M5
 */
public class ReleaseDescriptorBuilder {
    private static final Pattern PROPERTY_PATTERN = Pattern.compile("\\$\\{[^}]+}");

    private final Logger logger;

    /**
     * Hides inner logic of the release descriptor
     *
     * @author Robert Scholte
     */
    public static final class BuilderReleaseDescriptor extends ModelloReleaseDescriptor implements ReleaseDescriptor {
        private BuilderReleaseDescriptor() {}
    }

    private final BuilderReleaseDescriptor releaseDescriptor;

    /**
     * <p>Constructor for ReleaseDescriptorBuilder.</p>
     */
    public ReleaseDescriptorBuilder() {
        this(LoggerFactory.getLogger(ReleaseDescriptorBuilder.class));
    }

    /**
     * Constructor for testing purpose.
     */
    ReleaseDescriptorBuilder(Logger logger) {
        this.releaseDescriptor = new BuilderReleaseDescriptor();
        this.releaseDescriptor.setLineSeparator(ReleaseUtil.LS);
        this.logger = logger;
    }

    /**
     * <p>addCheckModificationExclude.</p>
     *
     * @param string a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder addCheckModificationExclude(String string) {
        releaseDescriptor.addCheckModificationExclude(string);
        return this;
    }

    /**
     * <p>setActivateProfiles.</p>
     *
     * @param profiles a {@link java.util.List} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setActivateProfiles(List<String> profiles) {
        releaseDescriptor.setActivateProfiles(profiles);
        return this;
    }

    /**
     * <p>setAddSchema.</p>
     *
     * @param addSchema a boolean
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setAddSchema(boolean addSchema) {
        releaseDescriptor.setAddSchema(addSchema);
        return this;
    }

    /**
     * <p>setAdditionalArguments.</p>
     *
     * @param additionalArguments a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setAdditionalArguments(String additionalArguments) {
        if (additionalArguments != null) {
            Matcher matcher = PROPERTY_PATTERN.matcher(additionalArguments);
            StringBuffer buf = new StringBuffer();
            while (matcher.find()) {
                matcher.appendReplacement(buf, "");
                logger.warn("arguments parameter contains unresolved property: '{}'", matcher.group());
            }
            matcher.appendTail(buf);

            releaseDescriptor.setAdditionalArguments(buf.toString());
        } else {
            releaseDescriptor.setAdditionalArguments(null);
        }
        return this;
    }

    /**
     * <p>setAllowTimestampedSnapshots.</p>
     *
     * @param allowTimestampedSnapshots a boolean
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setAllowTimestampedSnapshots(boolean allowTimestampedSnapshots) {
        releaseDescriptor.setAllowTimestampedSnapshots(allowTimestampedSnapshots);
        return this;
    }

    /**
     * <p>setAutoVersionSubmodules.</p>
     *
     * @param autoVersionSubmodules a boolean
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setAutoVersionSubmodules(boolean autoVersionSubmodules) {
        releaseDescriptor.setAutoVersionSubmodules(autoVersionSubmodules);
        return this;
    }

    /**
     * <p>setBranchCreation.</p>
     *
     * @param branchCreation a boolean
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setBranchCreation(boolean branchCreation) {
        releaseDescriptor.setBranchCreation(branchCreation);
        return this;
    }

    /**
     * <p>setCheckModificationExcludes.</p>
     *
     * @param checkModificationExcludes a {@link java.util.List} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setCheckModificationExcludes(List<String> checkModificationExcludes) {
        releaseDescriptor.setCheckModificationExcludes(checkModificationExcludes);
        return this;
    }

    /**
     * <p>setCheckoutDirectory.</p>
     *
     * @param checkoutDirectory a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setCheckoutDirectory(String checkoutDirectory) {
        releaseDescriptor.setCheckoutDirectory(checkoutDirectory);
        return this;
    }

    /**
     * <p>setCommitByProject.</p>
     *
     * @param commitByProject a boolean
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setCommitByProject(boolean commitByProject) {
        releaseDescriptor.setCommitByProject(commitByProject);
        return this;
    }

    /**
     * <p>setCompletedPhase.</p>
     *
     * @param completedPhase a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setCompletedPhase(String completedPhase) {
        releaseDescriptor.setCompletedPhase(completedPhase);
        return this;
    }

    /**
     * <p>setCompletionGoals.</p>
     *
     * @param completionGoals a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setCompletionGoals(String completionGoals) {
        releaseDescriptor.setCompletionGoals(completionGoals);
        return this;
    }

    /**
     * <p>setDefaultDevelopmentVersion.</p>
     *
     * @param defaultDevelopmentVersion a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setDefaultDevelopmentVersion(String defaultDevelopmentVersion) {
        releaseDescriptor.setDefaultDevelopmentVersion(defaultDevelopmentVersion);
        return this;
    }

    /**
     * <p>setDefaultReleaseVersion.</p>
     *
     * @param defaultReleaseVersion a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setDefaultReleaseVersion(String defaultReleaseVersion) {
        releaseDescriptor.setDefaultReleaseVersion(defaultReleaseVersion);
        return this;
    }

    /**
     * <p>setDescription.</p>
     *
     * @param description a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setDescription(String description) {
        releaseDescriptor.setDescription(description);
        return this;
    }

    /**
     * <p>setGenerateReleasePoms.</p>
     *
     * @param generateReleasePoms a boolean
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setGenerateReleasePoms(boolean generateReleasePoms) {
        releaseDescriptor.setGenerateReleasePoms(generateReleasePoms);
        return this;
    }

    /**
     * <p>setInteractive.</p>
     *
     * @param interactive a boolean
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setInteractive(boolean interactive) {
        releaseDescriptor.setInteractive(interactive);
        return this;
    }

    /**
     * <p>setLineSeparator.</p>
     *
     * @param ls a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setLineSeparator(String ls) {
        releaseDescriptor.setLineSeparator(ls);
        return this;
    }

    /**
     * <p>setLocalCheckout.</p>
     *
     * @param localCheckout a boolean
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setLocalCheckout(boolean localCheckout) {
        releaseDescriptor.setLocalCheckout(localCheckout);
        return this;
    }

    /**
     * <p>setModelEncoding.</p>
     *
     * @param modelEncoding a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setModelEncoding(String modelEncoding) {
        releaseDescriptor.setModelEncoding(modelEncoding);
        return this;
    }

    /**
     * <p>setName.</p>
     *
     * @param name a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setName(String name) {
        releaseDescriptor.setName(name);
        return this;
    }

    /**
     * <p>setPerformGoals.</p>
     *
     * @param performGoals a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setPerformGoals(String performGoals) {
        releaseDescriptor.setPerformGoals(performGoals);
        return this;
    }

    /**
     * <p>setPomFileName.</p>
     *
     * @param pomFileName a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setPomFileName(String pomFileName) {
        releaseDescriptor.setPomFileName(pomFileName);
        return this;
    }

    /**
     * <p>setPreparationGoals.</p>
     *
     * @param preparationGoals a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setPreparationGoals(String preparationGoals) {
        releaseDescriptor.setPreparationGoals(preparationGoals);
        return this;
    }

    /**
     * <p>setProjectNamingPolicyId.</p>
     *
     * @param projectNamingPolicyId a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setProjectNamingPolicyId(String projectNamingPolicyId) {
        releaseDescriptor.setProjectNamingPolicyId(projectNamingPolicyId);
        return this;
    }

    /**
     * <p>setProjectVersionPolicyId.</p>
     *
     * @param projectVersionPolicyId a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setProjectVersionPolicyId(String projectVersionPolicyId) {
        releaseDescriptor.setProjectVersionPolicyId(projectVersionPolicyId);
        return this;
    }

    /**
     * <p>setProjectVersionPolicyConfig.</p>
     *
     * @param setProjectVersionPolicyConfig a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setProjectVersionPolicyConfig(String setProjectVersionPolicyConfig) {
        releaseDescriptor.setProjectVersionPolicyConfig(setProjectVersionPolicyConfig);
        return this;
    }

    /**
     * <p>setPushChanges.</p>
     *
     * @param pushChanges a boolean
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setPushChanges(boolean pushChanges) {
        releaseDescriptor.setPushChanges(pushChanges);
        return this;
    }

    /**
     * <p>setWorkItem.</p>
     *
     * @param workItem a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setWorkItem(String workItem) {
        releaseDescriptor.setWorkItem(workItem);
        return this;
    }

    /**
     * <p>setReleaseStrategyId.</p>
     *
     * @param releaseStrategyId a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setReleaseStrategyId(String releaseStrategyId) {
        releaseDescriptor.setReleaseStrategyId(releaseStrategyId);
        return this;
    }

    /**
     * <p>setRemoteTagging.</p>
     *
     * @param remoteTagging a boolean
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setRemoteTagging(boolean remoteTagging) {
        releaseDescriptor.setRemoteTagging(remoteTagging);
        return this;
    }

    /**
     * <p>setScmBranchBase.</p>
     *
     * @param scmBranchBase a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setScmBranchBase(String scmBranchBase) {
        releaseDescriptor.setScmBranchBase(scmBranchBase);
        return this;
    }

    /**
     * <p>setScmCommentPrefix.</p>
     *
     * @param scmCommentPrefix a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setScmCommentPrefix(String scmCommentPrefix) {
        releaseDescriptor.setScmCommentPrefix(scmCommentPrefix);
        return this;
    }

    /**
     * <p>setScmShallowClone.</p>
     *
     * @param scmShallowClone a boolean
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     * @since 3.0.0-M6
     */
    public ReleaseDescriptorBuilder setScmShallowClone(boolean scmShallowClone) {
        releaseDescriptor.setScmShallowClone(scmShallowClone);
        return this;
    }

    /**
     * <p>setScmReleaseCommitComment.</p>
     *
     * @param scmReleaseCommitComment a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     * @since 3.0.0-M1
     */
    public ReleaseDescriptorBuilder setScmReleaseCommitComment(String scmReleaseCommitComment) {
        releaseDescriptor.setScmReleaseCommitComment(scmReleaseCommitComment);
        return this;
    }

    /**
     * <p>setScmDevelopmentCommitComment.</p>
     *
     * @param scmDevelopmentCommitComment a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     * @since 3.0.0-M1
     */
    public ReleaseDescriptorBuilder setScmDevelopmentCommitComment(String scmDevelopmentCommitComment) {
        releaseDescriptor.setScmDevelopmentCommitComment(scmDevelopmentCommitComment);
        return this;
    }

    /**
     * <p>setScmBranchCommitComment.</p>
     *
     * @param scmBranchCommitComment a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     * @since 3.0.0-M1
     */
    public ReleaseDescriptorBuilder setScmBranchCommitComment(String scmBranchCommitComment) {
        releaseDescriptor.setScmBranchCommitComment(scmBranchCommitComment);
        return this;
    }

    /**
     * <p>setScmRollbackCommitComment.</p>
     *
     * @param scmRollbackCommitComment a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     * @since 3.0.0-M1
     */
    public ReleaseDescriptorBuilder setScmRollbackCommitComment(String scmRollbackCommitComment) {
        releaseDescriptor.setScmRollbackCommitComment(scmRollbackCommitComment);
        return this;
    }

    /**
     * <p>setScmId.</p>
     *
     * @param scmId a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setScmId(String scmId) {
        releaseDescriptor.setScmId(scmId);
        return this;
    }

    /**
     * <p>setScmPassword.</p>
     *
     * @param scmPassword a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setScmPassword(String scmPassword) {
        releaseDescriptor.setScmPassword(scmPassword);
        return this;
    }

    /**
     * <p>setScmPrivateKey.</p>
     *
     * @param scmPrivateKey a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setScmPrivateKey(String scmPrivateKey) {
        releaseDescriptor.setScmPrivateKey(scmPrivateKey);
        return this;
    }

    /**
     * <p>setScmPrivateKeyPassPhrase.</p>
     *
     * @param scmPrivateKeyPassPhrase a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setScmPrivateKeyPassPhrase(String scmPrivateKeyPassPhrase) {
        releaseDescriptor.setScmPrivateKeyPassPhrase(scmPrivateKeyPassPhrase);
        return this;
    }

    /**
     * <p>setScmRelativePathProjectDirectory.</p>
     *
     * @param scmRelativePathProjectDirectory a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setScmRelativePathProjectDirectory(String scmRelativePathProjectDirectory) {
        releaseDescriptor.setScmRelativePathProjectDirectory(scmRelativePathProjectDirectory);
        return this;
    }

    /**
     * <p>setScmReleaseLabel.</p>
     *
     * @param scmReleaseLabel a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setScmReleaseLabel(String scmReleaseLabel) {
        releaseDescriptor.setScmReleaseLabel(scmReleaseLabel);
        return this;
    }

    /**
     * <p>setScmReleasedPomRevision.</p>
     *
     * @param scmReleasedPomRevision a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setScmReleasedPomRevision(String scmReleasedPomRevision) {
        releaseDescriptor.setScmReleasedPomRevision(scmReleasedPomRevision);
        return this;
    }

    /**
     * <p>setScmSourceUrl.</p>
     *
     * @param scmSourceUrl a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setScmSourceUrl(String scmSourceUrl) {
        releaseDescriptor.setScmSourceUrl(scmSourceUrl);
        return this;
    }

    /**
     * <p>setScmTagBase.</p>
     *
     * @param scmTagBase a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setScmTagBase(String scmTagBase) {
        releaseDescriptor.setScmTagBase(scmTagBase);
        return this;
    }

    /**
     * <p>setScmTagNameFormat.</p>
     *
     * @param scmTagNameFormat a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setScmTagNameFormat(String scmTagNameFormat) {
        releaseDescriptor.setScmTagNameFormat(scmTagNameFormat);
        return this;
    }

    /**
     * <p>setScmSignTags.</p>
     *
     * @param signTags a boolean
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setScmSignTags(boolean signTags) {
        releaseDescriptor.setScmSignTags(signTags);
        return this;
    }

    /**
     * <p>setScmUseEditMode.</p>
     *
     * @param scmUseEditMode a boolean
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setScmUseEditMode(boolean scmUseEditMode) {
        releaseDescriptor.setScmUseEditMode(scmUseEditMode);
        return this;
    }

    /**
     * <p>setScmUsername.</p>
     *
     * @param scmUsername a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setScmUsername(String scmUsername) {
        releaseDescriptor.setScmUsername(scmUsername);
        return this;
    }

    /**
     * <p>setSnapshotReleasePluginAllowed.</p>
     *
     * @param snapshotReleasePluginAllowed a boolean
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setSnapshotReleasePluginAllowed(boolean snapshotReleasePluginAllowed) {
        releaseDescriptor.setSnapshotReleasePluginAllowed(snapshotReleasePluginAllowed);
        return this;
    }

    /**
     * <p>setSuppressCommitBeforeTagOrBranch.</p>
     *
     * @param suppressCommitBeforeTagOrBranch a boolean
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setSuppressCommitBeforeTagOrBranch(boolean suppressCommitBeforeTagOrBranch) {
        releaseDescriptor.setSuppressCommitBeforeTagOrBranch(suppressCommitBeforeTagOrBranch);
        return this;
    }

    /**
     * <p>setUpdateBranchVersions.</p>
     *
     * @param updateBranchVersions a boolean
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setUpdateBranchVersions(boolean updateBranchVersions) {
        releaseDescriptor.setUpdateBranchVersions(updateBranchVersions);
        return this;
    }

    /**
     * <p>setUpdateDependencies.</p>
     *
     * @param updateDependencies a boolean
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setUpdateDependencies(boolean updateDependencies) {
        releaseDescriptor.setUpdateDependencies(updateDependencies);
        return this;
    }

    /**
     * <p>setUpdateVersionsToSnapshot.</p>
     *
     * @param updateVersionsToSnapshot a boolean
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setUpdateVersionsToSnapshot(boolean updateVersionsToSnapshot) {
        releaseDescriptor.setUpdateVersionsToSnapshot(updateVersionsToSnapshot);
        return this;
    }

    /**
     * <p>setUpdateWorkingCopyVersions.</p>
     *
     * @param updateWorkingCopyVersions a boolean
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setUpdateWorkingCopyVersions(boolean updateWorkingCopyVersions) {
        releaseDescriptor.setUpdateWorkingCopyVersions(updateWorkingCopyVersions);
        return this;
    }

    /**
     * <p>setUseReleaseProfile.</p>
     *
     * @param useReleaseProfile a boolean
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setUseReleaseProfile(boolean useReleaseProfile) {
        releaseDescriptor.setUseReleaseProfile(useReleaseProfile);
        return this;
    }

    /**
     * <p>setWaitBeforeTagging.</p>
     *
     * @param waitBeforeTagging a int
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setWaitBeforeTagging(int waitBeforeTagging) {
        releaseDescriptor.setWaitBeforeTagging(waitBeforeTagging);
        return this;
    }

    /**
     * <p>setWorkingDirectory.</p>
     *
     * @param workingDirectory a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setWorkingDirectory(String workingDirectory) {
        releaseDescriptor.setWorkingDirectory(workingDirectory);
        return this;
    }

    /**
     * <p>addReleaseVersion.</p>
     *
     * @param key   a {@link java.lang.String} object
     * @param value a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder addReleaseVersion(String key, String value) {
        releaseDescriptor.addReleaseVersion(key, value);
        return this;
    }

    /**
     * <p>addDevelopmentVersion.</p>
     *
     * @param key   a {@link java.lang.String} object
     * @param value a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder addDevelopmentVersion(String key, String value) {
        releaseDescriptor.addDevelopmentVersion(key, value);
        return this;
    }

    /**
     * <p>addOriginalScmInfo.</p>
     *
     * @param key   a {@link java.lang.String} object
     * @param value a {@link org.apache.maven.model.Scm} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder addOriginalScmInfo(String key, Scm value) {
        releaseDescriptor.addOriginalScmInfo(key, value);
        return this;
    }

    /**
     * <p>putOriginalVersion.</p>
     *
     * @param projectKey a {@link java.lang.String} object
     * @param version    a {@link java.lang.String} object
     */
    public void putOriginalVersion(String projectKey, String version) {
        releaseDescriptor.addOriginalVersion(projectKey, version);
    }

    /**
     * <p>addDependencyOriginalVersion.</p>
     *
     * @param dependencyKey a {@link java.lang.String} object
     * @param version       a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder addDependencyOriginalVersion(String dependencyKey, String version) {
        releaseDescriptor.addDependencyOriginalVersion(dependencyKey, version);
        return this;
    }

    /**
     * <p>addDependencyReleaseVersion.</p>
     *
     * @param dependencyKey a {@link java.lang.String} object
     * @param version       a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder addDependencyReleaseVersion(String dependencyKey, String version) {
        releaseDescriptor.addDependencyReleaseVersion(dependencyKey, version);
        return this;
    }

    /**
     * <p>addDependencyDevelopmentVersion.</p>
     *
     * @param dependencyKey a {@link java.lang.String} object
     * @param version       a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder addDependencyDevelopmentVersion(String dependencyKey, String version) {
        releaseDescriptor.addDependencyDevelopmentVersion(dependencyKey, version);
        return this;
    }

    /**
     * <p>setAutoResolveSnapshots.</p>
     *
     * @param autoResolveSnapshots a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setAutoResolveSnapshots(String autoResolveSnapshots) {
        releaseDescriptor.setAutoResolveSnapshots(autoResolveSnapshots);
        return this;
    }

    /**
     * <p>setPinExternals.</p>
     *
     * @param pinExternals a boolean
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptorBuilder} object
     */
    public ReleaseDescriptorBuilder setPinExternals(boolean pinExternals) {
        releaseDescriptor.setPinExternals(pinExternals);
        return this;
    }

    public BuilderReleaseDescriptor build() {
        return releaseDescriptor;
    }
}
