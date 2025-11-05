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
package org.apache.maven.shared.release.util;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for ReleaseUtil methods
 */
class ReleaseUtilTest {
    /**
     * MRELEASE-273 : Tests if there no pom passed as parameter
     */
    @Test
    void testProjectIsNull() {
        assertNull(ReleaseUtil.getReleasePom(null));
        assertNull(ReleaseUtil.getStandardPom(null));
    }

    @Test
    void testGetBaseScmUrlSingleLevel() throws Exception {
        assertEquals(
                "scm:svn:http://svn.repo.com/flat-multi-module/trunk",
                ReleaseUtil.realignScmUrl(0, "scm:svn:http://svn.repo.com/flat-multi-module/trunk"));
        assertEquals(
                "scm:svn:http://svn.repo.com/flat-multi-module/trunk/",
                ReleaseUtil.realignScmUrl(0, "scm:svn:http://svn.repo.com/flat-multi-module/trunk/"));
    }

    @Test
    void testGetBaseScmUrlSingleLevelDotCharacter() throws Exception {
        assertEquals(
                "scm:svn:http://svn.repo.com/flat-multi-module/trunk",
                ReleaseUtil.realignScmUrl(0, "scm:svn:http://svn.repo.com/flat-multi-module/trunk/."));
        assertEquals(
                "scm:svn:http://svn.repo.com/flat-multi-module/trunk/",
                ReleaseUtil.realignScmUrl(0, "scm:svn:http://svn.repo.com/flat-multi-module/trunk/./"));
        assertEquals(
                "scm:svn:http://svn.repo.com/flat-multi-module/trunk/project",
                ReleaseUtil.realignScmUrl(0, "scm:svn:http://svn.repo.com/flat-multi-module/trunk/./project"));
        assertEquals(
                "scm:svn:http://svn.repo.com/flat-multi-module",
                ReleaseUtil.realignScmUrl(0, "scm:svn:http://svn.repo.com/flat-multi-module/trunk/.."));
        assertEquals(
                "scm:svn:http://svn.repo.com/flat-multi-module/",
                ReleaseUtil.realignScmUrl(0, "scm:svn:http://svn.repo.com/flat-multi-module/trunk/../"));
        assertEquals(
                "scm:svn:http://svn.repo.com/flat-multi-module/branches",
                ReleaseUtil.realignScmUrl(0, "scm:svn:http://svn.repo.com/flat-multi-module/trunk/../branches"));
    }

    @Test
    void testGetBaseScmUrlReturnOriginal() throws Exception {
        assertEquals("no-path-elements", ReleaseUtil.realignScmUrl(1, "no-path-elements"));
        assertEquals("no-path-elements", ReleaseUtil.realignScmUrl(15, "no-path-elements"));
    }

    @Test
    void testGetBaseScmUrlOfFlatMultiModule() throws Exception {
        String actual =
                ReleaseUtil.realignScmUrl(1, "scm:svn:http://svn.repo.com/flat-multi-module/trunk/root-project");
        assertEquals("scm:svn:http://svn.repo.com/flat-multi-module/trunk", actual);

        actual = ReleaseUtil.realignScmUrl(1, "scm:svn:http://svn.repo.com/flat-multi-module/trunk/root-project/");
        assertEquals("scm:svn:http://svn.repo.com/flat-multi-module/trunk/", actual);
    }

    @Test
    void testGetBaseScmUrlOfFlatMultiModuleMultipleLevels() throws Exception {
        String actual =
                ReleaseUtil.realignScmUrl(3, "scm:svn:http://svn.repo.com/flat-multi-module/trunk/root-project/1/2");
        assertEquals("scm:svn:http://svn.repo.com/flat-multi-module/trunk", actual);

        actual = ReleaseUtil.realignScmUrl(3, "scm:svn:http://svn.repo.com/flat-multi-module/trunk/root-project/1/2/");
        assertEquals("scm:svn:http://svn.repo.com/flat-multi-module/trunk/", actual);
    }

    @Test
    void testGetBaseWorkingDirectoryParentCountSameDirectory() {
        Path workingDirectory = Paths.get("/working/directory/maven/release");
        Path basedir = Paths.get("/working/directory/maven/release");
        assertEquals(0, ReleaseUtil.getBaseWorkingDirectoryParentCount(basedir, workingDirectory));
    }

    @Test
    void testGetBaseWorkingDirectoryParentCountSameDirectoryDotCharacter() {
        Path workingDirectory = Paths.get("/working/directory/maven/release/.").toAbsolutePath();
        assertTrue(workingDirectory.toString().contains("."));
        Path basedir = Paths.get("/working/directory/maven/release").toAbsolutePath();
        assertEquals(0, ReleaseUtil.getBaseWorkingDirectoryParentCount(basedir, workingDirectory));

        // finish with slash
        workingDirectory = Paths.get("/working/directory/maven/release/./").toAbsolutePath();
        assertTrue(workingDirectory.toString().contains("."));
        basedir = Paths.get("/working/directory/maven/release").toAbsolutePath();
        assertEquals(0, ReleaseUtil.getBaseWorkingDirectoryParentCount(basedir, workingDirectory));
    }

