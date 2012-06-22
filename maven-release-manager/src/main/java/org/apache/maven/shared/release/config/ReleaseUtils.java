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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.shared.release.scm.IdentifiedScm;

/**
 * Class providing utility methods used during the release process
 *
 * @author <a href="mailto:jwhitlock@apache.org">Jeremy Whitlock</a>
 */
public class ReleaseUtils
{
    private ReleaseUtils()
    {
        // nothing to see here
    }

    /**
     * Merge two descriptors together. All SCM settings are overridden by the merge descriptor, as is the
     * <code>workingDirectory</code> field. The <code>completedPhase</code> field is used as
     * a default from the merge descriptor, but not overridden if it exists.
     *
     * @param mergeInto  the descriptor to be merged into
     * @param toBeMerged the descriptor to merge into mergeInto
     * @return ReleaseDescriptor the merged descriptor
     */
    public static ReleaseDescriptor merge( ReleaseDescriptor mergeInto, ReleaseDescriptor toBeMerged )
    {
        // Overridden if configured from the caller
        mergeInto.setScmId( mergeOverride( mergeInto.getScmId(), toBeMerged.getScmId() ) );
        mergeInto.setScmSourceUrl( mergeOverride( mergeInto.getScmSourceUrl(), toBeMerged.getScmSourceUrl() ) );
        mergeInto.setScmCommentPrefix(
            mergeOverride( mergeInto.getScmCommentPrefix(), toBeMerged.getScmCommentPrefix() ) );
        mergeInto.setScmReleaseLabel( mergeOverride( mergeInto.getScmReleaseLabel(), toBeMerged.getScmReleaseLabel() ) );
        mergeInto.setScmTagBase( mergeOverride( mergeInto.getScmTagBase(), toBeMerged.getScmTagBase() ) );
        mergeInto.setScmTagNameFormat(
            mergeOverride( mergeInto.getScmTagNameFormat(), toBeMerged.getScmTagNameFormat() ) );
        mergeInto.setScmBranchBase( mergeOverride( mergeInto.getScmBranchBase(), toBeMerged.getScmBranchBase() ) );
        mergeInto.setScmUsername( mergeOverride( mergeInto.getScmUsername(), toBeMerged.getScmUsername() ) );
        mergeInto.setScmPassword( mergeOverride( mergeInto.getScmPassword(), toBeMerged.getScmPassword() ) );
        mergeInto.setScmPrivateKey( mergeOverride( mergeInto.getScmPrivateKey(), toBeMerged.getScmPrivateKey() ) );
        mergeInto.setScmPrivateKeyPassPhrase(
            mergeOverride( mergeInto.getScmPrivateKeyPassPhrase(), toBeMerged.getScmPrivateKeyPassPhrase() ) );
        mergeInto.setScmCommentPrefix(
            mergeOverride( mergeInto.getScmCommentPrefix(), toBeMerged.getScmCommentPrefix() ) );
        mergeInto.setAdditionalArguments(
            mergeOverride( mergeInto.getAdditionalArguments(), toBeMerged.getAdditionalArguments() ) );
        mergeInto.setPreparationGoals(
            mergeOverride( mergeInto.getPreparationGoals(), toBeMerged.getPreparationGoals() ) );
        mergeInto.setCompletionGoals(
            mergeOverride( mergeInto.getCompletionGoals(), toBeMerged.getCompletionGoals() ) );
        mergeInto.setPerformGoals( mergeOverride( mergeInto.getPerformGoals(), toBeMerged.getPerformGoals() ) );
        mergeInto.setPomFileName( mergeOverride( mergeInto.getPomFileName(), toBeMerged.getPomFileName() ) );
        mergeInto.setCheckModificationExcludes( toBeMerged.getCheckModificationExcludes() );
        mergeInto.setScmUseEditMode( toBeMerged.isScmUseEditMode() );
        mergeInto.setAddSchema( toBeMerged.isAddSchema() );
        mergeInto.setGenerateReleasePoms( toBeMerged.isGenerateReleasePoms() );
        mergeInto.setInteractive( toBeMerged.isInteractive() );
        mergeInto.setUpdateDependencies( toBeMerged.isUpdateDependencies() );
        mergeInto.setCommitByProject( mergeOverride( mergeInto.isCommitByProject(), toBeMerged.isCommitByProject(),
                                                     false ) );
        mergeInto.setUseReleaseProfile( toBeMerged.isUseReleaseProfile() );
        mergeInto.setBranchCreation( toBeMerged.isBranchCreation() );
        mergeInto.setUpdateBranchVersions( toBeMerged.isUpdateBranchVersions() );
        mergeInto.setUpdateWorkingCopyVersions( toBeMerged.isUpdateWorkingCopyVersions() );
        mergeInto.setSuppressCommitBeforeTagOrBranch( toBeMerged.isSuppressCommitBeforeTagOrBranch() );
        mergeInto.setUpdateVersionsToSnapshot( toBeMerged.isUpdateVersionsToSnapshot() );
        mergeInto.setAllowTimestampedSnapshots( toBeMerged.isAllowTimestampedSnapshots() );
        mergeInto.setSnapshotReleasePluginAllowed( toBeMerged.isSnapshotReleasePluginAllowed() );
        mergeInto.setAutoVersionSubmodules( toBeMerged.isAutoVersionSubmodules() );
        mergeInto.setDefaultReleaseVersion( mergeOverride( mergeInto.getDefaultReleaseVersion(),
                                                           toBeMerged.getDefaultReleaseVersion() ) );
        mergeInto.setDefaultDevelopmentVersion( mergeOverride( mergeInto.getDefaultDevelopmentVersion(),
                                                               toBeMerged.getDefaultDevelopmentVersion() ) );
        mergeInto.setRemoteTagging( toBeMerged.isRemoteTagging() );
        mergeInto.setLocalCheckout( toBeMerged.isLocalCheckout() );
        mergeInto.setPushChanges( toBeMerged.isPushChanges() );
        mergeInto.setWaitBeforeTagging( toBeMerged.getWaitBeforeTagging() );

        // If the user specifies versions, these should be override the existing versions
        if ( toBeMerged.getReleaseVersions() != null )
        {
            mergeInto.getReleaseVersions().putAll( toBeMerged.getReleaseVersions() );
        }
        if ( toBeMerged.getDevelopmentVersions() != null )
        {
            mergeInto.getDevelopmentVersions().putAll( toBeMerged.getDevelopmentVersions() );
        }
        // These must be overridden, as they are not stored
        mergeInto.setWorkingDirectory(
            mergeOverride( mergeInto.getWorkingDirectory(), toBeMerged.getWorkingDirectory() ) );
        mergeInto.setCheckoutDirectory(
            mergeOverride( mergeInto.getCheckoutDirectory(), toBeMerged.getCheckoutDirectory() ) );

        // Not overridden - not configured from caller
        mergeInto.setCompletedPhase( mergeDefault( mergeInto.getCompletedPhase(), toBeMerged.getCompletedPhase() ) );

        return mergeInto;
    }

