package org.apache.maven.shared.release.config;

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

import java.util.List;

import org.apache.maven.model.Scm;

/**
 * 
 * @author Robert Scholte
 */
public interface ReleaseDescriptor
{
    boolean isUpdateDependencies();

    boolean isUseReleaseProfile();

    boolean isAutoVersionSubmodules();

    boolean isSnapshotReleasePluginAllowed();

    boolean isCommitByProject();

    boolean isBranchCreation();

    boolean isUpdateBranchVersions();

    boolean isUpdateWorkingCopyVersions();

    boolean isSuppressCommitBeforeTagOrBranch();

    boolean isAllowTimestampedSnapshots();

    boolean isUpdateVersionsToSnapshot();

    boolean isRemoteTagging();

    boolean isLocalCheckout();

    boolean isPushChanges();

    String getDefaultDevelopmentVersion();

    String getScmRelativePathProjectDirectory();

    String getCheckoutDirectory();

    String getPerformGoals();

    String getDefaultReleaseVersion();

    String getScmReleasedPomRevision();

    boolean isAddSchema();

    boolean isGenerateReleasePoms();

    boolean isInteractive();

    boolean isScmUseEditMode();

    String getCompletedPhase();

    List<String> getCheckModificationExcludes();

    String getAdditionalArguments();

    String getPreparationGoals();

    String getCompletionGoals();

    String getPomFileName();

    String getScmCommentPrefix();

    String getScmPrivateKeyPassPhrase();

    String getScmPassword();

    String getScmPrivateKey();

    String getScmReleaseLabel();

    String getScmTagBase();

    String getScmBranchBase();

    String getScmId();

    String getScmSourceUrl();

    String getScmUsername();

    int getWaitBeforeTagging();

    String getWorkingDirectory();

    String getScmTagNameFormat();

    String getProjectNamingPolicyId();

    String getProjectVersionPolicyId(); 

    String getReleaseStrategyId();

    String getDependencyOriginalVersion( String artifactKey );
    
    String getDependencyReleaseVersion( String artifactKey ); 
    
    String getDependencyDevelopmentVersion( String artifactKey ); 

    String getProjectOriginalVersion( String projectKey );

    String getProjectDevelopmentVersion( String projectKey );

    String getProjectReleaseVersion( String key );

    Scm getOriginalScmInfo( String projectKey );

    // Modifiable
    void addDependencyOriginalVersion( String versionlessKey, String string );

    void addDependencyReleaseVersion( String versionlessKey, String version ); 
    
    void addDependencyDevelopmentVersion( String versionlessKey, String version );

    void addReleaseVersion( String projectId, String nextVersion );

    void addDevelopmentVersion( String projectId, String nextVersion );

    void setScmReleaseLabel( String tag );

    void setScmReleasedPomRevision( String scmRevision );

    void setScmRelativePathProjectDirectory( String scmRelativePathProjectDirectory );

    void setScmSourceUrl( String scmUrl );


}
