package org.apache.maven.shared.release.phase;

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

import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.scm.ReleaseScmRepositoryException;
import org.apache.maven.shared.release.scm.ScmRepositoryConfigurator;

/**
 * Abstract release POM phase.
 *
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 */
public abstract class AbstractReleasePomsPhase extends AbstractReleasePhase
{
    /**
     * Tool that gets a configured SCM repository from release configuration.
     *
     * @plexus.requirement
     */
    private ScmRepositoryConfigurator scmRepositoryConfigurator;

    protected ScmRepository getScmRepository( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment )
        throws ReleaseFailureException, ReleaseExecutionException
    {
        try
        {
            return scmRepositoryConfigurator.getConfiguredRepository( releaseDescriptor, releaseEnvironment.getSettings() );
        }
        catch ( ScmRepositoryException exception )
        {
            throw new ReleaseScmRepositoryException( exception.getMessage(), exception.getValidationMessages() );
        }
        catch ( NoSuchScmProviderException exception )
        {
            throw new ReleaseExecutionException( "Unable to configure SCM repository: " + exception.getMessage(),
                                                 exception );
        }
    }

    protected ScmProvider getScmProvider( ScmRepository scmRepository )
        throws ReleaseExecutionException
    {
        try
        {
            return scmRepositoryConfigurator.getRepositoryProvider( scmRepository );
        }
        catch ( NoSuchScmProviderException exception )
        {
            throw new ReleaseExecutionException( "Unable to configure SCM repository: " + exception.getMessage(),
                                                 exception );
        }
    }
}
