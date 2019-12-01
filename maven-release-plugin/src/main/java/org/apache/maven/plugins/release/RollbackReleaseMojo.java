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
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.release.DefaultReleaseManagerListener;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.ReleaseRollbackRequest;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;

/**
 * Rollback changes made by a previous release. This requires that the previous release descriptor
 * <tt>release.properties</tt> is still available in the local working copy. For more info see <a
 * href="https://maven.apache.org/plugins/maven-release-plugin/examples/rollback-release.html"
 * >https://maven.apache.org/plugins/maven-release-plugin/examples/rollback-release.html</a>.
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
     * The SCM commit comment when rolling back.
     * Defaults to "@{prefix} rollback the release of @{releaseLabel}".
     * <p>
     * Property interpolation is performed on the value, but in order to ensure that the interpolation occurs
     * during release, you must use <code>@{...}</code> to reference the properties rather than <code>${...}</code>.
     * The following properties are available:
     * <ul>
     *     <li><code>prefix</code> - The comment prefix.
     *     <li><code>groupId</code> - The groupId of the root project.
     *     <li><code>artifactId</code> - The artifactId of the root project.
     *     <li><code>releaseLabel</code> - The release version of the root project.
     * </ul>
     *
     * @since 3.0.0
     */
    @Parameter(
            defaultValue = "@{prefix} rollback the release of @{releaseLabel}", 
            property = "scmRollbackCommitComment" )
    private String scmRollbackCommitComment = "@{prefix} rollback the release of @{releaseLabel}";

    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        super.execute();

        final ReleaseDescriptorBuilder config = createReleaseDescriptor();
        config.setScmRollbackCommitComment( scmRollbackCommitComment );

        try
        {
            ReleaseRollbackRequest rollbackRequest = new ReleaseRollbackRequest();
            rollbackRequest.setReleaseDescriptorBuilder( config );
            rollbackRequest.setReleaseEnvironment( getReleaseEnvironment() );
            rollbackRequest.setReactorProjects( getReactorProjects()  );
            rollbackRequest.setReleaseManagerListener( new DefaultReleaseManagerListener( getLog() ) );
            
            releaseManager.rollback( rollbackRequest );
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
