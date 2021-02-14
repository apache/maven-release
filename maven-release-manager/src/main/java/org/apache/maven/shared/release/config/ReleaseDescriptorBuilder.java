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
 * @since 3.0.0
 */
public class ReleaseDescriptorBuilder
{
    /**
     * Hides inner logic of the release descriptor
     * 
     * @author Robert Scholte
     *
     */
    public static final class BuilderReleaseDescriptor extends ModelloReleaseDescriptor implements ReleaseDescriptor
    {
        private BuilderReleaseDescriptor()
        {
        }
    }
    
    private final BuilderReleaseDescriptor releaseDescriptor;
    
    public ReleaseDescriptorBuilder()
    {
        this.releaseDescriptor = new BuilderReleaseDescriptor();
    }

    public ReleaseDescriptorBuilder addCheckModificationExclude( String string )
    {
        releaseDescriptor.addCheckModificationExclude( string );
        return this;
    }

    public ReleaseDescriptorBuilder setActivateProfiles( List<String> profiles )
    {
        releaseDescriptor.setActivateProfiles( profiles );
        return this;
    }

    public ReleaseDescriptorBuilder setAddSchema( boolean addSchema )
    {
        releaseDescriptor.setAddSchema( addSchema );
        return this;
    }

    public ReleaseDescriptorBuilder setAdditionalArguments( String additionalArguments )
    {
        releaseDescriptor.setAdditionalArguments( additionalArguments );
        return this;
    }

    public ReleaseDescriptorBuilder setAllowTimestampedSnapshots( boolean allowTimestampedSnapshots )
    {
        releaseDescriptor.setAllowTimestampedSnapshots( allowTimestampedSnapshots );
        return this;
    }

    public ReleaseDescriptorBuilder setAutoVersionSubmodules( boolean autoVersionSubmodules )
    {
        releaseDescriptor.setAutoVersionSubmodules( autoVersionSubmodules );
        return this;
    }

    public ReleaseDescriptorBuilder setBranchCreation( boolean branchCreation )
    {
        releaseDescriptor.setBranchCreation( branchCreation );
        return this;
    }

    public ReleaseDescriptorBuilder setCheckModificationExcludes( List<String> checkModificationExcludes )
    {
        releaseDescriptor.setCheckModificationExcludes( checkModificationExcludes );
        return this;
    }

    public ReleaseDescriptorBuilder setCheckoutDirectory( String checkoutDirectory )
    {
        releaseDescriptor.setCheckoutDirectory( checkoutDirectory );
        return this;
    }

    public ReleaseDescriptorBuilder setCommitByProject( boolean commitByProject )
    {
        releaseDescriptor.setCommitByProject( commitByProject );
        return this;
    }

    public ReleaseDescriptorBuilder setCompletedPhase( String completedPhase )
    {
        releaseDescriptor.setCompletedPhase( completedPhase );
        return this;
    }

    public ReleaseDescriptorBuilder setCompletionGoals( String completionGoals )
    {
        releaseDescriptor.setCompletionGoals( completionGoals );
        return this;
    }

    public ReleaseDescriptorBuilder setDefaultDevelopmentVersion( String defaultDevelopmentVersion )
    {
        releaseDescriptor.setDefaultDevelopmentVersion( defaultDevelopmentVersion );
        return this;
    }

    public ReleaseDescriptorBuilder setDefaultReleaseVersion( String defaultReleaseVersion )
    {
        releaseDescriptor.setDefaultReleaseVersion( defaultReleaseVersion );
        return this;
    }

    public ReleaseDescriptorBuilder setDescription( String description )
    {
        releaseDescriptor.setDescription( description );
        return this;
    }

    public ReleaseDescriptorBuilder setGenerateReleasePoms( boolean generateReleasePoms )
    {
        releaseDescriptor.setGenerateReleasePoms( generateReleasePoms );
        return this;
    }

    public ReleaseDescriptorBuilder setInteractive( boolean interactive )
    {
        releaseDescriptor.setInteractive( interactive );
        return this;
    }

    public ReleaseDescriptorBuilder setLineSeparator( String ls )
    {
        releaseDescriptor.setLineSeparator( ls );
        return this;
    }

    public ReleaseDescriptorBuilder setLocalCheckout( boolean localCheckout )
    {
        releaseDescriptor.setLocalCheckout( localCheckout );
        return this;
    }

    public ReleaseDescriptorBuilder setModelEncoding( String modelEncoding )
    {
        releaseDescriptor.setModelEncoding( modelEncoding );
        return this;
    }

    public ReleaseDescriptorBuilder setName( String name )
    {
        releaseDescriptor.setName( name );
        return this;
    }

