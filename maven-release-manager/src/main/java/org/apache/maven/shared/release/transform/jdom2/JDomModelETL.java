package org.apache.maven.shared.release.transform.jdom2;

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
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.transform.ModelETL;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.codehaus.plexus.util.WriterFactory;
import org.jdom2.CDATA;
import org.jdom2.Comment;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.ContentFilter;
import org.jdom2.filter.ElementFilter;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/**
 * JDOM2 implementation for extracting, transform, loading the Model (pom.xml)
 *
 * @author Robert Scholte
 * @since 3.0
 */
public class JDomModelETL implements ModelETL
{
    private ReleaseDescriptor releaseDescriptor;

    private MavenProject project;

    private Document document;

    private String intro = null;
    private String outtro = null;

    private String ls = ReleaseUtil.LS;

    public void setLs( String ls )
    {
        this.ls = ls;
    }

    public void setReleaseDescriptor( ReleaseDescriptor releaseDescriptor )
    {
        this.releaseDescriptor = releaseDescriptor;
    }

    public void setProject( MavenProject project )
    {
        this.project = project;
    }

    @Override
    public void extract( File pomFile ) throws ReleaseExecutionException
    {
        try
        {
            String content = ReleaseUtil.readXmlFile( pomFile, ls );
            // we need to eliminate any extra whitespace inside elements, as JDOM2 will nuke it
            content = content.replaceAll( "<([^!][^>]*?)\\s{2,}([^>]*?)>", "<$1 $2>" );
            content = content.replaceAll( "(\\s{2,})/>", "$1 />" );

            SAXBuilder builder = new SAXBuilder();
            document = builder.build( new StringReader( content ) );

            // Normalize line endings to platform's style (XML processors like JDOM2 normalize line endings to "\n" as
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
                 * fail. So let's try harder. Maybe some day, when JDOM2 offers a StaxBuilder and this builder employes
                 * XMLInputFactory2.P_REPORT_PROLOG_WHITESPACE, this whole mess can be avoided.
                 */
                // CHECKSTYLE_OFF: LocalFinalVariableName
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
                // CHECKSTYLE_ON: LocalFinalVariableName

                Matcher matcher = Pattern.compile( POM ).matcher( content );
                if ( matcher.matches() )
                {
                    intro = matcher.group( 1 );
                    outtro = matcher.group( matcher.groupCount() );
                }
            }
        }
        catch ( JDOMException | IOException e )
        {
            throw new ReleaseExecutionException( "Error reading POM: " + e.getMessage(), e );
        }
    }

    @Override
    public void transform()
    {

    }

    @Override
    public void load( File targetFile ) throws ReleaseExecutionException
    {
        writePom( targetFile, document, releaseDescriptor, project.getModelVersion(), intro, outtro );
    }

    @Override
    public Model getModel()
    {
        return new JDomModel( document );
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
                    + " https://maven.apache.org/xsd/maven-" + modelVersion + ".xsd", xsiNamespace );
            }

            // the empty namespace is considered equal to the POM namespace, so match them up to avoid extra xmlns=""
            ElementFilter elementFilter = new ElementFilter( Namespace.getNamespace( "" ) );
            for ( Iterator<?> i = rootElement.getDescendants( elementFilter ); i.hasNext(); )
            {
                Element e = (Element) i.next();
                e.setNamespace( pomNamespace );
            }
        }

        
        try ( Writer writer = WriterFactory.newXmlWriter( pomFile ) )
        {
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
    }

}
