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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.shared.release.DefaultReleaseManagerListener;
import org.apache.maven.shared.release.ReleaseCleanRequest;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;

/**
 * Clean up after a release preparation. This is done automatically after a successful <tt>release:perform</tt>,
 * so is best served for cleaning up a failed or abandoned release, or a dry run. Note that only the working copy
 * is cleaned up, no previous steps are rolled back.
 * For more info see <a href="https://maven.apache.org/plugins/maven-release-plugin/examples/clean-release.html"
 * >https://maven.apache.org/plugins/maven-release-plugin/examples/clean-release.html</a>.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
@Mojo( name = "clean", aggregator = true )
public class CleanReleaseMojo
    extends AbstractReleaseMojo
{
    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        ReleaseDescriptorBuilder releaseDescriptor = new ReleaseDescriptorBuilder();
        releaseDescriptor.setWorkingDirectory( getBasedir().getAbsolutePath() );

        ReleaseCleanRequest cleanRequest = new ReleaseCleanRequest();
        cleanRequest.setReleaseDescriptorBuilder( releaseDescriptor );
        cleanRequest.setReactorProjects( getReactorProjects() );
        cleanRequest.setReleaseManagerListener( new DefaultReleaseManagerListener( getLog() ) );
        
        try
        {
            releaseManager.clean( cleanRequest );
        }
        catch ( ReleaseFailureException e )
        {
            throw new MojoFailureException( e.getMessage() );
        }
    }

}
