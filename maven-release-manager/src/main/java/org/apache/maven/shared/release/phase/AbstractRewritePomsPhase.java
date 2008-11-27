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

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
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
import org.apache.maven.shared.release.scm.ReleaseScmCommandException;
import org.apache.maven.shared.release.scm.ReleaseScmRepositoryException;
import org.apache.maven.shared.release.scm.ScmRepositoryConfigurator;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;
import org.jdom.Comment;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.Text;
import org.jdom.filter.ContentFilter;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private ScmRepositoryConfigurator scmRepositoryConfigurator;

    /**
     * Configuration item for the suffix to add to rewritten POMs when simulating.
     */
    private String pomSuffix;

    public ReleaseResult execute( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment, List reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        ReleaseResult result = new ReleaseResult();

        transform( releaseDescriptor, releaseEnvironment, reactorProjects, false, result );

        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }

    private void transform( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment, List reactorProjects,
                            boolean simulate, ReleaseResult result )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        for ( Iterator it = reactorProjects.iterator(); it.hasNext(); )
        {
            MavenProject project = (MavenProject) it.next();

            logInfo( result, "Transforming '" + project.getName() + "'..." );

            transformProject( project, releaseDescriptor, releaseEnvironment, reactorProjects, simulate, result );
        }
    }

    private void transformProject( MavenProject project, ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                                   List reactorProjects, boolean simulate, ReleaseResult result )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        Document document;
        String intro = null;
        String outtro = null;
        try
        {
            String content = ReleaseUtil.readXmlFile( ReleaseUtil.getStandardPom( project ) );
            // we need to eliminate any extra whitespace inside elements, as JDOM will nuke it
            content = content.replaceAll( "<([^!][^>]*?)\\s{2,}([^>]*?)>", "<$1 $2>" );
            content = content.replaceAll( "(\\s{2,}|[^\\s])/>", "$1 />" );

            SAXBuilder builder = new SAXBuilder();
            document = builder.build( new StringReader( content ) );

            // Normalize line endings to platform's style (XML processors like JDOM normalize line endings to "\n" as
            // per section 2.11 of the XML spec)
            normaliseLineEndings( document );

            // rewrite DOM as a string to find differences, since text outside the root element is not tracked
            StringWriter w = new StringWriter();
            Format format = Format.getRawFormat();
            format.setLineSeparator( ReleaseUtil.LS );
            XMLOutputter out = new XMLOutputter( format );
            out.output( document.getRootElement(), w );

            int index = content.indexOf( w.toString() );
            if ( index >= 0 )
            {
                intro = content.substring( 0, index );
                outtro = content.substring( index + w.toString().length() );
            } else {
                /*
                 * NOTE: Due to whitespace, attribute reordering or entity expansion the above indexOf test can easily
                 * fail. So let's try harder. Maybe some day, when JDOM offers a StaxBuilder and this builder employes
                 * XMLInputFactory2.P_REPORT_PROLOG_WHITESPACE, this whole mess can be avoided.
                 */
                final String SPACE = "\\s++";
                final String XML = "<\\?(?:(?:[^\"'>]++)|(?:\"[^\"]*+\")|(?:'[^\']*+'))*+>";
                final String INTSUB = "\\[(?:(?:[^\"'\\]]++)|(?:\"[^\"]*+\")|(?:'[^\']*+'))*+\\]";
                final String DOCTYPE =
                    "<!DOCTYPE(?:(?:[^\"'\\[>]++)|(?:\"[^\"]*+\")|(?:'[^\']*+')|(?:" + INTSUB + "))*+>";
                final String PI = XML;
                final String COMMENT = "<!--(?:[^-]|(?:-[^-]))*+-->";

                final String INTRO =
                    "(?:(?:" + SPACE + ")|(?:" + XML + ")|(?:" + DOCTYPE + ")|(?:" + COMMENT + ")|(?:" + PI + "))*";
                final String OUTRO = "(?:(?:" + SPACE + ")|(?:" + COMMENT + ")|(?:" + PI + "))*";
                final String POM = "(?s)(" + INTRO + ")(.*?)(" + OUTRO + ")";

                Matcher matcher = Pattern.compile( POM ).matcher( content );
                if ( matcher.matches() )
                {
                    intro = matcher.group( 1 );
                    outtro = matcher.group( matcher.groupCount() );
                }
            }
        }
        catch ( JDOMException e )
        {
            throw new ReleaseExecutionException( "Error reading POM: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new ReleaseExecutionException( "Error reading POM: " + e.getMessage(), e );
        }

        ScmRepository scmRepository;
        ScmProvider provider;
        try
        {
            scmRepository = scmRepositoryConfigurator.getConfiguredRepository( releaseDescriptor, releaseEnvironment.getSettings() );

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

        transformDocument( project, document.getRootElement(), releaseDescriptor, reactorProjects, scmRepository,
                           result );

        File pomFile = ReleaseUtil.getStandardPom( project );

        if ( simulate )
        {
            File outputFile =
                new File( pomFile.getParentFile(), pomFile.getName() + "." + pomSuffix );
            writePom( outputFile, document, releaseDescriptor, project.getModelVersion(), intro, outtro );
        }
        else
        {
            writePom( pomFile, document, releaseDescriptor, project.getModelVersion(), intro, outtro,
                      scmRepository, provider );
        }
    }

    private void normaliseLineEndings( Document document )
    {
        for ( Iterator i = document.getDescendants( new ContentFilter( ContentFilter.COMMENT ) ); i.hasNext(); )
        {
            Comment c = (Comment) i.next();
            c.setText( ReleaseUtil.normalizeLineEndings( c.getText(), ReleaseUtil.LS ) );
        }
    }

    private void transformDocument( MavenProject project, Element rootElement, ReleaseDescriptor releaseDescriptor,
                                    List reactorProjects, ScmRepository scmRepository, ReleaseResult result )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        Namespace namespace = rootElement.getNamespace();
        Map mappedVersions = getNextVersionMap( releaseDescriptor );
        Map originalVersions = getOriginalVersionMap( releaseDescriptor, reactorProjects );
        Map resolvedSnapshotDependencies = releaseDescriptor.getResolvedSnapshotDependencies();
        Element properties = rootElement.getChild( "properties", namespace );

        String parentVersion = rewriteParent( project, rootElement, namespace, mappedVersions, originalVersions );

        String projectId = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

        rewriteVersion( rootElement, namespace, mappedVersions, projectId, project, parentVersion );

        rewriteDependencies( project.getDependencies(), rootElement, mappedVersions, resolvedSnapshotDependencies,
                             originalVersions, projectId, properties, result, releaseDescriptor );

        if ( project.getDependencyManagement() != null )
        {
            Element dependencyRoot = rootElement.getChild( "dependencyManagement", namespace );
            if ( dependencyRoot != null )
            {
                rewriteDependencies( project.getDependencyManagement().getDependencies(), dependencyRoot,
                                     mappedVersions, resolvedSnapshotDependencies, originalVersions, projectId,
                                     properties, result, releaseDescriptor );
            }
        }

        if ( project.getBuild() != null )
        {
            Element buildRoot = rootElement.getChild( "build", namespace );
            if ( buildRoot != null )
            {
                rewritePlugins( project.getBuildPlugins(), buildRoot, mappedVersions, resolvedSnapshotDependencies,
                                originalVersions, projectId, properties, result, releaseDescriptor );
                if ( project.getPluginManagement() != null )
                {
                    Element pluginsRoot = buildRoot.getChild( "pluginManagement", namespace );
                    if ( pluginsRoot != null )
                    {
                        rewritePlugins( project.getPluginManagement().getPlugins(), pluginsRoot, mappedVersions,
                                        resolvedSnapshotDependencies, originalVersions, projectId, properties, result,
                                        releaseDescriptor );
                    }
                }
                rewriteExtensions( project.getBuildExtensions(), buildRoot, mappedVersions,
                                   resolvedSnapshotDependencies, originalVersions, projectId, properties, result,
                                   releaseDescriptor );
            }
        }

        if ( project.getReporting() != null )
        {
            Element pluginsRoot = rootElement.getChild( "reporting", namespace );
            if ( pluginsRoot != null )
            {
                rewriteReportPlugins( project.getReportPlugins(), pluginsRoot, mappedVersions,
                                      resolvedSnapshotDependencies, originalVersions, projectId, properties, result,
                                      releaseDescriptor );
            }
        }

        transformScm( project, rootElement, namespace, releaseDescriptor, projectId, scmRepository, result,
                      ReleaseUtil.getRootProject( reactorProjects ) );
    }

    /**
     * Updates the text value of the given element. The primary purpose of this method is to preserve any whitespace and
     * comments around the original text value.
     *
     * @param element The element to update, must not be <code>null</code>.
     * @param value The text string to set, must not be <code>null</code>.
     */
    private void rewriteValue( Element element, String value )
    {
        Text text = null;
        if ( element.getContent() != null )
        {
            for ( Iterator it = element.getContent().iterator(); it.hasNext(); )
            {
                Object content = it.next();
                if ( ( content instanceof Text ) && ( (Text) content ).getTextTrim().length() > 0 )
                {
                    text = (Text) content;
                    while ( it.hasNext() )
                    {
                        content = it.next();
                        if ( content instanceof Text )
                        {
                            text.append( (Text) content );
                            it.remove();
                        }
                        else
                        {
                            break;
                        }
                    }
                    break;
                }
            }
        }
        if ( text == null )
        {
            element.addContent( value );
        }
        else
        {
            String chars = text.getText();
            String trimmed = text.getTextTrim();
            int idx = chars.indexOf( trimmed );
            String leadingWhitespace = chars.substring( 0, idx );
            String trailingWhitespace = chars.substring( idx + trimmed.length() );
            text.setText( leadingWhitespace + value + trailingWhitespace );
        }
    }

    private void rewriteVersion( Element rootElement, Namespace namespace, Map mappedVersions, String projectId,
                                 MavenProject project, String parentVersion )
        throws ReleaseFailureException
    {
        Element versionElement = rootElement.getChild( "version", namespace );
        String version = (String) mappedVersions.get( projectId );
        if ( version == null )
        {
            throw new ReleaseFailureException( "Version for '" + project.getName() + "' was not mapped" );
        }

        if ( versionElement == null )
        {
            if ( !version.equals( parentVersion ) )
            {
                // we will add this after artifactId, since it was missing but different from the inherited version
                Element artifactIdElement = rootElement.getChild( "artifactId", namespace );
                int index = rootElement.indexOf( artifactIdElement );

                versionElement = new Element( "version", namespace );
                versionElement.setText( version );
                rootElement.addContent( index + 1, new Text( "\n  " ) );
                rootElement.addContent( index + 2, versionElement );
            }
        }
        else
        {
            rewriteValue( versionElement, version );
        }
    }

    private String rewriteParent( MavenProject project, Element rootElement, Namespace namespace, Map mappedVersions,
                                  Map originalVersions )
        throws ReleaseFailureException
    {
        String parentVersion = null;
        if ( project.hasParent() )
        {
            Element parentElement = rootElement.getChild( "parent", namespace );
            Element versionElement = parentElement.getChild( "version", namespace );
            MavenProject parent = project.getParent();
            String key = ArtifactUtils.versionlessKey( parent.getGroupId(), parent.getArtifactId() );
            parentVersion = (String) mappedVersions.get( key );
            if ( parentVersion == null )
            {
                if ( parent.getVersion().equals( originalVersions.get( key ) ) )
                {
                    throw new ReleaseFailureException( "Version for parent '" + parent.getName() + "' was not mapped" );
                }
            }
            else
            {
                rewriteValue( versionElement, parentVersion );
            }
        }
        return parentVersion;
    }

    private void rewriteDependencies( List dependencies, Element dependencyRoot, Map mappedVersions,
                                      Map resolvedSnapshotDependencies, Map originalVersions, String projectId,
                                      Element properties, ReleaseResult result, ReleaseDescriptor releaseDescriptor )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        if ( dependencies != null )
        {
            List dependenciesAlreadyChanged = new ArrayList();
            for ( Iterator i = dependencies.iterator(); i.hasNext(); )
            {
                Dependency dep = (Dependency) i.next();
                String depId = ArtifactUtils.versionlessKey( dep.getGroupId(), dep.getArtifactId() );
                if ( !dependenciesAlreadyChanged.contains( depId ) )
                {
                    //This check is required because updateDomVersion update all dependencies with the current groupId/artifactId
                    //(standard dependencies and sub-dependencies like ejb-client) so we don't need to re-update them

                    dependenciesAlreadyChanged.add( depId );

                    updateDomVersion( dep.getGroupId(), dep.getArtifactId(), mappedVersions,
                                      resolvedSnapshotDependencies, dep.getVersion(), originalVersions, "dependencies",
                                      "dependency", dependencyRoot, projectId, properties, result, releaseDescriptor );
                }
            }
        }
    }

    private void rewritePlugins( List plugins, Element pluginRoot, Map mappedVersions, Map resolvedSnapshotDependencies,
                                 Map originalVersions, String projectId, Element properties, ReleaseResult result,
                                 ReleaseDescriptor releaseDescriptor )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        if ( plugins != null )
        {
            for ( Iterator i = plugins.iterator(); i.hasNext(); )
            {
                Plugin plugin = (Plugin) i.next();

                // We can ignore plugins whose version is assumed, they are only written into the release pom
                if ( plugin.getVersion() != null )
                {
                    updateDomVersion( plugin.getGroupId(), plugin.getArtifactId(), mappedVersions,
                                      resolvedSnapshotDependencies, plugin.getVersion(), originalVersions, "plugins",
                                      "plugin", pluginRoot, projectId, properties, result, releaseDescriptor );
                }
            }
        }
    }

    private void rewriteExtensions( List extensions, Element extensionRoot, Map mappedVersions,
                                    Map resolvedSnapshotDependencies, Map originalVersions, String projectId,
                                    Element properties, ReleaseResult result, ReleaseDescriptor releaseDescriptor )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        if ( extensions != null )
        {
            for ( Iterator i = extensions.iterator(); i.hasNext(); )
            {
                Extension extension = (Extension) i.next();

                if ( extension.getVersion() != null )
                {
                    updateDomVersion( extension.getGroupId(), extension.getArtifactId(), mappedVersions,
                                      resolvedSnapshotDependencies, extension.getVersion(), originalVersions,
                                      "extensions", "extension", extensionRoot, projectId, properties, result,
                                      releaseDescriptor );
                }
            }
        }
    }

    private void rewriteReportPlugins( List plugins, Element pluginRoot, Map mappedVersions,
                                       Map resolvedSnapshotDependencies, Map originalVersions, String projectId,
                                       Element properties, ReleaseResult result, ReleaseDescriptor releaseDescriptor )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        if ( plugins != null )
        {
            for ( Iterator i = plugins.iterator(); i.hasNext(); )
            {
                ReportPlugin plugin = (ReportPlugin) i.next();

                // We can ignore plugins whose version is assumed, they are only written into the release pom
                if ( plugin.getVersion() != null )
                {
                    updateDomVersion( plugin.getGroupId(), plugin.getArtifactId(), mappedVersions,
                                      resolvedSnapshotDependencies, plugin.getVersion(), originalVersions, "plugins",
                                      "plugin", pluginRoot, projectId, properties, result, releaseDescriptor );
                }
            }
        }
    }

    private List getDependencies( String groupId, String artifactId, String groupTagName, String tagName,
                                  Element dependencyRoot )
        throws JDOMException
    {
        XPath xpath;
        if ( !StringUtils.isEmpty( dependencyRoot.getNamespaceURI() ) )
        {
            xpath = XPath.newInstance( "./pom:" + groupTagName + "/pom:" + tagName + "[normalize-space(pom:groupId)='" + groupId +
                "' and normalize-space(pom:artifactId)='" + artifactId + "']" );
            xpath.addNamespace( "pom", dependencyRoot.getNamespaceURI() );
        }
        else
        {
            xpath = XPath.newInstance( "./" + groupTagName + "/" + tagName + "[normalize-space(groupId)='" + groupId +
                "' and normalize-space(artifactId)='" + artifactId + "']" );
        }

        List dependencies = xpath.selectNodes( dependencyRoot );

        //MRELEASE-147
        if ( ( dependencies == null || dependencies.isEmpty() ) && groupId.indexOf( "${" ) == -1 )
        {
            dependencies = getDependencies( "${project.groupId}", artifactId, groupTagName, tagName, dependencyRoot );

            if ( dependencies == null || dependencies.isEmpty() )
            {
                dependencies = getDependencies( "${pom.groupId}", artifactId, groupTagName, tagName, dependencyRoot );
            }
        }

        return dependencies;
    }

    private void updateDomVersion( String groupId, String artifactId, Map mappedVersions,
                                   Map resolvedSnapshotDepedencies, String version, Map originalVersions,
                                   String groupTagName, String tagName, Element dependencyRoot, String projectId,
                                   Element properties, ReleaseResult result, ReleaseDescriptor releaseDescriptor )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        String key = ArtifactUtils.versionlessKey( groupId, artifactId );
        String mappedVersion = (String) mappedVersions.get( key );
        String resolvedSnapshotVersion = getResolvedSnapshotVersion( key, resolvedSnapshotDepedencies );
        Object originalVersion = originalVersions.get( key );

        // workaround
        if ( originalVersion == null )
        {
            originalVersion = getOriginalResolvedSnapshotVersion( key, resolvedSnapshotDepedencies );
        }

        try
        {
            List dependencies = getDependencies( groupId, artifactId, groupTagName, tagName, dependencyRoot );

            for ( Iterator i = dependencies.iterator(); i.hasNext(); )
            {
                Element dependency = (Element) i.next();
                String dependencyVersion = "";
                Element versionElement = null;

                if ( dependency != null )
                {
                    versionElement = dependency.getChild( "version", dependencyRoot.getNamespace() );
                    if ( versionElement != null )
                    {
                        dependencyVersion = versionElement.getTextTrim();
                    }
                }

                //MRELEASE-220
                if ( mappedVersion != null && mappedVersion.endsWith( "SNAPSHOT" ) &&
                    !dependencyVersion.endsWith( "SNAPSHOT" ) && !releaseDescriptor.isUpdateDependencies() )
                {
                    return;
                }

                if ( version.equals( originalVersion ) || dependencyVersion.equals( originalVersion ) )
                {
                    if ( ( mappedVersion != null ) || ( resolvedSnapshotVersion != null ) )
                    {
                        logInfo( result, "Updating " + artifactId + " to " +
                            ( ( mappedVersion != null ) ? mappedVersion : resolvedSnapshotVersion ) );

                        // If it was inherited, nothing to do
                        if ( dependency != null )
                        {
                            // avoid if in management
                            if ( versionElement != null )
                            {
                                if ( mappedVersion == null )
                                {
                                    rewriteValue( versionElement, resolvedSnapshotVersion );
                                    return;
                                }

                                String versionText = versionElement.getTextTrim();

                                // avoid if it was not originally set to the original value (it may be an expression), unless mapped version differs
                                if ( originalVersion.equals( versionText ) ||
                                    !mappedVersion.equals( mappedVersions.get( projectId ) ) )
                                {
                                    rewriteValue( versionElement, mappedVersion );
                                }
                                else if ( versionText.matches( "\\$\\{project.+\\}" ) ||
                                    versionText.matches( "\\$\\{pom.+\\}" ) || "${version}".equals( versionText ) )
                                {
                                    logInfo( result,
                                             "Ignoring artifact version update for expression: " + versionText );
                                    //ignore... we cannot update this expression
                                }
                                else if ( versionText.matches( "\\$\\{.+\\}" ) && properties != null )
                                {
                                    //version is an expression, check for properties to update instead
                                    String expression = versionText.substring( 2, versionText.length() - 1 );
                                    Element property = properties.getChild( expression, properties.getNamespace() );
                                    if ( property != null )
                                    {
                                        String propertyValue = property.getTextTrim();

                                        if ( originalVersion.equals( propertyValue ) )
                                        {
                                            // change the property only if the property is the same as what's in the reactor
                                            rewriteValue( property, mappedVersion );
                                        }
                                        else if ( mappedVersion.equals( propertyValue ))
                                        {
                                           //this property may have been updated during processing a sibling.
                                            logInfo( result, "Ignoring artifact version update for expression: " +
                                                     mappedVersion+" because it is already updated." );
                                        }
                                        else if ( !mappedVersion.equals( versionText ) )
                                        {
                                            if ( mappedVersion.matches( "\\$\\{project.+\\}" ) ||
                                                mappedVersion.matches( "\\$\\{pom.+\\}" ) ||
                                                "${version}".equals( mappedVersion ) )
                                            {
                                                logInfo( result, "Ignoring artifact version update for expression: " +
                                                    mappedVersion );
                                                //ignore... we cannot update this expression
                                            }
                                            else
                                            {
                                                // the value of the expression conflicts with what the user wanted to release
                                                throw new ReleaseFailureException( "The artifact (" + key +
                                                    ") requires a " + "different version (" + mappedVersion +
                                                    ") than what is found (" + propertyValue +
                                                    ") for the expression (" + expression + ") in the " + "project (" +
                                                    projectId + ")." );
                                            }
                                        }
                                    }
                                    else
                                    {
                                        // the expression used to define the version of this artifact may be inherited
                                        // TODO needs a better error message, what pom? what dependency?
                                        throw new ReleaseFailureException(
                                            "The version could not be updated: " + versionText );
                                    }
                                }
                                else
                                {
                                    // the version for this artifact could not be updated.
                                    throw new ReleaseFailureException(
                                        "The version could not be updated: " + versionText );
                                }
                            }
                        }
                    }
                    else
                    {
                        throw new ReleaseFailureException(
                            "Version '" + version + "' for " + tagName + " '" + key + "' was not mapped" );
                    }
                }
            }
        }
        catch ( JDOMException e )
        {
            throw new ReleaseExecutionException( "Unable to locate " + tagName + " to process in document", e );
        }
    }

    private void writePom( File pomFile, Document document, ReleaseDescriptor releaseDescriptor, String modelVersion,
                           String intro, String outtro, ScmRepository repository, ScmProvider provider )
        throws ReleaseExecutionException, ReleaseScmCommandException
    {
        try
        {
            if ( releaseDescriptor.isScmUseEditMode() || provider.requiresEditMode() )
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

        writePom( pomFile, document, releaseDescriptor, modelVersion, intro, outtro );
    }

    private void writePom( File pomFile, Document document, ReleaseDescriptor releaseDescriptor, String modelVersion,
                           String intro, String outtro )
        throws ReleaseExecutionException
    {
        Element rootElement = document.getRootElement();

        if ( releaseDescriptor.isAddSchema() )
        {
            Namespace pomNamespace = Namespace.getNamespace( "", "http://maven.apache.org/POM/" + modelVersion );
            rootElement.setNamespace( pomNamespace );
            Namespace xsiNamespace = Namespace.getNamespace( "xsi", "http://www.w3.org/2001/XMLSchema-instance" );
            rootElement.addNamespaceDeclaration( xsiNamespace );

            if ( rootElement.getAttribute( "schemaLocation", xsiNamespace ) == null )
            {
                rootElement.setAttribute( "schemaLocation", "http://maven.apache.org/POM/" + modelVersion +
                    " http://maven.apache.org/maven-v" + modelVersion.replace( '.', '_' ) + ".xsd", xsiNamespace );
            }

            // the empty namespace is considered equal to the POM namespace, so match them up to avoid extra xmlns=""
            ElementFilter elementFilter = new ElementFilter( Namespace.getNamespace( "" ) );
            for ( Iterator i = rootElement.getDescendants( elementFilter ); i.hasNext(); )
            {
                Element e = (Element) i.next();
                e.setNamespace( pomNamespace );
            }
        }

        Writer writer = null;
        try
        {
            writer = WriterFactory.newXmlWriter( pomFile );

            if ( intro != null )
            {
                writer.write( intro );
            }

            Format format = Format.getRawFormat();
            format.setLineSeparator( ReleaseUtil.LS );
            XMLOutputter out = new XMLOutputter( format );
            out.output( document.getRootElement(), writer );

            if ( outtro != null )
            {
                writer.write( outtro );
            }
        }
        catch ( IOException e )
        {
            throw new ReleaseExecutionException( "Error writing POM: " + e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( writer );
        }
    }

    public ReleaseResult simulate( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment, List reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        ReleaseResult result = new ReleaseResult();

        transform( releaseDescriptor, releaseEnvironment, reactorProjects, true, result );

        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }

    public ReleaseResult clean( List reactorProjects )
    {
        ReleaseResult result = new ReleaseResult();

        super.clean( reactorProjects );

        if ( reactorProjects != null )
        {
            for ( Iterator i = reactorProjects.iterator(); i.hasNext(); )
            {
                MavenProject project = (MavenProject) i.next();

                File pomFile = ReleaseUtil.getStandardPom( project );
                // MRELEASE-273 : if no pom
                if ( pomFile != null )
                {
                    File file = new File( pomFile.getParentFile(), pomFile.getName() + "." + pomSuffix );
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

    protected abstract String getResolvedSnapshotVersion( String artifactVersionlessKey, Map resolvedSnapshots );

    protected abstract Map getOriginalVersionMap( ReleaseDescriptor releaseDescriptor, List reactorProjects );

    protected abstract Map getNextVersionMap( ReleaseDescriptor releaseDescriptor );

    protected abstract void transformScm( MavenProject project, Element rootElement, Namespace namespace,
                                          ReleaseDescriptor releaseDescriptor, String projectId,
                                          ScmRepository scmRepository, ReleaseResult result, MavenProject rootProject )
        throws ReleaseExecutionException;

    protected String getOriginalResolvedSnapshotVersion( String artifactVersionlessKey, Map resolvedSnapshots )
    {
        Map versionsMap = (Map) resolvedSnapshots.get( artifactVersionlessKey );

        if ( versionsMap != null )
        {
            return (String) ( versionsMap.get( ReleaseDescriptor.ORIGINAL_VERSION ) );
        }
        else
        {
            return null;
        }
    }

    protected Element rewriteElement( String name, String value, Element root, Namespace namespace )
    {
        Element tagElement = root.getChild( name, namespace );
        if ( tagElement != null )
        {
            if ( value != null )
            {
                rewriteValue( tagElement, value );
            }
            else
            {
                int index = root.indexOf( tagElement );
                root.removeContent( index );
                for ( int i = index - 1; i >= 0; i-- )
                {
                    if ( root.getContent( i ) instanceof Text )
                    {
                        root.removeContent( i );
                    }
                    else
                    {
                        break;
                    }
                }
            }
        }
        else
        {
            if ( value != null )
            {
                Element element = new Element( name, namespace );
                element.setText( value );
                root.addContent( "  " ).addContent( element ).addContent( "\n  " );
                tagElement = element;
            }
        }
        return tagElement;
    }
}
