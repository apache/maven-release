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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.StringReader;

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.junit.Test;

public class JDomBuildTest
{
    private SAXBuilder builder = new SAXBuilder();

    @Test
    public void testGetExtensions() throws Exception
    {
        String content = "<build></build>";
        Document document = builder.build( new StringReader( content ) );
        assertNotNull( new JDomBuild( document.getRootElement() ).getExtensions() );
        assertEquals( 0, new JDomBuild( document.getRootElement() ).getExtensions().size() );

        content = "<build><extensions/></build>";
        document = builder.build( new StringReader( content ) );
        assertEquals( 0, new JDomBuild( document.getRootElement() ).getExtensions().size() );

        content = "<build><extensions><extension/></extensions></build>";
        document = builder.build( new StringReader( content ) );
        assertEquals( 1, new JDomBuild( document.getRootElement() ).getExtensions().size() );

    }

    @Test
    public void testGetPluginManagement() throws Exception
    {
        String content = "<build></build>";
        Document document = builder.build( new StringReader( content ) );
        assertNull( new JDomBuild( document.getRootElement() ).getPluginManagement() );

        content = "<build><pluginManagement/></build>";
        document = builder.build( new StringReader( content ) );
        assertNotNull( new JDomBuild( document.getRootElement() ).getPluginManagement() );
    }

    @Test
    public void testGetPlugins() throws Exception
    {
        String content = "<build></build>";
        Document document = builder.build( new StringReader( content ) );
        assertNotNull( new JDomBuild( document.getRootElement() ).getPlugins() );
        assertEquals( 0, new JDomBuild( document.getRootElement() ).getPlugins().size() );

        content = "<build><plugins/></build>";
        document = builder.build( new StringReader( content ) );
        assertEquals( 0, new JDomBuild( document.getRootElement() ).getPlugins().size() );

        content = "<build><plugins><plugin/></plugins></build>";
        document = builder.build( new StringReader( content ) );
        assertEquals( 1, new JDomBuild( document.getRootElement() ).getPlugins().size() );
    }

    // All other methods throw UnsupportedOperationException

    @Test( expected = UnsupportedOperationException.class )
    public void testFlushPluginMap()
    {
        new JDomBuild( null ).flushPluginMap();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testAddExtension()
    {
        new JDomBuild( null ).addExtension( null );;
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testGetOutputDirectory()
    {
        new JDomBuild( null ).getOutputDirectory();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testGetScriptSourceDirectory()
    {
        new JDomBuild( null ).getScriptSourceDirectory();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testGetSourceDirectory()
    {
        new JDomBuild( null ).getSourceDirectory();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testGetTestOutputDirectory()
    {
        new JDomBuild( null ).getTestOutputDirectory();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testGetTestSourceDirectory()
    {
        new JDomBuild( null ).getTestSourceDirectory();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testRemoveExtension()
    {
        new JDomBuild( null ).removeExtension( null );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testSetExtensions()
    {
        new JDomBuild( null ).setExtensions( null );;
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testSetOutputDirectory()
    {
        new JDomBuild( null ).setOutputDirectory( null );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testSetScriptSourceDirectory()
    {
        new JDomBuild( null ).setScriptSourceDirectory( null );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testSetSourceDirectory()
    {
        new JDomBuild( null ).setSourceDirectory( null );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testSetTestOutputDirectoryString()
    {
        new JDomBuild( null ).setTestOutputDirectory( null );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testSetTestSourceDirectory()
    {
        new JDomBuild( null ).setTestSourceDirectory( null );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testAddFilter()
    {
        new JDomBuild( null ).addFilter( null );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testAddResource()
    {
        new JDomBuild( null ).addResource( null );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testAddTestResource()
    {
        new JDomBuild( null ).addTestResource( null );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testGetDefaultGoal()
    {
        new JDomBuild( null ).getDefaultGoal();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testGetDirectory()
    {
        new JDomBuild( null ).getDirectory();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testGetFilters()
    {
        new JDomBuild( null ).getFilters();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testGetFinalName()
    {
        new JDomBuild( null ).getFinalName();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testGetResources()
    {
        new JDomBuild( null ).getResources();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testGetTestResources()
    {
        new JDomBuild( null ).getTestResources();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testRemoveFilter()
    {
        new JDomBuild( null ).removeFilter( null );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testRemoveResource()
    {
        new JDomBuild( null ).removeResource( null );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testRemoveTestResource()
    {
        new JDomBuild( null ).removeTestResource( null );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testSetDefaultGoal()
    {
        new JDomBuild( null ).setDefaultGoal( null );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testSetDirectory()
    {
        new JDomBuild( null ).setDirectory( null );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testSetFilters()
    {
        new JDomBuild( null ).setFilters( null );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testSetFinalName()
    {
        new JDomBuild( null ).setFinalName( null );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testSetResources()
    {
        new JDomBuild( null ).setResources( null );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testSetTestResources()
    {
        new JDomBuild( null ).setTestResources( null );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testSetPluginManagement()
    {
        new JDomBuild( null ).setPluginManagement( null );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testAddPlugin()
    {
        new JDomBuild( null ).addPlugin( null );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testRemovePlugin()
    {
        new JDomBuild( null ).removePlugin( null );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testSetPlugins()
    {
        new JDomBuild( null ).setPlugins( null );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testGetPluginsAsMap()
    {
        new JDomBuild( null ).getPluginsAsMap();
    }
}