    private static String mergeOverride( String thisValue, String mergeValue )
    {
        return mergeValue != null ? mergeValue : thisValue;
    }

    private static String mergeDefault( String thisValue, String mergeValue )
    {
        return thisValue != null ? thisValue : mergeValue;
    }
    
    private static boolean mergeOverride( boolean thisValue, boolean mergeValue, boolean defaultValue )
    {
        return mergeValue != defaultValue ? mergeValue : thisValue;
    }

    public static ReleaseDescriptor copyPropertiesToReleaseDescriptor( Properties properties )
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setCompletedPhase( properties.getProperty( "completedPhase" ) );
        releaseDescriptor.setCommitByProject( Boolean.parseBoolean( properties.getProperty( "commitByProject" ) ) );
        releaseDescriptor.setScmId( properties.getProperty( "scm.id" ) );
        releaseDescriptor.setScmSourceUrl( properties.getProperty( "scm.url" ) );
        releaseDescriptor.setScmUsername( properties.getProperty( "scm.username" ) );
        releaseDescriptor.setScmPassword( properties.getProperty( "scm.password" ) );
        releaseDescriptor.setScmPrivateKey( properties.getProperty( "scm.privateKey" ) );
        releaseDescriptor.setScmPrivateKeyPassPhrase( properties.getProperty( "scm.passphrase" ) );
        releaseDescriptor.setScmTagBase( properties.getProperty( "scm.tagBase" ) );
        releaseDescriptor.setScmTagNameFormat( properties.getProperty( "scm.tagNameFormat" ) );
        releaseDescriptor.setScmBranchBase( properties.getProperty( "scm.branchBase" ) );
        releaseDescriptor.setScmReleaseLabel( properties.getProperty( "scm.tag" ) );
        releaseDescriptor.setScmCommentPrefix( properties.getProperty( "scm.commentPrefix" ) );
        releaseDescriptor.setAdditionalArguments( properties.getProperty( "exec.additionalArguments" ) );
        releaseDescriptor.setPomFileName( properties.getProperty( "exec.pomFileName" ) );
        releaseDescriptor.setPreparationGoals( properties.getProperty( "preparationGoals" ) );
        releaseDescriptor.setCompletionGoals( properties.getProperty( "completionGoals" ) );
        String snapshotReleasePluginAllowedStr = properties.getProperty( "exec.snapshotReleasePluginAllowed" );
        releaseDescriptor.setSnapshotReleasePluginAllowed( snapshotReleasePluginAllowedStr == null
                                                               ? false
                                                               : Boolean.valueOf(
                                                                   snapshotReleasePluginAllowedStr ).booleanValue() );
        String remoteTaggingStr = properties.getProperty( "remoteTagging" );
        releaseDescriptor.setRemoteTagging(
            remoteTaggingStr == null ? false : Boolean.valueOf( remoteTaggingStr ).booleanValue() );
        String pushChanges = properties.getProperty( "pushChanges" );
        releaseDescriptor.setPushChanges( pushChanges == null ? true : Boolean.valueOf( pushChanges ).booleanValue() );

