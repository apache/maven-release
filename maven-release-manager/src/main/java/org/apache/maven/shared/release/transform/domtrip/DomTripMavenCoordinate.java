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
import org.apache.maven.shared.release.transform.MavenCoordinate;

/**
 * DomTrip implementation of {@link MavenCoordinate}.
 *
 * @since 3.4
 */
public class DomTripMavenCoordinate implements MavenCoordinate {
    private final Element element;
    private final Editor editor;

    public DomTripMavenCoordinate(Element element, Editor editor) {
        this.element = element;
        this.editor = editor;
    }

    @Override
    public String getGroupId() {
        return element.childTextTrimmed("groupId");
    }

    @Override
    public String getArtifactId() {
        return element.childTextTrimmed("artifactId");
    }

    @Override
    public String getVersion() {
        return element.childElement("version").map(e -> e.textContentTrimmed()).orElse(null);
    }

    @Override
    public void setVersion(String version) {
        element.childElement("version").ifPresent(e -> DomTripUtils.rewriteValue(editor, e, version));
    }

    @Override
    public String getName() {
        return element.name();
    }
}
