package org.apache.maven.shared.release.phase;

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

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Repository;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectSorter;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.shared.release.scm.DefaultScmRepositoryConfigurator;
import org.apache.maven.shared.release.scm.ScmRepositoryConfigurator;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.jmock.Mock;

/**
 * Base class for some release tests.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class AbstractReleaseTestCase
    extends PlexusTestCase
{
    protected MavenProjectBuilder projectBuilder;

    protected ArtifactRepository localRepository;

    protected ReleasePhase phase;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        projectBuilder = (MavenProjectBuilder) lookup( MavenProjectBuilder.ROLE );

        ArtifactRepositoryLayout layout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, "default" );
        String localRepoPath = getTestFile( "target/local-repository" ).getAbsolutePath().replace( '\\', '/' );
        localRepository = new DefaultArtifactRepository( "local", "file://" + localRepoPath, layout );
    }

    private Map createManagedVersionMap( String projectId, DependencyManagement dependencyManagement,
                                         ArtifactFactory artifactFactory )
        throws ProjectBuildingException
    {
        Map map;
        if ( dependencyManagement != null && dependencyManagement.getDependencies() != null )
        {
            map = new HashMap();
            for ( Iterator i = dependencyManagement.getDependencies().iterator(); i.hasNext(); )
            {
                Dependency d = (Dependency) i.next();

                try
                {
                    VersionRange versionRange = VersionRange.createFromVersionSpec( d.getVersion() );
                    Artifact artifact = artifactFactory.createDependencyArtifact( d.getGroupId(), d.getArtifactId(),
                                                                                  versionRange, d.getType(),
                                                                                  d.getClassifier(), d.getScope() );
                    map.put( d.getManagementKey(), artifact );
                }
                catch ( InvalidVersionSpecificationException e )
                {
                    throw new ProjectBuildingException( projectId, "Unable to parse version '" + d.getVersion() +
                        "' for dependency '" + d.getManagementKey() + "': " + e.getMessage(), e );
                }
            }
        }
        else
        {
            map = Collections.EMPTY_MAP;
        }
        return map;
    }

    protected List createReactorProjects( String path, String subpath )
        throws Exception
    {
        return createReactorProjects( path, path, subpath );
    }

    protected List createReactorProjects( String path, String targetPath, String subpath )
        throws Exception
    {
        File testFile = getTestFile( "target/test-classes/projects/" + path + subpath + "/pom.xml" );
        Stack projectFiles = new Stack();
        projectFiles.push( testFile );

        List repos =
            Collections.singletonList( new DefaultArtifactRepository( "central", getRemoteRepositoryURL(), new DefaultRepositoryLayout() ) );

        Repository repository = new Repository();
        repository.setId( "central" );
        repository.setUrl( getRemoteRepositoryURL() );

        ProfileManager profileManager = new DefaultProfileManager( getContainer() );
        Profile profile = new Profile();
        profile.setId( "profile" );
        profile.addRepository( repository );
        profileManager.addProfile( profile );
        profileManager.activateAsDefault( profile.getId() );

        List reactorProjects = new ArrayList();
        while ( !projectFiles.isEmpty() )
        {
            File file = (File) projectFiles.pop();

            // Recopy the test resources since they are modified in some tests
            String filePath = file.getPath();
            int index = filePath.indexOf( "test-classes" ) + "test-classes".length() + 1;
            filePath = filePath.substring( index ).replace( '\\', '/' );

            File newFile = getTestFile( "target/test-classes/" + StringUtils.replace( filePath, path, targetPath ) );
            FileUtils.copyFile( getTestFile( "src/test/resources/" + filePath ), newFile );

            MavenProject project = projectBuilder.build( newFile, localRepository, profileManager );

            for ( Iterator i = project.getModules().iterator(); i.hasNext(); )
            {
                String module = (String) i.next();

                projectFiles.push( new File( file.getParentFile(), module + "/pom.xml" ) );
            }

            reactorProjects.add( project );
        }

        ProjectSorter sorter = new ProjectSorter( reactorProjects );

        reactorProjects = sorter.getSortedProjects();

        ArtifactFactory artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );
        ArtifactCollector artifactCollector = (ArtifactCollector) lookup( ArtifactCollector.class.getName() );
        ArtifactMetadataSource artifactMetadataSource = (ArtifactMetadataSource) lookup( ArtifactMetadataSource.ROLE );

        // pass back over and resolve dependencies - can't be done earlier as the order may not be correct
        for ( Iterator i = reactorProjects.iterator(); i.hasNext(); )
        {
            MavenProject project = (MavenProject) i.next();

            project.setRemoteArtifactRepositories( repos );
            project.setPluginArtifactRepositories( repos );

            Artifact projectArtifact = project.getArtifact();

            Map managedVersions = createManagedVersionMap(
                ArtifactUtils.versionlessKey( projectArtifact.getGroupId(), projectArtifact.getArtifactId() ),
                project.getDependencyManagement(), artifactFactory );

            project.setDependencyArtifacts( project.createArtifacts( artifactFactory, null, null ) );

            ArtifactResolutionResult result = artifactCollector.collect( project.getDependencyArtifacts(),
                                                                         projectArtifact, managedVersions,
                                                                         localRepository, repos, artifactMetadataSource,
                                                                         null, Collections.EMPTY_LIST );

            project.setArtifacts( result.getArtifacts() );
        }

        return reactorProjects;
    }

    protected void setMockScmManager( Mock scmManagerMock )
        throws Exception
    {
        ScmManager scmManager = (ScmManager) scmManagerMock.proxy();
        DefaultScmRepositoryConfigurator configurator =
            (DefaultScmRepositoryConfigurator) lookup( ScmRepositoryConfigurator.ROLE );
        configurator.setScmManager( scmManager );
    }

    protected static Map getProjectsAsMap( List reactorProjects )
    {
        Map map = new HashMap();
        for ( Iterator i = reactorProjects.iterator(); i.hasNext(); )
        {
            MavenProject project = (MavenProject) i.next();

            map.put( ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() ), project );
        }
        return map;
    }

    protected boolean comparePomFiles( List reactorProjects )
        throws IOException
    {
        return comparePomFiles( reactorProjects, true );
    }

    protected boolean comparePomFiles( List reactorProjects, boolean normalizeLineEndings )
        throws IOException
    {
        comparePomFiles( reactorProjects, "", normalizeLineEndings );

        // TODO: return void since this is redundant
        return true;
    }

    protected void comparePomFiles( List reactorProjects, String expectedFileSuffix )
        throws IOException
    {
        comparePomFiles( reactorProjects, expectedFileSuffix, true );
    }

    protected void comparePomFiles( List reactorProjects, String expectedFileSuffix, boolean normalizeLineEndings )
        throws IOException
    {
        for ( Iterator i = reactorProjects.iterator(); i.hasNext(); )
        {
            MavenProject project = (MavenProject) i.next();

            comparePomFiles( project, expectedFileSuffix, normalizeLineEndings );
        }
    }

    protected void comparePomFiles( MavenProject project, String expectedFileSuffix )
        throws IOException
    {
        comparePomFiles( project, expectedFileSuffix, true );
    }

    protected void comparePomFiles( MavenProject project, String expectedFileSuffix, boolean normalizeLineEndings )
        throws IOException
    {
        File actualFile = project.getFile();
        File expectedFile = new File( actualFile.getParentFile(), "expected-pom" + expectedFileSuffix + ".xml" );

        comparePomFiles( expectedFile, actualFile, normalizeLineEndings );
    }

    protected void comparePomFiles( File expectedFile, File actualFile )
        throws IOException
    {
        comparePomFiles( expectedFile, actualFile, true );
    }

    protected void comparePomFiles( File expectedFile, File actualFile, boolean normalizeLineEndings )
        throws IOException
    {
        String actual = read( actualFile, normalizeLineEndings );
        String expected = read( expectedFile, normalizeLineEndings );
        expected = expected.replaceAll( "\\$\\{remoterepo\\}", getRemoteRepositoryURL() );
        assertEquals( "Check the transformed POM", expected, actual );
    }

    /**
     * Mock-up of {@link ReleaseUtil#readXmlFile(File)}, except this one REMOVES line endings.
     * There is something fishy about the line ending conversion in that method, and it's not the
     * class under test in these test cases.
     */
    private String read( File file )
        throws IOException
    {
        return read( file, true );
    }

    /**
     * Mock-up of {@link ReleaseUtil#readXmlFile(File)}, except this one REMOVES line endings. There is something fishy
     * about the line ending conversion in that method, and it's not the class under test in these test cases.
     * 
     * @param normalizeLineEndings TODO
     */
    private String read( File file, boolean normalizeLineEndings )
        throws IOException
    {
        Reader reader = null;
        try
        {
            reader = ReaderFactory.newXmlReader( file );
            String xml = IOUtil.toString( reader );
            return normalizeLineEndings ? ReleaseUtil.normalizeLineEndings( xml, "" ) : xml;
        }
        finally
        {
            IOUtil.close( reader );
        }
    }

    private String getRemoteRepositoryURL()
    {
        return "file://" + getTestFile( "src/test/remote-repository" ).getAbsolutePath().replace( '\\', '/' );
    }
}
