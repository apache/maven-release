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
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.Resource;
import org.apache.maven.model.Scm;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.interpolation.ModelInterpolator;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.model.superpom.SuperPomProvider;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.add.AddScmResult;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.scm.ReleaseScmCommandException;
import org.apache.maven.shared.release.scm.ScmTranslator;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.WriterFactory;

/**
 * Generate release POMs.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 */
@Component( role = ReleasePhase.class, hint = "generate-release-poms" )
public class GenerateReleasePomsPhase
    extends AbstractReleasePomsPhase implements ResourceGenerator
{
    private static final String FINALNAME_EXPRESSION = "${project.artifactId}-${project.version}";

    @Requirement
    private SuperPomProvider superPomProvider;

    @Requirement
    private ModelInterpolator modelInterpolator;

    /**
     * SCM URL translators mapped by provider name.
     */
    @Requirement( role = ScmTranslator.class )
    private Map<String, ScmTranslator> scmTranslators;

    /*
     * @see org.apache.maven.shared.release.phase.ReleasePhase#execute(org.apache.maven.shared.release.config.ReleaseDescriptor,
     *      org.apache.maven.settings.Settings, java.util.List)
     */
    @Override
    public ReleaseResult execute( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                                  List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        return execute( releaseDescriptor, releaseEnvironment, reactorProjects, false );
    }

    private ReleaseResult execute( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                                   List<MavenProject> reactorProjects, boolean simulate )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        ReleaseResult result = new ReleaseResult();

        if ( releaseDescriptor.isGenerateReleasePoms() )
        {
            logInfo( result, "Generating release POMs..." );

            generateReleasePoms( releaseDescriptor, releaseEnvironment, reactorProjects, simulate, result );
        }
        else
        {
            logInfo( result, "Not generating release POMs" );
        }

        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }

    private void generateReleasePoms( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                                      List<MavenProject> reactorProjects, boolean simulate, ReleaseResult result )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        List<File> releasePoms = new ArrayList<>();

        for ( MavenProject project : reactorProjects )
        {
            logInfo( result, "Generating release POM for '" + project.getName() + "'..." );

            releasePoms.add( generateReleasePom( project, releaseDescriptor, releaseEnvironment, reactorProjects,
                                                 simulate, result ) );
        }

        addReleasePomsToScm( releaseDescriptor, releaseEnvironment, reactorProjects, simulate, result, releasePoms );
    }

    private File generateReleasePom( MavenProject project, ReleaseDescriptor releaseDescriptor,
                                     ReleaseEnvironment releaseEnvironment, List<MavenProject> reactorProjects,
                                     boolean simulate, ReleaseResult result )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        // create release pom

        Model releasePom = createReleaseModel( project, releaseDescriptor, releaseEnvironment, reactorProjects,
                                               result );

        // write release pom to file

        MavenXpp3Writer pomWriter = new MavenXpp3Writer();

        File releasePomFile = ReleaseUtil.getReleasePom( project );

        // MRELEASE-273 : A release pom can be null
        if ( releasePomFile == null )
        {
            throw new ReleaseExecutionException( "Cannot generate release POM : pom file is null" );
        }

        

        try ( Writer fileWriter = WriterFactory.newXmlWriter( releasePomFile ) ) 
        {
            pomWriter.write( fileWriter, releasePom );
        }
        catch ( IOException exception )
        {
            throw new ReleaseExecutionException( "Cannot generate release POM", exception );
        }

        return releasePomFile;
    }

    private void addReleasePomsToScm( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                                      List<MavenProject> reactorProjects, boolean simulate, ReleaseResult result,
                                      List<File> releasePoms )
        throws ReleaseFailureException, ReleaseExecutionException
    {
        if ( simulate )
        {
            logInfo( result, "Full run would be adding " + releasePoms );
        }
        else
        {
            ScmRepository scmRepository = getScmRepository( releaseDescriptor, releaseEnvironment );
            ScmProvider scmProvider = getScmProvider( scmRepository );

            MavenProject rootProject = ReleaseUtil.getRootProject( reactorProjects );
            ScmFileSet scmFileSet = new ScmFileSet( rootProject.getFile().getParentFile(), releasePoms );

            try
            {
                AddScmResult scmResult = scmProvider.add( scmRepository, scmFileSet );

                if ( !scmResult.isSuccess() )
                {
                    throw new ReleaseScmCommandException( "Cannot add release POM to SCM", scmResult );
                }
            }
            catch ( ScmException exception )
            {
                throw new ReleaseExecutionException( "Cannot add release POM to SCM: " + exception.getMessage(),
                                                     exception );
            }
        }
    }

    private Model createReleaseModel( MavenProject project, ReleaseDescriptor releaseDescriptor,
                                      ReleaseEnvironment releaseEnvironment, List<MavenProject> reactorProjects,
                                      ReleaseResult result )
        throws ReleaseFailureException, ReleaseExecutionException
    {
        MavenProject releaseProject = project.clone();
        Model releaseModel = releaseProject.getModel();

        // the release POM should reflect bits of these which were injected at build time...
        // we don't need these polluting the POM.
        releaseModel.setParent( null );
        releaseModel.setProfiles( Collections.<Profile>emptyList() );
        releaseModel.setDependencyManagement( null );
        releaseProject.getBuild().setPluginManagement( null );

        // update project version
        String projectVersion = releaseModel.getVersion();
        String releaseVersion =
            getNextVersion( releaseDescriptor, project.getGroupId(), project.getArtifactId(), projectVersion );
        releaseModel.setVersion( releaseVersion );

        String originalFinalName = releaseModel.getBuild().getFinalName();
        // update final name if implicit
        if ( !FINALNAME_EXPRESSION.equals( originalFinalName ) )
        {
            originalFinalName = findOriginalFinalName( project );

            if ( originalFinalName == null )
            {
                // as defined in super-pom
                originalFinalName = FINALNAME_EXPRESSION;
            }
        }

        // make finalName always explicit
        String finalName = ReleaseUtil.interpolate( originalFinalName, releaseModel );

        // still required?
        if ( finalName.indexOf( Artifact.SNAPSHOT_VERSION ) != -1 )
        {
            throw new ReleaseFailureException( "Cannot reliably adjust the finalName of project: "
                            + releaseProject.getId() );
        }
        releaseModel.getBuild().setFinalName( finalName );


        // update scm
        Scm scm = releaseModel.getScm();

        if ( scm != null )
        {
            ScmRepository scmRepository = getScmRepository( releaseDescriptor, releaseEnvironment );
            ScmTranslator scmTranslator = getScmTranslator( scmRepository );

            if ( scmTranslator != null )
            {
                releaseModel.setScm( createReleaseScm( releaseModel.getScm(), scmTranslator, releaseDescriptor ) );
            }
            else
            {
                String message = "No SCM translator found - skipping rewrite";

                result.appendDebug( message );

                getLogger().debug( message );
            }
        }

        // rewrite dependencies
        releaseModel.setDependencies( createReleaseDependencies( releaseDescriptor, releaseProject ) );

        // rewrite plugins
        releaseModel.getBuild().setPlugins( createReleasePlugins( releaseDescriptor, releaseProject ) );

        // rewrite reports
        releaseModel.getReporting().setPlugins( createReleaseReportPlugins( releaseDescriptor, 
                                                                            releaseProject ) );

        // rewrite extensions
        releaseModel.getBuild().setExtensions( createReleaseExtensions( releaseDescriptor, 
                                                                        releaseProject ) );

        unalignFromBaseDirectory( releaseModel, project.getBasedir() );

        return releaseModel;
    }


    private void unalignFromBaseDirectory( Model releaseModel, File basedir )
    {
        Model rawSuperModel = superPomProvider.getSuperModel( releaseModel.getModelVersion() );

        ModelBuildingRequest buildingRequest = new DefaultModelBuildingRequest();
        buildingRequest.setValidationLevel( ModelBuildingRequest.VALIDATION_LEVEL_STRICT );

        // inject proper values used by project.build.finalName
        Properties properties = new Properties();
        properties.put( "project.version", releaseModel.getVersion() );
        properties.put( "project.artifactId", releaseModel.getArtifactId() );
        buildingRequest.setUserProperties( properties );

        Model interpolatedSuperModel =
            modelInterpolator.interpolateModel( rawSuperModel.clone(), basedir, buildingRequest, null );

        Build currentBuild = releaseModel.getBuild();
        Build interpolatedSuperBuild = interpolatedSuperModel.getBuild();
        Build rawSuperBuild = rawSuperModel.getBuild();

        currentBuild.setSourceDirectory( resolvePath( basedir.toPath(), currentBuild.getSourceDirectory(),
                                                  interpolatedSuperBuild.getSourceDirectory(),
                                                  rawSuperBuild.getSourceDirectory() ) );
        currentBuild.setScriptSourceDirectory( resolvePath( basedir.toPath(), currentBuild.getScriptSourceDirectory(),
                                                  interpolatedSuperBuild.getScriptSourceDirectory(),
                                                  rawSuperBuild.getScriptSourceDirectory() ) );
        currentBuild.setTestSourceDirectory( resolvePath( basedir.toPath(), currentBuild.getTestSourceDirectory(),
                                                  interpolatedSuperBuild.getTestSourceDirectory(),
                                                  rawSuperBuild.getTestSourceDirectory() ) );
        currentBuild.setOutputDirectory( resolvePath( basedir.toPath(), currentBuild.getOutputDirectory(),
                                                        interpolatedSuperBuild.getOutputDirectory(),
                                                        rawSuperBuild.getOutputDirectory() ) );
        currentBuild.setTestOutputDirectory( resolvePath( basedir.toPath(), currentBuild.getTestOutputDirectory(),
                                                      interpolatedSuperBuild.getTestOutputDirectory(),
                                                      rawSuperBuild.getTestOutputDirectory() ) );
        currentBuild.setDirectory( resolvePath( basedir.toPath(), currentBuild.getDirectory(),
                                            interpolatedSuperBuild.getDirectory(),
                                            rawSuperBuild.getDirectory() ) );

        for ( Resource currentResource : currentBuild.getResources() )
        {
            Map<String, String> superResourceDirectories =
                new LinkedHashMap<>( interpolatedSuperBuild.getResources().size() );
            for ( int i = 0; i < interpolatedSuperBuild.getResources().size(); i++ )
            {
                superResourceDirectories.put( interpolatedSuperBuild.getResources().get( i ).getDirectory(),
                                              rawSuperBuild.getResources().get( i ).getDirectory() );
            }
            currentResource.setDirectory( resolvePath( basedir.toPath(), currentResource.getDirectory(),
                                                       superResourceDirectories ) );
        }

        for ( Resource currentResource : currentBuild.getTestResources() )
        {
            Map<String, String> superResourceDirectories =
                new LinkedHashMap<>( interpolatedSuperBuild.getTestResources().size() );
            for ( int i = 0; i < interpolatedSuperBuild.getTestResources().size(); i++ )
            {
                superResourceDirectories.put( interpolatedSuperBuild.getTestResources().get( i ).getDirectory(),
                                              rawSuperBuild.getTestResources().get( i ).getDirectory() );
            }
            currentResource.setDirectory( resolvePath( basedir.toPath(), currentResource.getDirectory(),
                                                       superResourceDirectories ) );
        }



        releaseModel.getReporting().setOutputDirectory( resolvePath( basedir.toPath(),
                                                         releaseModel.getReporting().getOutputDirectory(),
                                                         interpolatedSuperModel.getReporting().getOutputDirectory(),
                                                         rawSuperModel.getReporting().getOutputDirectory() ) );
    }

    private String resolvePath( Path basedir, String current, String superInterpolated, String superRaw )
    {
        return basedir.resolve( current ).equals( basedir.resolve( superInterpolated ) ) ? superRaw : current;
    }

    private String resolvePath( Path basedir,
                                String current,
                                Map<String /* interpolated */, String /* raw */> superValues )
    {
        for ( Map.Entry<String, String> superValue : superValues.entrySet() )
        {
            if ( basedir.resolve( current ).equals( basedir.resolve( superValue.getKey() ) ) )
            {
                return superValue.getValue();
            }
        }
        return current;
    }

    private String findOriginalFinalName( MavenProject project )
    {
        if ( project.getOriginalModel().getBuild() != null
            && project.getOriginalModel().getBuild().getFinalName() != null )
        {
            return project.getOriginalModel().getBuild().getFinalName();
        }
        else if ( project.hasParent() )
        {
            return findOriginalFinalName( project.getParent() );
        }
        else
        {
            return null;
        }
    }

    @Override
    public ReleaseResult simulate( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                                   List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        return execute( releaseDescriptor, releaseEnvironment, reactorProjects, true );
    }

    private String getNextVersion( ReleaseDescriptor releaseDescriptor, String groupId, String artifactId,
                                   String version )
        throws ReleaseFailureException
    {
        // TODO: share with RewritePomsForReleasePhase.rewriteVersion

        String id = ArtifactUtils.versionlessKey( groupId, artifactId );

        String nextVersion = releaseDescriptor.getProjectReleaseVersion( id );

        if ( nextVersion == null )
        {
            throw new ReleaseFailureException( "Version for '" + id + "' was not mapped" );
        }

        return nextVersion;
    }

    private ScmTranslator getScmTranslator( ScmRepository scmRepository )
    {
        return scmTranslators.get( scmRepository.getProvider() );
    }

    private Scm createReleaseScm( Scm scm, ScmTranslator scmTranslator, ReleaseDescriptor releaseDescriptor )
    {
        // TODO: share with RewritePomsForReleasePhase.translateScm

        String tag = releaseDescriptor.getScmReleaseLabel();
        String tagBase = releaseDescriptor.getScmTagBase();

        Scm releaseScm = new Scm();

        if ( scm.getConnection() != null )
        {
            String value = scmTranslator.translateTagUrl( scm.getConnection(), tag, tagBase );
            releaseScm.setConnection( value );
        }

        if ( scm.getDeveloperConnection() != null )
        {
            String value = scmTranslator.translateTagUrl( scm.getDeveloperConnection(), tag, tagBase );
            releaseScm.setDeveloperConnection( value );
        }

        if ( scm.getUrl() != null )
        {
            String value = scmTranslator.translateTagUrl( scm.getUrl(), tag, tagBase );
            releaseScm.setUrl( value );
        }

        if ( scm.getTag() != null )
        {
            String value = scmTranslator.resolveTag( scm.getTag() );
            releaseScm.setTag( value );
        }

        return releaseScm;
    }

    private List<Dependency> createReleaseDependencies( ReleaseDescriptor releaseDescriptor,
                                                        MavenProject project )
        throws ReleaseFailureException
    {
        Set<Artifact> artifacts = project.getArtifacts();

        List<Dependency> releaseDependencies = null;

        if ( artifacts != null )
        {
            // make dependency order deterministic for tests (related to MNG-1412)
            List<Artifact> orderedArtifacts = new ArrayList<>();
            orderedArtifacts.addAll( artifacts );
            Collections.sort( orderedArtifacts );

            releaseDependencies = new ArrayList<>();

            for ( Artifact artifact : orderedArtifacts )
            {
                Dependency releaseDependency = new Dependency();

                releaseDependency.setGroupId( artifact.getGroupId() );
                releaseDependency.setArtifactId( artifact.getArtifactId() );

                String version = getReleaseVersion( releaseDescriptor, artifact );

                releaseDependency.setVersion( version );
                releaseDependency.setType( artifact.getType() );
                releaseDependency.setScope( artifact.getScope() );
                releaseDependency.setClassifier( artifact.getClassifier() );

                releaseDependencies.add( releaseDependency );
            }
        }

        return releaseDependencies;
    }

    private String getReleaseVersion( ReleaseDescriptor releaseDescriptor,
                                      Artifact artifact )
        throws ReleaseFailureException
    {
        String key = ArtifactUtils.versionlessKey( artifact );

        String originalVersion = releaseDescriptor.getProjectOriginalVersion( key );
        String mappedVersion = releaseDescriptor.getProjectReleaseVersion( key );

        String version = artifact.getVersion();

        if ( version.equals( originalVersion ) )
        {
            if ( mappedVersion != null )
            {
                version = mappedVersion;
            }
            else
            {
                throw new ReleaseFailureException( "Version '" + version + "' for '" + key + "' was not mapped" );
            }
        }
        else
        {
            if ( !ArtifactUtils.isSnapshot( version ) )
            {
                version = artifact.getBaseVersion();
            }
        }

        return version;
    }

    private List<Plugin> createReleasePlugins( ReleaseDescriptor releaseDescriptor,
                                               MavenProject project )
        throws ReleaseFailureException
    {
        List<Plugin> releasePlugins = null;

        // Use original - don't want the lifecycle introduced ones
        Build build = project.getOriginalModel().getBuild();

        if ( build != null )
        {
            List<Plugin> plugins = build.getPlugins();

            if ( plugins != null )
            {
                Map<String, Artifact> artifactsById = project.getPluginArtifactMap();

                releasePlugins = new ArrayList<>();

                for ( Plugin plugin : plugins )
                {
                    String id = ArtifactUtils.versionlessKey( plugin.getGroupId(), plugin.getArtifactId() );
                    Artifact artifact = artifactsById.get( id );
                    String version = getReleaseVersion( releaseDescriptor, artifact );

                    Plugin releasePlugin = new Plugin();
                    releasePlugin.setGroupId( plugin.getGroupId() );
                    releasePlugin.setArtifactId( plugin.getArtifactId() );
                    releasePlugin.setVersion( version );
                    if ( plugin.getExtensions() != null )
                    {
                        releasePlugin.setExtensions( plugin.isExtensions() );
                    }
                    releasePlugin.setExecutions( plugin.getExecutions() );
                    releasePlugin.setDependencies( plugin.getDependencies() );
                    releasePlugin.setGoals( plugin.getGoals() );
                    releasePlugin.setInherited( plugin.getInherited() );
                    releasePlugin.setConfiguration( plugin.getConfiguration() );

                    releasePlugins.add( releasePlugin );
                }
            }
        }

        return releasePlugins;
    }

    private List<ReportPlugin> createReleaseReportPlugins( ReleaseDescriptor releaseDescriptor,
                                                           MavenProject project )
        throws ReleaseFailureException
    {
        List<ReportPlugin> releaseReportPlugins = null;

        Reporting reporting = project.getModel().getReporting();

        if ( reporting != null )
        {
            List<ReportPlugin> reportPlugins = reporting.getPlugins();

            if ( reportPlugins != null )
            {
                Map<String, Artifact> artifactsById = project.getReportArtifactMap();

                releaseReportPlugins = new ArrayList<>();

                for ( ReportPlugin reportPlugin : reportPlugins )
                {
                    String id = ArtifactUtils.versionlessKey( reportPlugin.getGroupId(), reportPlugin.getArtifactId() );
                    Artifact artifact = artifactsById.get( id );
                    String version = getReleaseVersion( releaseDescriptor, artifact );

                    ReportPlugin releaseReportPlugin = new ReportPlugin();
                    releaseReportPlugin.setGroupId( reportPlugin.getGroupId() );
                    releaseReportPlugin.setArtifactId( reportPlugin.getArtifactId() );
                    releaseReportPlugin.setVersion( version );
                    releaseReportPlugin.setInherited( reportPlugin.getInherited() );
                    releaseReportPlugin.setConfiguration( reportPlugin.getConfiguration() );
                    releaseReportPlugin.setReportSets( reportPlugin.getReportSets() );

                    releaseReportPlugins.add( releaseReportPlugin );
                }
            }
        }

        return releaseReportPlugins;
    }

    private List<Extension> createReleaseExtensions( ReleaseDescriptor releaseDescriptor,
                                                     MavenProject project )
        throws ReleaseFailureException
    {
        List<Extension> releaseExtensions = null;

        // Use original - don't want the lifecycle introduced ones
        Build build = project.getOriginalModel().getBuild();

        if ( build != null )
        {
            List<Extension> extensions = build.getExtensions();

            if ( extensions != null )
            {
                releaseExtensions = new ArrayList<>();

                for ( Extension extension : extensions )
                {
                    String id = ArtifactUtils.versionlessKey( extension.getGroupId(), extension.getArtifactId() );
                    Artifact artifact = project.getExtensionArtifactMap().get( id );
                    String version = getReleaseVersion( releaseDescriptor, artifact );

                    Extension releaseExtension = new Extension();
                    releaseExtension.setGroupId( extension.getGroupId() );
                    releaseExtension.setArtifactId( extension.getArtifactId() );
                    releaseExtension.setVersion( version );

                    releaseExtensions.add( releaseExtension );
                }
            }
        }

        return releaseExtensions;
    }

    /*
     * @see org.apache.maven.shared.release.phase.AbstractReleasePhase#clean(java.util.List)
     */
    @Override
    public ReleaseResult clean( List<MavenProject> reactorProjects )
    {
        ReleaseResult result = new ReleaseResult();

        for ( MavenProject project : reactorProjects )
        {
            File releasePom = ReleaseUtil.getReleasePom( project );

            // MRELEASE-273 : A release pom can be null
            if ( releasePom != null && releasePom.exists() )
            {
                logInfo( result, "Deleting release POM for '" + project.getName() + "'..." );

                if ( !releasePom.delete() )
                {
                    logWarn( result, "Cannot delete release POM: " + releasePom );
                }
            }
        }

        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }
}
