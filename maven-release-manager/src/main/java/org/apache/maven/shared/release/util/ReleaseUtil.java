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

import org.apache.commons.lang.StringUtils;
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

    public static final String POMv4 = "pom.xml";

    /**
     * The line separator to use.
     */
    public static final String LS = System.getProperty( "line.separator" );
    
    /**
     * The path separator to use.
     */
    public static final String FS = System.getProperty( "file.separator" );

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
        if ( project == null )
            return null;

        File pom = project.getFile();

        if ( pom == null )
            return null;

        File releasePom = getReleasePom( project );
        if ( pom.equals( releasePom ) )
        {
            pom = new File( pom.getParent(), POMv4 );
        }

        return pom;
    }

    public static File getReleasePom( MavenProject project )
    {
        if ( project == null )
            return null;

        File pom = project.getFile();

        if ( pom == null )
            return null;

        return new File( pom.getParent(), RELEASE_POMv4 );
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
    
    /**
     * Determines the base working directory with regard to the longest relative path of the modules. 
     * 
     * @param workingDirectory The working directory of the project to be released
     * @param modules The \<modules\> of the project to be released
     * @return The base working directory of the project
     */
    public static String getBaseWorkingDirectory( String workingDirectory, List modules )
    {        
        int count = getLongestPathCount( modules );
        workingDirectory = StringUtils.chomp( workingDirectory, FS );
        
        while( count > 0 )
        {   
            int lastSep = workingDirectory.lastIndexOf( FS );
            workingDirectory = StringUtils.substring( workingDirectory, 0, lastSep );            
            count--;
        }
        return workingDirectory;
    }
    
    /**
     * Determines the base scm url with regard to the longest relative path of the modules.
     * 
     * @param scmUrl The scm source url of the project to be released which is set in the release descriptor.
     * @param modules The \<modules\> of the project to be released
     * @return
     */
    public static String getBaseScmUrl( String scmUrl, List modules )
    {                
        int count = getLongestPathCount( modules );                
        scmUrl = StringUtils.chomp( scmUrl, "/" );
        
        while( count > 0 )
        {   
            int lastSep = scmUrl.lastIndexOf( "/" );
            scmUrl = StringUtils.substring( scmUrl, 0, lastSep );        
            count--;
        }
        return scmUrl;
    }
    
    /**
     * Returns the common path of the two paths specified.
     * 
     * @param path1 The first path
     * @param path2 The second path
     * @return The common path of the two paths.
     */
    public static String getCommonPath( String path1, String path2 )
    {
        if ( path2 == null || path2.equals( "" ) )
        {
            return path1;
        }
        else
        {
            int indexDiff = StringUtils.indexOfDifference( path1, path2 );            
            if( indexDiff > 0 )
            {
                return path1.substring( 0, indexDiff );
            }
            else
            {
                return path1;
            }
        }
    }
    
    private static int getLongestPathCount( List modules )
    {
        int count = 0;
        if( modules == null || modules.isEmpty() )
        {
            return 0;
        }
        
        for( Iterator iter = modules.iterator(); iter.hasNext(); )
        {
            String module = ( String ) iter.next();
            module = StringUtils.replace( module, "\\", "/" );
            
            // module is a path
            if( module.indexOf( '/' ) != -1 )
            {   
                int tmp = StringUtils.countMatches( module, "/" );
                if( tmp > count )
                {
                    count = tmp;
                }
            }                    
        }
        return count;
    }
    
    /**
     * Gets the path to the project root. Useful in determining whether the project has a flat structure.
     * 
     * @param project
     * @return
     */
    public static String getRootProjectPath( MavenProject project )
    {
        String relPath = "";
        
        // module is a flat multi-module project
        if( getLongestPathCount( project.getModules() ) > 0 )
        {     
            String projectBaseDir = project.getBasedir().getPath();            
        	projectBaseDir = StringUtils.replace( projectBaseDir, "\\", "/" );
            
            String projectPath = "";            
            if( project.getScm() != null )
            {
            	String scmConnection = project.getScm().getConnection();
            	scmConnection = StringUtils.replace( scmConnection, "\\", "/" );
            	
                projectPath =
                    ReleaseUtil.getCommonPath( StringUtils.reverse( StringUtils.chomp( projectBaseDir, "/" ) ),
                                               StringUtils.reverse( StringUtils.chomp( scmConnection, "/" ) ) );
            }
            
            relPath = StringUtils.reverse( projectPath );
        }
        
        return relPath;
    }
}
