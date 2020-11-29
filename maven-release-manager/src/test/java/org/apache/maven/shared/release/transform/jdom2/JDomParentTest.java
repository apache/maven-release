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

import static org.junit.Assert.*;

import java.io.StringReader;

import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.Test;

public class JDomParentTest
{
    private SAXBuilder builder = new SAXBuilder();

    @Test( expected = UnsupportedOperationException.class )
    public void testGetArtifactId()
    {
        new JDomParent( null ).getArtifactId();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testGetGroupId()
    {
        new JDomParent( null ).getGroupId();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testGetRelativePath()
    {
        new JDomParent( null ).getRelativePath();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testGetVersion()
    {
        new JDomParent( null ).getVersion();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testSetArtifactId()
    {
        new JDomParent( null ).setArtifactId( null );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testSetGroupId()
    {
        new JDomParent( null ).setGroupId( null );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testSetRelativePath()
    {
        new JDomParent( null ).setRelativePath( null );
    }

    @Test
    public void testSetVersionString() throws Exception
    {
        String content = "<parent></parent>";
        Element parentElm = builder.build( new StringReader( content ) ).getRootElement();

        assertNull( getVersion( parentElm ) );

        new JDomParent( parentElm ).setVersion( "VERSION" );
        assertEquals( "VERSION", getVersion( parentElm ) );

        new JDomParent( parentElm ).setVersion( null );
        assertNull( getVersion( parentElm ) );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testGetId()
    {
        new JDomParent( null ).getId();
    }

    private String getVersion( Element parentElm )
    {
        return parentElm.getChildText( "version", parentElm.getNamespace() );
    }
}
