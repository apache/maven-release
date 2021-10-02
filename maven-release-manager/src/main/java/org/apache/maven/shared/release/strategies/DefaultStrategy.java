package org.apache.maven.shared.release.strategies;

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

import java.util.List;

import org.apache.maven.shared.release.strategy.Strategy;

/**
 * <p>DefaultStrategy class.</p>
 *
 * @author Robert Scholte
 * @since 3.0.0
 */
public class DefaultStrategy implements Strategy
{
    /**
     * The phases of release to run, and in what order.
     */
    private List<String> preparePhases;

    /**
     * The phases of release to run to perform.
     */
    private List<String> performPhases;

    /**
     * The phases of release to run to rollback changes
     */
    private List<String> rollbackPhases;

    /**
     * The phases to create a branch.
     */
    private List<String> branchPhases;

    /**
     * The phases to create update versions.
     */
    private List<String> updateVersionsPhases;

    @Override
    public List<String> getPreparePhases()
    {
        return preparePhases;
    }

    /**
     * <p>Setter for the field <code>preparePhases</code>.</p>
     *
     * @param preparePhases a {@link java.util.List} object
     */
    public void setPreparePhases( List<String> preparePhases )
    {
        this.preparePhases = preparePhases;
    }

    @Override
    public List<String> getPerformPhases()
    {
        return performPhases;
    }

    /**
     * <p>Setter for the field <code>performPhases</code>.</p>
     *
     * @param performPhases a {@link java.util.List} object
     */
    public void setPerformPhases( List<String> performPhases )
    {
        this.performPhases = performPhases;
    }

    @Override
    public List<String> getRollbackPhases()
    {
        return rollbackPhases;
    }

    /**
     * <p>Setter for the field <code>rollbackPhases</code>.</p>
     *
     * @param rollbackPhases a {@link java.util.List} object
     */
    public void setRollbackPhases( List<String> rollbackPhases )
    {
        this.rollbackPhases = rollbackPhases;
    }

    @Override
    public List<String> getBranchPhases()
    {
        return branchPhases;
    }

    /**
     * <p>Setter for the field <code>branchPhases</code>.</p>
     *
     * @param branchPhases a {@link java.util.List} object
     */
    public void setBranchPhases( List<String> branchPhases )
    {
        this.branchPhases = branchPhases;
    }

    @Override
    public List<String> getUpdateVersionsPhases()
    {
        return updateVersionsPhases;
    }

    /**
     * <p>Setter for the field <code>updateVersionsPhases</code>.</p>
     *
     * @param updateVersionsPhases a {@link java.util.List} object
     */
    public void setUpdateVersionsPhases( List<String> updateVersionsPhases )
    {
        this.updateVersionsPhases = updateVersionsPhases;
    }
}
