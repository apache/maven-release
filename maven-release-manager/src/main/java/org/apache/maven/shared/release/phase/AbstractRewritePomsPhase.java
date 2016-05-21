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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Build;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Model;
import org.apache.maven.model.ModelBase;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.edit.EditScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.scm.IdentifiedScm;
import org.apache.maven.shared.release.scm.ReleaseScmCommandException;
import org.apache.maven.shared.release.scm.ReleaseScmRepositoryException;
import org.apache.maven.shared.release.scm.ScmRepositoryConfigurator;
import org.apache.maven.shared.release.scm.ScmTranslator;
import org.apache.maven.shared.release.transform.ModelETLRequest;
import org.apache.maven.shared.release.transform.MavenCoordinate;
import org.apache.maven.shared.release.transform.ModelETL;
import org.apache.maven.shared.release.transform.ModelETLFactory;
import org.apache.maven.shared.release.transform.jdom.JDomModelETLFactory;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.StringUtils;

/**
 * Base class for rewriting phases.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class AbstractRewritePomsPhase
    extends AbstractReleasePhase
{
    /**
     * Tool that gets a configured SCM repository from release configuration.
     */
    @Requirement
    private ScmRepositoryConfigurator scmRepositoryConfigurator;
    
    @Requirement( role = ModelETLFactory.class )
    private Map<String, ModelETLFactory> modelETLFactories;
    
    /**
     * Use jdom-sax as default
     */
    private String modelETL = JDomModelETLFactory.ROLE_HINT;

    /**
     * SCM URL translators mapped by provider name.
     */
    @Requirement( role = ScmTranslator.class )
    private Map<String, ScmTranslator> scmTranslators;
    
    protected final Map<String, ScmTranslator> getScmTranslators()
    {
        return scmTranslators;
    }

    private String ls = ReleaseUtil.LS;

    public void setLs( String ls )
    {
        this.ls = ls;
    }
    
    public void setModelETL( String modelETL )
    {
        this.modelETL = modelETL;
    }
    
    protected abstract String getPomSuffix(); 

    @Override
    public ReleaseResult execute( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                                  List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        ReleaseResult result = new ReleaseResult();

        transform( releaseDescriptor, releaseEnvironment, reactorProjects, false, result );

        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }
    
    @Override
    public ReleaseResult simulate( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                                   List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        ReleaseResult result = new ReleaseResult();

        transform( releaseDescriptor, releaseEnvironment, reactorProjects, true, result );

        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }

    @Override
    public ReleaseResult clean( List<MavenProject> reactorProjects )
    {
        ReleaseResult result = new ReleaseResult();

        super.clean( reactorProjects );

        if ( reactorProjects != null )
        {
            for ( MavenProject project : reactorProjects )
            {
                File pomFile = ReleaseUtil.getStandardPom( project );
                // MRELEASE-273 : if no pom
                if ( pomFile != null )
                {
                    File file = new File( pomFile.getParentFile(), pomFile.getName() + "." + getPomSuffix() );
                    if ( file.exists() )
                    {
                        file.delete();
                    }
                }
            }
        }

        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }

    private void transform( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                            List<MavenProject> reactorProjects, boolean simulate, ReleaseResult result )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        for ( MavenProject project : reactorProjects )
        {
            logInfo( result, "Transforming '" + project.getName() + "'..." );

            transformProject( project, releaseDescriptor, releaseEnvironment, reactorProjects, simulate, result );
        }
    }

    private void transformProject( MavenProject project, ReleaseDescriptor releaseDescriptor,
                                   ReleaseEnvironment releaseEnvironment, List<MavenProject> reactorProjects,
                                   boolean simulate, ReleaseResult result )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        File pomFile = ReleaseUtil.getStandardPom( project );

        ModelETLRequest request = new ModelETLRequest();
        request.setLineSeparator( ls );
        request.setProject( project );
        request.setReleaseDescriptor( releaseDescriptor );
        
        ModelETL etl = modelETLFactories.get( modelETL ).newInstance( request );
        
        etl.extract( pomFile );

        ScmRepository scmRepository = null;
        ScmProvider provider = null;

        if ( isUpdateScm() )
        {
            try
            {
                scmRepository = scmRepositoryConfigurator.getConfiguredRepository( releaseDescriptor,
                                                                                   releaseEnvironment.getSettings() );

                provider = scmRepositoryConfigurator.getRepositoryProvider( scmRepository );
            }
            catch ( ScmRepositoryException e )
            {
                throw new ReleaseScmRepositoryException( e.getMessage(), e.getValidationMessages() );
            }
            catch ( NoSuchScmProviderException e )
            {
                throw new ReleaseExecutionException( "Unable to configure SCM repository: " + e.getMessage(), e );
            }
        }

        transformDocument( project, etl.getModel(), releaseDescriptor, reactorProjects, scmRepository,
                           result, simulate );

        File outputFile;
        if ( simulate )
        {
            outputFile = new File( pomFile.getParentFile(), pomFile.getName() + "." + getPomSuffix() );
        }
        else
        {
            outputFile = pomFile;
            prepareScm( pomFile, releaseDescriptor, scmRepository, provider );
        }
        etl.load( outputFile );
    
    }

    private void transformDocument( MavenProject project, Model modelTarget, ReleaseDescriptor releaseDescriptor,
                                    List<MavenProject> reactorProjects, ScmRepository scmRepository,
                                    ReleaseResult result, boolean simulate )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        Map<String, String> mappedVersions = getNextVersionMap( releaseDescriptor );
        Map<String, String> originalVersions = getOriginalVersionMap( releaseDescriptor, reactorProjects, simulate );
        @SuppressWarnings( "unchecked" )
        Map<String, Map<String, String>> resolvedSnapshotDependencies =
            releaseDescriptor.getResolvedSnapshotDependencies();
        Model model = project.getModel();
        
        Properties properties = modelTarget.getProperties();

        String parentVersion = rewriteParent( project, modelTarget, mappedVersions,
                                              resolvedSnapshotDependencies, originalVersions );

        String projectId = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

        rewriteVersion( modelTarget, mappedVersions, projectId, project, parentVersion );

        Build buildTarget = modelTarget.getBuild();
        if ( buildTarget != null )
        {
            // profile.build.extensions doesn't exist, so only rewrite project.build.extensions  
            rewriteArtifactVersions( toMavenCoordinates( buildTarget.getExtensions() ), mappedVersions,
                                     resolvedSnapshotDependencies, originalVersions, model, properties, result,
                                     releaseDescriptor );
            
            rewriteArtifactVersions( toMavenCoordinates( buildTarget.getPlugins() ), mappedVersions,
                                     resolvedSnapshotDependencies, originalVersions, model, properties, result,
                                     releaseDescriptor );
            
            for ( Plugin plugin : buildTarget.getPlugins() )
            {
                rewriteArtifactVersions( toMavenCoordinates( plugin.getDependencies() ),
                                         mappedVersions, resolvedSnapshotDependencies, originalVersions, model,
                                         properties, result, releaseDescriptor );
            }
            
            if ( buildTarget.getPluginManagement() != null )
            {
                rewriteArtifactVersions( toMavenCoordinates( buildTarget.getPluginManagement().getPlugins() ),
                                         mappedVersions, resolvedSnapshotDependencies, originalVersions, model,
                                         properties, result, releaseDescriptor );
                
                for ( Plugin plugin : buildTarget.getPluginManagement().getPlugins() )
                {
                    rewriteArtifactVersions( toMavenCoordinates( plugin.getDependencies() ), mappedVersions,
                                             resolvedSnapshotDependencies, originalVersions, model, properties,
                                             result, releaseDescriptor );
                }
            }
        }
        
        for ( Profile profile : modelTarget.getProfiles() )
        {
            BuildBase profileBuild = profile.getBuild();
            if ( profileBuild != null )
            {
                rewriteArtifactVersions( toMavenCoordinates( profileBuild.getPlugins() ), mappedVersions,
                                         resolvedSnapshotDependencies, originalVersions, model, properties, result,
                                         releaseDescriptor );
                
                for ( Plugin plugin : profileBuild.getPlugins() )
                {
                    rewriteArtifactVersions( toMavenCoordinates( plugin.getDependencies() ),
                                             mappedVersions, resolvedSnapshotDependencies, originalVersions, model,
                                             properties, result, releaseDescriptor );
                }
                
                if ( profileBuild.getPluginManagement() != null )
                {
                    rewriteArtifactVersions( toMavenCoordinates( profileBuild.getPluginManagement().getPlugins() ),
                                             mappedVersions, resolvedSnapshotDependencies, originalVersions, model,
                                             properties, result, releaseDescriptor );
                    
                    for ( Plugin plugin : profileBuild.getPluginManagement().getPlugins() )
                    {
                        rewriteArtifactVersions( toMavenCoordinates( plugin.getDependencies() ), mappedVersions,
                                                 resolvedSnapshotDependencies, originalVersions, model, properties,
                                                 result, releaseDescriptor );
                    }
                }
            }
        }
        
        List<ModelBase> modelBases = new ArrayList<ModelBase>();
        modelBases.add( modelTarget );
        modelBases.addAll( modelTarget.getProfiles() );
        
        for ( ModelBase modelBase : modelBases )
        {
            rewriteArtifactVersions( toMavenCoordinates( modelBase.getDependencies() ), mappedVersions,
                                     resolvedSnapshotDependencies, originalVersions, model, properties, result,
                                     releaseDescriptor );
            
            if ( modelBase.getDependencyManagement() != null )
            {
                rewriteArtifactVersions( toMavenCoordinates( modelBase.getDependencyManagement().getDependencies() ),
                                         mappedVersions, resolvedSnapshotDependencies, originalVersions, model,
                                         properties, result, releaseDescriptor );
            }
            
            if ( modelBase.getReporting() != null )
            {
                rewriteArtifactVersions( toMavenCoordinates( modelBase.getReporting().getPlugins() ), mappedVersions,
                                         resolvedSnapshotDependencies, originalVersions, model, properties, result,
                                         releaseDescriptor );
            }
        }
        
        String commonBasedir;
        try
        {
            commonBasedir = ReleaseUtil.getCommonBasedir( reactorProjects );
        }
        catch ( IOException e )
        {
            throw new ReleaseExecutionException( "Exception occurred while calculating common basedir: "
                + e.getMessage(), e );
        }
        
        transformScm( project, modelTarget, releaseDescriptor, projectId, scmRepository, result,
                      commonBasedir );
    }

    private void rewriteVersion( Model modelTarget, Map<String, String> mappedVersions, String projectId,
                                 MavenProject project, String parentVersion )
        throws ReleaseFailureException
    {
        String version = mappedVersions.get( projectId );
        if ( version == null )
        {
            throw new ReleaseFailureException( "Version for '" + project.getName() + "' was not mapped" );
        }

        modelTarget.setVersion( version );
    }

    private String rewriteParent( MavenProject project, Model targetModel, Map<String, String> mappedVersions,
                                  Map<String, Map<String, String>> resolvedSnapshotDependencies,
                                  Map<String, String> originalVersions )
        throws ReleaseFailureException
    {
        String parentVersion = null;
        if ( project.hasParent() )
        {
            MavenProject parent = project.getParent();
            String key = ArtifactUtils.versionlessKey( parent.getGroupId(), parent.getArtifactId() );
            parentVersion = mappedVersions.get( key );
            if ( parentVersion == null )
            {
                //MRELEASE-317
                parentVersion = getResolvedSnapshotVersion( key, resolvedSnapshotDependencies );
            }
            if ( parentVersion == null )
            {
                if ( parent.getVersion().equals( originalVersions.get( key ) ) )
                {
                    throw new ReleaseFailureException( "Version for parent '" + parent.getName() + "' was not mapped" );
                }
            }
            else
            {
                targetModel.getParent().setVersion( parentVersion );
            }
        }
        return parentVersion;
    }

    private void rewriteArtifactVersions( Collection<MavenCoordinate> elements, Map<String, String> mappedVersions,
                                          Map<String, Map<String, String>> resolvedSnapshotDependencies,
                                          Map<String, String> originalVersions, Model projectModel,
                                          Properties properties, ReleaseResult result,
                                          ReleaseDescriptor releaseDescriptor )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        if ( elements == null )
        {
            return;
        }
        String projectId = ArtifactUtils.versionlessKey( projectModel.getGroupId(), projectModel.getArtifactId() );
        for ( MavenCoordinate coordinate : elements )
        {
            String rawVersion = coordinate.getVersion();
            if ( rawVersion == null )
            {
                // managed dependency or unversioned plugin
                continue;
            }

            String rawGroupId = coordinate.getGroupId();
            if ( rawGroupId == null )
            {
                if ( "plugin".equals( coordinate.getName() ) )
                {
                    rawGroupId = "org.apache.maven.plugins";
                }
                else
                {
                    // incomplete dependency
                    continue;
                }
            }
            String groupId = ReleaseUtil.interpolate( rawGroupId, projectModel );

            String rawArtifactId = coordinate.getArtifactId();
            if ( rawArtifactId == null )
            {
                // incomplete element
                continue;
            }
            String artifactId = ReleaseUtil.interpolate( rawArtifactId, projectModel );

            String key = ArtifactUtils.versionlessKey( groupId, artifactId );
            String resolvedSnapshotVersion = getResolvedSnapshotVersion( key, resolvedSnapshotDependencies );
            String mappedVersion = mappedVersions.get( key );
            String originalVersion = originalVersions.get( key );
            if ( originalVersion == null )
            {
                originalVersion = getOriginalResolvedSnapshotVersion( key, resolvedSnapshotDependencies );
            }

            // MRELEASE-220
            if ( mappedVersion != null && mappedVersion.endsWith( Artifact.SNAPSHOT_VERSION )
                && !rawVersion.endsWith( Artifact.SNAPSHOT_VERSION ) && !releaseDescriptor.isUpdateDependencies() )
            {
                continue;
            }

            if ( mappedVersion != null )
            {
                if ( rawVersion.equals( originalVersion ) )
                {
                    logInfo( result, "  Updating " + artifactId + " to " + mappedVersion );
                    coordinate.setVersion( mappedVersion );
                }
                else if ( rawVersion.matches( "\\$\\{.+\\}" ) )
                {
                    String expression = rawVersion.substring( 2, rawVersion.length() - 1 );

                    if ( expression.startsWith( "project." ) || expression.startsWith( "pom." )
                        || "version".equals( expression ) )
                    {
                        if ( !mappedVersion.equals( mappedVersions.get( projectId ) ) )
                        {
                            logInfo( result, "  Updating " + artifactId + " to " + mappedVersion );
                            coordinate.setVersion( mappedVersion );
                        }
                        else
                        {
                            logInfo( result, "  Ignoring artifact version update for expression " + rawVersion );
                        }
                    }
                    else if ( properties != null )
                    {
                        // version is an expression, check for properties to update instead
                        
                        String propertyValue = properties.getProperty( expression );
                        
                        if ( propertyValue != null )
                        {
                            if ( propertyValue.equals( originalVersion ) )
                            {
                                logInfo( result, "  Updating " + rawVersion + " to " + mappedVersion );
                                // change the property only if the property is the same as what's in the reactor
                                properties.setProperty( expression, mappedVersion );
                            }
                            else if ( mappedVersion.equals( propertyValue ) )
                            {
                                // this property may have been updated during processing a sibling.
                                logInfo( result, "  Ignoring artifact version update for expression " + rawVersion
                                    + " because it is already updated" );
                            }
                            else if ( !mappedVersion.equals( rawVersion ) )
                            {
                                if ( mappedVersion.matches( "\\$\\{project.+\\}" )
                                    || mappedVersion.matches( "\\$\\{pom.+\\}" )
                                    || "${version}".equals( mappedVersion ) )
                                {
                                    logInfo( result, "  Ignoring artifact version update for expression "
                                        + mappedVersion );
                                    // ignore... we cannot update this expression
                                }
                                else
                                {
                                    // the value of the expression conflicts with what the user wanted to release
                                    throw new ReleaseFailureException( "The artifact (" + key + ") requires a "
                                        + "different version (" + mappedVersion + ") than what is found ("
                                        + propertyValue + ") for the expression (" + expression + ") in the "
                                        + "project (" + projectId + ")." );
                                }
                            }
                        }
                        else
                        {
                            // the expression used to define the version of this artifact may be inherited
                            // TODO needs a better error message, what pom? what dependency?
                            throw new ReleaseFailureException( "The version could not be updated: " + rawVersion );
                        }
                    }
                }
                else
                {
                    // different/previous version not related to current release
                }
            }
            else if ( resolvedSnapshotVersion != null )
            {
                logInfo( result, "  Updating " + artifactId + " to " + resolvedSnapshotVersion );

                coordinate.setVersion( resolvedSnapshotVersion );
            }
            else
            {
                // artifact not related to current release
            }
        }
    }

    private void prepareScm( File pomFile, ReleaseDescriptor releaseDescriptor, ScmRepository repository,
                           ScmProvider provider )
        throws ReleaseExecutionException, ReleaseScmCommandException
    {
        try
        {
            if ( isUpdateScm() && ( releaseDescriptor.isScmUseEditMode() || provider.requiresEditMode() ) )
            {
                EditScmResult result = provider.edit( repository, new ScmFileSet(
                    new File( releaseDescriptor.getWorkingDirectory() ), pomFile ) );

                if ( !result.isSuccess() )
                {
                    throw new ReleaseScmCommandException( "Unable to enable editing on the POM", result );
                }
            }
        }
        catch ( ScmException e )
        {
            throw new ReleaseExecutionException( "An error occurred enabling edit mode: " + e.getMessage(), e );
        }
    }

    
    protected abstract String getResolvedSnapshotVersion( String artifactVersionlessKey,
                                                          Map<String, Map<String, String>> resolvedSnapshots );

    protected abstract Map<String, String> getOriginalVersionMap( ReleaseDescriptor releaseDescriptor,
                                                                  List<MavenProject> reactorProjects,
                                                                  boolean simulate );

    protected abstract Map<String, String> getNextVersionMap( ReleaseDescriptor releaseDescriptor );

    protected abstract void transformScm( MavenProject project, Model modelTarget, ReleaseDescriptor releaseDescriptor,
                                          String projectId, ScmRepository scmRepository,
                                          ReleaseResult result, String commonBasedir )
        throws ReleaseExecutionException;

    /**
     * 
     * @return {@code true} if the SCM-section should be updated, otherwise {@code false}
     * @since 2.4
     */
    protected boolean isUpdateScm()
    {
        return true;
    }

    protected String getOriginalResolvedSnapshotVersion( String artifactVersionlessKey,
                                                         Map<String, Map<String, String>> resolvedSnapshots )
    {
        Map<String, String> versionsMap = resolvedSnapshots.get( artifactVersionlessKey );

        if ( versionsMap != null )
        {
            return versionsMap.get( ReleaseDescriptor.ORIGINAL_VERSION );
        }
        else
        {
            return null;
        }
    }

    protected Scm buildScm( MavenProject project )
    {
        IdentifiedScm scm;
        if ( project.getOriginalModel().getScm() == null )
        {
            scm = null;
        }
        else
        {
            scm = new IdentifiedScm();
            scm.setConnection( project.getOriginalModel().getScm().getConnection() );
            scm.setDeveloperConnection( project.getOriginalModel().getScm().getDeveloperConnection() );
            scm.setTag( project.getOriginalModel().getScm().getTag() );
            scm.setUrl( project.getOriginalModel().getScm().getUrl() );
            scm.setId( project.getProperties().getProperty( "project.scm.id" ) );
        }
        return scm;
    }
    
    /**
     * Determines the relative path from trunk to tag, and adds this relative path
     * to the url.
     *
     * @param trunkPath - The trunk url
     * @param tagPath   - The tag base
     * @param urlPath   - scm.url or scm.connection
     * @return The url path for the tag.
     */
    protected static String translateUrlPath( String trunkPath, String tagPath, String urlPath )
    {
        trunkPath = trunkPath.trim();
        tagPath = tagPath.trim();
        //Strip the slash at the end if one is present
        if ( trunkPath.endsWith( "/" ) )
        {
            trunkPath = trunkPath.substring( 0, trunkPath.length() - 1 );
        }
        if ( tagPath.endsWith( "/" ) )
        {
            tagPath = tagPath.substring( 0, tagPath.length() - 1 );
        }
        char[] tagPathChars = trunkPath.toCharArray();
        char[] trunkPathChars = tagPath.toCharArray();
        // Find the common path between trunk and tags
        int i = 0;
        while ( ( i < tagPathChars.length ) && ( i < trunkPathChars.length ) && tagPathChars[i] == trunkPathChars[i] )
        {
            ++i;
        }
        // If there is nothing common between trunk and tags, or the relative
        // path does not exist in the url, then just return the tag.
        if ( i == 0 || urlPath.indexOf( trunkPath.substring( i ) ) < 0 )
        {
            return tagPath;
        }
        else
        {
            return StringUtils.replace( urlPath, trunkPath.substring( i ), tagPath.substring( i ) );
        }
    }
    
    private Collection<MavenCoordinate> toMavenCoordinates( List<?> objects )
    {
        Collection<MavenCoordinate> coordinates = new ArrayList<MavenCoordinate>( objects.size() );
        for ( Object object : objects )
        {
            if ( object instanceof MavenCoordinate )
            {
                coordinates.add( (MavenCoordinate) object );
            }
            else
            {
                throw new UnsupportedOperationException();
            }
        }
        return coordinates;
    }


}
