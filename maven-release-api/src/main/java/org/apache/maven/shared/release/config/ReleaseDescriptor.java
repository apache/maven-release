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

import org.apache.maven.model.Scm;

/**
 * <p>ReleaseDescriptor interface.</p>
 *
 * @author Robert Scholte
 */
public interface ReleaseDescriptor {
    /**
     * Get if updateDependencies is false, dependencies version won't be updated to the next development version.
     *
     * @return boolean
     */
    boolean isUpdateDependencies();

    /**
     * Get whether to use the release profile that adds sources and javadocs to the released artifact, if appropriate.
     * If set to true, this will set the property "performRelease" to true.
     *
     * @return boolean
     */
    boolean isUseReleaseProfile();

    /**
     * Get whether to use the parent pom version for submodule versions.
     *
     * @return boolean
     */
    boolean isAutoVersionSubmodules();

    /**
     * Get whether a SNAPSHOT of the release plugin is allowed.
     *
     * @return boolean
     */
    boolean isSnapshotReleasePluginAllowed();

    /**
     * Get the commits must be done by modules or not. Set it to true in case of flat directory structure.
     *
     * @return boolean
     */
    boolean isCommitByProject();

    /**
     * Get whether to create a branch instead of do a release.
     *
     * @return boolean
     */
    boolean isBranchCreation();

    /**
     * Get whether to update branch POM versions.
     *
     * @return boolean
     */
    boolean isUpdateBranchVersions();

    /**
     * Get whether to update working copy POM versions.
     *
     * @return boolean
     */
    boolean isUpdateWorkingCopyVersions();

    /**
     * Get whether to suppress a commit of changes to the working copy before a tag or branch is created.
     *
     * @return boolean
     */
    boolean isSuppressCommitBeforeTagOrBranch();

    /**
     * Get should timestamped SNAPSHOT dependencies be allowed? Default is to fail when any SNAPSHOT dependency is
     * found.
     *
     * @return boolean
     */
    boolean isAllowTimestampedSnapshots();

    /**
     * Get whether to update branch versions to SNAPSHOT.
     *
     * @return boolean
     */
    boolean isUpdateVersionsToSnapshot();

    /**
     * Get nOTE : currently only implemented with svn scm. Enable a workaround to prevent issue due to svn client &gt;
     * 1.5.0 (https://issues.apache.org/jira/browse/SCM-406).
     *
     * @return boolean
     */
    boolean isRemoteTagging();

    /**
     * Get if the scm provider should sign the tag. NOTE: currently only implemented with git-exe.
     *
     * @return boolean true if SCM tag should be signed
     */
    boolean isScmSignTags();

    /**
     * Get if the scm provider should use local checkouts via file://${basedir} instead of doing a clean checkout over
     * the network. This is very helpful for releasing large projects!
     *
     * @return boolean
     */
    boolean isLocalCheckout();

    /**
     * Get should distributed changes be pushed to the central repository? For many distributed SCMs like Git, a change
     * like a commit is only stored in your local copy of the repository. Pushing the change allows your to more easily
     * share it with other users.
     *
     * @return boolean
     */
    boolean isPushChanges();

    /**
     * Get default version to use for new working copy.
     *
     * Some SCMs may require a Work Item or a Task to allow the
     * changes to be pushed or delivered.
     * This field allows you to specify that Work Item
     * or Task. It is optional, and only relevant if pushChanges is true.
     *
     * @return String
     */
    String getWorkItem();

    /**
     * Get default version to use for new working copy.
     *
     * @return String
     */
    String getDefaultDevelopmentVersion();

    /**
     * Get relative path of the project returned by the checkout command.
     *
     * @return String
     */
    String getScmRelativePathProjectDirectory();

    /**
     * Get the directory where the tag will be checked out.
     *
     * @return String
     */
    String getCheckoutDirectory();

    /**
     * Get the goals to execute in perform phase for the release.
     *
     * @return String
     */
    String getPerformGoals();

    /**
     * Get default version to use for the tagged release or the new branch.
     *
     * @return String
     */
    String getDefaultReleaseVersion();

    /**
     * Get nOTE : currently only implemented with svn scm. It contains the revision of the committed released pom to
     * remotely tag the source code with this revision.
     *
     * @return String
     */
    String getScmReleasedPomRevision();

    /**
     * Get whether to add the model schema to the top of the rewritten POM if it wasn't there already. If
     * <code>false</code> then the root element will remain untouched.
     *
     * @return boolean
     */
    boolean isAddSchema();

    /**
     * Get whether to generate release POMs.
     *
     * @return boolean
     */
    boolean isGenerateReleasePoms();

    /**
     * Get whether the release process is interactive and the release manager should be prompted to confirm values, or
     * whether the defaults are used regardless.
     *
     * @return boolean
     */
    boolean isInteractive();

