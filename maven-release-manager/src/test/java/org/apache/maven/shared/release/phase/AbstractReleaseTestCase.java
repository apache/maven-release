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

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang.SystemUtils;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Repository;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingRequest.RepositoryMerging;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.project.ProjectBuildingResult;
import org.apache.maven.project.ProjectSorter;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.apache.maven.shared.release.PlexusJUnit4TestCase;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.sonatype.aether.impl.internal.SimpleLocalRepositoryManager;
import org.sonatype.aether.repository.WorkspaceReader;
import org.sonatype.aether.repository.WorkspaceRepository;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Comparison;
import org.xmlunit.diff.ComparisonResult;
import org.xmlunit.diff.ComparisonType;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.DifferenceEvaluator;
import org.xmlunit.diff.ElementSelectors;

/**
 * Base class for some release tests.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class AbstractReleaseTestCase
    extends PlexusJUnit4TestCase
{
    protected ProjectBuilder projectBuilder;

    protected ArtifactRepository localRepository;
    
    private ArtifactFactory artifactFactory;

    protected ReleasePhase phase;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        projectBuilder = lookup( ProjectBuilder.class );
        artifactFactory = lookup( ArtifactFactory.class ); 

        ArtifactRepositoryLayout layout = lookup( ArtifactRepositoryLayout.class, "default" );
        String localRepoPath = getTestFile( "target/local-repository" ).getAbsolutePath().replace( '\\', '/' );
        localRepository = new MavenArtifactRepository( "local", "file://" + localRepoPath, layout, null, null );
    }

    protected Path getWorkingDirectory( String workingDir )
    {
        return Paths.get( getBasedir(), "target/test-classes" ).resolve( Paths.get( "projects", workingDir ) ) ;
    }

    protected List<MavenProject> createReactorProjects( String path, String subpath )
        throws Exception
    {
        return createReactorProjects( path, path, subpath );
    }
    
    protected ReleaseDescriptorBuilder createReleaseDescriptorBuilder( List<MavenProject> reactorProjects )
    {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        for ( MavenProject project : reactorProjects )
        {
            builder.putOriginalVersion( project.getGroupId() + ':' + project.getArtifactId(), project.getVersion() );
        }
        return builder;
    }

    /**
     *
     * @param sourcePath sourceDirectory to copy from
     * @param targetPath targetDirectory to copy to
     * @param executionRoot sub directory of targetPath in case the root pom.xml is not used (e.g. flat projects)
     * @return all Maven projects
     * @throws Exception if any occurs
     */
    protected List<MavenProject> createReactorProjects( String sourcePath, String targetPath, String executionRoot )
        throws Exception
    {
        final Path testCaseRootFrom = Paths.get( getBasedir(), "src/test/resources" ).resolve( Paths.get( "projects", sourcePath ) ) ;

        final Path testCaseRootTo = getWorkingDirectory( targetPath );

        // Recopy the test resources since they are modified in some tests
        Files.walkFileTree( testCaseRootFrom, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile( Path file, BasicFileAttributes attrs )
                throws IOException
            {
                Path relPath = testCaseRootFrom.relativize( file );

                if ( !relPath.toFile().getName().startsWith( "expected-" ) )
                {
                  Files.createDirectories( testCaseRootTo.resolve( relPath ).getParent() );

                  Files.copy( file, testCaseRootTo.resolve( relPath ), StandardCopyOption.REPLACE_EXISTING );
                }

                return FileVisitResult.CONTINUE;
            }
        });

        Path projectFile;
        if ( executionRoot == null )
        {
            projectFile = testCaseRootTo.resolve( "pom.xml" );
        }
        else
        {
            projectFile = testCaseRootTo.resolve( Paths.get( executionRoot, "pom.xml" ) );
        }

        List<ArtifactRepository> repos =
            Collections.<ArtifactRepository>singletonList( new DefaultArtifactRepository( "central",
                                                                                          getRemoteRepositoryURL(),
                                                                                          new DefaultRepositoryLayout() ) );

        Repository repository = new Repository();
        repository.setId( "central" );
        repository.setUrl( getRemoteRepositoryURL() );

        Profile profile = new Profile();
        profile.setId( "profile" );
        profile.addRepository( repository );

        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest();
        buildingRequest.setLocalRepository( localRepository );
        buildingRequest.setRemoteRepositories( repos );
        buildingRequest.setPluginArtifactRepositories( repos );
        buildingRequest.setRepositoryMerging( RepositoryMerging.REQUEST_DOMINANT );
        MavenRepositorySystemSession repositorySession = new MavenRepositorySystemSession();
        repositorySession.setLocalRepositoryManager( new SimpleLocalRepositoryManager( localRepository.getBasedir() ) );
        buildingRequest.setRepositorySession( repositorySession );
        buildingRequest.addProfile( profile );
        buildingRequest.setActiveProfileIds( Arrays.asList( profile.getId() ) );
        buildingRequest.setResolveDependencies( true );

        List<ProjectBuildingResult> buildingResults =
            projectBuilder.build( Collections.singletonList( projectFile.toFile() ), true, buildingRequest );

        List<MavenProject> reactorProjects = new ArrayList<>();
        for ( ProjectBuildingResult buildingResult : buildingResults )
        {
            reactorProjects.add( buildingResult.getProject() ) ;
        }

        WorkspaceReader simpleReactorReader = new SimpleReactorWorkspaceReader( reactorProjects );
        repositorySession.setWorkspaceReader( simpleReactorReader );

        ProjectSorter sorter = new ProjectSorter( reactorProjects );
        reactorProjects = sorter.getSortedProjects();

        List<MavenProject> resolvedProjects = new ArrayList<>( reactorProjects.size() );
        for ( MavenProject project  : reactorProjects )
        {
            MavenProject resolvedProject = projectBuilder.build( project.getFile(), buildingRequest ).getProject();
            
            // from LifecycleDependencyResolver
            if ( project.getDependencyArtifacts() == null )
            {
                try
                {
                    resolvedProject.setDependencyArtifacts( resolvedProject.createArtifacts( artifactFactory, null, null ) );
                }
                catch ( InvalidDependencyVersionException e )
                {
                    throw new LifecycleExecutionException( e );
                }
            }
            
            resolvedProjects.add( resolvedProject );
        }
        return resolvedProjects;
    }

    protected static Map<String,MavenProject> getProjectsAsMap( List<MavenProject> reactorProjects )
    {
        Map<String,MavenProject> map = new HashMap<>();
        for ( MavenProject project : reactorProjects )
        {
            map.put( ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() ), project );
        }
        return map;
    }

    protected boolean comparePomFiles( List<MavenProject> reactorProjects )
        throws IOException
    {
        return comparePomFiles( reactorProjects, true );
    }

    protected boolean comparePomFiles( List<MavenProject> reactorProjects, boolean normalizeLineEndings )
        throws IOException
    {
        comparePomFiles( reactorProjects, "", normalizeLineEndings );

        // TODO: return void since this is redundant
        return true;
    }

    protected void comparePomFiles( List<MavenProject> reactorProjects, String expectedFileSuffix )
        throws IOException
    {
        comparePomFiles( reactorProjects, expectedFileSuffix, true );
    }

    protected void comparePomFiles( List<MavenProject> reactorProjects, String expectedFileSuffix, boolean normalizeLineEndings )
        throws IOException
    {
        for ( MavenProject project : reactorProjects )
        {
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

        comparePomFiles( expectedFile, actualFile, normalizeLineEndings, false );
    }

    protected void comparePomFiles( File expectedFile, File actualFile )
        throws IOException
    {
        comparePomFiles( expectedFile, actualFile, true, false );
    }

    protected void comparePomFiles( File expectedFile, File actualFile, boolean normalizeLineEndings, boolean ignoreComments )
        throws IOException
    {
        StringBuffer sb = new StringBuffer( "Check the transformed POM " + actualFile );
        sb.append( SystemUtils.LINE_SEPARATOR );

        final String remoteRepositoryURL = getRemoteRepositoryURL();

        DiffBuilder diffBuilder = DiffBuilder.compare( expectedFile ).withTest( actualFile );
        if ( normalizeLineEndings )
        {
            diffBuilder = diffBuilder.normalizeWhitespace();
        }
        if ( ignoreComments )
        {
            diffBuilder.ignoreComments();
        }
        // Order of elements has changed between M2 and M3, so match by name
        diffBuilder.withNodeMatcher( new DefaultNodeMatcher( ElementSelectors.byName ) ).checkForSimilar();

        diffBuilder.withDifferenceEvaluator( new DifferenceEvaluator()
        {
            @Override
            public ComparisonResult evaluate( Comparison comparison, ComparisonResult outcome )
            {
                if ( "${remoterepo}".equals( comparison.getControlDetails().getValue() ) &&
                                remoteRepositoryURL.equals( comparison.getTestDetails().getValue() ) )
                {
                    return ComparisonResult.EQUAL;
                }
                else if ( outcome == ComparisonResult.DIFFERENT
                    && comparison.getType() == ComparisonType.CHILD_NODELIST_SEQUENCE )
                {
                    // Order of elements has changed between M2 and M3
                    return ComparisonResult.EQUAL;
                }
                else if ( outcome == ComparisonResult.DIFFERENT
                                && comparison.getType() == ComparisonType.TEXT_VALUE
                                && "${project.build.directory}/site".equals( comparison.getTestDetails().getValue() ) )
                {
                    // M2 was target/site, M3 is ${project.build.directory}/site
                    return ComparisonResult.EQUAL;
                }
                else
                {
                    return outcome;
                }
            }
        } );

        Diff diff = diffBuilder.build();

        sb.append( diff.toString() );

        assertFalse( sb.toString(), diff.hasDifferences() );
    }

    private String getRemoteRepositoryURL()
      throws IOException
    {
        File testFile = getTestFile( "src/test/remote-repository" );
        if (testFile.getAbsolutePath().equals( testFile.getCanonicalPath() ) )
        {
            return "file://" + getTestFile( "src/test/remote-repository" ).getAbsolutePath().replace( '\\', '/' );
        }
        return "file://" + getTestFile( "src/test/remote-repository" ).getCanonicalPath().replace( '\\', '/' );
    }

    public static String getPath( File file )
        throws IOException
    {
        return file.toPath().toRealPath( LinkOption.NOFOLLOW_LINKS ).toString();
    }

    /**
     * WorkspaceReader to find versions and artifacts from reactor
     */
    private static final class SimpleReactorWorkspaceReader
        implements WorkspaceReader
    {
        private final List<MavenProject> reactorProjects;

        private SimpleReactorWorkspaceReader( List<MavenProject> reactorProjects )
        {
            this.reactorProjects = reactorProjects;
        }

        @Override
        public WorkspaceRepository getRepository()
        {
            return null;
        }

        @Override
        public List<String> findVersions( org.sonatype.aether.artifact.Artifact artifact )
        {
            for ( MavenProject mavenProject : reactorProjects )
            {
                if ( Objects.equals( artifact.toString(), mavenProject.getArtifact().toString() ) )
                {
                    return Collections.singletonList( mavenProject.getArtifact().getVersion() );
                }
            }
            return Collections.emptyList();
        }

        @Override
        public File findArtifact( org.sonatype.aether.artifact.Artifact artifact )
        {
            for ( MavenProject mavenProject : reactorProjects )
            {
                String pom = mavenProject.getGroupId() + ':' + mavenProject.getArtifactId() + ":pom:"
                    + mavenProject.getVersion();
                if ( Objects.equals( artifact.toString(), pom ) )
                {
                    return mavenProject.getFile();
                }
                else if ( Objects.equals( artifact.toString(), mavenProject.getArtifact().toString() ) )
                {
                    // just an existing, content doesn't matter
                    return mavenProject.getFile();
                }
            }
            return null;
        }
    }
}