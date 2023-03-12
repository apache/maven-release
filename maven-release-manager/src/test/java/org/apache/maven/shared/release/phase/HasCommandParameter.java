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

import org.apache.maven.scm.CommandParameter;
import org.apache.maven.scm.CommandParameters;
import org.apache.maven.scm.ScmException;
import org.mockito.ArgumentMatcher;

/**
 * Mockito constraint to check if a command parameter has a specific value.
 *
 * @author <a href="mailto:michael@bigmichi1.de">Michael Cramer</a>
 */
public class HasCommandParameter implements ArgumentMatcher<CommandParameters> {
    private final CommandParameter commandParameter;

    private final Object expected;

    public HasCommandParameter(CommandParameter commandParameter, Object expected) {
        this.commandParameter = commandParameter;
        this.expected = expected;
    }

    @Override
    public boolean matches(CommandParameters argument) {
        CommandParameters commandParameters = (CommandParameters) argument;

        try {
            return commandParameters.getString(this.commandParameter).equals(String.valueOf(expected));
        } catch (ScmException e) {
            return false;
        }
    }
}
