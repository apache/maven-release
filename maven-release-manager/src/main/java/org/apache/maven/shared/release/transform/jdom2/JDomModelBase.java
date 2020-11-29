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

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.jdom2.Element;

/**
 *
 * @author Robert Scholte
 * @since 3.0
 */
public class JDomModelBase
{
    private final Element modelBase;

    public JDomModelBase( Element modelBase )
    {
        this.modelBase = modelBase;
    }

    public Build getBuild()
    {
        Element elm = modelBase.getChild( "build", modelBase.getNamespace() );
        if ( elm == null )
        {
            return null;
        }
        else
        {
            // this way build setters change DOM tree immediately
            return new JDomBuild( elm );
        }
    }

    public List<Dependency> getDependencies()
    {
        Element dependenciesElm = modelBase.getChild( "dependencies", modelBase.getNamespace() );
        if ( dependenciesElm == null )
        {
            return Collections.emptyList();
        }
        else
        {
            List<Element> dependencyElms = dependenciesElm.getChildren( "dependency", modelBase.getNamespace() );

            List<Dependency> dependencies = new ArrayList<>( dependencyElms.size() );

            for ( Element dependencyElm : dependencyElms )
            {
                dependencies.add( new JDomDependency( dependencyElm ) );
            }

            return dependencies;
        }
    }

    public DependencyManagement getDependencyManagement()
    {
        Element elm = modelBase.getChild( "dependencyManagement", modelBase.getNamespace() );
        if ( elm == null )
        {
            return null;
        }
        else
        {
            // this way build setters change DOM tree immediately
            return new JDomDependencyManagement( elm );
        }
    }
}