    @Test
    void testGetBaseWorkingDirectoryParentCountSubdirectory() {
        Path workingDirectory = Paths.get("/working/directory/maven/release").toAbsolutePath();
        Path basedir = Paths.get("/working/directory/maven/release/maven-release-manager")
                .toAbsolutePath();
        assertEquals(0, ReleaseUtil.getBaseWorkingDirectoryParentCount(basedir, workingDirectory));
    }

    @Test
    void testGetBaseWorkingDirectoryParentCountParentDirectory() {
        Path workingDirectory = Paths.get("/working/directory/maven/release/maven-release-manager")
                .toAbsolutePath();
        Path basedir = Paths.get("/working/directory/maven/release").toAbsolutePath();
        assertEquals(1, ReleaseUtil.getBaseWorkingDirectoryParentCount(basedir, workingDirectory));
    }

    @Test
    void testGetBaseWorkingDirectoryParentCountParentDirectoryDotCharacter() {
        Path workingDirectory = Paths.get("/working/directory/maven/release/maven-release-manager/.")
                .toAbsolutePath();
        assertTrue(workingDirectory.toString().contains("."));
        Path basedir = Paths.get("/working/directory/maven/release").toAbsolutePath();
        assertEquals(1, ReleaseUtil.getBaseWorkingDirectoryParentCount(basedir, workingDirectory));

        // finish with slash
        workingDirectory = Paths.get("/working/directory/maven/release/maven-release-manager/./")
                .toAbsolutePath();
        assertTrue(workingDirectory.toString().contains("."));
        basedir = Paths.get("/working/directory/maven/release").toAbsolutePath();
        assertEquals(1, ReleaseUtil.getBaseWorkingDirectoryParentCount(basedir, workingDirectory));
    }

    @Test
    void testGetBaseWorkingDirectoryParentCountParentDirectoryMultiple() {
        Path workingDirectory = Paths.get("/working/directory/maven/release/maven-release-manager")
                .toAbsolutePath();
        Path basedir = Paths.get("/working/directory").toAbsolutePath();
        assertEquals(3, ReleaseUtil.getBaseWorkingDirectoryParentCount(basedir, workingDirectory));
    }

    @Test
    void testGetBaseWorkingDirectoryParentCountParentDirectoryMultipleDotCharacter() {
        Path workingDirectory = Paths.get("/working/directory/maven/release/maven-release-manager/./.")
                .toAbsolutePath();
        assertTrue(workingDirectory.toString().contains("."));
        Path basedir = Paths.get("/working/directory").toAbsolutePath();
        assertEquals(3, ReleaseUtil.getBaseWorkingDirectoryParentCount(basedir, workingDirectory));

        // finish with slash
        workingDirectory = Paths.get("/working/directory/maven/release/maven-release-manager/././")
                .toAbsolutePath();
        assertTrue(workingDirectory.toString().contains("."));
        basedir = Paths.get("/working/directory").toAbsolutePath();
        assertEquals(3, ReleaseUtil.getBaseWorkingDirectoryParentCount(basedir, workingDirectory));
    }

    @Test
    void testGetBaseWorkingDirectoryParentCountDifferentCase() {
        Path workingDirectory = Paths.get("/Working/Directory/maven/release/maven-release-manager")
                .toAbsolutePath();
        Path basedir = Paths.get("/working/directory").toAbsolutePath();
        assertEquals(3, ReleaseUtil.getBaseWorkingDirectoryParentCount(basedir, workingDirectory));
    }

    /**
     * MRELEASE-663
     */
    @Test
    void testGetWindowsRootBaseWorkingDirectoryParentCountDifferentCase() {
        Assumptions.assumeTrue(org.codehaus.plexus.util.Os.isFamily(org.codehaus.plexus.util.Os.FAMILY_WINDOWS));

        assertEquals(
                2,
                ReleaseUtil.getBaseWorkingDirectoryParentCount(
                        java.nio.file.Paths.get("C:"), java.nio.file.Paths.get("C:\\working\\directory")));
        assertEquals(
                2,
                ReleaseUtil.getBaseWorkingDirectoryParentCount(
                        java.nio.file.Paths.get("C:"), java.nio.file.Paths.get("C:\\working\\directory\\")));
        assertEquals(
                2,
                ReleaseUtil.getBaseWorkingDirectoryParentCount(
                        java.nio.file.Paths.get("C:\\"), java.nio.file.Paths.get("C:\\working\\directory")));
        assertEquals(
                2,
                ReleaseUtil.getBaseWorkingDirectoryParentCount(
                        java.nio.file.Paths.get("C:\\"), java.nio.file.Paths.get("C:\\working\\directory\\")));

        assertEquals(
                2,
                ReleaseUtil.getBaseWorkingDirectoryParentCount(
                        java.nio.file.Paths.get("c:"), java.nio.file.Paths.get("C:\\working\\directory")));
        assertEquals(
                2,
                ReleaseUtil.getBaseWorkingDirectoryParentCount(
                        java.nio.file.Paths.get("C:"), java.nio.file.Paths.get("c:\\working\\directory")));
        assertEquals(
                2,
                ReleaseUtil.getBaseWorkingDirectoryParentCount(
                        java.nio.file.Paths.get("c:"), java.nio.file.Paths.get("c:\\working\\directory")));
    }
}
