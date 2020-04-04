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

import org.apache.maven.shared.release.policy.PolicyException;

/**
 * API for branch and tag naming. Used by maven-release-plugin to suggest names for tags and branches.
 *
 * @since 3.0.0 (MRELEASE-979)
 */
public interface NamingPolicy
{
    /**
     * @return the calculation of the name used for branching or tagging.
     * 
     * @param request the {@code NamingPolicyRequest}
     * 
     * @throws PolicyException if exception in the policy
     */
    NamingPolicyResult getName( NamingPolicyRequest request )
        throws PolicyException;

}
