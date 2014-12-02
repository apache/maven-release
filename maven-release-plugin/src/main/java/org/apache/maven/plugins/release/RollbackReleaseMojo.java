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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.config.ReleaseDescriptor;

/**
 * Rollback changes made by a previous release. This requires that the previous release descriptor
 * <tt>release.properties</tt> is still available in the local working copy. For more info see <a
 * href="http://maven.apache.org/plugins/maven-release-plugin/examples/rollback-release.html"
 * >http://maven.apache.org/plugins/maven-release-plugin/examples/rollback-release.html</a>.
 *
 * @since 2.0-beta-5
 * @author Edwin Punzalan
 * @version $Id$
 */
@Mojo( name = "rollback", aggregator = true )
public class RollbackReleaseMojo
    extends AbstractScmReleaseMojo
{

    /**
     * {@inheritDoc}
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        super.execute();

        ReleaseDescriptor config = createReleaseDescriptor();

        try
        {
            releaseManager.rollback( config, getReleaseEnvironment(), getReactorProjects() );
        }
        catch ( ReleaseExecutionException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        catch ( ReleaseFailureException e )
        {
            throw new MojoFailureException( e.getMessage(), e );
        }
    }

}
