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
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Model;
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
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;
import org.jdom.CDATA;
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
     * SCM URL translators mapped by provider name.
     */
    private Map<String, ScmTranslator> scmTranslators;
    
    protected final Map<String, ScmTranslator> getScmTranslators()
    {
        return scmTranslators;
    }

    /**
     * Configuration item for the suffix to add to rewritten POMs when simulating.
     */
    private String pomSuffix;

    private String ls = ReleaseUtil.LS;

    public void setLs( String ls )
    {
        this.ls = ls;
    }

    public ReleaseResult execute( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                                  List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        ReleaseResult result = new ReleaseResult();

        transform( releaseDescriptor, releaseEnvironment, reactorProjects, false, result );

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
        Document document;
        String intro = null;
        String outtro = null;
        try
        {
            String content = ReleaseUtil.readXmlFile( ReleaseUtil.getStandardPom( project ), ls );
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
            format.setLineSeparator( ls );
            XMLOutputter out = new XMLOutputter( format );
            out.output( document.getRootElement(), w );

            int index = content.indexOf( w.toString() );
            if ( index >= 0 )
            {
                intro = content.substring( 0, index );
                outtro = content.substring( index + w.toString().length() );
            }
            else
            {
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

        transformDocument( project, document.getRootElement(), releaseDescriptor, reactorProjects, scmRepository,
                           result, simulate );

        File pomFile = ReleaseUtil.getStandardPom( project );

        if ( simulate )
        {
            File outputFile = new File( pomFile.getParentFile(), pomFile.getName() + "." + pomSuffix );
            writePom( outputFile, document, releaseDescriptor, project.getModelVersion(), intro, outtro );
        }
        else
        {
            writePom( pomFile, document, releaseDescriptor, project.getModelVersion(), intro, outtro, scmRepository,
                      provider );
        }
    }

    private void normaliseLineEndings( Document document )
    {
        for ( Iterator<?> i = document.getDescendants( new ContentFilter( ContentFilter.COMMENT ) ); i.hasNext(); )
        {
            Comment c = (Comment) i.next();
            c.setText( ReleaseUtil.normalizeLineEndings( c.getText(), ls ) );
        }
        for ( Iterator<?> i = document.getDescendants( new ContentFilter( ContentFilter.CDATA ) ); i.hasNext(); )
        {
            CDATA c = (CDATA) i.next();
            c.setText( ReleaseUtil.normalizeLineEndings( c.getText(), ls ) );
        }
    }

    private void transformDocument( MavenProject project, Element rootElement, ReleaseDescriptor releaseDescriptor,
                                    List<MavenProject> reactorProjects, ScmRepository scmRepository, ReleaseResult result,
                                    boolean simulate )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        Namespace namespace = rootElement.getNamespace();
        Map<String, String> mappedVersions = getNextVersionMap( releaseDescriptor );
        Map<String, String> originalVersions = getOriginalVersionMap( releaseDescriptor, reactorProjects, simulate );
        @SuppressWarnings( "unchecked" )
        Map<String, Map<String, String>> resolvedSnapshotDependencies =
            releaseDescriptor.getResolvedSnapshotDependencies();
        Model model = project.getModel();
        Element properties = rootElement.getChild( "properties", namespace );

        String parentVersion = rewriteParent( project, rootElement, namespace, mappedVersions, 
                                              resolvedSnapshotDependencies, originalVersions );

        String projectId = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

        rewriteVersion( rootElement, namespace, mappedVersions, projectId, project, parentVersion );

        List<Element> roots = new ArrayList<Element>();
        roots.add( rootElement );
        roots.addAll( getChildren( rootElement, "profiles", "profile" ) );

        for ( Element root : roots )
        {
            rewriteArtifactVersions( getChildren( root, "dependencies", "dependency" ), mappedVersions,
                                    resolvedSnapshotDependencies, originalVersions, model, properties, result,
                                    releaseDescriptor );

            rewriteArtifactVersions( getChildren( root, "dependencyManagement", "dependencies", "dependency" ),
                                    mappedVersions, resolvedSnapshotDependencies, originalVersions, model, properties,
                                    result, releaseDescriptor );

            rewriteArtifactVersions( getChildren( root, "build", "extensions", "extension" ), mappedVersions,
                                    resolvedSnapshotDependencies, originalVersions, model, properties, result,
                                    releaseDescriptor );

            List<Element> pluginElements = new ArrayList<Element>();
            pluginElements.addAll( getChildren( root, "build", "plugins", "plugin" ) );
            pluginElements.addAll( getChildren( root, "build", "pluginManagement", "plugins", "plugin" ) );

            rewriteArtifactVersions( pluginElements, mappedVersions, resolvedSnapshotDependencies, originalVersions,
                                    model, properties, result, releaseDescriptor );

            for ( Element pluginElement : pluginElements )
            {
                rewriteArtifactVersions( getChildren( pluginElement, "dependencies", "dependency" ), mappedVersions,
                                        resolvedSnapshotDependencies, originalVersions, model, properties, result,
                                        releaseDescriptor );
            }

            rewriteArtifactVersions( getChildren( root, "reporting", "plugins", "plugin" ), mappedVersions,
                                    resolvedSnapshotDependencies, originalVersions, model, properties, result,
                                    releaseDescriptor );
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
        transformScm( project, rootElement, namespace, releaseDescriptor, projectId, scmRepository, result,
                      commonBasedir );
    }

    @SuppressWarnings( "unchecked" )
    private List<Element> getChildren( Element root, String... names )
    {
        Element parent = root;
        for ( int i = 0; i < names.length - 1 && parent != null; i++ )
        {
            parent = parent.getChild( names[i], parent.getNamespace() );
        }
        if ( parent == null )
        {
            return Collections.emptyList();
        }
        return parent.getChildren( names[names.length - 1], parent.getNamespace() );
    }

    /**
     * Updates the text value of the given element. The primary purpose of this method is to preserve any whitespace and
     * comments around the original text value.
     *
     * @param element The element to update, must not be <code>null</code>.
     * @param value   The text string to set, must not be <code>null</code>.
     */
    private void rewriteValue( Element element, String value )
    {
        Text text = null;
        if ( element.getContent() != null )
        {
            for ( Iterator<?> it = element.getContent().iterator(); it.hasNext(); )
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

    private void rewriteVersion( Element rootElement, Namespace namespace, Map<String, String> mappedVersions, String projectId,
                                 MavenProject project, String parentVersion )
        throws ReleaseFailureException
    {
        Element versionElement = rootElement.getChild( "version", namespace );
        String version = mappedVersions.get( projectId );
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

    private String rewriteParent( MavenProject project, Element rootElement, Namespace namespace, Map<String, String> mappedVersions,
                                  Map<String, Map<String, String>> resolvedSnapshotDependencies, Map<String, String> originalVersions )
        throws ReleaseFailureException
    {
        String parentVersion = null;
        if ( project.hasParent() )
        {
            Element parentElement = rootElement.getChild( "parent", namespace );
            Element versionElement = parentElement.getChild( "version", namespace );
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
                rewriteValue( versionElement, parentVersion );
            }
        }
        return parentVersion;
    }

    private void rewriteArtifactVersions( Collection<Element> elements, Map<String, String> mappedVersions,
                                          Map<String, Map<String, String>> resolvedSnapshotDependencies, Map<String, String> originalVersions,
                                          Model projectModel, Element properties, ReleaseResult result,
                                          ReleaseDescriptor releaseDescriptor )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        if ( elements == null )
        {
            return;
        }
        String projectId = ArtifactUtils.versionlessKey( projectModel.getGroupId(), projectModel.getArtifactId() );
        for ( Element element : elements )
        {
            Element versionElement = element.getChild( "version", element.getNamespace() );
            if ( versionElement == null )
            {
                // managed dependency or unversioned plugin
                continue;
            }
            String rawVersion = versionElement.getTextTrim();

            Element groupIdElement = element.getChild( "groupId", element.getNamespace() );
            if ( groupIdElement == null )
            {
                if ( "plugin".equals( element.getName() ) )
                {
                    groupIdElement = new Element( "groupId", element.getNamespace() );
                    groupIdElement.setText( "org.apache.maven.plugins" );
                }
                else
                {
                    // incomplete dependency
                    continue;
                }
            }
            String groupId = ReleaseUtil.interpolate( groupIdElement.getTextTrim(), projectModel );

            Element artifactIdElement = element.getChild( "artifactId", element.getNamespace() );
            if ( artifactIdElement == null )
            {
                // incomplete element
                continue;
            }
            String artifactId = ReleaseUtil.interpolate( artifactIdElement.getTextTrim(), projectModel );

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
                    rewriteValue( versionElement, mappedVersion );
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
                            rewriteValue( versionElement, mappedVersion );
                        }
                        else
                        {
                            logInfo( result, "  Ignoring artifact version update for expression " + rawVersion );
                        }
                    }
                    else if ( properties != null )
                    {
                        // version is an expression, check for properties to update instead
                        Element property = properties.getChild( expression, properties.getNamespace() );
                        if ( property != null )
                        {
                            String propertyValue = property.getTextTrim();

                            if ( propertyValue.equals( originalVersion ) )
                            {
                                logInfo( result, "  Updating " + rawVersion + " to " + mappedVersion );
                                // change the property only if the property is the same as what's in the reactor
                                rewriteValue( property, mappedVersion );
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
                                    || mappedVersion.matches( "\\$\\{pom.+\\}" ) || "${version}".equals( mappedVersion ) )
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

                rewriteValue( versionElement, resolvedSnapshotVersion );
            }
            else
            {
                // artifact not related to current release
            }
        }
    }

    private void writePom( File pomFile, Document document, ReleaseDescriptor releaseDescriptor, String modelVersion,
                           String intro, String outtro, ScmRepository repository, ScmProvider provider )
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
                rootElement.setAttribute( "schemaLocation", "http://maven.apache.org/POM/" + modelVersion
                    + " http://maven.apache.org/maven-v" + modelVersion.replace( '.', '_' ) + ".xsd", xsiNamespace );
            }

            // the empty namespace is considered equal to the POM namespace, so match them up to avoid extra xmlns=""
            ElementFilter elementFilter = new ElementFilter( Namespace.getNamespace( "" ) );
            for ( Iterator<?> i = rootElement.getDescendants( elementFilter ); i.hasNext(); )
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
            format.setLineSeparator( ls );
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

    public ReleaseResult simulate( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                                   List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        ReleaseResult result = new ReleaseResult();

        transform( releaseDescriptor, releaseEnvironment, reactorProjects, true, result );

        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }

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

    protected abstract String getResolvedSnapshotVersion( String artifactVersionlessKey,
                                                          Map<String, Map<String, String>> resolvedSnapshots );

    protected abstract Map<String, String> getOriginalVersionMap( ReleaseDescriptor releaseDescriptor,
                                                                  List<MavenProject> reactorProjects, boolean simulate );

    protected abstract Map<String,String> getNextVersionMap( ReleaseDescriptor releaseDescriptor );

    protected abstract void transformScm( MavenProject project, Element rootElement, Namespace namespace,
                                          ReleaseDescriptor releaseDescriptor, String projectId,
                                          ScmRepository scmRepository, ReleaseResult result, String commonBasedir )
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
}
