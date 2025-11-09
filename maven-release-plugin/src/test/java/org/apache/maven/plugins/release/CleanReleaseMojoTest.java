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
package org.apache.maven.plugins.release;

import org.apache.maven.api.di.Provides;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.shared.release.ReleaseCleanRequest;
import org.apache.maven.shared.release.ReleaseManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Test release:clean.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
@ExtendWith(MockitoExtension.class)
@MojoTest
class CleanReleaseMojoTest {

    @Mock
    private ReleaseManager releaseManagerMock;

    @Provides
    private ReleaseManager releaseManager() {
        return releaseManagerMock;
    }

    @Test
    @InjectMojo(goal = "clean")
    void testClean(CleanReleaseMojo mojo) throws Exception {
        // prepare
        ArgumentCaptor<ReleaseCleanRequest> request = ArgumentCaptor.forClass(ReleaseCleanRequest.class);

        // execute
        mojo.execute();

        // verify
        verify(releaseManagerMock).clean(request.capture());

        assertEquals(mojo.getReactorProjects(), request.getValue().getReactorProjects());

        verifyNoMoreInteractions(releaseManagerMock);
    }
}
