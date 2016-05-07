package org.apache.maven.shared.release.transform.jdom;

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
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.model.Model;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.jdom.CDATA;
import org.jdom.Comment;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.filter.ContentFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * JDom implementation for extracting, transform, loading the Model (pom.xml) 
 * 
 * @author Robert Scholte
 * @since 3.0
 */
public class JDomModelETL
{
    private Document document;
    
    private String intro = null;
    private String outtro = null;
    
    private String ls = ReleaseUtil.LS;

    public void setLs( String ls )
    {
        this.ls = ls;
    }
    
    public void extract( File pomFile ) throws ReleaseExecutionException
    {
        try
        {
            String content = ReleaseUtil.readXmlFile( pomFile, ls );
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
        catch ( JDOMException e )
        {
            throw new ReleaseExecutionException( "Error reading POM: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new ReleaseExecutionException( "Error reading POM: " + e.getMessage(), e );
        }
    }
    
    public void transform()
    {
        
    }
    
    public void load()
    {
        
    }
    
    // will be removed once transform() is implemented
    public Model getModel()
    {
        return new JDomModel( document );
    }
    
    // will be removed once load() is implemented
    public Document getDocument()
    {
        return document;
    }
    
    // will be removed once load() is implemented
    public String getIntro()
    {
        return intro;
    }
    
    // will be removed once load() is implemented
    public String getOuttro()
    {
        return outtro;
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

}
