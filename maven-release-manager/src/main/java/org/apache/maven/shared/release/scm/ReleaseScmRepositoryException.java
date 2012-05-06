package org.apache.maven.shared.release.scm;

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

import org.apache.maven.shared.release.ReleaseFailureException;

import java.util.List;

/**
 * Exception occurring during an SCM repository operation.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ReleaseScmRepositoryException
    extends ReleaseFailureException
{
    public ReleaseScmRepositoryException( String message, List<String> validationMessages )
    {
        super( message + listValidationMessages( validationMessages ) );
    }

    private static String listValidationMessages( List<String> messages )
    {
        StringBuilder buffer = new StringBuilder();

        if ( messages != null )
        {
            for ( String message : messages )
            {
                buffer.append( "\n  - " );
                buffer.append( message );
            }
        }

        return buffer.toString();
    }
}
