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

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder.BuilderReleaseDescriptor;
import org.apache.maven.shared.release.scm.IdentifiedScm;

/**
 * Class providing utility methods used during the release process
 *
 * @author <a href="mailto:jwhitlock@apache.org">Jeremy Whitlock</a>
 */
public class ReleaseUtils
{
    private static final String DEVELOPMENT_KEY = "dev";

    private static final String RELEASE_KEY = "rel";

    private ReleaseUtils()
    {
        // nothing to see here
    }

    public static BuilderReleaseDescriptor buildReleaseDescriptor( ReleaseDescriptorBuilder builder )
    {
        return builder.build();
    }

    public static void copyPropertiesToReleaseDescriptor( Properties properties, ReleaseDescriptorBuilder builder )
    {
        if ( properties.containsKey( "completedPhase" ) )
        {
            builder.setCompletedPhase( properties.getProperty( "completedPhase" ) );
        }
        if ( properties.containsKey( "commitByProject" ) )
        {
            builder.setCommitByProject( Boolean.parseBoolean( properties.getProperty( "commitByProject" ) ) );
        }
        if ( properties.containsKey( "scm.id" ) )
        {
            builder.setScmId( properties.getProperty( "scm.id" ) );
        }
        if ( properties.containsKey( "scm.url" ) )
        {
            builder.setScmSourceUrl( properties.getProperty( "scm.url" ) );
        }
        if ( properties.containsKey( "scm.username" ) )
        {
            builder.setScmUsername( properties.getProperty( "scm.username" ) );
        }
        if ( properties.containsKey( "scm.password" ) )
        {
            builder.setScmPassword( properties.getProperty( "scm.password" ) );
        }
        if ( properties.containsKey( "scm.privateKey" ) )
        {
            builder.setScmPrivateKey( properties.getProperty( "scm.privateKey" ) );
        }
        if ( properties.containsKey( "scm.passphrase" ) )
        {
            builder.setScmPrivateKeyPassPhrase( properties.getProperty( "scm.passphrase" ) );
        }
        if ( properties.containsKey( "scm.tagBase" ) )
        {
            builder.setScmTagBase( properties.getProperty( "scm.tagBase" ) );
        }
        if ( properties.containsKey( "scm.tagNameFormat" ) )
        {
            builder.setScmTagNameFormat( properties.getProperty( "scm.tagNameFormat" ) );
        }
        if ( properties.containsKey( "scm.branchBase" ) )
        {
            builder.setScmBranchBase( properties.getProperty( "scm.branchBase" ) );
        }
        if ( properties.containsKey( "scm.tag" ) )
        {
            builder.setScmReleaseLabel( properties.getProperty( "scm.tag" ) );
        }
        if ( properties.containsKey( "scm.commentPrefix" ) )
        {
            builder.setScmCommentPrefix( properties.getProperty( "scm.commentPrefix" ) );
        }
        if ( properties.containsKey( "scm.developmentCommitComment" ) )
        {
            builder.setScmDevelopmentCommitComment( properties.getProperty( "scm.developmentCommitComment" ) );
        }
        if ( properties.containsKey( "scm.releaseCommitComment" ) )
        {
            builder.setScmReleaseCommitComment( properties.getProperty( "scm.releaseCommitComment" ) );
        }
        if ( properties.containsKey( "scm.branchCommitComment" ) )
        {
            builder.setScmBranchCommitComment( properties.getProperty( "scm.branchCommitComment" ) );
        }
        if ( properties.containsKey( "scm.rollbackCommitComment" ) )
        {
            builder.setScmRollbackCommitComment( properties.getProperty( "scm.rollbackCommitComment" ) );
        }
        if ( properties.containsKey( "exec.additionalArguments" ) )
        {
            builder.setAdditionalArguments( properties.getProperty( "exec.additionalArguments" ) );
        }
        if ( properties.containsKey( "exec.pomFileName" ) )
        {
            builder.setPomFileName( properties.getProperty( "exec.pomFileName" ) );
        }
        if ( properties.containsKey( "exec.activateProfiles" ) )
        {
            builder.setActivateProfiles( 
                         Arrays.asList( properties.getProperty( "exec.activateProfiles" ).split( "," ) ) );
        }
        if ( properties.containsKey( "preparationGoals" ) )
        {
            builder.setPreparationGoals( properties.getProperty( "preparationGoals" ) );
        }
        if ( properties.containsKey( "completionGoals" ) )
        {
            builder.setCompletionGoals( properties.getProperty( "completionGoals" ) );
        }
        if ( properties.containsKey( "projectVersionPolicyId" ) )
        {
            builder.setProjectVersionPolicyId( properties.getProperty( "projectVersionPolicyId" ) );
        }
        if ( properties.containsKey( "projectNamingPolicyId" ) )
        {
            builder.setProjectNamingPolicyId( properties.getProperty( "projectNamingPolicyId" ) );
        }
        if ( properties.containsKey( "releaseStrategyId" ) )
        {
            builder.setReleaseStrategyId( properties.getProperty( "releaseStrategyId" ) );
        }
        if ( properties.containsKey( "exec.snapshotReleasePluginAllowed" ) )
        {
            String snapshotReleasePluginAllowedStr = properties.getProperty( "exec.snapshotReleasePluginAllowed" );
            builder.setSnapshotReleasePluginAllowed( Boolean.valueOf( snapshotReleasePluginAllowedStr ) );
        }
        if ( properties.containsKey( "remoteTagging" ) )
        {
            String remoteTaggingStr = properties.getProperty( "remoteTagging" );
            builder.setRemoteTagging( Boolean.valueOf( remoteTaggingStr ) );
        }
        if ( properties.containsKey( "pinExternals" ) )
        {
            String pinExternals = properties.getProperty( "pinExternals" );
            builder.setPinExternals( Boolean.valueOf( pinExternals ) );
        }
        if ( properties.containsKey( "pushChanges" ) )
        {
            String pushChanges = properties.getProperty( "pushChanges" );
            builder.setPushChanges( Boolean.valueOf( pushChanges ) );
        }
        if ( properties.containsKey( "workItem" ) )
        {
            builder.setWorkItem( properties.getProperty( "workItem" ) );
        }
        if ( properties.containsKey( "autoResolveSnapshots" ) )
        {
            String resolve = properties.getProperty( "autoResolveSnapshots" );
            builder.setAutoResolveSnapshots( resolve );
        }

        loadResolvedDependencies( properties, builder );

        // boolean properties are not written to the properties file because the value from the caller is always used

        for ( Iterator<?> i = properties.keySet().iterator(); i.hasNext(); )
        {
            String property = (String) i.next();
            if ( property.startsWith( "project.rel." ) )
            {
                builder.addReleaseVersion( property.substring( "project.rel.".length() ),
                                                     properties.getProperty( property ) );
            }
            else if ( property.startsWith( "project.dev." ) )
            {
                builder.addDevelopmentVersion( property.substring( "project.dev.".length() ),
                                                         properties.getProperty( property ) );
            }
            else if ( property.startsWith( "dependency.rel." ) )
            {
                builder.addDependencyReleaseVersion( property.substring( "dependency.rel.".length() ),
                                                     properties.getProperty( property ) );
            }
            else if ( property.startsWith( "dependency.dev." ) )
            {
                builder.addDependencyDevelopmentVersion( property.substring( "dependency.dev.".length() ),
                                                         properties.getProperty( property ) );
            }
            else if ( property.startsWith( "project.scm." ) )
            {
                int index = property.lastIndexOf( '.' );
                if ( index > "project.scm.".length() )
                {
                    String key = property.substring( "project.scm.".length(), index );

                    if ( builder.build().getOriginalScmInfo( key ) == null )
                    {
                        if ( properties.getProperty( "project.scm." + key + ".empty" ) != null )
                        {
                            builder.addOriginalScmInfo( key, null );
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

                            builder.addOriginalScmInfo( key, scm );
                        }
                    }
                }
            }
        }
    }

    private static void loadResolvedDependencies( Properties prop, ReleaseDescriptorBuilder builder )
    {
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
                String artifactVersionlessKey;
                int startIndex = "dependency.".length();
                int endIndex;
                String versionType;

                if ( propertyName.indexOf( ".development" ) != -1 )
                {
                    endIndex = propertyName.lastIndexOf( ".development" );
                    versionType = DEVELOPMENT_KEY;
                }
                else if ( propertyName.indexOf( ".release" ) != -1 )
                {
                    endIndex = propertyName.lastIndexOf( ".release" );
                    versionType = RELEASE_KEY;
                }
                else
                {
                    // MRELEASE-834, probably a maven-dependency-plugin property
                    continue;
                }

                artifactVersionlessKey = propertyName.substring( startIndex, endIndex );

                if ( RELEASE_KEY.equals( versionType ) )
                {
                    builder.addDependencyReleaseVersion( artifactVersionlessKey, currentEntry.getValue() );
                }
                else if ( DEVELOPMENT_KEY.equals( versionType ) )
                {
                    builder.addDependencyDevelopmentVersion( artifactVersionlessKey, currentEntry.getValue() );
                }
            }
        }
    }

}
