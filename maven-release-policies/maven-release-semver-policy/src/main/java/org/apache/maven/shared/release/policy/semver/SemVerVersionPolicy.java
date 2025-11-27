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

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.shared.release.policy.version.VersionPolicyRequest;
import org.apache.maven.shared.release.policy.version.VersionPolicyResult;
import org.apache.maven.shared.release.versions.VersionParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Uses SemVer implementation to increase minor element when resolving the development version.
 *
 * @deprecated use {@link SemVerMinorDevelopmentVersionPolicy} instead.
 */
@Singleton
@Named("SemVerVersionPolicy")
@Deprecated
public class SemVerVersionPolicy extends SemVerMinorDevelopmentVersionPolicy {

    private final Logger logger = LoggerFactory.getLogger(SemVerVersionPolicy.class);

    @Override
    public VersionPolicyResult getReleaseVersion(VersionPolicyRequest request) throws VersionParseException {
        logger.warn("SemVerVersionPolicy is deprecated and will be removed in future releases. "
                + "Please use SemVerMinorDevelopment instead.");
        return super.getReleaseVersion(request);
    }
}
