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
package org.apache.maven.shared.release.policy.semver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the SemVer class.
 */
class SemVerTest {

    @ParameterizedTest
    @CsvSource({
        "'1.2.3', 1, 2, 3, , , '1.2.3'",
        "'1.2.3-alpha.1', 1, 2, 3, 'alpha.1', , '1.2.3-alpha.1'",
        "'1.2.3+build.123', 1, 2, 3, , 'build.123', '1.2.3+build.123'",
        "'1.2.3-beta.2+build.456', 1, 2, 3, 'beta.2', 'build.456', '1.2.3-beta.2+build.456'",
        "'1.0.0-SNAPSHOT', 1, 0, 0, 'SNAPSHOT', , '1.0.0-SNAPSHOT'",
        "'  1.2.3  ', 1, 2, 3, , , '1.2.3'",
        "'0.0.0', 0, 0, 0, , , '0.0.0'",
        "'123.456.789', 123, 456, 789, , , '123.456.789'"
    })
    void testParseVersion(
            String version,
            int expectedMajor,
            int expectedMinor,
            int expectedPatch,
            String expectedPreRelease,
            String expectedMetadata,
            String expectedToString) {
        SemVer semVer = SemVer.parse(version);
        assertEquals(expectedMajor, semVer.getMajor());
        assertEquals(expectedMinor, semVer.getMinor());
        assertEquals(expectedPatch, semVer.getPatch());
        assertEquals(expectedPreRelease, semVer.getPreRelease());
        assertEquals(expectedMetadata, semVer.getMetadata());
        assertEquals(expectedToString, semVer.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "1.2", "1.2.3.4", ""})
    void testParseInvalidVersionThrowsException(String invalidVersion) {
        assertThrows(IllegalArgumentException.class, () -> SemVer.parse(invalidVersion));
    }

    @Test
    void testParseNullVersionThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> SemVer.parse(null));
    }

    @ParameterizedTest
    @CsvSource({
        "'1.2.3-SNAPSHOT', '1.2.3'",
        "'1.2.3-beta+build', '1.2.3'",
        "'1.0.0-alpha', '1.0.0'",
        "'2.1.5+metadata', '2.1.5'"
    })
    void testToReleaseVersion(String input, String expected) {
        SemVer version = SemVer.parse(input);
        SemVer releaseVersion = version.toReleaseVersion();
        assertEquals(expected, releaseVersion.toString());
        assertNull(releaseVersion.getPreRelease());
        assertNull(releaseVersion.getMetadata());
    }

    @ParameterizedTest
    @CsvSource({"MAJOR, '2.0.0', 2, 0, 0", "MINOR, '1.3.0', 1, 3, 0", "PATCH, '1.2.4', 1, 2, 4"})
    void testNext(
            SemVer.Element element, String expectedVersion, int expectedMajor, int expectedMinor, int expectedPatch) {
        SemVer version = SemVer.parse("1.2.3");
        SemVer next = version.next(element);
        assertEquals(expectedVersion, next.toString());
        assertEquals(expectedMajor, next.getMajor());
        assertEquals(expectedMinor, next.getMinor());
        assertEquals(expectedPatch, next.getPatch());
    }

    @Test
    void testNextClearsPreReleaseAndMetadata() {
        SemVer version = SemVer.parse("1.2.3-beta+build");
        SemVer next = version.next(SemVer.Element.MINOR);
        assertEquals("1.2.3", next.toString());
        assertNull(next.getPreRelease());
        assertNull(next.getMetadata());
    }

    @Test
    void testNextWithNullElementThrowsException() {
        SemVer version = SemVer.parse("1.2.3");
        assertThrows(NullPointerException.class, () -> version.next(null));
    }

    @ParameterizedTest
    @CsvSource({
        "'1.2.3', '1.2.3-SNAPSHOT', 'SNAPSHOT', , false",
        "'1.0.0', '1.0.0-SNAPSHOT', 'SNAPSHOT', , false",
        "'1.2.3-beta.1', '1.2.3-SNAPSHOT', 'SNAPSHOT', , false",
        "'1.2.3-SNAPSHOT', '1.2.3-SNAPSHOT', 'SNAPSHOT', , true"
    })
    void testToSnapshotVersion(
            String input,
            String expectedVersion,
            String expectedPreRelease,
            String expectedMetadata,
            boolean shouldBeSameObject) {
        SemVer version = SemVer.parse(input);
        SemVer snapshotVersion = version.toSnapshotVersion();
        assertEquals(expectedVersion, snapshotVersion.toString());
        assertEquals(expectedPreRelease, snapshotVersion.getPreRelease());
        assertEquals(expectedMetadata, snapshotVersion.getMetadata());

        // For the already snapshot case, verify it returns the same object
        if (shouldBeSameObject) {
            assertSame(version, snapshotVersion);
        }
    }

    @ParameterizedTest
    @CsvSource({"-1, 0, 0", "0, -1, 0", "0, 0, -1"})
    void testConstructorValidation(int major, int minor, int patch) {
        assertThrows(IllegalArgumentException.class, () -> new SemVer(major, minor, patch, null, null));
    }
}