    public ReleaseDescriptorBuilder setPerformGoals( String performGoals )
    {
        releaseDescriptor.setPerformGoals( performGoals );
        return this;
    }

    public ReleaseDescriptorBuilder setPomFileName( String pomFileName )
    {
        releaseDescriptor.setPomFileName( pomFileName );
        return this;
    }

    public ReleaseDescriptorBuilder setPreparationGoals( String preparationGoals )
    {
        releaseDescriptor.setPreparationGoals( preparationGoals );
        return this;
    }

    public ReleaseDescriptorBuilder setProjectNamingPolicyId( String projectNamingPolicyId )
    {
        releaseDescriptor.setProjectNamingPolicyId( projectNamingPolicyId );
        return this;
    }

    public ReleaseDescriptorBuilder setProjectVersionPolicyId( String projectVersionPolicyId )
    {
        releaseDescriptor.setProjectVersionPolicyId( projectVersionPolicyId );
        return this;
    }

    public ReleaseDescriptorBuilder setPushChanges( boolean pushChanges )
    {
        releaseDescriptor.setPushChanges( pushChanges );
        return this;
    }

    public ReleaseDescriptorBuilder setWorkItem( String workItem )
    {
        releaseDescriptor.setWorkItem( workItem );
        return this;
    }

    public ReleaseDescriptorBuilder setReleaseStrategyId( String releaseStrategyId )
    {
        releaseDescriptor.setReleaseStrategyId( releaseStrategyId );
        return this;
    }

    public ReleaseDescriptorBuilder setRemoteTagging( boolean remoteTagging )
    {
        releaseDescriptor.setRemoteTagging( remoteTagging );
        return this;
    }

    public ReleaseDescriptorBuilder setScmBranchBase( String scmBranchBase )
    {
        releaseDescriptor.setScmBranchBase( scmBranchBase );
        return this;
    }

    public ReleaseDescriptorBuilder setScmCommentPrefix( String scmCommentPrefix )
    {
        releaseDescriptor.setScmCommentPrefix( scmCommentPrefix );
        return this;
    }

    /**
     * @since 3.0.0-M1
     */
    public ReleaseDescriptorBuilder setScmReleaseCommitComment( String scmReleaseCommitComment )
    {
        releaseDescriptor.setScmReleaseCommitComment( scmReleaseCommitComment );
        return this;
    }

    /**
     * @since 3.0.0-M1
     */
    public ReleaseDescriptorBuilder setScmDevelopmentCommitComment( String scmDevelopmentCommitComment )
    {
        releaseDescriptor.setScmDevelopmentCommitComment( scmDevelopmentCommitComment );
        return this;
    }

    /**
     * @since 3.0.0-M1
     */
    public ReleaseDescriptorBuilder setScmBranchCommitComment( String scmBranchCommitComment )
    {
        releaseDescriptor.setScmBranchCommitComment( scmBranchCommitComment );
        return this;
    }

    /**
     * @since 3.0.0-M1
     */
    public ReleaseDescriptorBuilder setScmRollbackCommitComment( String scmRollbackCommitComment )
    {
        releaseDescriptor.setScmRollbackCommitComment( scmRollbackCommitComment );
        return this;
    }

    public ReleaseDescriptorBuilder setScmId( String scmId )
    {
        releaseDescriptor.setScmId( scmId );
        return this;
    }

    public ReleaseDescriptorBuilder setScmPassword( String scmPassword )
    {
        releaseDescriptor.setScmPassword( scmPassword );
        return this;
    }

    public ReleaseDescriptorBuilder setScmPrivateKey( String scmPrivateKey )
    {
        releaseDescriptor.setScmPrivateKey( scmPrivateKey );
        return this;
    }

    public ReleaseDescriptorBuilder setScmPrivateKeyPassPhrase( String scmPrivateKeyPassPhrase )
    {
        releaseDescriptor.setScmPrivateKeyPassPhrase( scmPrivateKeyPassPhrase );
        return this;
    }

    public ReleaseDescriptorBuilder setScmRelativePathProjectDirectory( String scmRelativePathProjectDirectory )
    {
        releaseDescriptor.setScmRelativePathProjectDirectory( scmRelativePathProjectDirectory );
        return this;
    }

    public ReleaseDescriptorBuilder setScmReleaseLabel( String scmReleaseLabel )
    {
        releaseDescriptor.setScmReleaseLabel( scmReleaseLabel );
        return this;
    }

    public ReleaseDescriptorBuilder setScmReleasedPomRevision( String scmReleasedPomRevision )
    {
        releaseDescriptor.setScmReleasedPomRevision( scmReleasedPomRevision );
        return this;
    }

    public ReleaseDescriptorBuilder setScmSourceUrl( String scmSourceUrl )
    {
        releaseDescriptor.setScmSourceUrl( scmSourceUrl );
        return this;
    }

