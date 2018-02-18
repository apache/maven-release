package org.apache.maven.shared.release.policies;

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

import static org.junit.Assert.assertEquals;

import org.apache.maven.shared.release.policy.version.VersionPolicy;
import org.apache.maven.shared.release.policy.version.VersionPolicyRequest;
import org.junit.Test;

public class DefaultVersionPolicyTest
{
    private VersionPolicy policy = new DefaultVersionPolicy();

    @Test
    public void testOneDigitReleaseVersion() throws Exception
    {
        VersionPolicyRequest request = new VersionPolicyRequest().setVersion( "1-SNAPSHOT" );
        assertEquals( "1", policy.getReleaseVersion( request ).getVersion() );
    }

    @Test
    public void testOneDigitDevelopmentVersion() throws Exception
    {
        VersionPolicyRequest request = new VersionPolicyRequest().setVersion( "1" );
        assertEquals( "2-SNAPSHOT", policy.getDevelopmentVersion( request ).getVersion() );
    }
    
    @Test
    public void testTwoDigitsReleaseVersion() throws Exception
    {
        VersionPolicyRequest request = new VersionPolicyRequest().setVersion( "1.0-SNAPSHOT" );
        assertEquals( "1.0", policy.getReleaseVersion( request ).getVersion() );
    }
    
    @Test
    public void testTwoDigitsDevelopmentVersion() throws Exception
    {
        VersionPolicyRequest request = new VersionPolicyRequest().setVersion( "1.0" );
        assertEquals( "1.1-SNAPSHOT", policy.getDevelopmentVersion( request ).getVersion() );
    }

    @Test
    public void testThreeDigitsReleaseVersion() throws Exception
    {
        VersionPolicyRequest request = new VersionPolicyRequest().setVersion( "1.0.0-SNAPSHOT" );
        assertEquals( "1.0.0", policy.getReleaseVersion( request ).getVersion() );
    }
    
    @Test
    public void testThreeDigitsDevelopmentVersion() throws Exception
    {
        VersionPolicyRequest request = new VersionPolicyRequest().setVersion( "1.0.0" );
        assertEquals( "1.0.1-SNAPSHOT", policy.getDevelopmentVersion( request ).getVersion() );
    }
}
