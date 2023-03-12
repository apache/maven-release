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
package org.apache.maven.shared.release.policy.version;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepository;

/**
 * <p>VersionPolicyRequest class.</p>
 *
 * @since 2.5.1 (MRELEASE-431)
 */
public class VersionPolicyRequest {

    private String version;

    private Metadata metaData;

    private ScmRepository scmRepository;
    private ScmProvider scmProvider;
    private String workingDirectory;

    private String config;

    /**
     * <p>Getter for the field <code>version</code>.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getVersion() {
        return version;
    }

    /**
     * <p>Setter for the field <code>version</code>.</p>
     *
     * @param version a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.policy.version.VersionPolicyRequest} object
     */
    public VersionPolicyRequest setVersion(String version) {
        this.version = version;
        return this;
    }

    /**
     * <p>Getter for the field <code>metaData</code>.</p>
     *
     * @return a {@link org.apache.maven.artifact.repository.metadata.Metadata} object
     */
    public Metadata getMetaData() {
        return metaData;
    }

    /**
     * <p>Setter for the field <code>metaData</code>.</p>
     *
     * @param metaData a {@link org.apache.maven.artifact.repository.metadata.Metadata} object
     * @return a {@link org.apache.maven.shared.release.policy.version.VersionPolicyRequest} object
     */
    public VersionPolicyRequest setMetaData(Metadata metaData) {
        this.metaData = metaData;
        return this;
    }

    /**
     * <p>Getter for the field <code>scmRepository</code>.</p>
     *
     * @return a {@link ScmRepository} object
     */
    public ScmRepository getScmRepository() {
        return scmRepository;
    }

    /**
     * <p>Setter for the field <code>scmRepository</code>.</p>
     *
     * @param scmRepository The {@link ScmRepository} where the history can be retrieved.
     * @return a {@link org.apache.maven.shared.release.policy.version.VersionPolicyRequest} object
     */
    public VersionPolicyRequest setScmRepository(ScmRepository scmRepository) {
        this.scmRepository = scmRepository;
        return this;
    }

    /**
     * <p>Getter for the field <code>scmProvider</code>.</p>
     *
     * @return a {@link ScmProvider} object
     */
    public ScmProvider getScmProvider() {
        return scmProvider;
    }

    /**
     * <p>Setter for the field <code>scmProvider</code>.</p>
     *
     * @param scmProvider The {@link ScmProvider} where the history can be retrieved.
     * @return a {@link org.apache.maven.shared.release.policy.version.VersionPolicyRequest} object
     */
    public VersionPolicyRequest setScmProvider(ScmProvider scmProvider) {
        this.scmProvider = scmProvider;
        return this;
    }

    /**
     * <p>Getter for the field <code>workingDirectory</code>.</p>
     *
     * @return the {@link String} that contains the workingDirectory (can be null or empty).
     */
    public String getWorkingDirectory() {
        return workingDirectory;
    }

    /**
     * <p>Setter for the field <code>workingDirectory</code>.</p>
     *
     * @param workingDirectory The {@link String} that contains the workingDirectory (can be null or empty).
     * @return a {@link org.apache.maven.shared.release.policy.version.VersionPolicyRequest} object
     */
    public VersionPolicyRequest setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
        return this;
    }

    /**
     * <p>Getter for the field <code>config</code>.</p>
     *
     * @return the {@link String} that contains the config (can be null or empty).
     */
    public String getConfig() {
        return config;
    }

    /**
     * <p>Setter for the field <code>config</code>.</p>
     *
     * @param config The {@link String} that contains the config (can be null or empty).
     * @return a {@link org.apache.maven.shared.release.policy.version.VersionPolicyRequest} object
     */
    public VersionPolicyRequest setConfig(String config) {
        this.config = config;
        return this;
    }
}
