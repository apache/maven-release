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
package org.apache.maven.shared.release.strategy;

import java.util.List;

/**
 * Interface to override default strategy.
 *
 * If a method returns {@code null}, the default will be used, otherwise the provided collection of phaseIds
 *
 * @author Robert Scholte
 */
public interface Strategy {
    /**
     * <p>getPreparePhases.</p>
     *
     * @return The release phases to execute the calling the prepare goal
     */
    List<String> getPreparePhases();

    /**
     * <p>getPerformPhases.</p>
     *
     * @return The release phases to execute the calling the perform goal
     */
    List<String> getPerformPhases();

    /**
     * <p>getBranchPhases.</p>
     *
     * @return The release phases to execute the calling the branch goal
     */
    List<String> getBranchPhases();

    /**
     * <p>getRollbackPhases.</p>
     *
     * @return The release phases to execute the calling the rollback goal
     */
    List<String> getRollbackPhases();

    /**
     * <p>getUpdateVersionsPhases.</p>
     *
     * @return The release phases to execute the calling the update-versions goal
     */
    List<String> getUpdateVersionsPhases();

    
}
