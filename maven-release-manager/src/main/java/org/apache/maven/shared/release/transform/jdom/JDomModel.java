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

import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;
import org.jdom.Document;
import org.jdom.Element;

/**
 * JDom implementation of poms PROJECT element
 * 
 * @author Robert Scholte
 * @since 3.0
 */
public class JDomModel extends Model
{
    private Element project;
    
    public JDomModel( Document document )
    {
        this.project = document.getRootElement();
    }
    
    public JDomModel( Element project )
    {
        this.project = project;
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
}
