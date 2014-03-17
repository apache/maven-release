package org.apache.maven.shared.release.policies;

import org.apache.maven.shared.release.policy.PolicyException;
import org.apache.maven.shared.release.policy.version.VersionPolicy;
import org.apache.maven.shared.release.policy.version.VersionPolicyRequest;
import org.apache.maven.shared.release.policy.version.VersionPolicyResult;
import org.apache.maven.shared.release.versions.DefaultVersionInfo;
import org.apache.maven.shared.release.versions.VersionParseException;

/**
 * 
 * @author Robert Scholte
 * 
 * @plexus.component role="org.apache.maven.shared.release.policy.version.VersionPolicy" hint="default"
 */
public class DefaultVersionPolicy implements VersionPolicy
{

    public VersionPolicyResult getReleaseVersion( VersionPolicyRequest request )
        throws PolicyException, VersionParseException
    {
        String releaseVersion = new DefaultVersionInfo( request.getVersion() ).getReleaseVersionString();
        return new VersionPolicyResult().setVersion( releaseVersion );
    }

    public VersionPolicyResult getDevelopmentVersion( VersionPolicyRequest request )
        throws PolicyException, VersionParseException
    {
        String developmentVersion = new DefaultVersionInfo( request.getVersion() ).getNextVersion().getSnapshotVersionString();
        return new VersionPolicyResult().setVersion( developmentVersion );
    }

}
