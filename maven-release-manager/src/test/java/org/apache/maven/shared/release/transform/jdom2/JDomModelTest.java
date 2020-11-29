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

import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.Test;

public class JDomModelTest
{
    private SAXBuilder builder = new SAXBuilder();

    @Test
    public void testGetScm() throws Exception
    {
        String content = "<project></project>";
        Document document = builder.build( new StringReader( content ) );
        assertNull( new JDomModel( document ).getScm() );
    }

    @Test
    public void testSetScm() throws Exception
    {
        String content = "<project></project>";
        Document document = builder.build( new StringReader( content ) );
        Model model = new JDomModel( document );
        assertNull( model.getScm() );

        model.setScm( new Scm() );
        assertNotNull( model.getScm() );

        model.setScm( null );
        assertNull( model.getScm() );
    }

    @Test
    public void testSetVersion() throws Exception
    {
        String content = "<project></project>";
        Element projectElm = builder.build( new StringReader( content ) ).getRootElement();
        Model model = new JDomModel( projectElm );
        assertNull( model.getVersion() );

        model.setVersion( "VERSION" );
        assertEquals( "VERSION", getVersion( projectElm ) );

        model.setVersion( null );
        assertNull( model.getVersion() );

        // inherit from parent
        // this business logic might need to moved.
        content = "<project><parent><version>PARENT_VERSION</version></parent></project>";
        projectElm = builder.build( new StringReader( content ) ).getRootElement();
        model = new JDomModel( projectElm );
        assertNull( model.getVersion() );

        model.setVersion( "PARENT_VERSION" );
        assertNull( getVersion( projectElm ) );

        model.setVersion( "VERSION" );
        assertEquals( "VERSION", getVersion( projectElm ) );

        model.setVersion( null );
        assertNull( model.getVersion() );
    }

    private String getVersion( Element projectElm )
    {
        return projectElm.getChildText( "version", projectElm.getNamespace() );
    }

}
