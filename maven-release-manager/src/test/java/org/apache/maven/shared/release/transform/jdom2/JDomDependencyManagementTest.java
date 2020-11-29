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

import java.io.StringReader;

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.junit.Test;

public class JDomDependencyManagementTest
{
    private SAXBuilder builder = new SAXBuilder();

    @Test
    public void testGetDependencies() throws Exception
    {
        String content = "<dependencyManamgement></dependencyManamgement>";
        Document document = builder.build( new StringReader( content ) );
        assertNotNull( new JDomDependencyManagement( document.getRootElement() ).getDependencies() );
        assertEquals( 0, new JDomDependencyManagement( document.getRootElement() ).getDependencies().size() );

        content = "<dependencyManamgement><dependencies/></dependencyManamgement>";
        document = builder.build( new StringReader( content ) );
        assertEquals( 0, new JDomDependencyManagement( document.getRootElement() ).getDependencies().size() );

        content = "<dependencyManamgement><dependencies><dependency/></dependencies></dependencyManamgement>";
        document = builder.build( new StringReader( content ) );
        assertEquals( 1, new JDomDependencyManagement( document.getRootElement() ).getDependencies().size() );
    }

    // All other methods throw UnsupportedOperationException

    @Test( expected = UnsupportedOperationException.class )
    public void testAddDependency()
    {
        new JDomDependencyManagement( null ).addDependency( null );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testRemoveDependency()
    {
        new JDomDependencyManagement( null ).addDependency( null );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testSetDependenciesListOfDependency()
    {
        new JDomDependencyManagement( null ).setDependencies( null );
    }
}
