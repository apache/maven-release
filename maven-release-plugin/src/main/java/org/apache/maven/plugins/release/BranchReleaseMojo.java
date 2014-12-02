package org.apache.maven.plugins.release;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.Arrays;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.config.ReleaseUtils;

/**
 * Branch a project in SCM, using the same steps as the <tt>release:prepare</tt> goal, creating a branch instead of a
 * tag. For more info see <a href="http://maven.apache.org/plugins/maven-release-plugin/examples/branch.html"
 * >http://maven.apache.org/plugins/maven-release-plugin/examples/branch.html</a>.
 *
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 * @since 2.0-beta-6
 */
@Mojo( name = "branch", aggregator = true )
public class BranchReleaseMojo
    extends AbstractScmReleaseMojo
{
    /**
     * The branch name to use.
     *
     * @required
     * @since 2.0-beta-6
     */
    @Parameter( property = "branchName", required = true )
    private String branchName;

    /**
     * The branch base directory in SVN, you must define it if you don't use the standard svn layout
     * (trunk/tags/branches). For example, <code>http://svn.apache.org/repos/asf/maven/plugins/branches</code>. The URL
     * is an SVN URL and does not include the SCM provider and protocol.
     *
     * @since 2.0
     */
    @Parameter( property = "branchBase" )
    private String branchBase;

    /**
     * Whether to update versions in the branch.
     *
     * @since 2.0-beta-6
     */
    @Parameter( defaultValue = "false", property = "updateBranchVersions" )
    private boolean updateBranchVersions;

    /**
     * Whether to update versions in the working copy.
     *
     * @since 2.0-beta-6
     */
    @Parameter( defaultValue = "true", property = "updateWorkingCopyVersions" )
    private boolean updateWorkingCopyVersions;

    /**
     * Whether to suppress a commit of changes to the working copy
     * before the tag is created.
     * <br/>
     * <br/>This requires <code>remoteTagging</code> to be set to false.
     * <br/>
     * <br/><code>suppressCommitBeforeBranch</code> is useful when you want
     * to avoid poms with released versions in all revisions of your
     * trunk or development branch.
     *
     * @since 2.1
     */
    @Parameter( defaultValue = "false", property = "suppressCommitBeforeBranch" )
    private boolean suppressCommitBeforeBranch;

    /**
     * Whether to update versions to SNAPSHOT in the branch.
     *
     * @since 2.0-beta-6
     */
    @Parameter( defaultValue = "true", property = "updateVersionsToSnapshot" )
    private boolean updateVersionsToSnapshot;

    /**
     * Whether to use "edit" mode on the SCM, to lock the file for editing during SCM operations.
     *
     * @since 2.0-beta-6
     */
    @Parameter( defaultValue = "false", property = "useEditMode" )
    private boolean useEditMode;

    /**
     * Whether to update dependencies version to the next development version.
     *
     * @since 2.0-beta-6
     */
    @Parameter( defaultValue = "true", property = "updateDependencies" )
    private boolean updateDependencies;

    /**
     * Whether to automatically assign submodules the parent version.  If set to false,
     * the user will be prompted for the version of each submodules.
     *
     * @since 2.0-beta-6
     */
    @Parameter( defaultValue = "false", property = "autoVersionSubmodules" )
    private boolean autoVersionSubmodules;

    /**
     * Dry run: don't checkin or tag anything in the scm repository, or modify the checkout.
     * Running <code>mvn -DdryRun=true release:prepare</code> is useful in order to check that modifications to
     * poms and scm operations (only listed on the console) are working as expected.
     * Modified POMs are written alongside the originals without modifying them.
     *
     * @since 2.0-beta-6
     */
    @Parameter( defaultValue = "false", property = "dryRun" )
    private boolean dryRun;

    /**
     * Whether to add a schema to the POM if it was previously missing on release.
     *
     * @since 2.0-beta-6
     */
    @Parameter( defaultValue = "true", property = "addSchema" )
    private boolean addSchema;

    /**
     * currently only implemented with svn scm. Enable a workaround to prevent issue
     * due to svn client > 1.5.0 (http://jira.codehaus.org/browse/SCM-406)
     *
     * @since 2.0
     */
    @Parameter( defaultValue = "true", property = "remoteTagging" )
    private boolean remoteTagging;

     /**
     * A list of additional exclude filters that will be skipped when checking for
     * modifications on the working copy.
     *
     * Is ignored, when checkModificationExcludes is set.
     *
     * @since 2.1
     */
    @Parameter
    private String[] checkModificationExcludes;

    /**
     * Command-line version of checkModificationExcludes.
     *
     * @since 2.1
     */
    @Parameter( property = "checkModificationExcludeList" )
    private String checkModificationExcludeList;

    /**
     * Specify the new version for the branch. 
     * This parameter is only meaningful if {@link #updateBranchVersions} = {@code true}.
     *
     * @since 2.0
     */
    @Parameter( property = "releaseVersion" )
    private String releaseVersion;

    /**
     * Specify the new version for the working copy.
     * This parameter is only meaningful if {@link #updateWorkingCopyVersions} = {@code true}.
     *
     * @since 2.0
     */
    @Parameter( property = "developmentVersion" )
    private String developmentVersion;

    /**
     * {@inheritDoc}
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        super.execute();

        ReleaseDescriptor config = createReleaseDescriptor();
        config.setAddSchema( addSchema );
        config.setScmUseEditMode( useEditMode );
        config.setUpdateDependencies( updateDependencies );
        config.setAutoVersionSubmodules( autoVersionSubmodules );
        config.setScmReleaseLabel( branchName );
        config.setScmBranchBase( branchBase );
        config.setBranchCreation( true );
        config.setUpdateBranchVersions( updateBranchVersions );
        config.setUpdateWorkingCopyVersions( updateWorkingCopyVersions );
        config.setUpdateVersionsToSnapshot( updateVersionsToSnapshot );
        config.setRemoteTagging( remoteTagging );
        config.setDefaultReleaseVersion( releaseVersion );
        config.setDefaultDevelopmentVersion( developmentVersion );
        config.setSuppressCommitBeforeTagOrBranch( suppressCommitBeforeBranch );

        // Create a config containing values from the session properties (ie command line properties with cli).
        ReleaseDescriptor sysPropertiesConfig
                = ReleaseUtils.copyPropertiesToReleaseDescriptor( session.getExecutionProperties() );
        mergeCommandLineConfig( config, sysPropertiesConfig );

        if ( checkModificationExcludeList != null )
        {
            checkModificationExcludes = checkModificationExcludeList.replaceAll( "\\s", "" ).split( "," );
        }

        if ( checkModificationExcludes != null )
        {
            config.setCheckModificationExcludes( Arrays.asList( checkModificationExcludes ) );
        }

        try
        {
            releaseManager.branch( config, getReleaseEnvironment(), getReactorProjects(), dryRun );
        }
        catch ( ReleaseExecutionException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        catch ( ReleaseFailureException e )
        {
            throw new MojoFailureException( e.getMessage(), e );
        }
    }
}
