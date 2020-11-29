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

import org.apache.maven.model.Build;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Resource;
import org.jdom2.Element;
/**
 * JDOM2 implementation of poms BUILD element
 *
 * @author Robert Scholte
 * @since 3.0
 */
public class JDomBuild
    extends Build
{
    private final Element build;

    public JDomBuild( Element build )
    {
        this.build = build;
    }

    @Override
    public void addExtension( Extension extension )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Extension> getExtensions()
    {
        Element extensionsElm = build.getChild( "extensions", build.getNamespace() );
        if ( extensionsElm == null )
        {
            return Collections.emptyList();
        }
        else
        {
            List<Element> extensionElms = extensionsElm.getChildren( "extension", build.getNamespace() );

            List<Extension> extensions = new ArrayList<>( extensionElms.size() );
            for ( Element extensionElm : extensionElms )
            {
                extensions.add( new JDomExtension( extensionElm ) );
            }
            return extensions;
        }
    }

    @Override
    public String getOutputDirectory()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getScriptSourceDirectory()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSourceDirectory()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getTestOutputDirectory()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getTestSourceDirectory()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeExtension( Extension extension )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setExtensions( List<Extension> extensions )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setOutputDirectory( String outputDirectory )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setScriptSourceDirectory( String scriptSourceDirectory )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSourceDirectory( String sourceDirectory )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTestOutputDirectory( String testOutputDirectory )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTestSourceDirectory( String testSourceDirectory )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addFilter( String string )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addResource( Resource resource )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addTestResource( Resource resource )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDefaultGoal()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDirectory()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getFilters()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getFinalName()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Resource> getResources()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Resource> getTestResources()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeFilter( String string )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeResource( Resource resource )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeTestResource( Resource resource )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDefaultGoal( String defaultGoal )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDirectory( String directory )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFilters( List<String> filters )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFinalName( String finalName )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setResources( List<Resource> resources )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTestResources( List<Resource> testResources )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public PluginManagement getPluginManagement()
    {
        Element pluginManagementElm = build.getChild( "pluginManagement", build.getNamespace() );
        if ( pluginManagementElm == null )
        {
            return null;
        }
        else
        {
            return new JDomPluginManagement( pluginManagementElm );
        }
    }

    @Override
    public void setPluginManagement( PluginManagement pluginManagement )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addPlugin( Plugin plugin )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Plugin> getPlugins()
    {
        Element pluginsElm = build.getChild( "plugins", build.getNamespace() );
        if ( pluginsElm == null )
        {
            return Collections.emptyList();
        }
        else
        {
            List<Element> pluginElms =
                pluginsElm.getChildren( "plugin", build.getNamespace() );

            List<Plugin> plugins = new ArrayList<>( pluginElms.size() );

            for ( Element pluginElm : pluginElms )
            {
                plugins.add( new JDomPlugin( pluginElm ) );
            }

            return plugins;
        }
    }

    @Override
    public void removePlugin( Plugin plugin )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPlugins( List<Plugin> plugins )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flushPluginMap()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Plugin> getPluginsAsMap()
    {
        throw new UnsupportedOperationException();
    }
}
