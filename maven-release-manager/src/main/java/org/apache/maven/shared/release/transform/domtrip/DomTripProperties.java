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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Enumeration;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import java.util.Set;

import eu.maveniverse.domtrip.Editor;
import eu.maveniverse.domtrip.Element;

/**
 * DomTrip implementation of poms PROPERTIES element.
 * Only few methods are properly implemented as the underlying data structure of
 * {@link java.util.Hashtable} is never populated.
 *
 * @since 3.4
 */
public class DomTripProperties extends Properties {
    private final Element properties;
    private final Editor editor;

    public DomTripProperties(Element properties, Editor editor) {
        this.properties = properties;
        this.editor = editor;
    }

    @Override
    public synchronized Object put(Object key, Object value) {
        Element property = properties.childElement((String) key).orElse(null);
        String oldValue;
        if (property != null) {
            oldValue = property.textContentTrimmed();
            DomTripUtils.rewriteValue(editor, property, (String) value);
        } else {
            oldValue = null;
            editor.addElement(properties, (String) key, (String) value);
        }
        return oldValue;
    }

    @Override
    public synchronized void load(Reader reader) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void load(InputStream inStream) throws IOException {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void save(OutputStream out, String comments) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void store(Writer writer, String comments) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void store(OutputStream out, String comments) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void loadFromXML(InputStream in) throws IOException, InvalidPropertiesFormatException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void storeToXML(OutputStream os, String comment) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void storeToXML(OutputStream os, String comment, String encoding) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getProperty(String key) {
        return properties.childElement(key).map(Element::textContentTrimmed).orElse(null);
    }

    @Override
    public boolean containsKey(Object key) {
        if (key instanceof String) {
            return properties.childElement((String) key).isPresent();
        }
        return false;
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        String property = getProperty(key);
        return property == null ? defaultValue : property;
    }

    @Override
    public Enumeration<?> propertyNames() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> stringPropertyNames() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void list(PrintStream out) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void list(PrintWriter out) {
        throw new UnsupportedOperationException();
    }
}
