package org.apache.maven.plugins.release;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

/**
 * Perform a release from SCM to a staging repository.
 *
 * If no goals are given, these default to <code>deploy</code> or <code>deploy site:stage-deploy</code>,
 * if the project has a &lt;distributionManagement&gt;/&lt;site&gt; element.
 *
 * If the goals contain <code>site-deploy</code> or <code>site:deploy</code>, these
 * are overridden with <code>site:stage-deploy</code>.
 *
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 * @version $Id$
 * @since 2.0-beta-8
 */
@Mojo( name = "stage", aggregator = true, requiresProject = false )
public class StageReleaseMojo
    extends PerformReleaseMojo
{
    /**
     * URL of the staging repository to use.
     *
     * @since 2.0-beta-8
     */
    @Parameter( property = "stagingRepository", required = true )
    private String stagingRepository;

    @Override
    void createGoals()
    {
        if ( goals == null )
        {
            // set default
            goals = "deploy";
            if ( project.getDistributionManagement() != null
                && project.getDistributionManagement().getSite() != null )
            {
                goals += " site:stage-deploy";
            }
        }

        goals = StringUtils.replace( goals, "site-deploy", "site:stage-deploy" );
        goals = StringUtils.replace( goals, "site:deploy", "site:stage-deploy" );
    }

    @Override
    void setDeploymentRepository()
    {
        addArgument( "-DaltDeploymentRepository=\"" + stagingRepository + "\"" );
    }
}
