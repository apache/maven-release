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
package org.apache.maven.shared.release.exec;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.util.MavenCrypto;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class AbstractMavenExecutorTest {

    @Test
    public void testGoalSeparation() throws MavenExecutorException {
        AbstractMavenExecutor executor = spy(new AbstractMavenExecutorSpy(mock(MavenCrypto.class)));

        executor.executeGoals(null, (String) null, new DefaultReleaseEnvironment(), true, null, null, null);
        verify(executor)
                .executeGoals(
                        isNull(),
                        eq(new ArrayList<String>()),
                        isA(ReleaseEnvironment.class),
                        eq(true),
                        isNull(),
                        isNull(),
                        isNull());
        reset(executor);

        executor.executeGoals(null, " clean verify ", new DefaultReleaseEnvironment(), true, null, null, null);
        verify(executor)
                .executeGoals(
                        isNull(),
                        eq(Arrays.asList("clean", "verify")),
                        isA(ReleaseEnvironment.class),
                        eq(true),
                        isNull(),
                        isNull(),
                        isNull());
        reset(executor);

        executor.executeGoals(null, ",clean,verify,", new DefaultReleaseEnvironment(), true, null, null, null);
        verify(executor)
                .executeGoals(
                        isNull(),
                        eq(Arrays.asList("clean", "verify")),
                        isA(ReleaseEnvironment.class),
                        eq(true),
                        isNull(),
                        isNull(),
                        isNull());
        reset(executor);

        executor.executeGoals(null, "\nclean\nverify\n", new DefaultReleaseEnvironment(), true, null, null, null);
        verify(executor)
                .executeGoals(
                        isNull(),
                        eq(Arrays.asList("clean", "verify")),
                        isA(ReleaseEnvironment.class),
                        eq(true),
                        isNull(),
                        isNull(),
                        isNull());
        reset(executor);

        executor.executeGoals(null, "\rclean\rverify\r", new DefaultReleaseEnvironment(), true, null, null, null);
        verify(executor)
                .executeGoals(
                        isNull(),
                        eq(Arrays.asList("clean", "verify")),
                        isA(ReleaseEnvironment.class),
                        eq(true),
                        isNull(),
                        isNull(),
                        isNull());
        reset(executor);

        executor.executeGoals(null, "\r\nclean\r\nverify\r\n", new DefaultReleaseEnvironment(), true, null, null, null);
        verify(executor)
                .executeGoals(
                        isNull(),
                        eq(Arrays.asList("clean", "verify")),
                        isA(ReleaseEnvironment.class),
                        eq(true),
                        isNull(),
                        isNull(),
                        isNull());
        reset(executor);

        executor.executeGoals(null, "\tclean\tverify\t", new DefaultReleaseEnvironment(), true, null, null, null);
        verify(executor)
                .executeGoals(
                        isNull(),
                        eq(Arrays.asList("clean", "verify")),
                        isA(ReleaseEnvironment.class),
                        eq(true),
                        isNull(),
                        isNull(),
                        isNull());
        reset(executor);
    }

    protected class AbstractMavenExecutorSpy extends AbstractMavenExecutor {
        public AbstractMavenExecutorSpy(MavenCrypto mavenCrypto) {
            super(mavenCrypto);
        }

        @Override
        protected void executeGoals(
                File workingDirectory,
                List<String> goals,
                ReleaseEnvironment releaseEnvironment,
                boolean interactive,
                String additionalArguments,
                String pomFileName,
                ReleaseResult result)
                throws MavenExecutorException {}
    }
}
