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
import static org.junit.Assert.assertNull;

import java.io.StringReader;

import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.Test;

public class JDomExtensionTest
{
    private SAXBuilder builder = new SAXBuilder();

    @Test
    public void testGetArtifactId() throws Exception
    {
        String content = "<extension></extension>";
        Element extensionElm = builder.build( new StringReader( content ) ).getRootElement();
        assertNull( new JDomExtension( extensionElm ).getArtifactId() );

        content = "<extension><artifactId>ARTIFACTID</artifactId></extension>";
        extensionElm = builder.build( new StringReader( content ) ).getRootElement();
        assertEquals( "ARTIFACTID", new JDomExtension( extensionElm ).getArtifactId() );
    }

    @Test
    public void testGetGroupId() throws Exception
    {
        String content = "<extension></extension>";
        Element extensionElm = builder.build( new StringReader( content ) ).getRootElement();
        assertNull( new JDomExtension( extensionElm ).getGroupId() );

        content = "<extension><groupId>GROUPID</groupId></extension>";
        extensionElm = builder.build( new StringReader( content ) ).getRootElement();
        assertEquals( "GROUPID", new JDomExtension( extensionElm ).getGroupId() );
    }

    @Test
    public void testGetVersion() throws Exception
    {
        String content = "<extension></extension>";
        Element extensionElm = builder.build( new StringReader( content ) ).getRootElement();
        assertNull( new JDomExtension( extensionElm ).getVersion() );

        content = "<extension><version>VERSION</version></extension>";
        extensionElm = builder.build( new StringReader( content ) ).getRootElement();
        assertEquals( "VERSION", new JDomExtension( extensionElm ).getVersion() );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testSetArtifactId()
    {
        new JDomExtension( null ).setArtifactId( null );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testSetGroupId()
    {
        new JDomExtension( null ).setGroupId( null );
    }

    @Test
    public void testSetVersion() throws Exception
    {
        String content = "<extension><version>OLD_VERSION</version></extension>";
        Element extensionElm = builder.build( new StringReader( content ) ).getRootElement();
        new JDomExtension( extensionElm ).setVersion( "NEW_VERSION" );
        assertEquals( "NEW_VERSION", getVersion( extensionElm ) );
    }

    @Test
    public void testGetName()
    {
        assertEquals( "extension", new JDomExtension( null ).getName() );
    }

    private String getVersion( Element extensionElm )
    {
        return extensionElm.getChildTextTrim( "version", extensionElm.getNamespace() );
    }
}
