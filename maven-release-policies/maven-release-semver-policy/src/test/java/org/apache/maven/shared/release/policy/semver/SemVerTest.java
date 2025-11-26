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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the SemVer class.
 */
public class SemVerTest {

    @Test
    public void testParseSimpleVersion() {
        SemVer version = SemVer.parse("1.2.3");
        assertEquals(1, version.getMajor());
        assertEquals(2, version.getMinor());
        assertEquals(3, version.getPatch());
        assertNull(version.getPreRelease());
        assertNull(version.getMetadata());
        assertEquals("1.2.3", version.toString());
    }

    @Test
    public void testParseVersionWithPreRelease() {
        SemVer version = SemVer.parse("1.2.3-alpha.1");
        assertEquals(1, version.getMajor());
        assertEquals(2, version.getMinor());
        assertEquals(3, version.getPatch());
        assertEquals("alpha.1", version.getPreRelease());
        assertNull(version.getMetadata());
        assertEquals("1.2.3-alpha.1", version.toString());
    }

    @Test
    public void testParseVersionWithMetadata() {
        SemVer version = SemVer.parse("1.2.3+build.123");
        assertEquals(1, version.getMajor());
        assertEquals(2, version.getMinor());
        assertEquals(3, version.getPatch());
        assertNull(version.getPreRelease());
        assertEquals("build.123", version.getMetadata());
        assertEquals("1.2.3+build.123", version.toString());
    }

    @Test
    public void testParseVersionWithPreReleaseAndMetadata() {
        SemVer version = SemVer.parse("1.2.3-beta.2+build.456");
        assertEquals(1, version.getMajor());
        assertEquals(2, version.getMinor());
        assertEquals(3, version.getPatch());
        assertEquals("beta.2", version.getPreRelease());
        assertEquals("build.456", version.getMetadata());
        assertEquals("1.2.3-beta.2+build.456", version.toString());
    }

    @Test
    public void testParseSnapshot() {
        SemVer version = SemVer.parse("1.0.0-SNAPSHOT");
        assertEquals(1, version.getMajor());
        assertEquals(0, version.getMinor());
        assertEquals(0, version.getPatch());
        assertEquals("SNAPSHOT", version.getPreRelease());
        assertEquals("1.0.0-SNAPSHOT", version.toString());
    }

