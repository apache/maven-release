package org.apache.maven.shared.release.transform.jdom2;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.shared.release.transform.MavenCoordinate;
import org.jdom2.Element;

/**
 * JDOM2 implementation of poms PLUGIN element
 *
 * @author Robert Scholte
 * @since 3.0
 */
public class JDomPlugin extends Plugin implements MavenCoordinate
{
    private Element plugin;
    private final MavenCoordinate coordinate;

    public JDomPlugin( Element plugin )
    {
        this.plugin = plugin;
        this.coordinate = new JDomMavenCoordinate( plugin );
    }

    @Override
    public void addDependency( Dependency dependency )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addExecution( PluginExecution pluginExecution )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getArtifactId()
    {
        return coordinate.getArtifactId();
    }

    @Override
    public List<Dependency> getDependencies()
    {
        Element dependenciesElm = plugin.getChild( "dependencies", plugin.getNamespace() );
        if ( dependenciesElm == null )
        {
            return Collections.emptyList();
        }
        else
        {
            List<Element> dependencyElms =
                dependenciesElm.getChildren( "dependency", plugin.getNamespace() );

            List<Dependency> dependencies = new ArrayList<>( dependencyElms.size() );

            for ( Element dependencyElm : dependencyElms )
            {
                dependencies.add( new JDomDependency( dependencyElm ) );
            }

            return dependencies;
        }
    }

    @Override
    public List<PluginExecution> getExecutions()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getGoals()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getGroupId()
    {
        return coordinate.getGroupId();
    }

    @Override
    public String getVersion()
    {
        return coordinate.getVersion();
    }

    @Override
    public boolean isExtensions()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeDependency( Dependency dependency )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeExecution( PluginExecution pluginExecution )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setArtifactId( String artifactId )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDependencies( List<Dependency> dependencies )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setExecutions( List<PluginExecution> executions )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setExtensions( boolean extensions )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setGoals( Object goals )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setGroupId( String groupId )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setVersion( String version )
    {
        coordinate.setVersion( version );
    }

    @Override
    public void flushExecutionMap()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, PluginExecution>  getExecutionsAsMap()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName()
    {
        return "plugin";
    }
}
