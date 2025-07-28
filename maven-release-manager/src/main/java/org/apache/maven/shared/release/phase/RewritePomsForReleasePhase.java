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

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.scm.ScmRepositoryConfigurator;
import org.apache.maven.shared.release.scm.ScmTranslator;
import org.apache.maven.shared.release.transform.ModelETLFactory;
import org.apache.maven.shared.release.util.ReleaseUtil;

/**
 * Rewrite POMs for release.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
@Singleton
@Named("rewrite-poms-for-release")
public class RewritePomsForReleasePhase extends AbstractRewritePomsPhase {
    @Inject
    public RewritePomsForReleasePhase(
            ScmRepositoryConfigurator scmRepositoryConfigurator,
            Map<String, ModelETLFactory> modelETLFactories,
            Map<String, ScmTranslator> scmTranslators) {
        super(scmRepositoryConfigurator, modelETLFactories, scmTranslators);
    }

    @Override
    protected final String getPomSuffix() {
        return "tag";
    }

    @Override
    protected void transformScm(
            MavenProject project,
            Model modelTarget,
            ReleaseDescriptor releaseDescriptor,
            String projectId,
            ScmRepository scmRepository,
            ReleaseResult result)
            throws ReleaseExecutionException {
        // If SCM is null in original model, it is inherited, no mods needed
        if (project.getScm() != null) {
            Scm scmRoot = modelTarget.getScm();
            if (scmRoot != null) {
                try {
                    translateScm(project, releaseDescriptor, scmRoot, scmRepository, result);
                } catch (IOException e) {
                    throw new ReleaseExecutionException(e.getMessage(), e);
                }
            } else {
                MavenProject parent = project.getParent();
                if (parent != null) {
                    // If the SCM element is not present, only add it if the parent was not mapped (ie, it's external to
                    // the release process and so has not been modified, so the values will not be correct on the tag),
                    String parentId = ArtifactUtils.versionlessKey(parent.getGroupId(), parent.getArtifactId());
                    if (!releaseDescriptor.hasOriginalScmInfo(parentId)) {
                        // we need to add it, since it has changed from the inherited value
                        Scm scmTarget = new Scm();
                        // reset default value (HEAD)
                        scmTarget.setTag(null);

                        try {
                            if (translateScm(project, releaseDescriptor, scmTarget, scmRepository, result)) {
                                modelTarget.setScm(scmTarget);
                            }
                        } catch (IOException e) {
                            throw new ReleaseExecutionException(e.getMessage(), e);
                        }
                    }
                }
            }
        }
    }

    private boolean translateScm(
            MavenProject project,
            ReleaseDescriptor releaseDescriptor,
            Scm scmTarget,
            ScmRepository scmRepository,
            ReleaseResult relResult)
            throws IOException {
        ScmTranslator translator = getScmTranslators().get(scmRepository.getProvider());
        boolean result = false;
        if (translator != null) {
            Scm scm = project.getOriginalModel().getScm();
            if (scm == null) {
                scm = project.getScm();
            }

            String tag = releaseDescriptor.getScmReleaseLabel();
            String tagBase = releaseDescriptor.getScmTagBase();

            // TODO: svn utils should take care of prepending this
            if (tagBase != null) {
                tagBase = "scm:svn:" + tagBase;
            }

            Path projectBasedir = project.getBasedir().toPath().toRealPath(LinkOption.NOFOLLOW_LINKS);
            Path workingDirectory = Paths.get(releaseDescriptor.getWorkingDirectory());

            int count = ReleaseUtil.getBaseWorkingDirectoryParentCount(workingDirectory, projectBasedir);

            if (scm.getConnection() != null) {
                String rootUrl = ReleaseUtil.realignScmUrl(count, scm.getConnection());

                String subDirectoryTag = scm.getConnection().substring(rootUrl.length());
                if (!subDirectoryTag.startsWith("/")) {
                    subDirectoryTag = "/" + subDirectoryTag;
                }

                String scmConnectionTag = tagBase;
                if (scmConnectionTag != null) {
                    String trunkUrl = scm.getDeveloperConnection();
                    if (trunkUrl == null) {
                        trunkUrl = scm.getConnection();
                    }
                    scmConnectionTag = translateUrlPath(trunkUrl, tagBase, scm.getConnection());
                }
                String value = translator.translateTagUrl(scm.getConnection(), tag + subDirectoryTag, scmConnectionTag);

                if (!value.equals(scm.getConnection())) {
                    scmTarget.setConnection(value);
                    result = true;
                }
            }

            if (scm.getDeveloperConnection() != null) {
                String rootUrl = ReleaseUtil.realignScmUrl(count, scm.getDeveloperConnection());

                String subDirectoryTag = scm.getDeveloperConnection().substring(rootUrl.length());
                if (!subDirectoryTag.startsWith("/")) {
                    subDirectoryTag = "/" + subDirectoryTag;
                }

                String value = translator.translateTagUrl(scm.getDeveloperConnection(), tag + subDirectoryTag, tagBase);

                if (!value.equals(scm.getDeveloperConnection())) {
                    scmTarget.setDeveloperConnection(value);
                    result = true;
                }
            }

            if (scm.getUrl() != null) {
                String rootUrl = ReleaseUtil.realignScmUrl(count, scm.getUrl());

                String subDirectoryTag = scm.getUrl().substring(rootUrl.length());
                if (!subDirectoryTag.startsWith("/")) {
                    subDirectoryTag = "/" + subDirectoryTag;
                }

                String tagScmUrl = tagBase;
                if (tagScmUrl != null) {
                    String trunkUrl = scm.getDeveloperConnection();
                    if (trunkUrl == null) {
                        trunkUrl = scm.getConnection();
                    }
                    tagScmUrl = translateUrlPath(trunkUrl, tagBase, scm.getUrl());
                }
                // use original tag base without protocol
                String value = translator.translateTagUrl(scm.getUrl(), tag + subDirectoryTag, tagScmUrl);
                if (!value.equals(scm.getUrl())) {
                    scmTarget.setUrl(value);
                    result = true;
                }
            }

            if (tag != null) {
                String value = translator.resolveTag(tag);
                if (value != null && !value.equals(scm.getTag())) {
                    scmTarget.setTag(value);
                    result = true;
                }
            }
        } else {
            String message = "No SCM translator found - skipping rewrite";

            relResult.appendDebug(message);

            getLogger().debug(message);
        }
        return result;
    }

    @Override
    protected String getOriginalVersion(ReleaseDescriptor releaseDescriptor, String projectKey, boolean simulate) {
        return releaseDescriptor.getProjectOriginalVersion(projectKey);
    }

    @Override
    protected String getNextVersion(ReleaseDescriptor releaseDescriptor, String key) {
        return releaseDescriptor.getProjectReleaseVersion(key);
    }

    @Override
    protected String getResolvedSnapshotVersion(String artifactVersionlessKey, ReleaseDescriptor releaseDescriptor) {
        return releaseDescriptor.getDependencyReleaseVersion(artifactVersionlessKey);
    }
}