        loadResolvedDependencies( properties, releaseDescriptor );

        // boolean properties are not written to the properties file because the value from the caller is always used

        for ( Iterator<?> i = properties.keySet().iterator(); i.hasNext(); )
        {
            String property = (String) i.next();
            if ( property.startsWith( "project.rel." ) )
            {
                releaseDescriptor.mapReleaseVersion( property.substring( "project.rel.".length() ),
                                                     properties.getProperty( property ) );
            }
            else if ( property.startsWith( "project.dev." ) )
            {
                releaseDescriptor.mapDevelopmentVersion( property.substring( "project.dev.".length() ),
                                                         properties.getProperty( property ) );
            }
            else if ( property.startsWith( "project.scm." ) )
            {
                int index = property.lastIndexOf( '.' );
                if ( index > "project.scm.".length() )
                {
                    String key = property.substring( "project.scm.".length(), index );

                    if ( !releaseDescriptor.getOriginalScmInfo().containsKey( key ) )
                    {
                        if ( properties.getProperty( "project.scm." + key + ".empty" ) != null )
                        {
                            releaseDescriptor.mapOriginalScmInfo( key, null );
                        }
                        else
                        {
                            IdentifiedScm scm = new IdentifiedScm();
                            scm.setConnection( properties.getProperty( "project.scm." + key + ".connection" ) );
                            scm.setDeveloperConnection(
                                properties.getProperty( "project.scm." + key + ".developerConnection" ) );
                            scm.setUrl( properties.getProperty( "project.scm." + key + ".url" ) );
                            scm.setTag( properties.getProperty( "project.scm." + key + ".tag" ) );
                            scm.setId( properties.getProperty( "project.scm." + key + ".id" ) );

                            releaseDescriptor.mapOriginalScmInfo( key, scm );
                        }
                    }
                }
            }
        }
        return releaseDescriptor;
    }

    private static void loadResolvedDependencies( Properties prop, ReleaseDescriptor descriptor )
    {
        Map<String, Map<String, String>> resolvedDependencies = new HashMap<String, Map<String, String>>();

        Set entries = prop.entrySet();
        Iterator<Entry<String, String>> iterator = entries.iterator();
        String propertyName;
        Entry<String, String> currentEntry;

        while ( iterator.hasNext() )
        {
            currentEntry = iterator.next();
            propertyName = currentEntry.getKey();

            if ( propertyName.startsWith( "dependency." ) )
            {
                Map<String, String> versionMap;
                String artifactVersionlessKey;
                int startIndex = "dependency.".length();
                int endIndex;
                String versionType;

                versionMap = new HashMap<String, String>();

                if ( propertyName.indexOf( ".development" ) != -1 )
                {
                    endIndex = propertyName.lastIndexOf( ".development" );
                    versionType = ReleaseDescriptor.DEVELOPMENT_KEY;
                }
                else
                {
                    endIndex = propertyName.lastIndexOf( ".release" );
                    versionType = ReleaseDescriptor.RELEASE_KEY;
                }

                artifactVersionlessKey = propertyName.substring( startIndex, endIndex );

                if ( resolvedDependencies.containsKey( artifactVersionlessKey ) )
                {
                    versionMap = resolvedDependencies.get( artifactVersionlessKey );
                }
                else
                {
                    versionMap = new HashMap<String, String>();
                    resolvedDependencies.put( artifactVersionlessKey, versionMap );
                }

                versionMap.put( versionType, currentEntry.getValue() );
            }
        }

        descriptor.setResolvedSnapshotDependencies( resolvedDependencies );
    }

}
