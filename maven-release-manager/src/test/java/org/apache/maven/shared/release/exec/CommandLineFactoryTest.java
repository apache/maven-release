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

import javax.inject.Inject;

import org.codehaus.plexus.testing.PlexusTest;
import org.codehaus.plexus.util.cli.Commandline;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the command line factory.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
@PlexusTest
class CommandLineFactoryTest {

    @Inject
    private CommandLineFactory factory;

    @Test
    void testCreation() throws Exception {
        Commandline cl = factory.createCommandLine("exec");

        String executable = cl.getExecutable();
        assertTrue(executable.contains("exec"), "Check executable " + executable);
        assertNotNull(cl.getEnvironmentVariables(), "Check environment");
        assertFalse(cl.getEnvironmentVariables().length == 0, "Check environment");
    }
}