    /**
     * Get whether to use edit mode when making SCM modifications. This setting is disregarded if the SCM does not
     * support edit mode, or if edit mode is compulsory for the given SCM.
     *
     * @return boolean
     */
    boolean isScmUseEditMode();

    /**
     * <p>getActivateProfiles.</p>
     *
     * @return list of profiles to activate
     */
    List<String> getActivateProfiles();

    /**
     * Get the last completed phase.
     *
     * @return String
     */
    String getCompletedPhase();

    /**
     * Method getCheckModificationExcludes.
     *
     * @return List
     */
    List<String> getCheckModificationExcludes();

    /**
     * Get additional arguments to pass to any executed Maven process.
     *
     * @return String
     */
    String getAdditionalArguments();

    /**
     * Get the goals to execute in preparation for the release.
     *
     * @return String
     */
    String getPreparationGoals();

    /**
     * Get the goals to execute in on completion of preparation for the release.
     *
     * @return String
     */
    String getCompletionGoals();

    /**
     * Get the file name of the POM to pass to any executed Maven process.
     *
     * @return String
     */
    String getPomFileName();

    /**
     * Get the prefix of SCM modification messages.
     *
     * @return String
     */
    String getScmCommentPrefix();

    /**
     * Get whether to use a shallow clone with no history or a full clone containing the full history during the
     * release.
     *
     * @return boolean
     * @since 3.0.0-M6
     */
    boolean isScmShallowClone();

    /**
     * Get the SCM commit comment when setting pom.xml to release.
     *
     * @return String
     * @since 3.0.0-M1
     */
    String getScmReleaseCommitComment();

    /**
     * Get the SCM commit comment when setting pom.xml back to development.
     *
     * @return String
     * @since 3.0.0-M1
     */
    String getScmDevelopmentCommitComment();

    /**
     * Get the SCM commit comment when branching.
     *
     * @return String
     * @since 3.0.0-M1
     */
    String getScmBranchCommitComment();

    /**
     * Get the SCM commit comment when rolling back.
     *
     * @return String
     * @since 3.0.0-M1
     */
    String getScmRollbackCommitComment();

    /**
     * Get pass phrase for the private key.
     *
     * @return String
     */
    String getScmPrivateKeyPassPhrase();

    /**
     * Get the password for the user interacting with the scm.
     *
     * @return String
     */
    String getScmPassword();

    /**
     * Get private key for an SSH based SCM repository.
     *
     * @return String
     */
    String getScmPrivateKey();

    /**
     * Get tag or branch name: the identifier for the tag/branch. Example: maven-release-plugin-2.0.
     *
     * @return String
     */
    String getScmReleaseLabel();

    /**
     * Get where you are going to put your tagged sources Example https://svn.apache.org/repos/asf/maven/plugins/tags.
     *
     * @return String
     */
    String getScmTagBase();

    /**
     * Get where you are going to put your branched sources Example
     * https://svn.apache.org/repos/asf/maven/plugins/branches.
     *
     * @return String
     */
    String getScmBranchBase();

    /**
     * Get the id which can be used to get the (optionally encrypted) credentials with the given id from the {@code settings.xml}.
     * Explicit credentials in {@link #getScmUsername()}, {@link #getScmPassword()}, {@link #getScmPrivateKey()} or
     * {@link #getScmPrivateKeyPassPhrase()} always take precedence, though.
     *
     * @return the server id of a server in {@code settings.xml}
     */
    String getScmId();

    /**
     * Get this is a MavenSCM of where you're going to get the sources to make the release with. Example:
     * scm:svn:https://svn.apache.org/repos/asf/maven/plugins/trunk/maven-release-plugin.
     *
     * @return String
     */
    String getScmSourceUrl();

    /**
     * Get the user name to interact with the scm.
     *
     * @return String
     */
    String getScmUsername();

    /**
     * Get wait the specified number of seconds before creating a tag.
     *
     * @return int
     */
    int getWaitBeforeTagging();

    /**
     * Get the directory where the release is performed.
     *
     * @return String
     */
    String getWorkingDirectory();

    /**
     * Get specifies the format for generating a tag name. Property expansion is used with the optional prefix of
     * project, where properties are delimited with @{ and }.
     *
     * @return String
     */
    String getScmTagNameFormat();

    /**
     * Get the role-hint for the NamingPolicy implementation used to calculate the project branch and tag names.
     *
     * @return String
     */
    String getProjectNamingPolicyId();

    /**
     * Get the role-hint for the VersionPolicy implementation used to calculate the project versions.
     *
     * @return String
     */
    String getProjectVersionPolicyId();

    /**
     * Get the (optional) config for the VersionPolicy implementation used to calculate the project versions.
     *
     * @return The parsed XML of the provided config (an instance of PlexusConfiguration) or null.
     */
    Object getProjectVersionPolicyConfig();

    /**
     * Get the role-hint for the release Strategy implementation.
     *
     * @return String
     */
    String getReleaseStrategyId();

