package org.apache.maven.shared.release.policy.stub;

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

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.shared.release.policy.version.VersionPolicy;
import org.apache.maven.shared.release.policy.version.VersionPolicyRequest;
import org.apache.maven.shared.release.policy.version.VersionPolicyResult;

/**
 * A {@link VersionPolicy} implementation that switches continously between shapsnot and release version.
 */
@Singleton
@Named( "StubVersionPolicy" )
public final class StubVersionPolicy
    implements VersionPolicy
{

    public VersionPolicyResult getReleaseVersion( VersionPolicyRequest request )
    {
        return new VersionPolicyResult().setVersion( request.getVersion().replace( "-SNAPSHOT", "" ) );
    }

    public VersionPolicyResult getDevelopmentVersion( VersionPolicyRequest request )
    {
        return new VersionPolicyResult().setVersion( request.getVersion().replace( "-SNAPSHOT", "" ) + "-SNAPSHOT" );
    }

}
