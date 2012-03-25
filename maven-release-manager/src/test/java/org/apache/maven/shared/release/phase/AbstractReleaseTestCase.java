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

import org.apache.commons.lang.SystemUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.manager.WagonManager;
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
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.context.DefaultContext;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceListener;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

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

    private static final DefaultContext EMPTY_CONTEXT = new DefaultContext()
    {
        public Object get( Object key )
            throws ContextException
        {
            return null;
        }
    };

    protected void setUp()
        throws Exception
    {
        super.setUp();

        projectBuilder = (MavenProjectBuilder) lookup( MavenProjectBuilder.ROLE );

        ArtifactRepositoryLayout layout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, "default" );
        String localRepoPath = getTestFile( "target/local-repository" ).getAbsolutePath().replace( '\\', '/' );
        localRepository = new DefaultArtifactRepository( "local", "file://" + localRepoPath, layout );
    }

    protected void tearDown()
        throws Exception
    {
        // unhook circular references to the container that would avoid memory being cleaned up
        ( (Contextualizable) projectBuilder ).contextualize( EMPTY_CONTEXT );
        ( (Contextualizable) lookup( WagonManager.ROLE ) ).contextualize( EMPTY_CONTEXT );

        super.tearDown();
    }

    private Map<String,Artifact> createManagedVersionMap( String projectId, DependencyManagement dependencyManagement,
                                         ArtifactFactory artifactFactory )
        throws ProjectBuildingException
    {
        Map<String,Artifact> map;
        if ( dependencyManagement != null && dependencyManagement.getDependencies() != null )
        {
            map = new HashMap<String,Artifact>();
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
            map = Collections.emptyMap();
        }
        return map;
    }

    protected List<MavenProject> createReactorProjects( String path, String subpath )
        throws Exception
    {
        return createReactorProjects( path, path, subpath );
    }

    protected List<MavenProject> createReactorProjects( String path, String targetPath, String subpath )
        throws Exception
    {
        File testFile = getTestFile( "target/test-classes/projects/" + path + subpath + "/pom.xml" );
        Stack<File> projectFiles = new Stack<File>();
        projectFiles.push( testFile );

        List<DefaultArtifactRepository> repos =
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

        List<MavenProject> reactorProjects = new ArrayList<MavenProject>();
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

    protected static Map<String,MavenProject> getProjectsAsMap( List<MavenProject> reactorProjects )
    {
        Map<String,MavenProject> map = new HashMap<String,MavenProject>();
        for ( Iterator<MavenProject> i = reactorProjects.iterator(); i.hasNext(); )
        {
            MavenProject project = (MavenProject) i.next();

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
        for ( Iterator<MavenProject> i = reactorProjects.iterator(); i.hasNext(); )
        {
            MavenProject project = i.next();

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
        Reader expected = null;
        Reader actual = null;
        try
        {
            expected = ReaderFactory.newXmlReader( expectedFile );
            actual = ReaderFactory.newXmlReader( actualFile );
            
            StringBuffer sb = new StringBuffer( "Check the transformed POM " + actualFile );
            sb.append( SystemUtils.LINE_SEPARATOR );
            
            final String remoteRepositoryURL = getRemoteRepositoryURL();

            XMLUnit.setNormalizeWhitespace( true );
            
            Diff diff = XMLUnit.compareXML( expected, actual );
            
            diff.overrideDifferenceListener( new DifferenceListener()
            {
                
                public void skippedComparison( Node arg0, Node arg1 )
                {
                    //do nothing
                }
                
                public int differenceFound( Difference difference )
                {
                    if ( "${remoterepo}".equals( difference.getControlNodeDetail().getValue() ) &&
                                    remoteRepositoryURL.equals( difference.getTestNodeDetail().getValue() ) )
                    {
                        return DifferenceListener.RETURN_IGNORE_DIFFERENCE_NODES_IDENTICAL;
                    }
                    else 
                    {
                        return DifferenceListener.RETURN_ACCEPT_DIFFERENCE;
                    }
                }
            });
            diff.appendMessage( sb );
            
            XMLAssert.assertXMLIdentical( diff, true );
        }
        catch ( SAXException e )
        {
            fail( e.getMessage() );
        }
        finally 
        {
            IOUtil.close( expected );
            IOUtil.close( actual );
        }
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
        return ReleaseUtil.isSymlink( file ) ? file.getCanonicalPath() : file.getAbsolutePath();
    }    
}