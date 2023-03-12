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
package org.apache.maven.shared.release.strategies;

import javax.inject.Named;
import javax.inject.Singleton;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.shared.release.strategy.Strategy;

/**
 * <p>DefaultStrategy class.</p>
 *
 * @author Robert Scholte
 * @since 3.0.0-M5
 */
@Singleton
@Named
public class DefaultStrategy implements Strategy {
    /**
     * The phases of release to run to prepare.
     */
    private final List<String> preparePhases;

    /**
     * The phases of release to run to perform.
     */
    private final List<String> performPhases;

    /**
     * The phases of release to run to rollback changes
     */
    private final List<String> rollbackPhases;

    /**
     * The phases to create a branch.
     */
    private final List<String> branchPhases;

    /**
     * The phases to create update versions.
     */
    private final List<String> updateVersionsPhases;

    public DefaultStrategy() {
        this.preparePhases = Collections.unmodifiableList(Arrays.asList(
                // START SNIPPET: prepare
                "check-poms",
                "scm-check-modifications",
                "check-dependency-snapshots",
                "create-backup-poms",
                "map-release-versions",
                "input-variables",
                "map-development-versions",
                "rewrite-poms-for-release",
                "generate-release-poms",
                "run-preparation-goals",
                "scm-commit-release",
                "scm-tag",
                "rewrite-poms-for-development",
                "remove-release-poms",
                "run-completion-goals",
                "scm-commit-development",
                "end-release"
                // END SNIPPET: prepare
                ));
        this.performPhases = Collections.unmodifiableList(Arrays.asList(
                // START SNIPPET: perform
                "verify-completed-prepare-phases", "checkout-project-from-scm", "run-perform-goals"
                // END SNIPPET: perform
                ));
        this.rollbackPhases = Collections.unmodifiableList(Arrays.asList(
                // START SNIPPET: rollback
                "restore-backup-poms", "scm-commit-rollback", "remove-scm-tag"
                // END SNIPPET: rollback
                ));
        this.branchPhases = Collections.unmodifiableList(Arrays.asList(
                // START SNIPPET: branch
                "check-poms",
                "scm-check-modifications",
                "create-backup-poms",
                "map-branch-versions",
                "branch-input-variables",
                "map-development-versions",
                "rewrite-poms-for-branch",
                "scm-commit-branch",
                "scm-branch",
                "rewrite-poms-for-development",
                "scm-commit-development",
                "end-release"
                // END SNIPPET: branch
                ));
        this.updateVersionsPhases = Collections.unmodifiableList(Arrays.asList(
                // START SNIPPET: update-versions
                "check-poms-updateversions", "create-backup-poms", "map-development-versions", "rewrite-pom-versions"
                // END SNIPPET: update-versions
                ));
    }

    @Override
    public final List<String> getPreparePhases() {
        return preparePhases;
    }

    @Override
    public List<String> getPerformPhases() {
        return performPhases;
    }

    @Override
    public List<String> getRollbackPhases() {
        return rollbackPhases;
    }

    @Override
    public List<String> getBranchPhases() {
        return branchPhases;
    }

    @Override
    public List<String> getUpdateVersionsPhases() {
        return updateVersionsPhases;
    }
}
