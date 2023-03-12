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
package org.apache.maven.shared.release.config;

import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

public class ReleaseDescriptorBuilderTest {
    private final Logger logger = Mockito.mock(Logger.class);

    @Test
    public void testCleanupArguments() {
        setAdditionalArguments("abc abc -Dxxx", "abc abc -Dxxx");
        verifyZeroInteractions(logger);
        reset(logger);

        setAdditionalArguments("abc abc ${arguments}", "abc abc ");
        verify(logger).warn(anyString(), eq("${arguments}"));
        verifyNoMoreInteractions(logger);
        reset(logger);

        setAdditionalArguments("abc ${first} abc ${arguments}", "abc  abc ");
        verify(logger).warn(anyString(), eq("${first}"));
        verify(logger).warn(anyString(), eq("${arguments}"));
        verifyNoMoreInteractions(logger);
    }

    private void setAdditionalArguments(String input, String expected) {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder(logger);
        builder.setAdditionalArguments(input);
        assertEquals(expected, builder.build().getAdditionalArguments());
    }
}
