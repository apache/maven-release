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

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple Semantic Versioning 2.0.0 implementation.
 * <p>
 * This class provides basic parsing and manipulation of semantic version strings
 * following the format: MAJOR.MINOR.PATCH[-PRERELEASE][+METADATA]
 * </p>
 *
 * @see <a href="https://semver.org/">Semantic Versioning 2.0.0</a>
 */
class SemVer {

    /**
     * Regex pattern for parsing semantic versions from semver.org.
     * Groups: 1=major, 2=minor, 3=patch, 4=prerelease, 5=metadata
     *
     * @see <a href="https://semver.org/#is-there-a-suggested-regular-expression-regex-to-check-a-semver-string">SemVer Regex</a>
     */
    private static final Pattern SEMVER_PATTERN = Pattern.compile("^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)"
            + "(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)"
            + "(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$");

    private final int major;
    private final int minor;
    private final int patch;
    private final String preRelease;
    private final String metadata;

    /**
     * Version element types that can be incremented.
     */
    public enum Element {
        MAJOR,
        MINOR,
        PATCH
    }

    /**
     * Constructs a new SemVer instance.
     *
     * @param major the major version number
     * @param minor the minor version number
     * @param patch the patch version number
     * @param preRelease the pre-release identifier (can be null)
     * @param metadata the build metadata (can be null)
     */
    protected SemVer(int major, int minor, int patch, String preRelease, String metadata) {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("Version numbers must be non-negative");
        }
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.preRelease = preRelease;
        this.metadata = metadata;
    }

    /**
     * Parses a version string into a SemVer object.
     *
     * @param version the version string to parse
     * @return the parsed SemVer object
     * @throws IllegalArgumentException if the version string is invalid
     */
    static SemVer parse(String version) {
        if (version == null || version.trim().isEmpty()) {
            throw new IllegalArgumentException("Version string cannot be null or empty");
        }

        Matcher matcher = SEMVER_PATTERN.matcher(version.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid semantic version format: " + version);
        }

        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = Integer.parseInt(matcher.group(3));
        String preRelease = matcher.group(4);
        String metadata = matcher.group(5);

        return new SemVer(major, minor, patch, preRelease, metadata);
    }

    /**
     * Returns a new SemVer with the release version (removes -SNAPSHOT and any pre-release/metadata).
     * <p>
     * Examples:
     * <ul>
     *   <li>1.2.3-SNAPSHOT → 1.2.3</li>
     *   <li>1.2.3-beta+build → 1.2.3</li>
     *   <li>1.2.3 → 1.2.3</li>
     * </ul>
     *
     * @return a new SemVer representing the release version
     */
    SemVer toReleaseVersion() {
        return new SemVer(major, minor, patch, null, null);
    }

    /**
     * Returns a new SemVer with -SNAPSHOT appended as pre-release identifier.
     * If the version already has a pre-release identifier, it will be replaced with SNAPSHOT.
     * Metadata is removed.
     * <p>
     * Examples:
     * <ul>
     *   <li>1.2.3 → 1.2.3-SNAPSHOT</li>
     *   <li>1.2.3-beta → 1.2.3-SNAPSHOT</li>
     *   <li>1.2.3+build → 1.2.3-SNAPSHOT</li>
     *   <li>1.2.3-SNAPSHOT → 1.2.3-SNAPSHOT (no change)</li>
     * </ul>
     *
     * @return a new SemVer with SNAPSHOT pre-release identifier, or this instance if already a SNAPSHOT version
     */
    SemVer toSnapshotVersion() {
        if ("SNAPSHOT".equals(preRelease) && metadata == null) {
            return this;
        }
        return new SemVer(major, minor, patch, "SNAPSHOT", null);
    }

    /**
     * Returns a new SemVer with the specified element incremented.
     * If the version has pre-release or metadata, returns the release version without incrementing.
     * Otherwise, increments the specified element and all lower elements are reset to 0.
     * Pre-release and metadata are always cleared in the result.
     *
     * @param element the element to increment (MAJOR, MINOR, or PATCH)
     * @return a new SemVer with the specified element incremented (or release version if pre-release/metadata present)
     */
    SemVer next(Element element) {
        Objects.requireNonNull(element, "Element cannot be null");

        // If version has pre-release or metadata, just return release version without incrementing
        if (hasPreRelease() || hasMetadata()) {
            return toReleaseVersion();
        }

        switch (element) {
            case MAJOR:
                return new SemVer(major + 1, 0, 0, null, null);
            case MINOR:
                return new SemVer(major, minor + 1, 0, null, null);
            case PATCH:
                return new SemVer(major, minor, patch + 1, null, null);
            default:
                throw new IllegalArgumentException("Unknown element: " + element);
        }
    }

    /**
     * Checks if this version has a pre-release identifier.
     *
     * @return true if pre-release identifier is present, false otherwise
     */
    private boolean hasPreRelease() {
        return preRelease != null && !preRelease.isEmpty();
    }

    /**
     * Checks if this version has build metadata.
     *
     * @return true if build metadata is present, false otherwise
     */
    private boolean hasMetadata() {
        return metadata != null && !metadata.isEmpty();
    }

    /**
     * Returns the major version number.
     *
     * @return the major version
     */
    int getMajor() {
        return major;
    }

    /**
     * Returns the minor version number.
     *
     * @return the minor version
     */
    int getMinor() {
        return minor;
    }

    /**
     * Returns the patch version number.
     *
     * @return the patch version
     */
    int getPatch() {
        return patch;
    }

    /**
     * Returns the pre-release identifier.
     *
     * @return the pre-release identifier, or null if not present
     */
    String getPreRelease() {
        return preRelease;
    }

    /**
     * Returns the build metadata.
     *
     * @return the build metadata, or null if not present
     */
    String getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(major).append('.').append(minor).append('.').append(patch);

        if (preRelease != null && !preRelease.isEmpty()) {
            sb.append('-').append(preRelease);
        }

        if (metadata != null && !metadata.isEmpty()) {
            sb.append('+').append(metadata);
        }

        return sb.toString();
    }
}
