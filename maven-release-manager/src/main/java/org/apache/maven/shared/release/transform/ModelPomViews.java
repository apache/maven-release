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
package org.apache.maven.shared.release.transform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.maven.model.Build;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Model;
import org.apache.maven.model.ModelBase;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;

/**
 * Exposes a Maven 3 {@link Model} returned by {@link ModelETL#getModel()} as a {@link PomModel}. The list elements
 * of the model must implement {@link MavenCoordinate}; any other element throws
 * {@link UnsupportedOperationException} when its list is read. Every call reads through to the model object, which
 * edits the document.
 *
 * @since 3.4
 */
public final class ModelPomViews {
    private ModelPomViews() {}

    /**
     * @param model the model of a {@link ModelETL}
     * @return the view of that model
     */
    public static PomModel of(Model model) {
        return new ModelView(model);
    }

    private static List<MavenCoordinate> coordinates(List<?> objects) {
        List<MavenCoordinate> coordinates = new ArrayList<>(objects.size());
        for (Object object : objects) {
            if (object instanceof MavenCoordinate) {
                coordinates.add((MavenCoordinate) object);
            } else {
                throw new UnsupportedOperationException();
            }
        }
        return coordinates;
    }

    private static List<PomPlugin> plugins(List<Plugin> plugins) {
        List<PomPlugin> views = new ArrayList<>(plugins.size());
        for (Plugin plugin : plugins) {
            views.add(new PluginView(plugin));
        }
        return views;
    }

    private abstract static class ModelBaseView<T extends ModelBase> implements PomModelBase {
        final T delegate;

        ModelBaseView(T delegate) {
            this.delegate = delegate;
        }

        @Override
        public List<MavenCoordinate> getDependencies() {
            return coordinates(delegate.getDependencies());
        }

        @Override
        public List<MavenCoordinate> getManagedDependencies() {
            return delegate.getDependencyManagement() == null
                    ? Collections.emptyList()
                    : coordinates(delegate.getDependencyManagement().getDependencies());
        }

        @Override
        public boolean hasDependencyManagement() {
            return delegate.getDependencyManagement() != null;
        }

        @Override
        public List<MavenCoordinate> getReportPlugins() {
            return delegate.getReporting() == null
                    ? Collections.emptyList()
                    : coordinates(delegate.getReporting().getPlugins());
        }

        @Override
        public boolean hasReporting() {
            return delegate.getReporting() != null;
        }
    }

    private static final class ModelView extends ModelBaseView<Model> implements PomModel {
        ModelView(Model model) {
            super(model);
        }

        @Override
        public PomBuild getBuild() {
            Build build = delegate.getBuild();
            return build == null ? null : new BuildView(build);
        }

        @Override
        public MavenCoordinate getParent() {
            Parent parent = delegate.getParent();
            if (parent == null) {
                return null;
            }
            return parent instanceof MavenCoordinate ? (MavenCoordinate) parent : new ParentView(parent);
        }

        @Override
        public List<PomProfile> getProfiles() {
            List<PomProfile> profiles = new ArrayList<>();
            for (Profile profile : delegate.getProfiles()) {
                profiles.add(new ProfileView(profile));
            }
            return profiles;
        }

        @Override
        public Properties getProperties() {
            return delegate.getProperties();
        }

        @Override
        public void setVersion(String version) {
            delegate.setVersion(version);
        }
    }

    private static final class ProfileView extends ModelBaseView<Profile> implements PomProfile {
        ProfileView(Profile profile) {
            super(profile);
        }

        @Override
        public PomBuildBase getBuild() {
            BuildBase build = delegate.getBuild();
            return build == null ? null : new BuildView(build);
        }
    }

    private static final class BuildView implements PomBuild {
        private final BuildBase delegate;

        BuildView(BuildBase build) {
            this.delegate = build;
        }

        @Override
        public List<MavenCoordinate> getExtensions() {
            return delegate instanceof Build
                    ? coordinates(((Build) delegate).getExtensions())
                    : Collections.emptyList();
        }

        @Override
        public List<PomPlugin> getPlugins() {
            return plugins(delegate.getPlugins());
        }

        @Override
        public List<PomPlugin> getManagedPlugins() {
            return delegate.getPluginManagement() == null
                    ? Collections.emptyList()
                    : plugins(delegate.getPluginManagement().getPlugins());
        }

        @Override
        public boolean hasPluginManagement() {
            return delegate.getPluginManagement() != null;
        }
    }

    private static final class PluginView implements PomPlugin {
        private final Plugin delegate;

        PluginView(Plugin plugin) {
            this.delegate = plugin;
        }

        @Override
        public List<MavenCoordinate> getDependencies() {
            return coordinates(delegate.getDependencies());
        }

        @Override
        public String getGroupId() {
            return delegate.getGroupId();
        }

        @Override
        public String getArtifactId() {
            return delegate.getArtifactId();
        }

        @Override
        public String getVersion() {
            return delegate.getVersion();
        }

        @Override
        public void setVersion(String version) {
            delegate.setVersion(version);
        }

        @Override
        public String getName() {
            return "plugin";
        }
    }

    private static final class ParentView implements MavenCoordinate {
        private final Parent delegate;

        ParentView(Parent parent) {
            this.delegate = parent;
        }

        @Override
        public String getGroupId() {
            return delegate.getGroupId();
        }

        @Override
        public String getArtifactId() {
            return delegate.getArtifactId();
        }

        @Override
        public String getVersion() {
            return delegate.getVersion();
        }

        @Override
        public void setVersion(String version) {
            delegate.setVersion(version);
        }

        @Override
        public String getName() {
            return "parent";
        }
    }
}
