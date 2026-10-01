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
package org.apache.maven.shared.release;

import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class DefaultReleaseManagerListenerTest {
    private static final List<String> PHASES = Arrays.asList("a", "b");

    @Test
    void slf4jLoggerReceivesMessages() {
        Logger logger = mock(Logger.class);
        DefaultReleaseManagerListener listener = new DefaultReleaseManagerListener(logger, true);

        listener.goalStart("prepare", PHASES);
        listener.phaseStart("a");
        listener.phaseStart("wrong");
        listener.error("boom");

        ArgumentCaptor<String> info = ArgumentCaptor.forClass(String.class);
        verify(logger, org.mockito.Mockito.times(3)).info(info.capture());
        assertTrue(info.getAllValues().get(0).contains("in dry-run mode"));
        assertTrue(info.getAllValues().get(1).startsWith("1/2 "));
        verify(logger).warn("inconsistent phase name: expected 'b' but got 'wrong'");
        ArgumentCaptor<String> error = ArgumentCaptor.forClass(String.class);
        verify(logger).error(error.capture());
        assertTrue(error.getValue().endsWith(": boom"));
    }

    @Test
    @SuppressWarnings("deprecation")
    void deprecatedLogReceivesMessages() {
        Log log = mock(Log.class);
        DefaultReleaseManagerListener listener = new DefaultReleaseManagerListener(log);

        listener.goalStart("prepare", PHASES);
        listener.phaseStart("a");
        listener.phaseStart("wrong");
        listener.error("boom");

        verify(log, org.mockito.Mockito.times(3)).info(anyString());
        verify(log).warn("inconsistent phase name: expected 'b' but got 'wrong'");
        verify(log).error(anyString());
        verify(log, never()).debug(anyString());
    }
}
