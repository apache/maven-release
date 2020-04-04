package org.apache.maven.shared.release.versions;

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

/**
 * 
 */
public class VersionComparisonConflictException
    extends RuntimeException
{

    private final String lhsVersion;

    private final String rhsVersion;

    private int aetherComparisonResult;

    private int mavenComparisonResult;

    public VersionComparisonConflictException( String lhsVersion, String rhsVersion, int aetherComparisonResult,
                                               int mavenComparisonResult )
    {
        this.lhsVersion = lhsVersion;
        this.rhsVersion = rhsVersion;
        this.aetherComparisonResult = aetherComparisonResult;
        this.mavenComparisonResult = mavenComparisonResult;
    }

    @Override
    public String getMessage()
    {
        return "Conflict when comparing " + lhsVersion + " with " + rhsVersion + "; Aether: " + aetherComparisonResult
            + "; Maven: " + mavenComparisonResult;
    }

}
