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
package org.apache.maven.shared.release;

import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;

import static org.apache.maven.shared.utils.logging.MessageUtils.buffer;

/**
 * <p>DefaultReleaseManagerListener class.</p>
 *
 * @author Hervé Boutemy
 */
public class DefaultReleaseManagerListener implements ReleaseManagerListener {
    private final Log log;

    private final boolean dryRun;

    private String goal;

    private List<String> phases;

    private int currentPhase;

    /**
     * <p>Constructor for DefaultReleaseManagerListener.</p>
     *
     * @param log a {@link org.apache.maven.plugin.logging.Log} object
     */
    public DefaultReleaseManagerListener(Log log) {
        this(log, false);
    }

    /**
     * <p>Constructor for DefaultReleaseManagerListener.</p>
     *
     * @param log a {@link org.apache.maven.plugin.logging.Log} object
     * @param dryRun a boolean
     */
    public DefaultReleaseManagerListener(Log log, boolean dryRun) {
        this.log = log;
        this.dryRun = dryRun;
    }

    private void nextPhase(String name) {
        currentPhase++;
        if (!name.equals(phases.get(currentPhase))) {
            log.warn("inconsistent phase name: expected '" + phases.get(currentPhase) + "' but got '" + name + "'");
        }
    }

    public void goalStart(String goal, List<String> phases) {
        log.info("starting " + buffer().mojo(goal) + " goal" + (dryRun ? " in dry-run mode" : "") + ", composed of "
                + phases.size() + " phases: " + StringUtils.join(phases.iterator(), ", "));
        currentPhase = -1;
        this.phases = phases;
        this.goal = goal;
    }

    public void phaseStart(String name) {
        nextPhase(name);
        log.info((currentPhase + 1) + "/" + phases.size() + ' ' + buffer().mojo(goal + ':' + name)
                + (dryRun ? " dry-run" : ""));
    }

    /**
     * <p>phaseEnd.</p>
     */
    public void phaseEnd() {
        // NOOP
    }

    public void phaseSkip(String name) {
        nextPhase(name);
    }

    /**
     * <p>goalEnd.</p>
     */
    public void goalEnd() {
        goal = null;
        phases = null;
    }

    public void error(String reason) {
        log.error("error during phase " + (currentPhase + 1) + "/" + phases.size() + " " + phases.get(currentPhase)
                + ": " + reason);
    }
}
