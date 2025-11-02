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
package org.apache.maven.shared.release.transform.jdom2;

import java.io.IOException;
import java.io.StringReader;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JDomParentTest {
    private SAXBuilder builder = new SAXBuilder();

    @Test
    public void testGetArtifactId() {
        assertThrows(UnsupportedOperationException.class, () ->
            new JDomParent(null).getArtifactId());
    }

    @Test
    public void testGetGroupId() {
        assertThrows(UnsupportedOperationException.class, () ->
            new JDomParent(null).getGroupId());
    }

    @Test
    public void testGetRelativePath() {
        assertThrows(UnsupportedOperationException.class, () ->
            new JDomParent(null).getRelativePath());
    }

    @Test
    public void testGetVersion() throws JDOMException, IOException {
        String content = "<parent><version>1.0</version></parent>";
        Element parentElm = builder.build(new StringReader(content)).getRootElement();

        assertEquals("1.0", new JDomParent(parentElm).getVersion());
    }

    @Test
    public void testSetArtifactId() {
        assertThrows(UnsupportedOperationException.class, () ->
            new JDomParent(null).setArtifactId(null));
    }

    @Test
    public void testSetGroupId() {
        assertThrows(UnsupportedOperationException.class, () ->
            new JDomParent(null).setGroupId(null));
    }

    @Test
    public void testSetRelativePath() {
        assertThrows(UnsupportedOperationException.class, () ->
            new JDomParent(null).setRelativePath(null));
    }

    @Test
    public void testSetVersionString() throws Exception {
        String content = "<parent></parent>";
        Element parentElm = builder.build(new StringReader(content)).getRootElement();

        assertNull(getVersion(parentElm));

        new JDomParent(parentElm).setVersion("VERSION");
        assertEquals("VERSION", getVersion(parentElm));

        new JDomParent(parentElm).setVersion(null);
        assertNull(getVersion(parentElm));
    }

    @Test
    public void testGetId() {
        assertThrows(UnsupportedOperationException.class, () ->
            new JDomParent(null).getId());
    }

    private String getVersion(Element parentElm) {
        return parentElm.getChildText("version", parentElm.getNamespace());
    }
}