    public ReleaseDescriptorBuilder setScmTagBase( String scmTagBase )
    {
        releaseDescriptor.setScmTagBase( scmTagBase );
        return this;
    }

    public ReleaseDescriptorBuilder setScmTagNameFormat( String scmTagNameFormat )
    {
        releaseDescriptor.setScmTagNameFormat( scmTagNameFormat );
        return this;
    }

    public ReleaseDescriptorBuilder setScmUseEditMode( boolean scmUseEditMode )
    {
        releaseDescriptor.setScmUseEditMode( scmUseEditMode );
        return this;
    }

    public ReleaseDescriptorBuilder setScmUsername( String scmUsername )
    {
        releaseDescriptor.setScmUsername( scmUsername );
        return this;
    }

    public ReleaseDescriptorBuilder setSnapshotReleasePluginAllowed( boolean snapshotReleasePluginAllowed )
    {
        releaseDescriptor.setSnapshotReleasePluginAllowed( snapshotReleasePluginAllowed );
        return this;
    }

    public ReleaseDescriptorBuilder setSuppressCommitBeforeTagOrBranch( boolean suppressCommitBeforeTagOrBranch )
    {
        releaseDescriptor.setSuppressCommitBeforeTagOrBranch( suppressCommitBeforeTagOrBranch );
        return this;
    }

    public ReleaseDescriptorBuilder setUpdateBranchVersions( boolean updateBranchVersions )
    {
        releaseDescriptor.setUpdateBranchVersions( updateBranchVersions );
        return this;
    }

    public ReleaseDescriptorBuilder setUpdateDependencies( boolean updateDependencies )
    {
        releaseDescriptor.setUpdateDependencies( updateDependencies );
        return this;
    }

    public ReleaseDescriptorBuilder setUpdateVersionsToSnapshot( boolean updateVersionsToSnapshot )
    {
        releaseDescriptor.setUpdateVersionsToSnapshot( updateVersionsToSnapshot );
        return this;
    }

    public ReleaseDescriptorBuilder setUpdateWorkingCopyVersions( boolean updateWorkingCopyVersions )
    {
        releaseDescriptor.setUpdateWorkingCopyVersions( updateWorkingCopyVersions );
        return this;
    }

    public ReleaseDescriptorBuilder setUseReleaseProfile( boolean useReleaseProfile )
    {
        releaseDescriptor.setUseReleaseProfile( useReleaseProfile );
        return this;
    }

    public ReleaseDescriptorBuilder setWaitBeforeTagging( int waitBeforeTagging )
    {
        releaseDescriptor.setWaitBeforeTagging( waitBeforeTagging );
        return this;
    }

    public ReleaseDescriptorBuilder setWorkingDirectory( String workingDirectory )
    {
        releaseDescriptor.setWorkingDirectory( workingDirectory );
        return this;
    }

    public ReleaseDescriptorBuilder addReleaseVersion( String key, String value )
    {
        releaseDescriptor.addReleaseVersion( key, value );
        return this;
    }

    public ReleaseDescriptorBuilder addDevelopmentVersion( String key, String value )
    {
        releaseDescriptor.addDevelopmentVersion( key, value );
        return this;
    }

    public ReleaseDescriptorBuilder addOriginalScmInfo( String key, Scm value )
    {
        releaseDescriptor.addOriginalScmInfo( key, value );
        return this;
    }

    public void putOriginalVersion( String projectKey, String version )
    {
        releaseDescriptor.addOriginalVersion( projectKey, version );
    }
    
    public ReleaseDescriptorBuilder addDependencyOriginalVersion( String dependencyKey, String version )
    {
        releaseDescriptor.addDependencyOriginalVersion( dependencyKey, version );
        return this;
        
    }
    
    public ReleaseDescriptorBuilder addDependencyReleaseVersion( String dependencyKey, String version )
    {
        releaseDescriptor.addDependencyReleaseVersion( dependencyKey, version );
        return this;
    }
    
    public ReleaseDescriptorBuilder addDependencyDevelopmentVersion( String dependencyKey, String version )
    {
        releaseDescriptor.addDependencyDevelopmentVersion( dependencyKey, version );
        return this;
    }

    public ReleaseDescriptorBuilder setAutoResolveSnapshots( String autoResolveSnapshots )
    {
        releaseDescriptor.setAutoResolveSnapshots( autoResolveSnapshots );
        return this;
    }

    public ReleaseDescriptorBuilder setPinExternals( boolean pinExternals )
    {
        releaseDescriptor.setPinExternals( pinExternals );
        return this;
    }

    public BuilderReleaseDescriptor build()
    {
        return releaseDescriptor;
    }
}
