package org.apache.maven.shared.release.config;

import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder.BuilderReleaseDescriptor;

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

import org.apache.maven.shared.release.phase.AbstractReleaseTestCase;
import org.apache.maven.shared.release.scm.IdentifiedScm;
import org.codehaus.plexus.PlexusTestCase;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;

/**
 * Test the properties store.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class PropertiesReleaseDescriptorStoreTest
    extends PlexusTestCase
{
    private PropertiesReleaseDescriptorStore store;

    private SecDispatcher secDispatcher;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
        store = (PropertiesReleaseDescriptorStore) lookup( ReleaseDescriptorStore.class, "properties" );
        secDispatcher = lookup( SecDispatcher.class, "mng-4384" );
    }

    public void testReadFromFile()
        throws ReleaseDescriptorStoreException
    {
        File file = getTestFile( "target/test-classes/release.properties" );

        ReleaseDescriptor config = store.read( file ).build();

        ReleaseDescriptor expected = createExpectedReleaseConfiguration().build();

        assertEquals( "check matches", expected, config );
    }

    public void testReadFromFileUsingWorkingDirectory()
        throws Exception
    {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setWorkingDirectory( AbstractReleaseTestCase.getPath(  getTestFile( "target/test-classes" ) ) );
        ReleaseDescriptor config = store.read( builder ).build();

        ReleaseDescriptorBuilder expected = createExpectedReleaseConfiguration();
        expected.setWorkingDirectory( builder.build().getWorkingDirectory() );

        assertEquals( "check matches", expected.build(), config );
    }

    public void testReadFromEmptyFile()
        throws ReleaseDescriptorStoreException
    {
        File file = getTestFile( "target/test-classes/empty-release.properties" );

        BuilderReleaseDescriptor config = store.read( file ).build();

        assertDefaultReleaseConfiguration( config );
    }

    public void testReadMissingFile()
        throws ReleaseDescriptorStoreException
    {
        File file = getTestFile( "target/test-classes/no-release.properties" );

        BuilderReleaseDescriptor config = store.read( file ).build();

        assertDefaultReleaseConfiguration( config );
    }

    public void testMergeFromEmptyFile()
        throws ReleaseDescriptorStoreException, IOException
    {
        File file = getTestFile( "target/test-classes/empty-release.properties" );

        ReleaseDescriptorBuilder mergeDescriptor = createMergeConfiguration();
        ReleaseDescriptor config = store.read( mergeDescriptor, file ).build();

        assertEquals( "Check configurations merged", mergeDescriptor.build(), config );
    }

    public void testMergeFromMissingFile()
        throws ReleaseDescriptorStoreException, IOException
    {
        File file = getTestFile( "target/test-classes/no-release.properties" );

        ReleaseDescriptorBuilder mergeDescriptor = createMergeConfiguration();
        ReleaseDescriptor config = store.read( mergeDescriptor, file ).build();

        assertEquals( "Check configurations merged", mergeDescriptor.build(), config );
    }

    public void testWriteToNewFile()
        throws Exception
    {
        File file = getTestFile( "target/test-classes/new-release.properties" );
        file.delete();
        assertFalse( "Check file doesn't exist", file.exists() );

        ReleaseDescriptorBuilder config = createReleaseConfigurationForWriting();

        store.write( config.build(), file );

        ReleaseDescriptor rereadDescriptor = store.read( file ).build();

        assertAndAdjustScmPassword( config, rereadDescriptor );
        assertAndAdjustScmPrivateKeyPassPhrase( config, rereadDescriptor );

        assertEquals( "compare configuration", config.build(), rereadDescriptor );
    }

    public void testWriteToWorkingDirectory()
        throws Exception
    {
        File file = getTestFile( "target/test-classes/new/release.properties" );
        file.delete();
        assertFalse( "Check file doesn't exist", file.exists() );
        file.getParentFile().mkdirs();

        ReleaseDescriptorBuilder config = createReleaseConfigurationForWriting();
        config.setWorkingDirectory( AbstractReleaseTestCase.getPath( file.getParentFile() ) );

        store.write( config.build() );

        ReleaseDescriptorBuilder rereadDescriptorBuilder = store.read( file );
        rereadDescriptorBuilder.setWorkingDirectory( AbstractReleaseTestCase.getPath( file.getParentFile() ) );

        assertAndAdjustScmPassword( config, rereadDescriptorBuilder.build() );
        assertAndAdjustScmPrivateKeyPassPhrase( config, rereadDescriptorBuilder.build() );

        assertEquals( "compare configuration", config.build(), rereadDescriptorBuilder.build() );
    }

    public void testWriteToNewFileRequiredOnly()
        throws ReleaseDescriptorStoreException
    {
        File file = getTestFile( "target/test-classes/new-release.properties" );
        file.delete();
        assertFalse( "Check file doesn't exist", file.exists() );

        ReleaseDescriptorBuilder config = new ReleaseDescriptorBuilder();
        config.setCompletedPhase( "completed-phase-write" );
        config.setScmSourceUrl( "url-write" );

        store.write( config.build(), file );

        ReleaseDescriptor rereadDescriptor = store.read( file ).build();

        assertEquals( "compare configuration", config.build(), rereadDescriptor );
    }

    public void testWriteToNewFileDottedIds()
        throws ReleaseDescriptorStoreException
    {
        File file = getTestFile( "target/test-classes/new-release.properties" );
        file.delete();
        assertFalse( "Check file doesn't exist", file.exists() );

        ReleaseDescriptorBuilder config = new ReleaseDescriptorBuilder();
        config.setCompletedPhase( "completed-phase-write" );
        config.setScmSourceUrl( "url-write" );

        config.addReleaseVersion( "group.id:artifact.id", "1.1" );
        config.addDevelopmentVersion( "group.id:artifact.id", "1.2-SNAPSHOT" );

        IdentifiedScm scm = new IdentifiedScm();
        scm.setId( "id" );
        scm.setConnection( "connection" );
        scm.setDeveloperConnection( "devConnection" );
        scm.setTag( "tag" );
        scm.setUrl( "url" );
        config.addOriginalScmInfo( "group.id:artifact.id", scm );

        store.write( config.build(), file );

        ReleaseDescriptor rereadDescriptor = store.read( file ).build();

        assertEquals( "compare configuration", config.build(), rereadDescriptor );
    }

    public void testWriteToNewFileNullMappedScm()
        throws ReleaseDescriptorStoreException
    {
        File file = getTestFile( "target/test-classes/new-release.properties" );
        file.delete();
        assertFalse( "Check file doesn't exist", file.exists() );

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setCompletedPhase( "completed-phase-write" );
        builder.setScmSourceUrl( "url-write" );

        builder.addReleaseVersion( "group.id:artifact.id", "1.1" );
        builder.addDevelopmentVersion( "group.id:artifact.id", "1.2-SNAPSHOT" );

        builder.addOriginalScmInfo( "group.id:artifact.id", null );

        store.write( builder.build(), file );

        ReleaseDescriptor rereadDescriptor = store.read( file ).build();

        assertNull( "check null scm is mapped correctly",
                    rereadDescriptor.getOriginalScmInfo( "group.id:artifact.id" ) );

        assertEquals( "compare configuration", builder.build(), rereadDescriptor );
    }

    public void testOverwriteFile()
        throws Exception
    {
        File file = getTestFile( "target/test-classes/rewrite-release.properties" );
        assertTrue( "Check file already exists", file.exists() );

        ReleaseDescriptorBuilder config = createReleaseConfigurationForWriting();

        store.write( config.build(), file );

        ReleaseDescriptor rereadDescriptor = store.read( file ).build();

        assertAndAdjustScmPassword( config, rereadDescriptor );
        assertAndAdjustScmPrivateKeyPassPhrase( config, rereadDescriptor );

        assertEquals( "compare configuration", config.build(), rereadDescriptor );
    }
    
    public void testDeleteFile()
        throws ReleaseDescriptorStoreException, IOException
    {
        File file = getTestFile( "target/test-classes/delete/release.properties" );
        file.getParentFile().mkdirs();
        file.createNewFile();
        assertTrue( "Check file already exists", file.exists() );

        ReleaseDescriptorBuilder config = createReleaseConfigurationForWriting();
        config.setWorkingDirectory( AbstractReleaseTestCase.getPath( file.getParentFile() ) );

        store.delete( config.build() );

        assertFalse( "Check file already exists", file.exists() );
    }

    public void testMissingDeleteFile()
        throws ReleaseDescriptorStoreException, IOException
    {
        File file = getTestFile( "target/test-classes/delete/release.properties" );
        file.getParentFile().mkdirs();
        file.delete();
        assertFalse( "Check file already exists", file.exists() );

        ReleaseDescriptorBuilder config = createReleaseConfigurationForWriting();
        config.setWorkingDirectory( AbstractReleaseTestCase.getPath( file.getParentFile() ) );

        store.delete( config.build() );

        assertFalse( "Check file already exists", file.exists() );
    }
    
    public void testWriteEncryptedProperties()
        throws Exception
    {
        final String scmPassword = "s3cr3t_SCMPASSWORD";
        final String scmPassPhrase = "s3cr3t_SCMPASSPHRASE";

        ReleaseDescriptorBuilder config = new ReleaseDescriptorBuilder();
        config.setCompletedPhase( "completed-phase-write" );
        config.setScmSourceUrl( "url-write" );
        
        config.setScmPassword( scmPassword );
        config.setScmPrivateKeyPassPhrase( scmPassPhrase );

        File file = getTestFile( "target/test-classes/encrypt/release.properties" );
        file.getParentFile().mkdirs();
        
        store.write( config.build(), file );
        
        Properties persistedProperties = new Properties();
        try ( InputStream is = new FileInputStream( file ) )
        {
            persistedProperties.load( is );
        }

        String persistedPassword = persistedProperties.getProperty( "scm.password" );
        assertNotNull( persistedPassword );
        assertNotEquals( scmPassword, persistedPassword );        

        String persistedPassPhrase = persistedProperties.getProperty( "scm.passphrase" );
        assertNotNull( persistedPassPhrase );
        assertNotEquals( scmPassPhrase, persistedPassPhrase );
        
        ReleaseDescriptorBuilder builder = store.read( file );
        BuilderReleaseDescriptor descriptor = builder.build();
        assertEquals( scmPassword, descriptor.getScmPassword() );
        assertEquals( scmPassPhrase, descriptor.getScmPrivateKeyPassPhrase() );
    }

    private ReleaseDescriptorBuilder createReleaseConfigurationForWriting()
    {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setCompletedPhase( "completed-phase-write" );
        builder.setCommitByProject( true );
        builder.setScmSourceUrl( "url-write" );
        builder.setScmId( "id-write" );
        builder.setScmUsername( "username-write" );
        builder.setScmPassword( "password-write" );
        builder.setScmPrivateKey( "private-key-write" );
        builder.setScmPrivateKeyPassPhrase( "passphrase-write" );
        builder.setScmTagBase( "tag-base-write" );
        builder.setScmBranchBase( "branch-base-write" );
        builder.setScmReleaseLabel( "tag-write" );
        builder.setAdditionalArguments( "additional-args-write" );
        builder.setPreparationGoals( "preparation-goals-write" );
        builder.setCompletionGoals( "completion-goals-write" );
        builder.setPomFileName( "pom-file-name-write" );

        builder.addReleaseVersion( "groupId:artifactId", "1.0" );
        builder.addDevelopmentVersion( "groupId:artifactId", "1.1-SNAPSHOT" );

        IdentifiedScm scm = new IdentifiedScm();
        scm.setId( "id-write" );
        scm.setConnection( "connection-write" );
        scm.setDeveloperConnection( "developerConnection-write" );
        scm.setUrl( "url-write" );
        scm.setTag( "tag-write" );
        builder.addOriginalScmInfo( "groupId:artifactId", scm );

        scm = new IdentifiedScm();
        scm.setConnection( "connection-write" );
        // omit optional elements
        builder.addOriginalScmInfo( "groupId:subproject1", scm );

        return builder;
    }

    private static void assertDefaultReleaseConfiguration( BuilderReleaseDescriptor config )
    {
        assertNull( "Expected no completedPhase", config.getCompletedPhase() );
        assertFalse( "Expected no commitPerProject", config.isCommitByProject() );
        assertNull( "Expected no id", config.getScmId() );
        assertNull( "Expected no url", config.getScmSourceUrl() );
        assertNull( "Expected no username", config.getScmUsername() );
        assertNull( "Expected no password", config.getScmPassword() );
        assertNull( "Expected no privateKey", config.getScmPrivateKey() );
        assertNull( "Expected no passphrase", config.getScmPrivateKeyPassPhrase() );
        assertNull( "Expected no tagBase", config.getScmTagBase() );
        assertNull( "Expected no tag", config.getScmReleaseLabel() );
        assertNull( "Expected no additional arguments", config.getAdditionalArguments() );
        assertNull( "Expected no preparation goals", config.getPreparationGoals() );
        assertNull( "Expected no completion goals", config.getCompletionGoals() );
        assertNull( "Expected no pom file name", config.getPomFileName() );

        assertNull( "Expected no workingDirectory", config.getWorkingDirectory() );
        assertFalse( "Expected no generateReleasePoms", config.isGenerateReleasePoms() );
        assertFalse( "Expected no useEditMode", config.isScmUseEditMode() );
        assertTrue( "Expected default interactive", config.isInteractive() );
        assertFalse( "Expected no addScema", config.isAddSchema() );

        for ( ReleaseStageVersions versions : config.getProjectVersions().values() )
        {
            assertNull( "Expected no release version mappings", versions.getRelease() );
            assertNull( "Expected no dev version mappings", versions.getDevelopment() );
        }
        assertTrue( "Expected no scm mappings", config.getOriginalScmInfo().isEmpty() );
        assertNotNull( "Expected resolved snapshot dependencies map", config.getResolvedSnapshotDependencies() );
    }

    public ReleaseDescriptorBuilder createMergeConfiguration()
        throws IOException
    {
        ReleaseDescriptorBuilder releaseDescriptor = new ReleaseDescriptorBuilder();

        releaseDescriptor.setScmSourceUrl( "scm-url" );
        releaseDescriptor.setScmUsername( "username" );
        // Not setting other optional SCM settings for brevity
        
        File workingDir = getTestFile( "target/test-working-directory" );
        if ( !workingDir.exists() )
        {
            assertTrue( "Failed to create the directory, along with all necessary parent directories",
                        workingDir.mkdirs() );
        }
        
        releaseDescriptor.setWorkingDirectory( AbstractReleaseTestCase.getPath( workingDir ) );
        // Not setting non-override setting completedPhase

        return releaseDescriptor;
    }

    private void assertAndAdjustScmPassword( ReleaseDescriptorBuilder expected, ReleaseDescriptor original )
        throws Exception
    {
        String expectedPassword = expected.build().getScmPassword();
        String originalPassword = original.getScmPassword();

        // encrypting the same password twice doesn't have to be the same result
        if ( expectedPassword != null ? !expectedPassword.equals( originalPassword ) : originalPassword != null )
        {
            assertEquals( secDispatcher.decrypt( expectedPassword ), secDispatcher.decrypt( originalPassword ) );

            expected.setScmPassword( originalPassword );
        }
        assertEquals( expected.build().getScmPassword(), original.getScmPassword() );
    }

    private void assertAndAdjustScmPrivateKeyPassPhrase( ReleaseDescriptorBuilder expected, ReleaseDescriptor original )
        throws Exception
    {
        String expectedPassPhrase = expected.build().getScmPrivateKeyPassPhrase();
        String originalPassPhrase = original.getScmPrivateKeyPassPhrase();

        // encrypting the same passphrase twice doesn't have to be the same result
        if ( expectedPassPhrase != null ? !expectedPassPhrase.equals( originalPassPhrase ) : originalPassPhrase != null )
        {
            assertEquals( secDispatcher.decrypt( expectedPassPhrase ), secDispatcher.decrypt( originalPassPhrase ) );

            expected.setScmPrivateKeyPassPhrase( originalPassPhrase );
        }
        assertEquals( expected.build().getScmPrivateKeyPassPhrase(), original.getScmPrivateKeyPassPhrase() );
    }

    private ReleaseDescriptorBuilder createExpectedReleaseConfiguration()
    {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setCompletedPhase( "step1" );
        builder.setCommitByProject( true );
        builder.setScmId( "scm-id" );
        builder.setScmSourceUrl( "scm-url" );
        builder.setScmUsername( "username" );
        builder.setScmPassword( "password" );
        builder.setScmPrivateKey( "private-key" );
        builder.setScmPrivateKeyPassPhrase( "passphrase" );
        builder.setScmTagBase( "tagBase" );
        builder.setScmTagNameFormat( "expectedTagNameFormat" );
        builder.setScmBranchBase( "branchBase" );
        builder.setScmReleaseLabel( "tag" );
        builder.setAdditionalArguments( "additional-arguments" );
        builder.setPreparationGoals( "preparation-goals" );
        builder.setCompletionGoals( "completion-goals" );
        builder.setPomFileName( "pom-file-name" );
        builder.setWorkingDirectory( null );
        builder.setGenerateReleasePoms( false );
        builder.setScmUseEditMode( false );
        builder.setInteractive( true );
        builder.setAddSchema( false );
        builder.addReleaseVersion( "groupId:artifactId1", "2.0" );
        builder.addReleaseVersion( "groupId:artifactId2", "3.0" );
        builder.addDevelopmentVersion( "groupId:artifactId1", "2.1-SNAPSHOT" );
        builder.addDevelopmentVersion( "groupId:artifactId2", "3.0.1-SNAPSHOT" );
        IdentifiedScm scm = new IdentifiedScm();
        scm.setId( "id" );
        scm.setConnection( "connection" );
        scm.setDeveloperConnection( "developerConnection" );
        scm.setUrl( "url" );
        scm.setTag( "tag" );
        builder.addOriginalScmInfo( "groupId:artifactId1", scm );
        scm = new IdentifiedScm();
        scm.setId( null );
        scm.setConnection( "connection2" );
        scm.setUrl( "url2" );
        scm.setTag( null );
        scm.setDeveloperConnection( null );
        builder.addOriginalScmInfo( "groupId:artifactId2", scm );
        builder.addDependencyReleaseVersion( "external:artifactId",  "1.0" );
        builder.addDependencyDevelopmentVersion( "external:artifactId", "1.1-SNAPSHOT" );

        return builder;
    }

}
