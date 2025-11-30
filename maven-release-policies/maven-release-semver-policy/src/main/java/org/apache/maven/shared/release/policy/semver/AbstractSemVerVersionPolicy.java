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

import org.apache.maven.shared.release.policy.version.VersionPolicy;
import org.apache.maven.shared.release.policy.version.VersionPolicyRequest;
import org.apache.maven.shared.release.policy.version.VersionPolicyResult;
import org.apache.maven.shared.release.versions.VersionParseException;

/**
 * Abstract base class for SemVer-based VersionPolicy implementations.
 *
 * @since 3.3.0
 */
abstract class AbstractSemVerVersionPolicy implements VersionPolicy {

    protected SemVer createVersionFromRequest(VersionPolicyRequest request) throws VersionParseException {
        try {
            return SemVer.parse(request.getVersion());
        } catch (IllegalArgumentException e) {
            throw new VersionParseException(e.getMessage());
        }
    }

    protected VersionPolicyResult createResult(SemVer version) {
        return new VersionPolicyResult().setVersion(version.toString());
    }

    protected VersionPolicyResult createSnapshotResult(SemVer version) {
        return new VersionPolicyResult().setVersion(version.toSnapshotVersion().toString());
    }
}
