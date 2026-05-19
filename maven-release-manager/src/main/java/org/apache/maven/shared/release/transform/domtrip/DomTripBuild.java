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
import org.apache.maven.model.Build;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Resource;

/**
 * DomTrip implementation of poms BUILD element.
 *
 * @since 3.4
 */
public class DomTripBuild extends Build {
    private final Element build;
    private final Editor editor;

    public DomTripBuild(Element build, Editor editor) {
        this.build = build;
        this.editor = editor;
    }

    @Override
    public void addExtension(Extension extension) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Extension> getExtensions() {
        return build.childElement("extensions")
                .map(exts -> exts.childElements("extension")
                        .map(ext -> (Extension) new DomTripExtension(ext, editor))
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    @Override
    public String getOutputDirectory() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getScriptSourceDirectory() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSourceDirectory() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getTestOutputDirectory() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getTestSourceDirectory() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeExtension(Extension extension) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setExtensions(List<Extension> extensions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setOutputDirectory(String outputDirectory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setScriptSourceDirectory(String scriptSourceDirectory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSourceDirectory(String sourceDirectory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTestOutputDirectory(String testOutputDirectory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTestSourceDirectory(String testSourceDirectory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addFilter(String string) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addResource(Resource resource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addTestResource(Resource resource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDefaultGoal() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDirectory() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getFilters() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getFinalName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Resource> getResources() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Resource> getTestResources() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeFilter(String string) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeResource(Resource resource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeTestResource(Resource resource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDefaultGoal(String defaultGoal) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDirectory(String directory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFilters(List<String> filters) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFinalName(String finalName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setResources(List<Resource> resources) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTestResources(List<Resource> testResources) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PluginManagement getPluginManagement() {
        return build.childElement("pluginManagement")
                .map(elm -> (PluginManagement) new DomTripPluginManagement(elm, editor))
                .orElse(null);
    }

    @Override
    public void setPluginManagement(PluginManagement pluginManagement) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addPlugin(Plugin plugin) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Plugin> getPlugins() {
        return build.childElement("plugins")
                .map(plugins -> plugins.childElements("plugin")
                        .map(plugin -> (Plugin) new DomTripPlugin(plugin, editor))
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    @Override
    public void removePlugin(Plugin plugin) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPlugins(List<Plugin> plugins) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flushPluginMap() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Plugin> getPluginsAsMap() {
        throw new UnsupportedOperationException();
    }
}
