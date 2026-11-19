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
package org.apache.maven.shared.release.phase;

import java.io.File;
import java.util.Collections;

import org.apache.maven.scm.CommandParameters;
import org.apache.maven.scm.ScmBranchParameters;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmTagParameters;
import org.apache.maven.scm.command.branch.BranchScmResult;
import org.apache.maven.scm.command.tag.TagScmResult;
import org.apache.maven.scm.command.untag.UntagScmResult;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.provider.ScmProviderRepository;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.apache.maven.shared.release.config.ReleaseUtils;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.scm.ScmRepositoryConfigurator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GitScmPhaseTest {
    @ParameterizedTest
    @ValueSource(strings = {"pom.xml", "parent/pom.xml", "modules/parent/pom.xml"})
    void testTagBranchAndRollbackUseSameGitRepository(String pomFileName) throws Exception {
        String scmUrl = "scm:git:https://example.com/team/repository.git";
        File workingDirectory = new File("target/checkout").getAbsoluteFile();
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl(scmUrl);
        builder.setWorkingDirectory(workingDirectory.toString());
        builder.setPomFileName(pomFileName);
        builder.setScmReleaseLabel("release-label");
        builder.setPushChanges(true);
        ReleaseDescriptor descriptor = ReleaseUtils.buildReleaseDescriptor(builder);
        DefaultReleaseEnvironment environment = new DefaultReleaseEnvironment();

        ScmProviderRepository providerRepository = mock(ScmProviderRepository.class);
        ScmRepository repository = new ScmRepository("git", providerRepository);
        ScmProvider provider = mock(ScmProvider.class);
        ScmRepositoryConfigurator configurator = mock(ScmRepositoryConfigurator.class);
        when(configurator.getConfiguredRepository(eq(scmUrl), eq(descriptor), any()))
                .thenReturn(repository);
        when(configurator.getRepositoryProvider(repository)).thenReturn(provider);
        when(provider.tag(eq(repository), any(ScmFileSet.class), eq("release-label"), any(ScmTagParameters.class)))
                .thenAnswer(invocation -> {
                    assertEquals(workingDirectory, ((ScmFileSet) invocation.getArgument(1)).getBasedir());
                    return new TagScmResult("tag", Collections.emptyList());
                });
        when(provider.branch(
                        eq(repository), any(ScmFileSet.class), eq("release-label"), any(ScmBranchParameters.class)))
                .thenAnswer(invocation -> {
                    assertEquals(workingDirectory, ((ScmFileSet) invocation.getArgument(1)).getBasedir());
                    return new BranchScmResult("branch", Collections.emptyList());
                });
        when(provider.untag(eq(repository), any(ScmFileSet.class), any(CommandParameters.class)))
                .thenAnswer(invocation -> {
                    assertEquals(workingDirectory, ((ScmFileSet) invocation.getArgument(1)).getBasedir());
                    return new UntagScmResult("untag", "", "", true);
                });

        new ScmTagPhase(configurator).execute(descriptor, environment, Collections.emptyList());
        new ScmBranchPhase(configurator).execute(descriptor, environment, Collections.emptyList());
        new RemoveScmTagPhase(configurator).execute(descriptor, environment, Collections.emptyList());

        verify(provider).tag(eq(repository), any(ScmFileSet.class), eq("release-label"), any(ScmTagParameters.class));
        verify(provider)
                .branch(eq(repository), any(ScmFileSet.class), eq("release-label"), any(ScmBranchParameters.class));
        verify(provider).untag(eq(repository), any(ScmFileSet.class), any(CommandParameters.class));
    }
}
