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

import java.io.StringReader;

import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JDomParentTest {
    private SAXBuilder builder = new SAXBuilder();

    @Test
    void testGetArtifactId() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomParent(null).getArtifactId());
    }

    @Test
    void testGetGroupId() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomParent(null).getGroupId());
    }

    @Test
    void testGetRelativePath() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomParent(null).getRelativePath());
    }

    @Test
    void testGetVersion() throws Exception {
        String content = "<parent><version>1.0</version></parent>";
        Element parentElm = builder.build(new StringReader(content)).getRootElement();
        assertEquals("1.0", new JDomParent(parentElm).getVersion());
    }

    @Test
    void testSetArtifactId() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomParent(null).setArtifactId(null));
    }

    @Test
    void testSetGroupId() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomParent(null).setGroupId(null));
    }

    @Test
    void testSetRelativePath() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomParent(null).setRelativePath(null));
    }

    @Test
    void testSetVersionString() throws Exception {
        String content = "<parent></parent>";
        Element parentElm = builder.build(new StringReader(content)).getRootElement();
        assertNull(getVersion(parentElm));
        new JDomParent(parentElm).setVersion("VERSION");
        assertEquals("VERSION", getVersion(parentElm));
        new JDomParent(parentElm).setVersion(null);
        assertNull(getVersion(parentElm));
    }

    @Test
    void testGetId() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomParent(null).getId());
    }

    private String getVersion(Element parentElm) {
        return parentElm.getChildText("version", parentElm.getNamespace());
    }
}
