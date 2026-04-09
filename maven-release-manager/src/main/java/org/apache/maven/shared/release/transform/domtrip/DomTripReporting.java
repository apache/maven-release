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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import eu.maveniverse.domtrip.Editor;
import eu.maveniverse.domtrip.Element;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.Reporting;

/**
 * DomTrip implementation of poms REPORTING element.
 *
 * @since 3.4
 */
public class DomTripReporting extends Reporting {

    private final Element reporting;
    private final Editor editor;

    public DomTripReporting(Element reporting, Editor editor) {
        this.reporting = reporting;
        this.editor = editor;
    }

    @Override
    public void addPlugin(ReportPlugin reportPlugin) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getOutputDirectory() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ReportPlugin> getPlugins() {
        return reporting
                .childElement("plugins")
                .map(plugins -> plugins.childElements("plugin")
                        .map(plugin -> (ReportPlugin) new DomTripReportPlugin(plugin, editor))
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    @Override
    public void removePlugin(ReportPlugin reportPlugin) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setOutputDirectory(String outputDirectory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPlugins(List<ReportPlugin> plugins) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flushReportPluginMap() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, ReportPlugin> getReportPluginsAsMap() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isExcludeDefaults() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setExcludeDefaults(boolean excludeDefaults) {
        throw new UnsupportedOperationException();
    }
}
