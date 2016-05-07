package org.apache.maven.shared.release.transform.jdom;

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

import java.util.Properties;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Scm;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Text;

/**
 * JDom implementation of poms PROJECT element
 * 
 * @author Robert Scholte
 * @since 3.0
 */
public class JDomModel extends Model
{
    private final Element project;
    
    public JDomModel( Document document )
    {
        this.project = document.getRootElement();
    }
    
    public JDomModel( Element project )
    {
        this.project = project;
    }

    @Override
    public Parent getParent()
    {
        Element elm = getParentElement();
        if ( elm == null )
        {
            return null;
        }
        else
        {
            // this way scm setters change DOM tree immediately
            return new JDomParent( elm );
        }
    }

    private Element getParentElement()
    {
        return project.getChild( "parent", project.getNamespace() );
    }
    
    @Override
    public Properties getProperties()
    {
        Element properties = project.getChild( "properties", project.getNamespace() );
        
        if ( properties == null )
        {
            return null;
        }
        else
        {
            return new JDomProperties( properties );
        }
    }
    
    @Override
    public void setScm( Scm scm )
    {
        if ( scm == null )
        {
            JDomUtils.rewriteElement( "scm", null, project, project.getNamespace() );
        }
        else
        {
            Element scmRoot = new Element( "scm" );
            scmRoot.addContent( "\n  " );
            
            // Write current values to JDom tree
            Scm jdomScm = new JDomScm( scmRoot );
            jdomScm.setConnection( scm.getConnection() );
            jdomScm.setDeveloperConnection( scm.getDeveloperConnection() );
            jdomScm.setTag( scm.getTag() );
            jdomScm.setUrl( scm.getUrl() );
            
            project.addContent( "\n  " ).addContent( scmRoot ).addContent( "\n" );
        }
    }
    
    @Override
    public Scm getScm()
    {   Element elm = project.getChild( "scm", project.getNamespace() );
        if ( elm == null )
        {
            return null;
        }
        else
        {
            // this way scm setters change DOM tree immediately
            return new JDomScm( elm );
        }
    }
    
    @Override
    public void setVersion( String version )
    {
        Element versionElement = project.getChild( "version", project.getNamespace() );
        
        String parentVersion;
        Element parent = getParentElement();
        if ( parent != null )
        {
            parentVersion = parent.getChildTextTrim( "version", project.getNamespace() );
        }
        else
        {
            parentVersion = null;
        }
        
        if ( versionElement == null )
        {
            if ( !version.equals( parentVersion ) )
            {
                // we will add this after artifactId, since it was missing but different from the inherited version
                Element artifactIdElement = project.getChild( "artifactId", project.getNamespace() );
                int index = project.indexOf( artifactIdElement );

                versionElement = new Element( "version", project.getNamespace() );
                versionElement.setText( version );
                project.addContent( index + 1, new Text( "\n  " ) );
                project.addContent( index + 2, versionElement );
            }
        }
        else
        {
            JDomUtils.rewriteValue( versionElement, version );
        }

    }
}
