package org.apache.maven.shared.release.phase;

import org.apache.maven.scm.ScmTagParameters;
import org.jmock.core.Constraint;
import org.mockito.ArgumentMatcher;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * @author <a href="mailto:olamy@apache.org">olamy</a>
 */
public class IsScmTagParamtersEquals extends ArgumentMatcher<ScmTagParameters>
    implements Constraint
{
    private final ScmTagParameters scmTagParameters;

    public IsScmTagParamtersEquals( ScmTagParameters scmTagParameters )
    {
        this.scmTagParameters = scmTagParameters;
    }
    
    /** 
     * @see org.jmock.core.Constraint#eval(java.lang.Object)
     */
    public boolean eval( Object o )
    {
        return matches( o );
    }

    @Override
    public boolean matches( Object argument )
    {
        ScmTagParameters stp = (ScmTagParameters) argument;
        return stp.getMessage().equals( this.scmTagParameters.getMessage() )
            && stp.isRemoteTagging() == this.scmTagParameters.isRemoteTagging();
    }

    /** 
     * @see org.jmock.core.SelfDescribing#describeTo(java.lang.StringBuffer)
     */
    public StringBuffer describeTo( StringBuffer buffer )
    {
        // TODO Auto-generated method stub
        return null;
    }

}
