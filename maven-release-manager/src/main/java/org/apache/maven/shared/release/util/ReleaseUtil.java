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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.MapBasedValueSource;
import org.codehaus.plexus.interpolation.ObjectBasedValueSource;
import org.codehaus.plexus.interpolation.PrefixAwareRecursionInterceptor;
import org.codehaus.plexus.interpolation.PrefixedObjectValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.codehaus.plexus.util.FileUtils;
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

    private static final String FS = File.separator;

    /**
     * The line separator to use.
     */
    public static final String LS = System.getProperty( "line.separator" );

    private ReleaseUtil()
    {
        // noop
    }

    public static MavenProject getRootProject( List<MavenProject> reactorProjects )
    {
        MavenProject project = reactorProjects.get( 0 );
        for ( MavenProject currentProject : reactorProjects )
        {
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
        {
            return null;
        }

        File pom = project.getFile();

        if ( pom == null )
        {
            return null;
        }

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
        {
            return null;
        }

        File pom = project.getFile();

        if ( pom == null )
        {
            return null;
        }

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
        return readXmlFile( file, LS );
    }

    public static String readXmlFile( File file, String ls )
        throws IOException
    {
        Reader reader = null;
        try
        {
            reader = ReaderFactory.newXmlReader( file );
            return normalizeLineEndings( IOUtil.toString( reader ), ls );
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

    public static ReleaseDescriptor createBasedirAlignedReleaseDescriptor( ReleaseDescriptor releaseDescriptor,
                                                                           List<MavenProject> reactorProjects )
        throws ReleaseExecutionException
    {
        String basedir;
        try
        {
            basedir = getCommonBasedir( reactorProjects );
        }
        catch ( IOException e )
        {
            throw new ReleaseExecutionException( "Exception occurred while calculating common basedir: "
                + e.getMessage(), e );
        }

        int parentLevels =
            getBaseWorkingDirectoryParentCount( basedir, FileUtils.normalize( releaseDescriptor.getWorkingDirectory() ) );

        String url = releaseDescriptor.getScmSourceUrl();
        url = realignScmUrl( parentLevels, url );

        ReleaseDescriptor descriptor = new ReleaseDescriptor();
        descriptor.setWorkingDirectory( basedir );
        descriptor.setScmSourceUrl( url );
        return descriptor;
    }

    public static String getCommonBasedir( List<MavenProject> reactorProjects )
        throws IOException
    {
        return getCommonBasedir( reactorProjects, FS );
    }

    public static String getCommonBasedir( List<MavenProject> reactorProjects, String separator )
        throws IOException
    {
        String[] baseDirs = new String[reactorProjects.size()];
        int idx = 0;
        for ( MavenProject p : reactorProjects )
        {
            String dir = p.getBasedir().getCanonicalPath();

            // always end with separator so that we know what is a path and what is a partial directory name in the
            // next call
            if ( !dir.endsWith( separator ) )
            {
                dir = dir + separator;
            }
            baseDirs[idx++] = dir;
        }

        String basedir = StringUtils.getCommonPrefix( baseDirs );

        int separatorPos = basedir.lastIndexOf( separator );
        if ( !basedir.endsWith( separator ) && separatorPos >= 0 )
        {
            basedir = basedir.substring( 0, separatorPos );
        }

        if ( basedir.endsWith( separator ) && basedir.length() > 1 )
        {
            basedir = basedir.substring( 0, basedir.length() - 1 );
        }

        return basedir;
    }

    public static int getBaseWorkingDirectoryParentCount( String basedir, String workingDirectory )
    {
        int num = 0;

        // we can safely assume case-insensitivity as we are just backtracking, not comparing. This helps with issues
        // on Windows with C: vs c:
        workingDirectory = workingDirectory.toLowerCase( Locale.ENGLISH );
        basedir = basedir.toLowerCase( Locale.ENGLISH );

        // MRELEASE-663
        // For Windows is does matter if basedir ends with a file-separator or not to be able to compare.
        // Using the parent of a dummy file makes it possible to compare them OS-independent
        File workingDirectoryFile = new File( workingDirectory, ".tmp" ).getParentFile();
        File basedirFile = new File( basedir, ".tmp" ).getParentFile();

        if ( !workingDirectoryFile.equals( basedirFile ) && workingDirectory.startsWith( basedir ) )
        {
            do
            {
                workingDirectoryFile = workingDirectoryFile.getParentFile();
                num++;
            }
            while ( !workingDirectoryFile.equals( basedirFile ) );
        }
        return num;
    }

    public static String realignScmUrl( int parentLevels, String url )
    {
        if ( !StringUtils.isEmpty( url ) )
        {
            int index = url.length();
            String suffix = "";
            if ( url.endsWith( "/" ) )
            {
                index--;
                suffix = "/";
            }
            for ( int i = 0; i < parentLevels && index > 0; i++ )
            {
                index = url.lastIndexOf( '/', index - 1 );
            }

            if ( index > 0 )
            {
                url = url.substring( 0, index ) + suffix;
            }
        }
        return url;
    }

    public static boolean isSymlink( File file )
        throws IOException
    {
        return !file.getAbsolutePath().equals( file.getCanonicalPath() );
    }

    public static String interpolate( String value, Model model )
        throws ReleaseExecutionException
    {
        if ( value != null && value.contains( "${" ) )
        {
            StringSearchInterpolator interpolator = new StringSearchInterpolator();
            List<String> pomPrefixes = Arrays.asList( "pom.", "project." );
            interpolator.addValueSource( new PrefixedObjectValueSource( pomPrefixes, model, false ) );
            interpolator.addValueSource( new MapBasedValueSource( model.getProperties() ) );
            interpolator.addValueSource( new ObjectBasedValueSource( model ) );
            try
            {
                value = interpolator.interpolate( value, new PrefixAwareRecursionInterceptor( pomPrefixes ) );
            }
            catch ( InterpolationException e )
            {
                throw new ReleaseExecutionException(
                                                     "Failed to interpolate " + value + " for project " + model.getId(),
                                                     e );
            }
        }
        return value;
    }
}
