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

import java.util.Map;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Scm;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;

/**
 * Abstract Mojo containing SCM parameters
 *
 * @author Robert Scholte
 */
// Extra layer since 2.4. Don't use @since doclet, these would be inherited by the subclasses
public abstract class AbstractScmReleaseMojo
    extends AbstractReleaseMojo
{
    /**
     * The SCM username to use.
     */
    @Parameter( property = "username" )
    private String username;

    /**
     * The SCM password to use.
     */
    @Parameter( property = "password" )
    private String password;

    /**
     * The SCM tag to use.
     */
    @Parameter( alias = "releaseLabel", property = "tag" )
    private String tag;

    /**
     * Format to use when generating the tag name if none is specified. Property interpolation is performed on the
     * tag, but in order to ensure that the interpolation occurs during release, you must use <code>@{...}</code>
     * to reference the properties rather than <code>${...}</code>. The following properties are available:
     * <ul>
     *     <li><code>groupId</code> or <code>project.groupId</code> - The groupId of the root project.
     *     <li><code>artifactId</code> or <code>project.artifactId</code> - The artifactId of the root project.
     *     <li><code>version</code> or <code>project.version</code> - The release version of the root project.
     * </ul>
     *
     * @since 2.2.0
     */
    @Parameter( defaultValue = "@{project.artifactId}-@{project.version}", property = "tagNameFormat" )
    private String tagNameFormat;

    /**
     * The tag base directory in SVN, you must define it if you don't use the standard svn layout (trunk/tags/branches).
     * For example, <code>http://svn.apache.org/repos/asf/maven/plugins/tags</code>. The URL is an SVN URL and does not
     * include the SCM provider and protocol.
     */
    @Parameter( property = "tagBase" )
    private String tagBase;

    /**
     * The message prefix to use for all SCM changes.
     *
     * @since 2.0-beta-5
     */
    @Parameter( defaultValue = "[maven-release-plugin] ", property = "scmCommentPrefix" )
    private String scmCommentPrefix;

    /**
     * Implemented with git will or not push changes to the upstream repository.
     * <code>true</code> by default to preserve backward compatibility.
     * @since 2.1
     */
    @Parameter( defaultValue = "true", property = "pushChanges" )
    private boolean pushChanges = true;

    /**
     * A workItem for SCMs like RTC, TFS etc, that may require additional
     * information to perform a pushChange operation.
     *
     * @since 3.0.0
     */
    @Parameter( property = "workItem" )
    private String workItem;

    /**
     * Add a new or overwrite the default implementation per provider. 
     * The key is the scm prefix and the value is the role hint of the
     * {@link org.apache.maven.scm.provider.ScmProvider}.
     *
     * @since 2.0-beta-6
     * @see ScmManager#setScmProviderImplementation(String, String)
     */
    @Parameter
    private Map<String, String> providerImplementations;

    /**
     * The SCM manager.
     */
    @Component
    private ScmManager scmManager;

    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( providerImplementations != null )
        {
            for ( Map.Entry<String, String> providerEntry : providerImplementations.entrySet() )
            {
                getLog().info( "Change the default '" + providerEntry.getKey() + "' provider implementation to '"
                    + providerEntry.getValue() + "'." );
                scmManager.setScmProviderImplementation( providerEntry.getKey(), providerEntry.getValue() );
            }
        }
    }

    @Override
    protected ReleaseDescriptorBuilder createReleaseDescriptor()
    {
        ReleaseDescriptorBuilder descriptor = super.createReleaseDescriptor();

        descriptor.setScmPassword( password );
        descriptor.setScmReleaseLabel( tag );
        descriptor.setScmTagNameFormat( tagNameFormat );
        descriptor.setScmTagBase( tagBase );
        descriptor.setScmUsername( username );
        descriptor.setScmCommentPrefix( scmCommentPrefix );

        descriptor.setPushChanges( pushChanges );
        descriptor.setWorkItem( workItem );
        
        if ( project.getScm() != null )
        {
            if ( project.getScm().getDeveloperConnection() != null )
            {
                descriptor.setScmSourceUrl( project.getScm().getDeveloperConnection() );
            }
            else if ( project.getScm().getConnection() != null )
            {
                descriptor.setScmSourceUrl( project.getScm().getConnection() );
            }
        }
        
        // As long as Scm.getId() does not exist, read it as a property
        descriptor.setScmId( project.getProperties().getProperty( "project.scm.id" ) );
        
        for ( MavenProject reactorProject : session.getProjects() )
        {
            if ( reactorProject.getScm() != null )
            {
                String projectId =
                    ArtifactUtils.versionlessKey( reactorProject.getGroupId(), reactorProject.getArtifactId() );
                
                descriptor.addOriginalScmInfo( projectId, buildScm( project ) );
            }
        }

        return descriptor;
    }
    
    protected Scm buildScm( MavenProject project )
    {
        Scm scm;
        if ( project.getOriginalModel().getScm() == null )
        {
            scm = null;
        }
        else
        {
            scm = new Scm();
            scm.setConnection( project.getOriginalModel().getScm().getConnection() );
            scm.setDeveloperConnection( project.getOriginalModel().getScm().getDeveloperConnection() );
            scm.setTag( project.getOriginalModel().getScm().getTag() );
            scm.setUrl( project.getOriginalModel().getScm().getUrl() );
        }
        return scm;
    }
}
