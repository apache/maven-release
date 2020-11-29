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

import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.jdom2.Element;

/**
 * JDOM2 implementation of poms PLUGINMANAGEMENT element
 *
 * @author Robert Scholte
 * @since 3.0
 */
public class JDomPluginManagement extends PluginManagement
{
    private final Element pluginManagement;

    public JDomPluginManagement( Element pluginManagement )
    {
        this.pluginManagement = pluginManagement;
    }

    @Override
    public void addPlugin( Plugin plugin )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Plugin> getPlugins()
    {
        Element pluginsElm = pluginManagement.getChild( "plugins", pluginManagement.getNamespace() );
        if ( pluginsElm == null )
        {
            return Collections.emptyList();
        }
        else
        {
            List<Element> pluginElms = pluginsElm.getChildren( "plugin", pluginManagement.getNamespace() );

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
