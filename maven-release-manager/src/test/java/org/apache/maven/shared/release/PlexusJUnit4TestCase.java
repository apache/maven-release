package org.apache.maven.shared.release;

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

import static org.junit.Assert.fail;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.context.Context;
import org.junit.After;
import org.junit.Before;

/**
 * Based on PlexusTestCase from org.codehaus.plexus:plexus-container-default
 * 
 * @author Robert Scholte
 */
public abstract class PlexusJUnit4TestCase
{
    private PlexusContainer container;

    private static String basedir;

    @Before
    public void setUp()
        throws Exception
    {
        InputStream configuration = null;

        try
        {
            configuration = getCustomConfiguration();

            if ( configuration == null )
            {
                configuration = getConfiguration();
            }
        }
        catch ( Exception e )
        {
            System.out.println( "Error with configuration:" );

            System.out.println( "configuration = " + configuration );

            fail( e.getMessage() );
        }

        basedir = getBasedir();

        container = createContainerInstance();

        container.addContextValue( "basedir", getBasedir() );

        // this method was deprecated
        customizeContext();

        customizeContext( getContext() );

        boolean hasPlexusHome = getContext().contains( "plexus.home" );

        if ( !hasPlexusHome )
        {
            File f = getTestFile( "target/plexus-home" );

            if ( !f.isDirectory() )
            {
                f.mkdir();
            }

            getContext().put( "plexus.home", f.getAbsolutePath() );
        }

        if ( configuration != null )
        {
            container.setConfigurationResource( new InputStreamReader( configuration ) );
        }

        container.initialize();

        container.start();
    }

    protected PlexusContainer createContainerInstance()
    {
        return new DefaultPlexusContainer();
    }

    private Context getContext()
    {
        return container.getContext();
    }

    //!!! this should probably take a context as a parameter so that the
    //    user is not forced to do getContainer().addContextValue(..)
    //    this would require a change to PlexusContainer in order to get
    //    hold of the context ...
    // @deprecated use void customizeContext( Context context )
    protected void customizeContext()
        throws Exception
    {
    }


    protected void customizeContext( Context context )
        throws Exception
    {
    }


    protected InputStream getCustomConfiguration()
        throws Exception
    {
        return null;
    }

    @After
    public void tearDown()
        throws Exception
    {
        container.dispose();

        container = null;
    }

    protected PlexusContainer getContainer()
    {
        return container;
    }

    protected InputStream getConfiguration()
        throws Exception
    {
        return getConfiguration( null );
    }

    protected InputStream getConfiguration( String subname )
        throws Exception
    {
        String className = getClass().getName();

        String base = className.substring( className.lastIndexOf( "." ) + 1 );

        String config = null;

        if ( subname == null || subname.equals( "" ) )
        {
            config = base + ".xml";
        }
        else
        {
            config = base + "-" + subname + ".xml";
        }
        
        InputStream configStream = getResourceAsStream( config );

        return configStream;
    }

    protected InputStream getResourceAsStream( String resource )
    {
        return getClass().getResourceAsStream( resource );
    }

    protected ClassLoader getClassLoader()
    {
        return getClass().getClassLoader();
    }

    // ----------------------------------------------------------------------
    // Container access
    // ----------------------------------------------------------------------

    protected Object lookup( String componentKey )
        throws Exception
    {
        return getContainer().lookup( componentKey );
    }

    protected Object lookup( String role, String id )
        throws Exception
    {
        return getContainer().lookup( role, id );
    }

    protected void release( Object component )
        throws Exception
    {
        getContainer().release( component );
    }

    // ----------------------------------------------------------------------
    // Helper methods for sub classes
    // ----------------------------------------------------------------------

    public static File getTestFile( String path )
    {
        return new File( getBasedir(), path );
    }

    public static File getTestFile( String basedir, String path )
    {
        File basedirFile = new File( basedir );

        if ( ! basedirFile.isAbsolute() )
        {
            basedirFile = getTestFile( basedir );
        }

        return new File( basedirFile, path );
    }

    public static String getTestPath( String path )
    {
        return getTestFile( path ).getAbsolutePath();
    }

    public static String getTestPath( String basedir, String path )
    {
        return getTestFile( basedir, path ).getAbsolutePath();
    }

    public static String getBasedir()
    {
        if ( basedir != null )
        {
            return basedir;
        }

        basedir = System.getProperty( "basedir" );

        if ( basedir == null )
        {
            basedir = new File( "" ).getAbsolutePath();
        }

        return basedir;
    }
}
