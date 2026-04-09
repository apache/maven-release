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
package org.apache.maven.shared.release.transform.domtrip;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Editor;
import eu.maveniverse.domtrip.Element;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomTripPropertiesTest {

    private DomTripProperties createProperties(String xml) {
        Document document = Document.of(xml);
        Editor editor = new Editor(document);
        return new DomTripProperties(document.root(), editor);
    }

    @Test
    void testSetPropertyExisting() {
        String xml = "<properties><KEY>OLD_VALUE</KEY></properties>";
        Document document = Document.of(xml);
        Editor editor = new Editor(document);
        Element root = document.root();

        assertEquals("OLD_VALUE", root.childTextTrimmed("KEY"));
        new DomTripProperties(root, editor).setProperty("KEY", "NEW_VALUE");
        assertEquals("NEW_VALUE", root.childTextTrimmed("KEY"));
    }

    @Test
    void testSetPropertyNew() {
        String xml = "<properties></properties>";
        Document document = Document.of(xml);
        Editor editor = new Editor(document);
        Element root = document.root();

        assertNull(root.childTextTrimmed("KEY"));
        new DomTripProperties(root, editor).setProperty("KEY", "VALUE");
        assertEquals("VALUE", root.childTextTrimmed("KEY"));
    }

    @Test
    void testGetPropertyMissing() {
        assertNull(createProperties("<properties></properties>").getProperty("KEY"));
    }

    @Test
    void testGetPropertyPresent() {
        assertEquals(
                "VALUE",
                createProperties("<properties><KEY>VALUE</KEY></properties>").getProperty("KEY"));
    }

    @Test
    void testGetPropertyDefault() {
        DomTripProperties props = createProperties("<properties></properties>");
        assertNull(props.getProperty("KEY", null));
        assertEquals("", props.getProperty("KEY", ""));
        assertEquals("DEFAULT", props.getProperty("KEY", "DEFAULT"));
    }

    @Test
    void testGetPropertyDefaultWithExisting() {
        DomTripProperties props = createProperties("<properties><KEY>VALUE</KEY></properties>");
        assertEquals("VALUE", props.getProperty("KEY", "DEFAULT"));
    }

    @Test
    void testContainsKeyPresent() {
        assertTrue(createProperties("<properties><KEY>VALUE</KEY></properties>").containsKey("KEY"));
    }

    @Test
    void testContainsKeyMissing() {
        assertFalse(createProperties("<properties></properties>").containsKey("KEY"));
    }

    @Test
    void testContainsKeyNonString() {
        assertFalse(createProperties("<properties></properties>").containsKey(42));
    }

    @Test
    void testLoadReader() {
        assertThrows(UnsupportedOperationException.class, () -> createProperties("<properties/>")
                .load((Reader) null));
    }

    @Test
    void testLoadInputStream() {
        assertThrows(UnsupportedOperationException.class, () -> createProperties("<properties/>")
                .load((InputStream) null));
    }

    @Test
    void testSave() {
        assertThrows(UnsupportedOperationException.class, () -> createProperties("<properties/>")
                .save(null, null));
    }

    @Test
    void testStoreWriter() {
        assertThrows(UnsupportedOperationException.class, () -> createProperties("<properties/>")
                .store((Writer) null, null));
    }

    @Test
    void testStoreOutputStream() {
        assertThrows(UnsupportedOperationException.class, () -> createProperties("<properties/>")
                .store((OutputStream) null, null));
    }

    @Test
    void testLoadFromXML() {
        assertThrows(UnsupportedOperationException.class, () -> createProperties("<properties/>")
                .loadFromXML(null));
    }

    @Test
    void testStoreToXML() {
        assertThrows(UnsupportedOperationException.class, () -> createProperties("<properties/>")
                .storeToXML(null, null));
    }

    @Test
    void testStoreToXMLEncoded() {
        assertThrows(UnsupportedOperationException.class, () -> createProperties("<properties/>")
                .storeToXML(null, null, (String) null));
    }

    @Test
    void testPropertyNames() {
        assertThrows(UnsupportedOperationException.class, () -> createProperties("<properties/>")
                .propertyNames());
    }

    @Test
    void testStringPropertyNames() {
        assertThrows(UnsupportedOperationException.class, () -> createProperties("<properties/>")
                .stringPropertyNames());
    }

    @Test
    void testListPrintStream() {
        assertThrows(UnsupportedOperationException.class, () -> createProperties("<properties/>")
                .list((PrintStream) null));
    }

    @Test
    void testListPrintWriter() {
        assertThrows(UnsupportedOperationException.class, () -> createProperties("<properties/>")
                .list((PrintWriter) null));
    }
}
