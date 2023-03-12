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

import java.io.File;

import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.util.ReleaseUtil;

/**
 * <p>Abstract AbstractBackupPomsPhase class.</p>
 *
 * @author Edwin Punzalan
 */
public abstract class AbstractBackupPomsPhase extends AbstractReleasePhase {
    protected static final String BACKUP_SUFFIX = ".releaseBackup";

    /**
     * <p>getPomBackup.</p>
     *
     * @param project a {@link org.apache.maven.project.MavenProject} object
     * @return a {@link java.io.File} object
     */
    protected File getPomBackup(MavenProject project) {
        File pomFile = ReleaseUtil.getStandardPom(project);

        if (pomFile != null) {
            return new File(pomFile.getAbsolutePath() + BACKUP_SUFFIX);
        } else {
            return null;
        }
    }

    /**
     * <p>deletePomBackup.</p>
     *
     * @param project a {@link org.apache.maven.project.MavenProject} object
     */
    protected void deletePomBackup(MavenProject project) {
        File pomBackup = getPomBackup(project);

        if (pomBackup != null && pomBackup.exists()) {
            pomBackup.delete();
        }
    }
}
