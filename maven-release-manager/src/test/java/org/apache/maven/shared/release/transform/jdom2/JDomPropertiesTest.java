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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;

import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JDomPropertiesTest {
    private SAXBuilder builder = new SAXBuilder();

    @Test
    void testSetProperty() throws Exception {
        String content = "<properties></properties>";
        Element propertiesElm = builder.build(new StringReader(content)).getRootElement();
        assertNull(getProperty(propertiesElm, "KEY"));

        // Adding not allowed, prepare properties
        content = "<properties><KEY>OLD_VALUE</KEY></properties>";
        propertiesElm = builder.build(new StringReader(content)).getRootElement();
        assertEquals("OLD_VALUE", getProperty(propertiesElm, "KEY"));
        new JDomProperties(propertiesElm).setProperty("KEY", "NEW_VALUE");
        assertEquals("NEW_VALUE", getProperty(propertiesElm, "KEY"));
    }

    @Test
    void testLoadReader() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomProperties(null).load((Reader) null));
    }

    @Test
    void testLoadInputStream() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomProperties(null).load((InputStream) null));
    }

    @Test
    void testSave() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomProperties(null).save(null, null));
    }

    @Test
    void testStoreWriter() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomProperties(null).store((Writer) null, null));
    }

    @Test
    void testStoreOutputStream() {
        assertThrows(
                UnsupportedOperationException.class, () -> new JDomProperties(null).store((OutputStream) null, null));
    }

    @Test
    void testLoadFromXML() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomProperties(null).loadFromXML(null));
    }

    @Test
    void testStoreToXML() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomProperties(null).storeToXML(null, null));
    }

    @Test
    void testStoreToXMLEncoded() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomProperties(null)
                .storeToXML(null, null, (String) null));
    }

    @Test
    void testGetProperty() throws Exception {
        String content = "<properties></properties>";
        Element propertiesElm = builder.build(new StringReader(content)).getRootElement();
        assertNull(new JDomProperties(propertiesElm).getProperty("KEY"));

        content = "<properties><KEY>VALUE</KEY></properties>";
        propertiesElm = builder.build(new StringReader(content)).getRootElement();
        assertEquals("VALUE", new JDomProperties(propertiesElm).getProperty("KEY"));
    }

    @Test
    void testGetPropertyDefault() throws Exception {
        String content = "<properties></properties>";
        Element propertiesElm = builder.build(new StringReader(content)).getRootElement();
        assertNull(new JDomProperties(propertiesElm).getProperty("KEY", null));
        assertEquals("", new JDomProperties(propertiesElm).getProperty("KEY", ""));
    }

    @Test
    void testPropertyNames() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomProperties(null).propertyNames());
    }

    @Test
    void testStringPropertyNames() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomProperties(null).stringPropertyNames());
    }

    @Test
    void testListPrintStream() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomProperties(null).list((PrintStream) null));
    }

    @Test
    void testListPrintWriter() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomProperties(null).list((PrintWriter) null));
    }

    private String getProperty(Element propertiesElm, String key) {
        return propertiesElm.getChildText(key, propertiesElm.getNamespace());
    }
}