    @Test
    public void testParseInvalidVersionThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> SemVer.parse("invalid"));
        assertThrows(IllegalArgumentException.class, () -> SemVer.parse("1.2"));
        assertThrows(IllegalArgumentException.class, () -> SemVer.parse("1.2.3.4"));
        assertThrows(IllegalArgumentException.class, () -> SemVer.parse(""));
        assertThrows(IllegalArgumentException.class, () -> SemVer.parse(null));
    }

    @Test
    public void testToReleaseVersion() {
        SemVer version = SemVer.parse("1.2.3-SNAPSHOT");
        SemVer releaseVersion = version.toReleaseVersion();
        assertEquals("1.2.3", releaseVersion.toString());
        assertNull(releaseVersion.getPreRelease());
        assertNull(releaseVersion.getMetadata());
    }

    @Test
    public void testToReleaseVersionWithMetadata() {
        SemVer version = SemVer.parse("1.2.3-beta+build");
        SemVer releaseVersion = version.toReleaseVersion();
        assertEquals("1.2.3", releaseVersion.toString());
        assertNull(releaseVersion.getPreRelease());
        assertNull(releaseVersion.getMetadata());
    }

    @Test
    public void testToSnapshotVersion() {
        SemVer version = SemVer.parse("1.2.3");
        SemVer snapshotVersion = version.toSnapshotVersion();
        assertEquals("1.2.3-SNAPSHOT", snapshotVersion.toString());
        assertEquals("SNAPSHOT", snapshotVersion.getPreRelease());
        assertNull(snapshotVersion.getMetadata());
    }

    @Test
    public void testToSnapshotVersionFromRelease() {
        SemVer version = SemVer.parse("1.0.0");
        SemVer snapshotVersion = version.toSnapshotVersion();
        assertEquals("1.0.0-SNAPSHOT", snapshotVersion.toString());
    }

    @Test
    public void testToSnapshotVersionReplacesPreRelease() {
        SemVer version = SemVer.parse("1.2.3-beta.1");
        SemVer snapshotVersion = version.toSnapshotVersion();
        assertEquals("1.2.3-SNAPSHOT", snapshotVersion.toString());
        assertEquals("SNAPSHOT", snapshotVersion.getPreRelease());
    }

    @Test
    public void testToSnapshotVersionPreservesMetadata() {
        SemVer version = SemVer.parse("1.2.3+build.456");
        SemVer snapshotVersion = version.toSnapshotVersion();
        assertEquals("1.2.3-SNAPSHOT+build.456", snapshotVersion.toString());
        assertEquals("SNAPSHOT", snapshotVersion.getPreRelease());
        assertEquals("build.456", snapshotVersion.getMetadata());
    }

    @Test
    public void testToSnapshotVersionAlreadySnapshot() {
        SemVer version = SemVer.parse("1.2.3-SNAPSHOT");
        SemVer snapshotVersion = version.toSnapshotVersion();
        assertEquals("1.2.3-SNAPSHOT", snapshotVersion.toString());
        assertEquals(version, snapshotVersion);
    }

    @Test
    public void testNextMajor() {
        SemVer version = SemVer.parse("1.2.3");
        SemVer next = version.next(SemVer.Element.MAJOR);
        assertEquals("2.0.0", next.toString());
        assertEquals(2, next.getMajor());
        assertEquals(0, next.getMinor());
        assertEquals(0, next.getPatch());
    }

    @Test
    public void testNextMinor() {
        SemVer version = SemVer.parse("1.2.3");
        SemVer next = version.next(SemVer.Element.MINOR);
        assertEquals("1.3.0", next.toString());
        assertEquals(1, next.getMajor());
        assertEquals(3, next.getMinor());
        assertEquals(0, next.getPatch());
    }

    @Test
    public void testNextPatch() {
        SemVer version = SemVer.parse("1.2.3");
        SemVer next = version.next(SemVer.Element.PATCH);
        assertEquals("1.2.4", next.toString());
        assertEquals(1, next.getMajor());
        assertEquals(2, next.getMinor());
        assertEquals(4, next.getPatch());
    }

    @Test
    public void testNextClearsPreReleaseAndMetadata() {
        SemVer version = SemVer.parse("1.2.3-beta+build");
        SemVer next = version.next(SemVer.Element.MINOR);
        assertEquals("1.3.0", next.toString());
        assertNull(next.getPreRelease());
        assertNull(next.getMetadata());
    }

    @Test
    public void testNextWithNullElementThrowsException() {
        SemVer version = SemVer.parse("1.2.3");
        assertThrows(NullPointerException.class, () -> version.next(null));
    }

    @Test
    public void testConstructorValidation() {
        assertThrows(IllegalArgumentException.class, () -> new SemVer(-1, 0, 0, null, null));
        assertThrows(IllegalArgumentException.class, () -> new SemVer(0, -1, 0, null, null));
        assertThrows(IllegalArgumentException.class, () -> new SemVer(0, 0, -1, null, null));
    }

    @Test
    public void testEqualsAndHashCode() {
        SemVer v1 = SemVer.parse("1.2.3-beta+build");
        SemVer v2 = SemVer.parse("1.2.3-beta+build");
        SemVer v3 = SemVer.parse("1.2.3");

        assertEquals(v1, v2);
        assertEquals(v1.hashCode(), v2.hashCode());
        assertNotEquals(v1, v3);
        assertNotEquals(null, v1);
        assertNotEquals("1.2.3", v1.toString());
    }

    @Test
    public void testParseWithWhitespace() {
        SemVer version = SemVer.parse("  1.2.3  ");
        assertEquals("1.2.3", version.toString());
    }

    @Test
    public void testParseZeroVersion() {
        SemVer version = SemVer.parse("0.0.0");
        assertEquals(0, version.getMajor());
        assertEquals(0, version.getMinor());
        assertEquals(0, version.getPatch());
    }

    @Test
    public void testParseLargeVersionNumbers() {
        SemVer version = SemVer.parse("123.456.789");
        assertEquals(123, version.getMajor());
        assertEquals(456, version.getMinor());
        assertEquals(789, version.getPatch());
    }
}