    /**
     * <p>getDependencyOriginalVersion.</p>
     *
     * @return {@code String} The original version for the resolved snapshot dependency.
     * @param artifactKey the artifact key {@code String}
     */
    String getDependencyOriginalVersion(String artifactKey);

    /**
     * <p>getDependencyReleaseVersion.</p>
     *
     * @return {@code String} the release version for the resolved snapshot dependency.
     * @param artifactKey the artifact key {@code String}
     */
    String getDependencyReleaseVersion(String artifactKey);

    /**
     * <p>getDependencyDevelopmentVersion.</p>
     *
     * @return {@code String} the release version for the resolved snapshot dependency.
     * @param artifactKey the artifact key {@code String}
     */
    String getDependencyDevelopmentVersion(String artifactKey);

    /**
     * <p>getProjectOriginalVersion.</p>
     *
     * @param projectKey a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    String getProjectOriginalVersion(String projectKey);

    /**
     * <p>getProjectDevelopmentVersion.</p>
     *
     * @param projectKey a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    String getProjectDevelopmentVersion(String projectKey);

    /**
     * <p>getProjectReleaseVersion.</p>
     *
     * @param key a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    String getProjectReleaseVersion(String key);

    /**
     * <p>getOriginalScmInfo.</p>
     *
     * @param projectKey the project key {@code String}
     * @return the original {@code Scm} information.
     */
    Scm getOriginalScmInfo(String projectKey);

    /**
     * <p>hasOriginalScmInfo.</p>
     *
     * @param projectKey the project key {@code String}
     * @return has original Scm info.
     */
    boolean hasOriginalScmInfo(String projectKey);

    // Modifiable
    /**
     * <p>addDependencyOriginalVersion.</p>
     *
     * @param versionlessKey a {@link java.lang.String} object
     * @param string a {@link java.lang.String} object
     */
    void addDependencyOriginalVersion(String versionlessKey, String string);

    /**
     * <p>addDependencyReleaseVersion.</p>
     *
     * @param versionlessKey a {@link java.lang.String} object
     * @param version a {@link java.lang.String} object
     */
    void addDependencyReleaseVersion(String versionlessKey, String version);

    /**
     * <p>addDependencyDevelopmentVersion.</p>
     *
     * @param versionlessKey a {@link java.lang.String} object
     * @param version a {@link java.lang.String} object
     */
    void addDependencyDevelopmentVersion(String versionlessKey, String version);

    /**
     * <p>addReleaseVersion.</p>
     *
     * @param projectId a {@link java.lang.String} object
     * @param nextVersion a {@link java.lang.String} object
     */
    void addReleaseVersion(String projectId, String nextVersion);

    /**
     * <p>addDevelopmentVersion.</p>
     *
     * @param projectId a {@link java.lang.String} object
     * @param nextVersion a {@link java.lang.String} object
     */
    void addDevelopmentVersion(String projectId, String nextVersion);

    /**
     * <p>setScmReleaseLabel.</p>
     *
     * @param tag a {@link java.lang.String} object
     */
    void setScmReleaseLabel(String tag);

    /**
     * <p>setScmReleasedPomRevision.</p>
     *
     * @param scmRevision a {@link java.lang.String} object
     */
    void setScmReleasedPomRevision(String scmRevision);

    /**
     * <p>setScmRelativePathProjectDirectory.</p>
     *
     * @param scmRelativePathProjectDirectory a {@link java.lang.String} object
     */
    void setScmRelativePathProjectDirectory(String scmRelativePathProjectDirectory);

    /**
     * <p>setScmSourceUrl.</p>
     *
     * @param scmUrl a {@link java.lang.String} object
     */
    void setScmSourceUrl(String scmUrl);

    /**
     * Returns whether unresolved SNAPSHOT dependencies should automatically be resolved.
     * If this is set, then this specifies the default answer to be used when unresolved SNAPSHOT
     * dependencies should automatically be resolved ( 0:All 1:Project Dependencies 2:Plugins
     * 3:Reports 4:Extensions ). Possible values are:
     * <ul>
     * <li>"all" or "0": resolve all kinds of snapshots, ie. project, plugin, report and extension dependencies </li>
     * <li>"dependencies" or "1": resolve project dependencies</li>
     * <li>"plugins" or "2": resolve plugin dependencis</li>
     * <li>"reports" or "3": resolve report dependencies</li>
     * <li>"extensions" or "4": resolve extension dependencies</li>
     * </ul>
     *
     * @return String
     */
    String getAutoResolveSnapshots();

    /**
     * Get the line separator to use in the pom.xml.
     *
     * @return String
     */
    String getLineSeparator();

    /**
     * Determines whether the {@code --pin-externals} option in {@code svn copy} command is enabled
     * which is new in Subversion 1.9.
     *
     * @return boolean
     */
    boolean isPinExternals();
}
