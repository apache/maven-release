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

import eu.maveniverse.domtrip.Editor;
import eu.maveniverse.domtrip.Element;
import org.apache.maven.model.Scm;

/**
 * DomTrip implementation of poms SCM element.
 *
 * @since 3.4
 */
public class DomTripScm extends Scm {
    private final Element scm;
    private final Editor editor;

    DomTripScm(Element scm, Editor editor) {
        this.scm = scm;
        this.editor = editor;
    }

    @Override
    public String getConnection() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setConnection(String connection) {
        DomTripUtils.rewriteElement(editor, "connection", connection, scm);
    }

    @Override
    public String getDeveloperConnection() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDeveloperConnection(String developerConnection) {
        DomTripUtils.rewriteElement(editor, "developerConnection", developerConnection, scm);
    }

    @Override
    public String getTag() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTag(String tag) {
        DomTripUtils.rewriteElement(editor, "tag", tag, scm);
    }

    @Override
    public String getUrl() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setUrl(String url) {
        DomTripUtils.rewriteElement(editor, "url", url, scm);
    }
}
