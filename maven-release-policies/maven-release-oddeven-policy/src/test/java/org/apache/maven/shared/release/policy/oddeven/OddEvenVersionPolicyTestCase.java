package org.apache.maven.shared.release.policy.oddeven;

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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class OddEvenVersionPolicyTestCase
{

    private VersionPolicy versionPolicy;

    @Before
    public void setUp()
    {
        versionPolicy = new OddEvenVersionPolicy();
    }

    @After
    public void tearDown()
    {
        versionPolicy = null;
    }

    @Test
    public void testConvertToSnapshot()
        throws Exception
    {
        String suggestedVersion = versionPolicy.getDevelopmentVersion( newVersionPolicyRequest( "1.0.0" ) )
                                               .getVersion();

        assertEquals( "1.0.1-SNAPSHOT", suggestedVersion );
    }

    @Test
    public void testConvertToRelease()
        throws Exception
    {
        String suggestedVersion = versionPolicy.getReleaseVersion( newVersionPolicyRequest( "1.0.0-SNAPSHOT" ) )
                                               .getVersion();

        assertEquals( "1.0.0", suggestedVersion );
    }

    @Test
    public void testConvertOddToRelease()
        throws Exception
    {
        String suggestedVersion = versionPolicy.getReleaseVersion( newVersionPolicyRequest( "1.0.1-SNAPSHOT" ) )
                                               .getVersion();

        assertEquals( "1.0.2", suggestedVersion );
    }

    private static VersionPolicyRequest newVersionPolicyRequest( String version )
    {
        return new VersionPolicyRequest().setVersion( version );
    }

}
