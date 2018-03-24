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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.apache.maven.shared.release.config.ReleaseUtils;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.MapBasedValueSource;
import org.codehaus.plexus.interpolation.ObjectBasedValueSource;
import org.codehaus.plexus.interpolation.PrefixAwareRecursionInterceptor;
import org.codehaus.plexus.interpolation.PrefixedObjectValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;

/**
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 */
public class ReleaseUtil
{
    @SuppressWarnings( "checkstyle:constantname" )
    public static final String RELEASE_POMv4 = "release-pom.xml";

    @SuppressWarnings( "checkstyle:constantname" )
    public static final String POMv4 = "pom.xml";

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
        try ( Reader reader = ReaderFactory.newXmlReader( file ) )
        {
            return normalizeLineEndings( IOUtil.toString( reader ), ls );
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
        int parentLevels = Paths.get( releaseDescriptor.getPomFileName() ).getNameCount() - 1;

        String url = releaseDescriptor.getScmSourceUrl();
        url = realignScmUrl( parentLevels, url );

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setWorkingDirectory( releaseDescriptor.getWorkingDirectory() );
        builder.setScmSourceUrl( url );

        return ReleaseUtils.buildReleaseDescriptor( builder );
    }

    public static int getBaseWorkingDirectoryParentCount( final Path baseDirectory, final Path workingDirectory )
    {
        return Math.max( 0, workingDirectory.normalize().getNameCount() - baseDirectory.normalize().getNameCount() );
    }

    public static String realignScmUrl( int parentLevels, String url )
    {
        if ( !StringUtils.isEmpty( url ) )
        {
            // normalize
            url = url.replaceAll( "/\\./", "/" ).replaceAll( "/\\.$", "" ).
                            replaceAll( "/[^/]+/\\.\\./", "/" ).replaceAll( "/[^/]+/\\.\\.$", "" );

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
