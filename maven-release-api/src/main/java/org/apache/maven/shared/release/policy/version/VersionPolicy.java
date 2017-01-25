package org.apache.maven.shared.release.policy.version;

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

import org.apache.maven.shared.release.policy.PolicyException;
import org.apache.maven.shared.release.versions.VersionParseException;

/**
 * API for next version calculations, used by maven-release-plugin to suggest release and next develoment versions.
 *
 * @since 2.5.1 (MRELEASE-431)
 */
public interface VersionPolicy
{
    /**
     * Calculation of the branch or tag version from development version.
     */
    VersionPolicyResult getBranchOrTagVersion( VersionPolicyRequest request )
        throws PolicyException, VersionParseException;

    /**
     * Calculation of the release version from development state.
     */
    VersionPolicyResult getReleaseVersion( VersionPolicyRequest request )
        throws PolicyException, VersionParseException;

    /**
     * Calculation of the next development version from release state.
     */
    VersionPolicyResult getDevelopmentVersion( VersionPolicyRequest request )
        throws PolicyException, VersionParseException;
}
