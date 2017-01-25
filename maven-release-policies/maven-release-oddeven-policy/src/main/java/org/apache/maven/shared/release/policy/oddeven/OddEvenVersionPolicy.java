package org.apache.maven.shared.release.policy.oddeven;

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

import java.util.List;

import org.apache.maven.shared.release.policy.PolicyException;
import org.apache.maven.shared.release.policy.version.VersionPolicy;
import org.apache.maven.shared.release.policy.version.VersionPolicyRequest;
import org.apache.maven.shared.release.policy.version.VersionPolicyResult;
import org.apache.maven.shared.release.versions.DefaultVersionInfo;
import org.apache.maven.shared.release.versions.VersionInfo;
import org.apache.maven.shared.release.versions.VersionParseException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.StringUtils;

/**
 * A {@link VersionPolicy} implementation that propose even version numbers only for releases and odd
 * numbers for development. For example:<ul>
 * <li><code>1.0.0-SNAPSHOT</code> gets <code>1.0.0</code> for next release,</li>
 * <li><code>1.0.1-SNAPSHOT</code> gets <code>1.0.2</code> for next release,</li>
 * <li><code>1.0.2</code> gets <code>1.0.3-SNAPSHOT</code> for next development version.</li>
 * </ul>
 */
@Component(
    role = VersionPolicy.class,
    hint = "OddEvenVersionPolicy",
    description = "A VersionPolicy implementation that selects even version numbers only for releases"
)
public final class OddEvenVersionPolicy
    implements VersionPolicy
{

    public VersionPolicyResult getReleaseVersion( VersionPolicyRequest request )
        throws PolicyException
    {
        return calculateNextVersion( request, false );
    }

    public VersionPolicyResult getDevelopmentVersion( VersionPolicyRequest request )
        throws PolicyException
    {
        return calculateNextVersion( request, true );
    }

    public VersionPolicyResult getBranchOrTagVersion(VersionPolicyRequest request)
        throws PolicyException, VersionParseException
    {
        return null;
    }

    private VersionPolicyResult calculateNextVersion( VersionPolicyRequest request, boolean development )
    {
        DefaultVersionInfo defaultVersionInfo = null;

        try
        {
            defaultVersionInfo = new DefaultVersionInfo( request.getVersion() );
        }
        catch ( VersionParseException e )
        {
            throw new IllegalArgumentException( "Can't tell if version with no digits is even: " + e.getMessage(), e );
        }

        // by default, never reuse revisions
        int versionNumbersToSkip = 1;

        // do we need a snapshot? make sure the version info is odd
        if ( development && !isEven( defaultVersionInfo ) )
        {
            versionNumbersToSkip = 2;
        }

        // do we need a release? make sure the version info is even
        if ( !development && isEven( defaultVersionInfo ) )
        {
            versionNumbersToSkip = 0;
        }

        VersionInfo suggestedVersionInfo = defaultVersionInfo;
        while ( versionNumbersToSkip != 0 )
        {
            suggestedVersionInfo = suggestedVersionInfo.getNextVersion();
            versionNumbersToSkip--;
        }

        String nextVersion = development ? suggestedVersionInfo.getSnapshotVersionString()
                                         : suggestedVersionInfo.getReleaseVersionString();

        return new VersionPolicyResult().setVersion( nextVersion );
    }

    private boolean isEven( DefaultVersionInfo defaultVersionInfo )
    {
        int mostSignificantSegment;

        if ( StringUtils.isNumeric( defaultVersionInfo.getAnnotationRevision() ) )
        {
            mostSignificantSegment = Integer.parseInt( defaultVersionInfo.getAnnotationRevision() );
        }
        else
        {
            List<String> digits = defaultVersionInfo.getDigits();

            if ( digits == null )
            {
                throw new IllegalArgumentException( "Can't tell if version with no digits is even." );
            }

            mostSignificantSegment = Integer.parseInt( digits.get( digits.size() - 1 ) );
        }

        return mostSignificantSegment % 2 == 0;
    }
}
