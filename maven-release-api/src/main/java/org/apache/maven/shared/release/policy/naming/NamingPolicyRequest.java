package org.apache.maven.shared.release.policy.naming;

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
 * @since 3.0.0 (MRELEASE-979)
 */
public class NamingPolicyRequest
{

    /**
     * Artifact release version for which the branch or tag is created.
     */
    private String version;

    /**
     * Proposed name for the branch or tag.
     */
    private String name;

    /**
     * true if branch operation, false for tag operation.
     */
    private boolean branch;

    /**
     * Generated (from scmTagFormat and project settings) name proposal.
     */
    private String proposal;

    public String getVersion()
    {
        return version;
    }

    public NamingPolicyRequest setVersion( String version )
    {
        this.version = version;
        return this;
    }

    public String getName()
    {
        return name;
    }

    public NamingPolicyRequest setName( String name )
    {
        this.name = name;
        return this;
    }

    public String getProposal()
    {
        return proposal;
    }

    public NamingPolicyRequest setProposal( String proposal )
    {
        this.proposal = proposal;
        return this;
    }

    public boolean isBranch()
    {
        return branch;
    }

    public NamingPolicyRequest setBranch( boolean branch )
    {
        this.branch = branch;
        return this;
    }
}
