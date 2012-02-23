package org.apache.maven.shared.release.phase;

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

import org.apache.maven.scm.ScmTag;
import org.mockito.ArgumentMatcher;

/**
 * JMock constraint to compare tags since it has no equals method.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @todo add an equals() method
 */
public class IsScmTagEquals extends ArgumentMatcher<ScmTag>
{
    private final ScmTag tag;

    public IsScmTagEquals( ScmTag tag )
    {
        this.tag = tag;
    }

    @Override
    public boolean matches( Object argument )
    {
        ScmTag tag = (ScmTag) argument;

        return tag.getName().equals( this.tag.getName() );
    }
}