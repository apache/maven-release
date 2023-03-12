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

import org.apache.maven.shared.release.env.ReleaseEnvironment;

/**
 * <p>ReleaseRollbackRequest class.</p>
 *
 * @author Robert Scholte
 * @since 2.3
 */
public class ReleaseRollbackRequest extends AbstractReleaseRequest {
    private ReleaseEnvironment releaseEnvironment;

    /**
     * <p>Getter for the field <code>releaseEnvironment</code>.</p>
     *
     * @return the releaseEnvironment
     */
    public ReleaseEnvironment getReleaseEnvironment() {
        return releaseEnvironment;
    }

    /**
     * <p>Setter for the field <code>releaseEnvironment</code>.</p>
     *
     * @param releaseEnvironment the releaseEnvironment to set
     */
    public void setReleaseEnvironment(ReleaseEnvironment releaseEnvironment) {
        this.releaseEnvironment = releaseEnvironment;
    }
}
