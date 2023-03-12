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

import java.util.List;

/**
 * <p>ReleaseManagerListener interface.</p>
 *
 * @author Edwin Punzalan
 */
public interface ReleaseManagerListener {
    /**
     * <p>goalStart.</p>
     *
     * @param goal a {@link java.lang.String} object
     * @param phases a {@link java.util.List} object
     */
    void goalStart(String goal, List<String> phases);

    /**
     * <p>phaseStart.</p>
     *
     * @param name a {@link java.lang.String} object
     */
    void phaseStart(String name);

    /**
     * <p>phaseEnd.</p>
     */
    void phaseEnd();

    /**
     * <p>phaseSkip.</p>
     *
     * @param name a {@link java.lang.String} object
     */
    void phaseSkip(String name);

    /**
     * <p>goalEnd.</p>
     */
    void goalEnd();

    /**
     * <p>error.</p>
     *
     * @param reason a {@link java.lang.String} object
     */
    void error(String reason);
}
