package org.apache.maven.shared.release.policy.semver;

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
import org.apache.maven.shared.release.policy.version.VersionPolicy;
import org.apache.maven.shared.release.policy.version.VersionPolicyRequest;
import org.apache.maven.shared.release.policy.version.VersionPolicyResult;
import org.apache.maven.shared.release.versions.VersionParseException;
import org.codehaus.plexus.component.annotations.Component;
import org.semver.Version;
import org.semver.Version.Element;

/**
 * 
 * Uses SemVer implementation to increase minor element when resolving the development version
 *
 */
@Component(
           role = VersionPolicy.class,
           hint = "SemVerVersionPolicy",
           description = "A VersionPolicy following the SemVer rules"
       )
public class SemVerVersionPolicy implements VersionPolicy
{

    public VersionPolicyResult getReleaseVersion( VersionPolicyRequest request )
        throws PolicyException, VersionParseException
    {
        Version version;
        try 
        {
            version = Version.parse( request.getVersion() );
        }
        catch ( IllegalArgumentException e )
        {
            throw new VersionParseException( e.getMessage() );
        }
        
        VersionPolicyResult result = new VersionPolicyResult();
        result.setVersion( version.toReleaseVersion().toString() );
        return result;
    }

    public VersionPolicyResult getDevelopmentVersion( VersionPolicyRequest request )
        throws PolicyException, VersionParseException
    {
        Version version;
        try 
        {
            version = Version.parse( request.getVersion() );
        }
        catch ( IllegalArgumentException e )
        {
            throw new VersionParseException( e.getMessage() );
        }
        
        version = version.next( Element.MINOR );  
        VersionPolicyResult result = new VersionPolicyResult();
        result.setVersion( version.toString() + "-SNAPSHOT" );
        return result;
    }
}
