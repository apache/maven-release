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

package org.apache.maven.its.release.version;

import org.apache.maven.scm.ChangeSet;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.changelog.ChangeLogScmRequest;
import org.apache.maven.scm.command.changelog.ChangeLogScmResult;
import org.apache.maven.shared.release.policy.version.VersionPolicy;
import org.apache.maven.shared.release.policy.version.VersionPolicyRequest;
import org.apache.maven.shared.release.policy.version.VersionPolicyResult;
import org.apache.maven.shared.release.versions.VersionParseException;
import org.eclipse.sisu.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;

@Singleton
@Named( "DummyVersionPolicy" )
@Description( "A dummy VersionPolicy that always gives version 1.2.3. and only logs the provided input." )
public class DummyVersionPolicy implements VersionPolicy
{
    private static final Logger LOG = LoggerFactory.getLogger( DummyVersionPolicy.class );

    public VersionPolicyResult getReleaseVersion( VersionPolicyRequest request )
    {
        LOG.info("[INPUT:Config] #{}# [INPUT:Config]", request.getConfig());
        LOG.info("[INPUT:Version] #{}# [INPUT:Version]", request.getVersion());
        LOG.info("[INPUT:WorkingDirectory] #{}# [INPUT:WorkingDirectory]", request.getWorkingDirectory());

        ChangeLogScmRequest changeLogRequest = new ChangeLogScmRequest(
                request.getScmRepository(),
                new ScmFileSet(new File(request.getWorkingDirectory()))
        );

        ChangeLogScmResult changeLog;
        try {
            changeLog = request.getScmProvider().changeLog(changeLogRequest);
        } catch (ScmException e) {
            throw new RuntimeException(e);
        }

        int index=0 ;
        for (ChangeSet changeSet : changeLog.getChangeLog().getChangeSets()) {
            LOG.info("[INPUT:Changeset[{}].comment] #{}#", index, changeSet.getComment());
            LOG.info("[INPUT:Changeset[{}].tag] #{}#", index, String.join(",", changeSet.getTags()));
            index++;
        }

        VersionPolicyResult result = new VersionPolicyResult();
        result.setVersion("1.2.3");
        return result;
    }

    public VersionPolicyResult getDevelopmentVersion(VersionPolicyRequest request)
        throws VersionParseException {
        VersionPolicyResult result = new VersionPolicyResult();
        result.setVersion("4.5.6-SNAPSHOT");
        return result;
    }
}
