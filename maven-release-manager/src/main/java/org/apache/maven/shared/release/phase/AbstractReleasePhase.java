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

import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Base class for all phases.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class AbstractReleasePhase
    extends AbstractLogEnabled
    implements ReleasePhase
{
    protected ResourceBundle getResourceBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "release-messages", locale, AbstractReleasePhase.class.getClassLoader() );
    }
    
    public ReleaseResult clean( List<MavenProject> reactorProjects )
    {
        // nothing to do by default

        return getReleaseResultSuccess();
    }

    public ReleaseResult execute( ReleaseDescriptor releaseDescriptor,
                                  Settings settings,
                                  List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        return execute( releaseDescriptor, new DefaultReleaseEnvironment().setSettings( settings ), reactorProjects );
    }

    public ReleaseResult simulate( ReleaseDescriptor releaseDescriptor,
                                   Settings settings,
                                   List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        return simulate( releaseDescriptor, new DefaultReleaseEnvironment().setSettings( settings ), reactorProjects );
    }

    protected void logInfo( ReleaseResult result, String message )
    {
        result.appendInfo( message );
        getLogger().info( message );
    }

    protected void logWarn( ReleaseResult result, String message )
    {
        result.appendWarn( message );
        getLogger().warn( message );
    }

    protected void logError( ReleaseResult result, String message )
    {
        result.appendWarn( message );
        getLogger().error( message );
    }

    protected void logDebug( ReleaseResult result, String message )
    {
        result.appendDebug( message );
        getLogger().debug( message );
    }

    protected void logDebug( ReleaseResult result, String message, Exception e )
    {
        result.appendDebug( message, e );
        getLogger().debug( message, e );
    }
    
    protected ReleaseResult getReleaseResultSuccess()
    {
        ReleaseResult result = new ReleaseResult();

        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }
}
