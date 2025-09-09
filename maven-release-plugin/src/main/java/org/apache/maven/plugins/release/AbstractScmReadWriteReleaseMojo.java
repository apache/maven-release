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

import javax.inject.Inject;

import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.shared.release.ReleaseManager;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;

/**
 * Abstract Mojo containing SCM parameters for read/write operations.
 *
 * @author Robert Scholte
 */
// Extra layer since 2.4. Don't use @since doclet, these would be inherited by the subclasses
public abstract class AbstractScmReadWriteReleaseMojo extends AbstractScmReadReleaseMojo {
    /**
     * The SCM tag to use.
     */
    @Parameter(alias = "releaseLabel", property = "tag")
    private String tag;

    /**
     * Format to use when generating the tag name if none is specified. Property interpolation is performed on the
     * tag, but in order to ensure that the interpolation occurs during release, you must use <code>@{...}</code>
     * to reference the properties rather than <code>${...}</code>. The following properties are available:
     * <ul>
     *     <li><code>groupId</code> or <code>project.groupId</code> - The groupId of the root project.
     *     <li><code>artifactId</code> or <code>project.artifactId</code> - The artifactId of the root project.
     *     <li><code>version</code> or <code>project.version</code> - The release version of the root project.
     * </ul>
     *
     * @since 2.2.0
     */
    @Parameter(defaultValue = "@{project.artifactId}-@{project.version}", property = "tagNameFormat")
    private String tagNameFormat;

    /**
     * The tag base directory in SVN, you must define it if you don't use the standard svn layout (trunk/tags/branches).
     * For example, <code>http://svn.apache.org/repos/asf/maven/plugins/tags</code>. The URL is an SVN URL and does not
     * include the SCM provider and protocol.
     */
    @Parameter(property = "tagBase")
    private String tagBase;

    /**
     * The message prefix to use for all SCM changes.
     *
     * @since 2.0-beta-5
     */
    @Parameter(defaultValue = "[maven-release-plugin] ", property = "scmCommentPrefix")
    private String scmCommentPrefix;

    /**
     * Implemented with git will or not push changes to the upstream repository.
     * <code>true</code> by default to preserve backward compatibility.
     * @since 2.1
     */
    @Parameter(defaultValue = "true", property = "pushChanges")
    private boolean pushChanges = true;

    /**
     * A workItem for SCMs like RTC, TFS etc, that may require additional
     * information to perform a pushChange operation.
     *
     * @since 3.0.0-M5
     */
    @Parameter(property = "workItem")
    private String workItem;

    @Inject
    protected AbstractScmReadWriteReleaseMojo(ReleaseManager releaseManager, ScmManager scmManager) {
        super(releaseManager, scmManager);
    }

    @Override
    protected ReleaseDescriptorBuilder createReleaseDescriptor() {
        ReleaseDescriptorBuilder descriptor = super.createReleaseDescriptor();

        // extend the descriptor with SCM parameter relevant for write operations
        descriptor.setScmReleaseLabel(tag);
        descriptor.setScmTagNameFormat(tagNameFormat);
        descriptor.setScmTagBase(tagBase);
        descriptor.setScmCommentPrefix(scmCommentPrefix);

        descriptor.setPushChanges(pushChanges);
        descriptor.setWorkItem(workItem);

        return descriptor;
    }
}
