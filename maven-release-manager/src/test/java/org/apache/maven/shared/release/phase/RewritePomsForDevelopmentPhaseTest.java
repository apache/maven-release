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

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.util.ReleaseUtil;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Test the SCM modification check phase.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class RewritePomsForDevelopmentPhaseTest
    extends AbstractEditModeRewritingReleasePhaseTestCase
{
    private static final String NEXT_VERSION = "1.1-SNAPSHOT";

    private static final String ALTERNATIVE_NEXT_VERSION = "2.1-SNAPSHOT";

    private static final String RELEASE_VERSION = "1.0";

    private static final String ALTERNATIVE_RELEASE_VERSION = "2.0";

    protected void setUp()
        throws Exception
    {
        super.setUp();

        phase = (ReleasePhase) lookup( ReleasePhase.ROLE, "rewrite-poms-for-development" );
    }

    public void testSimulateRewrite()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjectsWhenSimulated( "basic-pom" );
        ReleaseDescriptor config = createDescriptorFromBasicPom( reactorProjects );
        config.mapReleaseVersion( "groupId:artifactId", RELEASE_VERSION );
        config.mapDevelopmentVersion( "groupId:artifactId", NEXT_VERSION );

        String expected = readTestProjectFile( "basic-pom/pom.xml" );

        phase.simulate( config, new DefaultReleaseEnvironment(), reactorProjects );

        String actual = readTestProjectFile( "basic-pom/pom.xml" );
        assertEquals( "Check the original POM untouched", expected, actual );

        expected = readTestProjectFile( "basic-pom/expected-pom.xml" );
        actual = readTestProjectFile( "basic-pom/pom.xml.next" );
        assertEquals( "Check the transformed POM", expected, actual );
    }

    private List<MavenProject> createReactorProjectsWhenSimulated( String name )
        throws Exception
    {
        return createReactorProjects( "rewrite-for-release/", "rewrite-for-development/", name );
    }

    public void testSimulateRewriteEjbClientDeps()
        throws Exception
    {
        List<MavenProject> reactorProjects = new LinkedList<MavenProject>( createReactorProjects( "basic-pom-ejb-client-dep/project" ) );
        reactorProjects.addAll( createReactorProjects( "basic-pom-ejb-client-dep/ejb" ) );
        ReleaseDescriptor config = createDescriptorFromBasicPom( reactorProjects );
        config.mapReleaseVersion( "groupId:artifactId", RELEASE_VERSION );
        config.mapDevelopmentVersion( "groupId:artifactId", NEXT_VERSION );
        config.addDevelopmentVersion( ArtifactUtils.versionlessKey( "groupId", "artifactId1" ), NEXT_VERSION );
        config.addReleaseVersion( ArtifactUtils.versionlessKey( "groupId", "artifactId1" ), RELEASE_VERSION );

        String expected = readTestProjectFile( "basic-pom-ejb-client-dep/project/pom.xml" );

        phase.simulate( config, new DefaultReleaseEnvironment(), reactorProjects );

        String actual = readTestProjectFile( "basic-pom-ejb-client-dep/project/pom.xml" );
        assertEquals( "Check the original POM untouched", expected, actual );

        expected = readTestProjectFile( "basic-pom-ejb-client-dep/project/expected-pom.xml" );
        actual = readTestProjectFile( "basic-pom-ejb-client-dep/project/pom.xml.next" );
        assertEquals( "Check the transformed POM", expected, actual );
    }

    public void testClean()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjectsWhenSimulated( "basic-pom" );
        ReleaseDescriptor config = createDescriptorFromBasicPom( reactorProjects );
        config.mapReleaseVersion( "groupId:artifactId", RELEASE_VERSION );
        config.mapDevelopmentVersion( "groupId:artifactId", NEXT_VERSION );

        File testFile = getTestFile( "target/test-classes/projects/rewrite-for-development/basic-pom/pom.xml.next" );
        testFile.delete();
        assertFalse( testFile.exists() );

        phase.simulate( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( testFile.exists() );

        phase.clean( reactorProjects );

        assertFalse( testFile.exists() );
    }

    public void testCleanNotExists()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjectsFromBasicPom();
        ReleaseDescriptor config = createDescriptorFromBasicPom( reactorProjects );
        config.mapReleaseVersion( "groupId:artifactId", RELEASE_VERSION );
        config.mapDevelopmentVersion( "groupId:artifactId", NEXT_VERSION );

        File testFile = getTestFile( "target/test-classes/projects/rewrite-for-development/basic-pom/pom.xml.next" );
        testFile.delete();
        assertFalse( testFile.exists() );

        phase.clean( reactorProjects );

        assertFalse( testFile.exists() );
    }

    public void testRewriteBasicPomUnmappedScm()
        throws Exception
    {
        List<MavenProject> reactorProjects = prepareReactorProjects( "basic-pom", true );

        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );

        mapNextVersion( config, "groupId:artifactId" );

        try
        {
            phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Expected failure" );
        }
        catch ( ReleaseExecutionException e )
        {
            verifyReactorProjects( "basic-pom", true );
        }
    }

    protected String readTestProjectFile( String fileName )
        throws IOException
    {
        return readTestProjectFile( fileName, "rewrite-for-development/" );
    }

    protected String readTestProjectFile( String fileName, String subpath )
        throws IOException
    {
        return ReleaseUtil.readXmlFile( getTestFile( "target/test-classes/projects/"+ subpath + fileName ) );
    }

    protected List<MavenProject> prepareReactorProjects( String path, boolean copyFiles )
        throws Exception
    {
        return createReactorProjects( "rewrite-for-development/", path );
    }

    protected ReleaseDescriptor createDescriptorFromBasicPom( List<MavenProject> reactorProjects )
        throws Exception
    {
        ReleaseDescriptor config = super.createDescriptorFromBasicPom( reactorProjects );

        mapScm( config );

        return config;
    }

    private void mapScm( ReleaseDescriptor config )
    {
        Scm scm = new Scm();
        scm.setConnection( "scm:svn:file://localhost/tmp/scm-repo/trunk" );
        scm.setDeveloperConnection( "scm:svn:file://localhost/tmp/scm-repo/trunk" );
        scm.setUrl( "file://localhost/tmp/scm-repo/trunk" );
        config.mapOriginalScmInfo( "groupId:artifactId", scm );
    }

    protected void mapAlternateNextVersion( ReleaseDescriptor config, String projectId )
    {
        config.mapReleaseVersion( projectId, ALTERNATIVE_RELEASE_VERSION );
        config.mapDevelopmentVersion( projectId, ALTERNATIVE_NEXT_VERSION );
    }

    protected void mapNextVersion( ReleaseDescriptor config, String projectId )
    {
        config.mapReleaseVersion( projectId, RELEASE_VERSION );
        config.mapDevelopmentVersion( projectId, NEXT_VERSION );
    }

    protected void unmapNextVersion( ReleaseDescriptor config, String projectId )
    {
        config.mapReleaseVersion( projectId, RELEASE_VERSION );
    }

    protected ReleaseDescriptor createConfigurationForPomWithParentAlternateNextVersion( List<MavenProject> reactorProjects )
        throws Exception
    {
        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );

        config.mapReleaseVersion( "groupId:artifactId", RELEASE_VERSION );
        config.mapDevelopmentVersion( "groupId:artifactId", NEXT_VERSION );
        config.mapReleaseVersion( "groupId:subproject1", ALTERNATIVE_RELEASE_VERSION );
        config.mapDevelopmentVersion( "groupId:subproject1", ALTERNATIVE_NEXT_VERSION );
        mapScm( config );

        return config;
    }

    protected ReleaseDescriptor createConfigurationForWithParentNextVersion( List<MavenProject> reactorProjects )
        throws Exception
    {
        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );

        config.mapReleaseVersion( "groupId:artifactId", RELEASE_VERSION );
        config.mapDevelopmentVersion( "groupId:artifactId", NEXT_VERSION );
        config.mapReleaseVersion( "groupId:subproject1", RELEASE_VERSION );
        config.mapDevelopmentVersion( "groupId:subproject1", NEXT_VERSION );
        mapScm( config );

        return config;
    }

    public void testRewriteBasicPomWithCvs()
        throws Exception
    {

        List<MavenProject> reactorProjects = createReactorProjects( "basic-pom-with-cvs" );
        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );
        mapNextVersion( config, "groupId:artifactId" );

        Scm scm = new Scm();
        scm.setConnection( "${scm.base}:pserver:anoncvs@localhost:/tmp/scm-repo:module" );
        scm.setDeveloperConnection( "${scm.base}:ext:${username}@localhost:/tmp/scm-repo:module" );
        scm.setUrl( "${baseUrl}/module" );
        config.mapOriginalScmInfo( "groupId:artifactId", scm );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    public void testRewriteBasicPomWithCvsFromTag()
        throws Exception
    {

        List<MavenProject> reactorProjects = createReactorProjects( "basic-pom-with-cvs-from-tag" );
        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );
        mapNextVersion( config, "groupId:artifactId" );

        Scm scm = new Scm();
        scm.setConnection( "scm:cvs:pserver:anoncvs@localhost:/tmp/scm-repo:module" );
        scm.setDeveloperConnection( "scm:cvs:ext:${username}@localhost:/tmp/scm-repo:module" );
        scm.setUrl( "http://localhost/viewcvs.cgi/module" );
        scm.setTag( "original-label" );
        config.mapOriginalScmInfo( "groupId:artifactId", scm );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    public void testRewriteBasicPomWithInheritedScm()
        throws Exception
    {

        List<MavenProject> reactorProjects = createReactorProjects( "basic-pom-inherited-scm" );
        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );

        config.mapReleaseVersion( "groupId:artifactId", RELEASE_VERSION );
        config.mapDevelopmentVersion( "groupId:artifactId", NEXT_VERSION );
        config.mapReleaseVersion( "groupId:subproject1", RELEASE_VERSION );
        config.mapDevelopmentVersion( "groupId:subproject1", NEXT_VERSION );
        config.mapReleaseVersion( "groupId:subsubproject", RELEASE_VERSION );
        config.mapDevelopmentVersion( "groupId:subsubproject", NEXT_VERSION );
        config.mapOriginalScmInfo( "groupId:artifactId", null );
        Scm scm = new Scm();
        scm.setConnection( "scm:svn:file://localhost/tmp/scm-repo/trunk/subproject1" );
        scm.setDeveloperConnection( "scm:svn:file://localhost/tmp/scm-repo/trunk/subproject1" );
        //MRELEASE-107
        scm.setUrl( "http://localhost/viewvc/mypath/trunk/subproject1" );
        config.mapOriginalScmInfo( "groupId:subproject1", scm );
        config.mapOriginalScmInfo( "groupId:subsubproject", null );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    public void testRewritePomWithParentAndProperties()
        throws Exception
    {
        performTestRewritePomWithParentAndProperties( "pom-with-parent-and-properties" );
    }

    //MRELEASE-454
    public void testRewritePomWithParentAndPropertiesInDependencyManagement()
        throws Exception
    {
        performTestRewritePomWithParentAndProperties( "pom-with-parent-and-properties-in-dependency-management" );
    }

    //MRELEASE-454
    public void testRewritePomWithParentAndPropertiesInDependencyManagementImport()
        throws Exception
    {
        performTestRewritePomWithParentAndProperties( "pom-with-parent-and-properties-in-dependency-management-import" );
    }

    private void performTestRewritePomWithParentAndProperties( String path )
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( path );
  
        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );
        config.mapReleaseVersion( "groupId:artifactId", RELEASE_VERSION );
        config.mapDevelopmentVersion( "groupId:artifactId", NEXT_VERSION );
        config.mapReleaseVersion( "groupId:subproject1", ALTERNATIVE_RELEASE_VERSION );
        config.mapDevelopmentVersion( "groupId:subproject1", ALTERNATIVE_NEXT_VERSION );
        config.mapReleaseVersion( "groupId:subproject2", ALTERNATIVE_RELEASE_VERSION );
        config.mapDevelopmentVersion( "groupId:subproject2", ALTERNATIVE_NEXT_VERSION );

        mapScm( config );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    public void testSimulateRewritePomWithParentAndProperties()
        throws Exception
    {
        // use the original ones since simulation didn't modify them
        List<MavenProject> reactorProjects = createReactorProjects( "pom-with-parent-and-properties-sim" );

        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );
        config.mapReleaseVersion( "groupId:artifactId", RELEASE_VERSION );
        config.mapDevelopmentVersion( "groupId:artifactId", NEXT_VERSION );
        config.mapReleaseVersion( "groupId:subproject1", ALTERNATIVE_RELEASE_VERSION );
        config.mapDevelopmentVersion( "groupId:subproject1", ALTERNATIVE_NEXT_VERSION );
        config.mapReleaseVersion( "groupId:subproject2", ALTERNATIVE_RELEASE_VERSION );
        config.mapDevelopmentVersion( "groupId:subproject2", ALTERNATIVE_NEXT_VERSION );

        mapScm( config );

        phase.simulate( config, new DefaultReleaseEnvironment(), reactorProjects );

        for ( Iterator<MavenProject> i = reactorProjects.iterator(); i.hasNext(); )
        {
            MavenProject project = i.next();

            File pomFile = project.getFile();
            File actualFile = new File( pomFile.getParentFile(), pomFile.getName() + ".next" );
            File expectedFile = new File( actualFile.getParentFile(), "expected-pom.xml" );

            comparePomFiles( expectedFile, actualFile, true );
        }
    }

    // MRELEASE-311
    public void testRewritePomWithDependencyPropertyCoordinate()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "pom-with-property-dependency-coordinate" );

        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );
        config.mapReleaseVersion( "groupId:artifactId", RELEASE_VERSION );
        config.mapDevelopmentVersion( "groupId:artifactId", NEXT_VERSION );
        config.mapReleaseVersion( "groupId:subproject1-3.4", ALTERNATIVE_RELEASE_VERSION );
        config.mapDevelopmentVersion( "groupId:subproject1-3.4", ALTERNATIVE_NEXT_VERSION );
        config.mapReleaseVersion( "groupId:subproject2", ALTERNATIVE_RELEASE_VERSION );
        config.mapDevelopmentVersion( "groupId:subproject2", ALTERNATIVE_NEXT_VERSION );

        mapScm( config );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    public void testRewritePomDependenciesWithoutDependenciesVersionUpdate()
        throws Exception
    {
        List<MavenProject> reactorProjects =
            createReactorProjects( "internal-snapshot-dependencies-without-dependencies-version-update" );
        ReleaseDescriptor config = createDefaultConfiguration( reactorProjects );
        config.setUpdateDependencies( false );
        mapNextVersion( config, "groupId:subsubproject" );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }
}
