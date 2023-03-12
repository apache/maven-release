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
package org.apache.maven.shared.release.phase;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.Map;

import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.scm.ScmRepositoryConfigurator;
import org.apache.maven.shared.release.scm.ScmTranslator;
import org.apache.maven.shared.release.transform.ModelETLFactory;

/**
 * Rewrite POMs for future development
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
@Singleton
@Named("rewrite-pom-versions")
public class RewritePomVersionsPhase extends AbstractRewritePomsPhase {
    @Inject
    public RewritePomVersionsPhase(
            ScmRepositoryConfigurator scmRepositoryConfigurator,
            Map<String, ModelETLFactory> modelETLFactories,
            Map<String, ScmTranslator> scmTranslators) {
        super(scmRepositoryConfigurator, modelETLFactories, scmTranslators);
    }

    @Override
    protected final String getPomSuffix() {
        return "next";
    }

    @Override
    protected void transformScm(
            MavenProject project,
            Model modelTarget,
            ReleaseDescriptor releaseDescriptor,
            String projectId,
            ScmRepository scmRepository,
            ReleaseResult result) {
        // We are only updating versions no mods to scm needed
    }

    @Override
    protected boolean isUpdateScm() {
        return false;
    }

    @Override
    protected String getOriginalVersion(ReleaseDescriptor releaseDescriptor, String projectKey, boolean simulate) {
        return releaseDescriptor.getProjectOriginalVersion(projectKey);
    }

    @Override
    protected String getNextVersion(ReleaseDescriptor releaseDescriptor, String key) {
        return releaseDescriptor.getProjectDevelopmentVersion(key);
    }

    @Override
    protected String getResolvedSnapshotVersion(String artifactVersionlessKey, ReleaseDescriptor resolvedSnapshotsMap) {
        // Only update the pom version, not the dependency versions
        return null;
    }
}
