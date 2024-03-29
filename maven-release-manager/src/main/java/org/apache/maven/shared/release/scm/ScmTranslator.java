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
package org.apache.maven.shared.release.scm;

/**
 * Translate the SCM information after tagging/reverting to trunk.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
// TODO move this API into SCM?
public interface ScmTranslator {
    /**
     * Take an URL and find the correct replacement URL for a given branch.
     *
     * @param url        the source URL
     * @param branchName the branch name
     * @param branchBase the branch base for providers that support it
     * @return the replacement URL
     */
    String translateBranchUrl(String url, String branchName, String branchBase);

    /**
     * Take an URL and find the correct replacement URL for a given tag.
     *
     * @param url     the source URL
     * @param tag     the tag
     * @param tagBase the tag base for providers that support it
     * @return the replacement URL
     */
    String translateTagUrl(String url, String tag, String tagBase);

    /**
     * Determine what tag should be added to the POM given the original tag and the new one.
     *
     * @param tag the new tag
     * @return the tag to use, or <code>null</code> if the provider does not use tags
     */
    String resolveTag(String tag);

    /**
     * Translates an ScmFile path to a path relative to the working directory.
     *
     * @param path the ScmFile path
     * @return the relative path with OS specific File separator
     * @since 2.3.1
     */
    String toRelativePath(String path);
}
