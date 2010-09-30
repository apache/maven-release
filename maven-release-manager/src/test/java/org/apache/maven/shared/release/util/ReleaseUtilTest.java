package org.apache.maven.shared.release.util;

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
import java.util.Arrays;
import java.util.Collections;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusTestCase;

/**
 * Tests for ReleaseUtil methods
 */
public class ReleaseUtilTest
    extends PlexusTestCase
{
    /**
     * MRELEASE-273 : Tests if there no pom passed as parameter
     */
    public void testProjectIsNull()
    {
        assertNull( ReleaseUtil.getReleasePom( null ) );
        assertNull( ReleaseUtil.getStandardPom( null ) );
    }

    public void testGetCommonBasedirSingleProject()
    {
        assertEquals( "/working/directory/flat-multi-module/project", ReleaseUtil.getCommonBasedir(
            Collections.singletonList( createProject( "/working/directory/flat-multi-module/project" ) ), '/' ) );
    }

    public void testGetCommonBasedirSingleProjectWindows()
    {
        assertEquals( "c:\\working\\directory\\flat-multi-module\\project", ReleaseUtil.getCommonBasedir(
            Collections.singletonList( createProject( "c:\\working\\directory\\flat-multi-module\\project" ) ),
            '\\' ) );
    }

    public void testGetCommonBasedirOfFlatMultiModule()
        throws Exception
    {
        assertEquals( "/working/directory/flat-multi-module", ReleaseUtil.getCommonBasedir( Arrays.asList(
            new MavenProject[]{createProject( "/working/directory/flat-multi-module/root-project" ),
                createProject( "/working/directory/flat-multi-module/core" ),
                createProject( "/working/directory/flat-multi-module/webapp" )} ), '/' ) );
    }

    public void testGetCommonBasedirOfFlatMultiModuleWindows()
        throws Exception
    {
        assertEquals( "c:\\working\\directory\\flat-multi-module", ReleaseUtil.getCommonBasedir( Arrays.asList(
            new MavenProject[]{createProject( "c:\\working\\directory\\flat-multi-module\\root-project" ),
                createProject( "c:\\working\\directory\\flat-multi-module\\core" ),
                createProject( "c:\\working\\directory\\flat-multi-module\\webapp" )} ), '\\' ) );
    }

    public void testGetCommonBasedirUppercaseLowerCaseWindows()
        throws Exception
    {
        assertEquals( "c:\\working\\root", ReleaseUtil.getCommonBasedir( Arrays.asList(
            new MavenProject[]{createProject( "c:\\working\\root" ), createProject( "C:\\WoRkInG\\root\\project1" ),
                createProject( "c:\\working\\root\\project2" )} ), '\\' ) );
    }

    public void testGetCommonBasedirOfFlatMultiModuleSimilarArtifactIds()
        throws Exception
    {
        assertEquals( "/working/directory/flat-multi-module", ReleaseUtil.getCommonBasedir( Arrays.asList(
            new MavenProject[]{createProject( "/working/directory/flat-multi-module/release-parent" ),
                createProject( "/working/directory/flat-multi-module/release-module1" ),
                createProject( "/working/directory/flat-multi-module/release-module2" )} ), '/' ) );
    }

    public void testGetCommonBasedirOfFlatMultiModuleSimilarArtifactIdsWindows()
        throws Exception
    {
        assertEquals( "c:\\working\\directory\\flat-multi-module", ReleaseUtil.getCommonBasedir( Arrays.asList(
            new MavenProject[]{createProject( "c:\\working\\directory\\flat-multi-module\\release-parent" ),
                createProject( "c:\\working\\directory\\flat-multi-module\\release-module1" ),
                createProject( "c:\\working\\directory\\flat-multi-module\\release-module2" )} ), '\\' ) );
    }

    public void testGetCommonBasedirOfRegularMultiModule()
        throws Exception
    {
        assertEquals( "/working/directory/flat-multi-module", ReleaseUtil.getCommonBasedir( Arrays.asList(
            new MavenProject[]{createProject( "/working/directory/flat-multi-module" ),
                createProject( "/working/directory/flat-multi-module/core" ),
                createProject( "/working/directory/flat-multi-module/webapp" )} ), '/' ) );
    }

    public void testGetCommonBasedirOfRegularMultiModuleParentNotBeeingFirstInReactor()
        throws Exception
    {
        assertEquals( "/working/directory/flat-multi-module", ReleaseUtil.getCommonBasedir( Arrays.asList(
            new MavenProject[]{
                createProject( "/working/directory/flat-multi-module/core" ),
                createProject( "/working/directory/flat-multi-module" ),
                createProject( "/working/directory/flat-multi-module/webapp" )} ), '/' ) );
    }

    public void testGetCommonBasedirOfRegularMultiModuleWindowsPath()
        throws Exception
    {
        assertEquals( "c:\\working\\directory\\flat-multi-module", ReleaseUtil.getCommonBasedir( Arrays.asList(
            new MavenProject[]{
                createProject( "c:\\working\\directory\\flat-multi-module\\core" ),
                createProject( "c:\\working\\directory\\flat-multi-module" ),
                createProject( "c:\\working\\directory\\flat-multi-module\\webapp" )} ), '\\' ) );
    }

    public void testGetCommonBasedirOfFlatMultiModuleWithMultipleLevels()
        throws Exception
    {
        assertEquals( "/working/directory/flat-multi-module", ReleaseUtil.getCommonBasedir( Arrays.asList(
            new MavenProject[]{createProject( "/working/directory/flat-multi-module/root-project" ),
                createProject( "/working/directory/flat-multi-module/core" ),
                createProject( "/working/directory/flat-multi-module/common/utils" ),
                createProject( "/working/directory/flat-multi-module/common/xml" ),
                createProject( "/working/directory/flat-multi-module/webapp" )} ), '/' ) );
    }

    public void testGetCommonBasedirOfFlatMultiModuleWithDescendingHierarchy()
        throws Exception
    {
        assertEquals( "/working/directory/flat-multi-module", ReleaseUtil.getCommonBasedir( Arrays.asList(
            new MavenProject[]{createProject( "/working/directory/flat-multi-module/level/1/2/3" ),
                createProject( "/working/directory/flat-multi-module/level/1/2" ),
                createProject( "/working/directory/flat-multi-module/level/1" ),
                createProject( "/working/directory/flat-multi-module/level" ),
                createProject( "/working/directory/flat-multi-module/other" )} ), '/' ) );
    }

    public void testGetBaseScmUrlSingleLevel()
        throws Exception
    {
        assertEquals( "scm:svn:http://svn.repo.com/flat-multi-module/trunk",
                      ReleaseUtil.realignScmUrl( 0, "scm:svn:http://svn.repo.com/flat-multi-module/trunk" ) );
        assertEquals( "scm:svn:http://svn.repo.com/flat-multi-module/trunk/",
                      ReleaseUtil.realignScmUrl( 0, "scm:svn:http://svn.repo.com/flat-multi-module/trunk/" ) );
    }

    public void testGetBaseScmUrlReturnOriginal()
        throws Exception
    {
        assertEquals( "no-path-elements", ReleaseUtil.realignScmUrl( 1, "no-path-elements" ) );
        assertEquals( "no-path-elements", ReleaseUtil.realignScmUrl( 15, "no-path-elements" ) );
    }

    public void testGetBaseScmUrlOfFlatMultiModule()
        throws Exception
    {
        String actual =
            ReleaseUtil.realignScmUrl( 1, "scm:svn:http://svn.repo.com/flat-multi-module/trunk/root-project" );
        assertEquals( "scm:svn:http://svn.repo.com/flat-multi-module/trunk", actual );

        actual = ReleaseUtil.realignScmUrl( 1, "scm:svn:http://svn.repo.com/flat-multi-module/trunk/root-project/" );
        assertEquals( "scm:svn:http://svn.repo.com/flat-multi-module/trunk/", actual );
    }

    public void testGetBaseScmUrlOfFlatMultiModuleMultipleLevels()
        throws Exception
    {
        String actual =
            ReleaseUtil.realignScmUrl( 3, "scm:svn:http://svn.repo.com/flat-multi-module/trunk/root-project/1/2" );
        assertEquals( "scm:svn:http://svn.repo.com/flat-multi-module/trunk", actual );

        actual =
            ReleaseUtil.realignScmUrl( 3, "scm:svn:http://svn.repo.com/flat-multi-module/trunk/root-project/1/2/" );
        assertEquals( "scm:svn:http://svn.repo.com/flat-multi-module/trunk/", actual );
    }

    public void testGetBaseWorkingDirectoryParentCountSameDirectory()
    {
        String workingDirectory = "/working/directory/maven/release";
        String basedir = "/working/directory/maven/release";
        assertEquals( 0, ReleaseUtil.getBaseWorkingDirectoryParentCount( basedir, workingDirectory ) );
    }

    public void testGetBaseWorkingDirectoryParentCountSubdirectory()
    {
        String workingDirectory = "/working/directory/maven/release";
        String basedir = "/working/directory/maven/release/maven-release-manager";
        assertEquals( 0, ReleaseUtil.getBaseWorkingDirectoryParentCount( basedir, workingDirectory ) );
    }

    public void testGetBaseWorkingDirectoryParentCountParentDirectory()
    {
        String workingDirectory = "/working/directory/maven/release/maven-release-manager";
        String basedir = "/working/directory/maven/release";
        assertEquals( 1, ReleaseUtil.getBaseWorkingDirectoryParentCount( basedir, workingDirectory ) );
    }

    public void testGetBaseWorkingDirectoryParentCountParentDirectoryMultiple()
    {
        String workingDirectory = "/working/directory/maven/release/maven-release-manager";
        String basedir = "/working/directory";
        assertEquals( 3, ReleaseUtil.getBaseWorkingDirectoryParentCount( basedir, workingDirectory ) );
    }

    public void testGetBaseWorkingDirectoryParentCountDifferentCase()
    {
        String workingDirectory = "/Working/Directory/maven/release/maven-release-manager";
        String basedir = "/working/directory";
        assertEquals( 3, ReleaseUtil.getBaseWorkingDirectoryParentCount( basedir, workingDirectory ) );
    }

    private static MavenProject createProject( final String basedir )
    {
        return new MavenProject()
        {
            public File getBasedir()
            {
                return new File( basedir );
            }
        };
    }
}
