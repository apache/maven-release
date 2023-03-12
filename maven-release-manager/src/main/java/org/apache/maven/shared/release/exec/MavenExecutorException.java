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

/**
 * Exception executing Maven.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class MavenExecutorException extends Exception {
    private int exitCode;

    /**
     * <p>Constructor for MavenExecutorException.</p>
     *
     * @param message  a {@link java.lang.String} object
     * @param exitCode a int
     */
    public MavenExecutorException(String message, int exitCode) {
        super(message);

        this.exitCode = exitCode;
    }

    /**
     * <p>Constructor for MavenExecutorException.</p>
     *
     * @param message a {@link java.lang.String} object
     * @param cause   a {@link java.lang.Throwable} object
     */
    public MavenExecutorException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * <p>Getter for the field <code>exitCode</code>.</p>
     *
     * @return a int
     */
    public int getExitCode() {
        return exitCode;
    }
}
