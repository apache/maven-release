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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder.BuilderReleaseDescriptor;
import org.apache.maven.shared.release.config.ReleaseDescriptorStore;
import org.apache.maven.shared.release.config.ReleaseDescriptorStoreException;
import org.apache.maven.shared.release.config.ReleaseUtils;
import org.apache.maven.shared.release.phase.ReleasePhase;
import org.apache.maven.shared.release.phase.ResourceGenerator;
import org.apache.maven.shared.release.strategy.Strategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of the release manager.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
@Singleton
@Named
public class DefaultReleaseManager implements ReleaseManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Map<String, Strategy> strategies;

    /**
     * The available phases.
     */
    private final Map<String, ReleasePhase> releasePhases;

    /**
     * The configuration storage.
     */
    private final AtomicReference<ReleaseDescriptorStore> configStore;

    @Inject
    public DefaultReleaseManager(
            Map<String, Strategy> strategies,
            Map<String, ReleasePhase> releasePhases,
            @Named("properties") ReleaseDescriptorStore configStore) {
        this.strategies = requireNonNull(strategies);
        this.releasePhases = requireNonNull(releasePhases);
        this.configStore = new AtomicReference<>(requireNonNull(configStore));
    }

    /**
     * For easier testing only!
     */
    public void setConfigStore(ReleaseDescriptorStore configStore) {
        this.configStore.set(configStore);
    }

    @Override
    public ReleaseResult prepareWithResult(ReleasePrepareRequest prepareRequest) {
        ReleaseResult result = new ReleaseResult();

        result.setStartTime(System.currentTimeMillis());

        try {
            prepare(prepareRequest, result);

            result.setResultCode(ReleaseResult.SUCCESS);
        } catch (ReleaseExecutionException | ReleaseFailureException e) {
            captureException(result, prepareRequest.getReleaseManagerListener(), e);
        } finally {
            result.setEndTime(System.currentTimeMillis());
        }

        return result;
    }

    @Override
    public void prepare(ReleasePrepareRequest prepareRequest)
            throws ReleaseExecutionException, ReleaseFailureException {
        prepare(prepareRequest, new ReleaseResult());
    }

    private void prepare(ReleasePrepareRequest prepareRequest, ReleaseResult result)
            throws ReleaseExecutionException, ReleaseFailureException {

        final ReleaseDescriptorBuilder builder = prepareRequest.getReleaseDescriptorBuilder();

        // Create a config containing values from the session properties (ie command line properties with cli).
        ReleaseUtils.copyPropertiesToReleaseDescriptor(
                prepareRequest.getUserProperties(), new ReleaseDescriptorBuilder() {
                    public ReleaseDescriptorBuilder addDevelopmentVersion(String key, String value) {
                        builder.addDevelopmentVersion(key, value);
                        return this;
                    }

                    public ReleaseDescriptorBuilder addReleaseVersion(String key, String value) {
                        builder.addReleaseVersion(key, value);
                        return this;
                    }

                    public ReleaseDescriptorBuilder addDependencyReleaseVersion(String dependencyKey, String version) {
                        builder.addDependencyReleaseVersion(dependencyKey, version);
                        return this;
                    }

                    public ReleaseDescriptorBuilder addDependencyDevelopmentVersion(
                            String dependencyKey, String version) {
                        builder.addDependencyDevelopmentVersion(dependencyKey, version);
                        return this;
                    }
                });

        BuilderReleaseDescriptor config;
        if (BooleanUtils.isNotFalse(prepareRequest.getResume())) {
            config = loadReleaseDescriptor(builder, prepareRequest.getReleaseManagerListener());
        } else {
            config = ReleaseUtils.buildReleaseDescriptor(builder);
        }

        Strategy releaseStrategy = getStrategy(config.getReleaseStrategyId());

        List<String> preparePhases = getGoalPhases(releaseStrategy, "prepare");

        goalStart(prepareRequest.getReleaseManagerListener(), "prepare", preparePhases);

        // Later, it would be a good idea to introduce a proper workflow tool so that the release can be made up of a
        // more flexible set of steps.

        String completedPhase = config.getCompletedPhase();
        int index = preparePhases.indexOf(completedPhase);

        for (int idx = 0; idx <= index; idx++) {
            phaseSkip(prepareRequest.getReleaseManagerListener(), preparePhases.get(idx));
        }

        if (index == preparePhases.size() - 1) {
            logInfo(
                    result,
                    "Release preparation already completed. You can now continue with release:perform, "
                            + "or start again using the -Dresume=false flag");
        } else if (index >= 0) {
            logInfo(result, "Resuming release from phase '" + preparePhases.get(index + 1) + "'");
        }

        // start from next phase
        for (int i = index + 1; i < preparePhases.size(); i++) {
            String name = preparePhases.get(i);

            ReleasePhase phase = releasePhases.get(name);

            if (phase == null) {
                throw new ReleaseExecutionException("Unable to find phase '" + name + "' to execute");
            }

            phaseStart(prepareRequest.getReleaseManagerListener(), name);

            ReleaseResult phaseResult = null;
            try {
                if (BooleanUtils.isTrue(prepareRequest.getDryRun())) {
                    phaseResult = phase.simulate(
                            config, prepareRequest.getReleaseEnvironment(), prepareRequest.getReactorProjects());
                } else {
                    phaseResult = phase.execute(
                            config, prepareRequest.getReleaseEnvironment(), prepareRequest.getReactorProjects());
                }
            } finally {
                if (result != null && phaseResult != null) {
                    result.appendOutput(phaseResult.getOutput());
                }
            }

            config.setCompletedPhase(name);
            try {
                configStore.get().write(config);
            } catch (ReleaseDescriptorStoreException e) {
                // TODO: rollback?
                throw new ReleaseExecutionException("Error writing release properties after completing phase", e);
            }

            phaseEnd(prepareRequest.getReleaseManagerListener());
        }

        goalEnd(prepareRequest.getReleaseManagerListener());
    }

    @Override
    public void rollback(ReleaseRollbackRequest rollbackRequest)
            throws ReleaseExecutionException, ReleaseFailureException {
        ReleaseDescriptor releaseDescriptor =
                loadReleaseDescriptor(rollbackRequest.getReleaseDescriptorBuilder(), null);

        Strategy releaseStrategy = getStrategy(releaseDescriptor.getReleaseStrategyId());

        List<String> rollbackPhases = getGoalPhases(releaseStrategy, "rollback");

        goalStart(rollbackRequest.getReleaseManagerListener(), "rollback", rollbackPhases);

        for (String name : rollbackPhases) {
            ReleasePhase phase = releasePhases.get(name);

            if (phase == null) {
                throw new ReleaseExecutionException("Unable to find phase '" + name + "' to execute");
            }

            phaseStart(rollbackRequest.getReleaseManagerListener(), name);
            phase.execute(
                    releaseDescriptor, rollbackRequest.getReleaseEnvironment(), rollbackRequest.getReactorProjects());
            phaseEnd(rollbackRequest.getReleaseManagerListener());
        }

        // call release:clean so that resume will not be possible anymore after a rollback
        clean(rollbackRequest);
        goalEnd(rollbackRequest.getReleaseManagerListener());
    }

    @Override
    public ReleaseResult performWithResult(ReleasePerformRequest performRequest) {
        ReleaseResult result = new ReleaseResult();

        try {
            result.setStartTime(System.currentTimeMillis());

            perform(performRequest, result);

            result.setResultCode(ReleaseResult.SUCCESS);
        } catch (ReleaseExecutionException | ReleaseFailureException e) {
            captureException(result, performRequest.getReleaseManagerListener(), e);
        } finally {
            result.setEndTime(System.currentTimeMillis());
        }

        return result;
    }

    @Override
    public void perform(ReleasePerformRequest performRequest)
            throws ReleaseExecutionException, ReleaseFailureException {
        perform(performRequest, new ReleaseResult());
    }

    private void perform(ReleasePerformRequest performRequest, ReleaseResult result)
            throws ReleaseExecutionException, ReleaseFailureException {

        // https://issues.apache.org/jira/browse/MRELEASE-1104 because stageRepository is an additional arg
        // and only adding at perform stage it's not available during prepare and so not save the not available
        // when reloading. save this then change again after load
        String additionalArguments =
                performRequest.getReleaseDescriptorBuilder().build().getAdditionalArguments();

        List<String> specificProfiles = ReleaseUtils.buildReleaseDescriptor(
                        performRequest.getReleaseDescriptorBuilder())
                .getActivateProfiles();

        ReleaseDescriptorBuilder builder = loadReleaseDescriptorBuilder(
                performRequest.getReleaseDescriptorBuilder(), performRequest.getReleaseManagerListener());

        builder.setAdditionalArguments(additionalArguments);

        if (specificProfiles != null && !specificProfiles.isEmpty()) {
            List<String> allProfiles =
                    new ArrayList<>(ReleaseUtils.buildReleaseDescriptor(builder).getActivateProfiles());
            for (String specificProfile : specificProfiles) {
                if (!allProfiles.contains(specificProfile)) {
                    allProfiles.add(specificProfile);
                }
            }
            builder.setActivateProfiles(allProfiles);
        }

        ReleaseDescriptor releaseDescriptor = ReleaseUtils.buildReleaseDescriptor(builder);

        Strategy releaseStrategy = getStrategy(releaseDescriptor.getReleaseStrategyId());

        List<String> performPhases = getGoalPhases(releaseStrategy, "perform");

        goalStart(performRequest.getReleaseManagerListener(), "perform", performPhases);

        for (String name : performPhases) {
            ReleasePhase phase = releasePhases.get(name);

            if (phase == null) {
                throw new ReleaseExecutionException("Unable to find phase '" + name + "' to execute");
            }

            phaseStart(performRequest.getReleaseManagerListener(), name);

            ReleaseResult phaseResult = null;
            try {
                if (BooleanUtils.isTrue(performRequest.getDryRun())) {
                    phaseResult = phase.simulate(
                            releaseDescriptor,
                            performRequest.getReleaseEnvironment(),
                            performRequest.getReactorProjects());
                } else {
                    phaseResult = phase.execute(
                            releaseDescriptor,
                            performRequest.getReleaseEnvironment(),
                            performRequest.getReactorProjects());
                }
            } finally {
                if (result != null && phaseResult != null) {
                    result.appendOutput(phaseResult.getOutput());
                }
            }

            phaseEnd(performRequest.getReleaseManagerListener());
        }

        if (BooleanUtils.isNotFalse(performRequest.getClean())) {
            // call release:clean so that resume will not be possible anymore after a perform
            clean(performRequest);
        }

        goalEnd(performRequest.getReleaseManagerListener());
    }

    @Override
    public void branch(ReleaseBranchRequest branchRequest) throws ReleaseExecutionException, ReleaseFailureException {
        final ReleaseDescriptorBuilder builder = branchRequest.getReleaseDescriptorBuilder();

        ReleaseUtils.copyPropertiesToReleaseDescriptor(
                branchRequest.getUserProperties(), new ReleaseDescriptorBuilder() {
                    public ReleaseDescriptorBuilder addDevelopmentVersion(String key, String value) {
                        builder.addDevelopmentVersion(key, value);
                        return this;
                    }

                    public ReleaseDescriptorBuilder addReleaseVersion(String key, String value) {
                        builder.addReleaseVersion(key, value);
                        return this;
                    }
                });

        ReleaseDescriptor releaseDescriptor = loadReleaseDescriptor(builder, branchRequest.getReleaseManagerListener());

        boolean dryRun = BooleanUtils.isTrue(branchRequest.getDryRun());

        Strategy releaseStrategy = getStrategy(releaseDescriptor.getReleaseStrategyId());

        List<String> branchPhases = getGoalPhases(releaseStrategy, "branch");

        goalStart(branchRequest.getReleaseManagerListener(), "branch", branchPhases);

        for (String name : branchPhases) {
            ReleasePhase phase = releasePhases.get(name);

            if (phase == null) {
                throw new ReleaseExecutionException("Unable to find phase '" + name + "' to execute");
            }

            phaseStart(branchRequest.getReleaseManagerListener(), name);

            if (dryRun) {
                phase.simulate(
                        releaseDescriptor, branchRequest.getReleaseEnvironment(), branchRequest.getReactorProjects());
            } else // getDryRun is null or FALSE
            {
                phase.execute(
                        releaseDescriptor, branchRequest.getReleaseEnvironment(), branchRequest.getReactorProjects());
            }

            phaseEnd(branchRequest.getReleaseManagerListener());
        }

        if (!dryRun) {
            clean(branchRequest);
        }

        goalEnd(branchRequest.getReleaseManagerListener());
    }

    @Override
    public void updateVersions(ReleaseUpdateVersionsRequest updateVersionsRequest)
            throws ReleaseExecutionException, ReleaseFailureException {
        final ReleaseDescriptorBuilder builder = updateVersionsRequest.getReleaseDescriptorBuilder();

        // Create a config containing values from the session properties (ie command line properties with cli).
        ReleaseUtils.copyPropertiesToReleaseDescriptor(
                updateVersionsRequest.getUserProperties(), new ReleaseDescriptorBuilder() {
                    public ReleaseDescriptorBuilder addDevelopmentVersion(String key, String value) {
                        builder.addDevelopmentVersion(key, value);
                        return this;
                    }

                    public ReleaseDescriptorBuilder addReleaseVersion(String key, String value) {
                        builder.addReleaseVersion(key, value);
                        return this;
                    }
                });

        ReleaseDescriptor releaseDescriptor =
                loadReleaseDescriptor(builder, updateVersionsRequest.getReleaseManagerListener());

        Strategy releaseStrategy = getStrategy(releaseDescriptor.getReleaseStrategyId());

        List<String> updateVersionsPhases = getGoalPhases(releaseStrategy, "updateVersions");

        goalStart(updateVersionsRequest.getReleaseManagerListener(), "updateVersions", updateVersionsPhases);

        for (String name : updateVersionsPhases) {
            ReleasePhase phase = releasePhases.get(name);

            if (phase == null) {
                throw new ReleaseExecutionException("Unable to find phase '" + name + "' to execute");
            }

            phaseStart(updateVersionsRequest.getReleaseManagerListener(), name);
            phase.execute(
                    releaseDescriptor,
                    updateVersionsRequest.getReleaseEnvironment(),
                    updateVersionsRequest.getReactorProjects());
            phaseEnd(updateVersionsRequest.getReleaseManagerListener());
        }

        clean(updateVersionsRequest);

        goalEnd(updateVersionsRequest.getReleaseManagerListener());
    }

    /**
     * Determines the path of the working directory. By default, this is the
     * checkout directory. For some SCMs, the project root directory is not the
     * checkout directory itself, but a SCM-specific subdirectory.
     *
     * @param checkoutDirectory            The checkout directory as java.io.File
     * @param relativePathProjectDirectory The relative path of the project directory within the checkout
     *                                     directory or ""
     * @return The working directory
     */
    protected File determineWorkingDirectory(File checkoutDirectory, String relativePathProjectDirectory) {
        if (relativePathProjectDirectory != null && !relativePathProjectDirectory.isEmpty()) {
            return new File(checkoutDirectory, relativePathProjectDirectory);
        } else {
            return checkoutDirectory;
        }
    }

    private BuilderReleaseDescriptor loadReleaseDescriptor(
            ReleaseDescriptorBuilder builder, ReleaseManagerListener listener) throws ReleaseExecutionException {
        return ReleaseUtils.buildReleaseDescriptor(loadReleaseDescriptorBuilder(builder, listener));
    }

    private ReleaseDescriptorBuilder loadReleaseDescriptorBuilder(
            ReleaseDescriptorBuilder builder, ReleaseManagerListener listener) throws ReleaseExecutionException {
        try {
            return configStore.get().read(builder);
        } catch (ReleaseDescriptorStoreException e) {
            throw new ReleaseExecutionException("Error reading stored configuration: " + e.getMessage(), e);
        }
    }

    /**
     * <p>clean.</p>
     *
     * @param releaseRequest a {@link org.apache.maven.shared.release.AbstractReleaseRequest} object
     * @throws org.apache.maven.shared.release.ReleaseFailureException if any.
     */
    protected void clean(AbstractReleaseRequest releaseRequest) throws ReleaseFailureException {
        ReleaseCleanRequest cleanRequest = new ReleaseCleanRequest();
        cleanRequest.setReleaseDescriptorBuilder(releaseRequest.getReleaseDescriptorBuilder());
        cleanRequest.setReleaseManagerListener(releaseRequest.getReleaseManagerListener());
        cleanRequest.setReactorProjects(releaseRequest.getReactorProjects());

        clean(cleanRequest);
    }

    @Override
    public void clean(ReleaseCleanRequest cleanRequest) throws ReleaseFailureException {
        logger.info("Cleaning up after release...");

        ReleaseDescriptor releaseDescriptor =
                ReleaseUtils.buildReleaseDescriptor(cleanRequest.getReleaseDescriptorBuilder());

        configStore.get().delete(releaseDescriptor);

        Strategy releaseStrategy = getStrategy(releaseDescriptor.getReleaseStrategyId());

        Set<String> phases = new LinkedHashSet<>();
        phases.addAll(getGoalPhases(releaseStrategy, "prepare"));
        phases.addAll(getGoalPhases(releaseStrategy, "branch"));

        for (String name : phases) {
            ReleasePhase phase = releasePhases.get(name);

            if (phase instanceof ResourceGenerator) {
                ((ResourceGenerator) phase).clean(cleanRequest.getReactorProjects());
            }
        }
    }

    void goalStart(ReleaseManagerListener listener, String goal, List<String> phases) {
        if (listener != null) {
            listener.goalStart(goal, phases);
        }
    }

    void goalEnd(ReleaseManagerListener listener) {
        if (listener != null) {
            listener.goalEnd();
        }
    }

    void phaseSkip(ReleaseManagerListener listener, String name) {
        if (listener != null) {
            listener.phaseSkip(name);
        }
    }

    void phaseStart(ReleaseManagerListener listener, String name) {
        if (listener != null) {
            listener.phaseStart(name);
        }
    }

    void phaseEnd(ReleaseManagerListener listener) {
        if (listener != null) {
            listener.phaseEnd();
        }
    }

    void error(ReleaseManagerListener listener, String name) {
        if (listener != null) {
            listener.error(name);
        }
    }

    private Strategy getStrategy(String strategyId) throws ReleaseFailureException {
        Strategy strategy = strategies.get(strategyId);
        if (strategy == null) {
            throw new ReleaseFailureException("Unknown strategy: " + strategyId);
        }
        return strategy;
    }

    private List<String> getGoalPhases(Strategy strategy, String goal) {
        List<String> phases;

        if ("prepare".equals(goal)) {
            phases = strategy.getPreparePhases();
            if (phases == null) {
                phases = strategies.get("default").getPreparePhases();
            }
        } else if ("perform".equals(goal)) {
            phases = strategy.getPerformPhases();
            if (phases == null) {
                phases = strategies.get("default").getPerformPhases();
            }
        } else if ("rollback".equals(goal)) {
            phases = strategy.getRollbackPhases();
            if (phases == null) {
                phases = strategies.get("default").getRollbackPhases();
            }
        } else if ("branch".equals(goal)) {
            phases = strategy.getBranchPhases();
            if (phases == null) {
                phases = strategies.get("default").getBranchPhases();
            }
        } else if ("updateVersions".equals(goal)) {
            phases = strategy.getUpdateVersionsPhases();
            if (phases == null) {
                phases = strategies.get("default").getUpdateVersionsPhases();
            }
        } else {
            phases = null;
        }

        return Collections.unmodifiableList(phases); // TODO: NPE here in phases=null above!
    }

    private void logInfo(ReleaseResult result, String message) {
        if (result != null) {
            result.appendInfo(message);
        }

        logger.info(message);
    }

    private void captureException(ReleaseResult result, ReleaseManagerListener listener, Exception e) {
        if (listener != null) {
            listener.error(e.getMessage());
        }

        result.appendError(e);

        result.setResultCode(ReleaseResult.ERROR);
    }
}
