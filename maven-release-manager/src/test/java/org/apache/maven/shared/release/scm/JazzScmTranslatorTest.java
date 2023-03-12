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
package org.apache.maven.shared.release.scm;

import java.io.File;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class JazzScmTranslatorTest {

    private final JazzScmTranslator scmTranslator = new JazzScmTranslator();

    /**
     * @see org.apache.maven.model.Scm#getTag()
     */
    @Test
    public void testResolveTag() {
        assertNull(scmTranslator.resolveTag("HEAD"));
        assertNull(scmTranslator.resolveTag("project-1.0"));
    }

    @Test
    public void testTranslateTagUrl() {
        assertEquals("url:tag", scmTranslator.translateTagUrl("url:module", "tag", null));
        assertEquals("url:tag", scmTranslator.translateTagUrl("url:module", "tag", "tagBase"));
    }

    @Test
    public void testTranslateBranchUrl() {
        assertEquals("url:branchName", scmTranslator.translateBranchUrl("url:module", "branchName", null));
        assertEquals("url:branchName", scmTranslator.translateBranchUrl("url:module", "branchName", "tagBase"));
    }

    @Test
    public void testGetRelativePath() {
        assertEquals(
                "BogusTest" + File.separator + "release.properties",
                scmTranslator.toRelativePath("BogusTest/release.properties"));
        assertEquals(
                "BogusTest" + File.separator + "release.properties",
                scmTranslator.toRelativePath("/BogusTest/release.properties"));
        assertEquals(
                "BogusTest" + File.separator + "release.properties",
                scmTranslator.toRelativePath("BogusTest\\release.properties"));
        assertEquals(
                "BogusTest" + File.separator + "release.properties",
                scmTranslator.toRelativePath("\\BogusTest\\release.properties"));
    }
}
