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
package org.apache.maven.shared.release.policies;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.shared.release.policy.version.VersionPolicy;
import org.apache.maven.shared.release.policy.version.VersionPolicyRequest;
import org.apache.maven.shared.release.policy.version.VersionPolicyResult;
import org.apache.maven.shared.release.versions.DefaultVersionInfo;
import org.apache.maven.shared.release.versions.VersionParseException;

/**
 * Default version policy: proposed release version just removes {@code -SNAPSHOT},
 * next development version adds a minor increment to release and adds {@code -SNAPSHOT}.
 *
 * @author Robert Scholte
 */
@Singleton
@Named
public class DefaultVersionPolicy implements VersionPolicy {
    @Override
    public VersionPolicyResult getReleaseVersion(VersionPolicyRequest request) throws VersionParseException {
        String releaseVersion = new DefaultVersionInfo(request.getVersion()).getReleaseVersionString();
        return new VersionPolicyResult().setVersion(releaseVersion);
    }

    @Override
    public VersionPolicyResult getDevelopmentVersion(VersionPolicyRequest request) throws VersionParseException {
        String developmentVersion =
                new DefaultVersionInfo(request.getVersion()).getNextVersion().getSnapshotVersionString();
        return new VersionPolicyResult().setVersion(developmentVersion);
    }
}
