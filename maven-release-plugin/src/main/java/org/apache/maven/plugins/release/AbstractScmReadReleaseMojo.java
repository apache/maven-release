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
package org.apache.maven.plugins.release;

import java.io.File;
import java.util.Map;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Scm;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.shared.release.ReleaseManager;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;

/**
 * Abstract Mojo containing SCM parameters for read operations.
 */
public abstract class AbstractScmReadReleaseMojo extends AbstractReleaseMojo {

    /**
     * The server id of the server which provides the credentials for the SCM in the <a href="https://maven.apache.org/settings.html">settings.xml</a> file.
     * If not set the default lookup uses the SCM URL to construct the server id like this:
     * {@code server-id=scm-host[":"scm-port]}.
     * <p>
     * Currently the POM does not allow to specify a server id for the SCM section.
     * <p>
     * Explicit authentication information provided via {@link #username}, {@link #password} or {@link #privateKey} will take precedence.
     * @since 3.2.0
     * @see <a href="https://maven.apache.org/scm/authentication.html">SCM Authentication</a>
     */
    @Parameter(property = "project.scm.id", defaultValue = "${project.scm.id}")
    private String serverId;

    /**
     * The username to use for authentication with the SCM.
     * @see <a href="https://maven.apache.org/scm/authentication.html">SCM Authentication</a>
     */
    @Parameter(property = "username")
    private String username;

    /**
     * The password to use for authentication with the SCM.
     * @see <a href="https://maven.apache.org/scm/authentication.html">SCM Authentication</a>
     */
    @Parameter(property = "password")
    private String password;

    /**
     * The path to the SSH private key to use for authentication with the SCM.
     * @since 3.2.0
     * @see <a href="https://maven.apache.org/scm/authentication.html">SCM Authentication</a>
     */
    @Parameter(property = "privateKey")
    private File privateKey;

    /**
     * Add a new or overwrite the default implementation per provider.
     * The key is the scm prefix and the value is the role hint/provider id of the
     * {@link org.apache.maven.scm.provider.ScmProvider}.
     *
     * @since 2.0-beta-6
     * @see ScmManager#setScmProviderImplementation(String, String)
     * @see <a href="https://maven.apache.org/scm/scms-overview.html">SCM Providers</a>
     */
    @Parameter
    private Map<String, String> providerImplementations;

    /**
     * When cloning a repository if it should be a shallow clone or a full clone.
     */
    @Parameter(defaultValue = "true", property = "scmShallowClone")
    private boolean scmShallowClone = true;

    /**
     * The SCM manager.
     */
    private final ScmManager scmManager;

    protected AbstractScmReadReleaseMojo(ReleaseManager releaseManager, ScmManager scmManager) {
        super(releaseManager);
        this.scmManager = scmManager;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (providerImplementations != null) {
            for (Map.Entry<String, String> providerEntry : providerImplementations.entrySet()) {
                getLog().info("Change the default '" + providerEntry.getKey() + "' provider implementation to '"
                        + providerEntry.getValue() + "'.");
                scmManager.setScmProviderImplementation(providerEntry.getKey(), providerEntry.getValue());
            }
        }
    }

    @Override
    protected ReleaseDescriptorBuilder createReleaseDescriptor() {
        ReleaseDescriptorBuilder descriptor = super.createReleaseDescriptor();

        if (privateKey != null) {
            descriptor.setScmPrivateKey(privateKey.getAbsolutePath());
        }
        descriptor.setScmPassword(password);
        descriptor.setScmUsername(username);
        descriptor.setScmShallowClone(scmShallowClone);

        if (project.getScm() != null) {
            if (project.getScm().getDeveloperConnection() != null) {
                descriptor.setScmSourceUrl(project.getScm().getDeveloperConnection());
            } else if (project.getScm().getConnection() != null) {
                descriptor.setScmSourceUrl(project.getScm().getConnection());
            }
        }

        descriptor.setScmId(serverId);

        for (MavenProject reactorProject : session.getProjects()) {
            if (reactorProject.getOriginalModel() != null
                    && reactorProject.getOriginalModel().getScm() != null) {
                String projectId =
                        ArtifactUtils.versionlessKey(reactorProject.getGroupId(), reactorProject.getArtifactId());

                descriptor.addOriginalScmInfo(projectId, buildScm(reactorProject));
            }
        }

        return descriptor;
    }

    /**
     * <p>buildScm.</p>
     *
     * @param project a {@link org.apache.maven.project.MavenProject} object
     * @return a {@link org.apache.maven.model.Scm} object
     */
    protected Scm buildScm(MavenProject project) {
        Scm scm;
        if (project.getOriginalModel().getScm() == null) {
            scm = null;
        } else {
            scm = new Scm();
            scm.setConnection(project.getOriginalModel().getScm().getConnection());
            scm.setDeveloperConnection(project.getOriginalModel().getScm().getDeveloperConnection());
            scm.setTag(project.getOriginalModel().getScm().getTag());
            scm.setUrl(project.getOriginalModel().getScm().getUrl());
        }
        return scm;
    }
}
