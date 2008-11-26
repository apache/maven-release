package org.apache.maven.shared.release.util;

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
import java.io.Reader;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;

/**
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 */
public class ReleaseUtil
{
    public static final String RELEASE_POMv4 = "release-pom.xml";

    private static final String POMv4 = "pom.xml";

    /**
     * The line separator to use.
     */
    public static final String LS = System.getProperty( "line.separator" );

    private ReleaseUtil()
    {
    }

    public static MavenProject getRootProject( List reactorProjects )
    {
        MavenProject project = (MavenProject) reactorProjects.get( 0 );
        for ( Iterator i = reactorProjects.iterator(); i.hasNext(); )
        {
            MavenProject currentProject = (MavenProject) i.next();
            if ( currentProject.isExecutionRoot() )
            {
                project = currentProject;
                break;
            }
        }

        return project;
    }

    public static File getStandardPom( MavenProject project )
    {
        File pom = project.getFile();
        File releasePom = getReleasePom( project );

        // MRELEASE-273 : pom can be null here
        if ( pom != null && pom.equals( releasePom ) )
        {
            pom = new File( pom.getParent(), POMv4 );
        }

        return pom;
    }

    public static File getReleasePom( MavenProject project )
    {
        return new File( project.getFile().getParent(), RELEASE_POMv4 );
    }

    /**
     * Gets the string contents of the specified XML file. Note: In contrast to an XML processor, the line separators in
     * the returned string will be normalized to use the platform's native line separator. This is basically to save
     * another normalization step when writing the string contents back to an XML file.
     * 
     * @param file The path to the XML file to read in, must not be <code>null</code>.
     * @return The string contents of the XML file.
     * @throws IOException If the file could not be opened/read.
     */
    public static String readXmlFile( File file )
        throws IOException
    {
        Reader reader = null;
        try
        {
            reader = ReaderFactory.newXmlReader( file );
            return normalizeLineEndings( IOUtil.toString( reader ), LS );
        }
        finally
        {
            IOUtil.close( reader );
        }
    }

    /**
     * Normalizes the line separators in the specified string.
     * 
     * @param text The string to normalize, may be <code>null</code>.
     * @param separator The line separator to use for normalization, typically "\n" or "\r\n", must not be
     *            <code>null</code>.
     * @return The input string with normalized line separators or <code>null</code> if the string was <code>null</code>
     *         .
     */
    public static String normalizeLineEndings( String text, String separator )
    {
        String norm = text;
        if ( text != null )
        {
            norm = text.replaceAll( "(\r\n)|(\n)|(\r)", separator );
        }
        return norm;
    }

}
