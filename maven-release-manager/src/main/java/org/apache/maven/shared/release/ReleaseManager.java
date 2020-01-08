package org.apache.maven.shared.release;

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
 * Release management classes.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public interface ReleaseManager
{
    /**
     * Prepare a release.
     *
     * @param prepareRequest             all prepare arguments
     * @throws ReleaseExecutionException if there is a problem performing the release
     * @throws ReleaseFailureException   if there is a problem performing the release
     * @since 2.3
     */
    void prepare( ReleasePrepareRequest prepareRequest ) throws ReleaseExecutionException, ReleaseFailureException;

    ReleaseResult prepareWithResult( ReleasePrepareRequest prepareRequest );

    ReleaseResult performWithResult( ReleasePerformRequest performRequest );

    /**
     * Perform a release
     *
     * @param performRequest   all perform arguments
     * @throws ReleaseExecutionException if there is a problem performing the release
     * @throws ReleaseFailureException   if there is a problem performing the release
     * @since 2.3
     */
    void perform( ReleasePerformRequest performRequest )
        throws ReleaseExecutionException, ReleaseFailureException;

    /**
     * Clean a release.
     *
     * @param cleanRequest all clean arguments
     * @throws ReleaseFailureException if exception when releasing
     * @since 2.3
     */
    void clean( ReleaseCleanRequest cleanRequest ) throws ReleaseFailureException;

    /**
     * Rollback changes made by the previous release
     *
     * @param rollbackRequest            all rollback arguments
     * @throws ReleaseExecutionException if there is a problem during release rollback
     * @throws ReleaseFailureException   if there is a problem during release rollback
     * @since 2.3
     */
    void rollback( ReleaseRollbackRequest rollbackRequest )
        throws ReleaseExecutionException, ReleaseFailureException;

    /**
     * Branch a project
     *
     * @param branchRequest              all branch arguments
     * @throws ReleaseExecutionException if there is a problem during release branch
     * @throws ReleaseFailureException   if there is a problem during release branch
     * @since 2.3
     */
    void branch( ReleaseBranchRequest branchRequest ) throws ReleaseExecutionException, ReleaseFailureException;

    /**
     * Update version numbers for a project
     *
     * @param updateVersionsRequest      all update versions arguments
     * @throws ReleaseExecutionException if there is a problem during update versions
     * @throws ReleaseFailureException   if there is a problem during update versions
     * @since 2.3
     */
    void updateVersions( ReleaseUpdateVersionsRequest updateVersionsRequest )
        throws ReleaseExecutionException, ReleaseFailureException;
}
