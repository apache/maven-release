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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HgScmTranslatorTest {
    private final HgScmTranslator scmTranslator = new HgScmTranslator();

    /**
     * @see org.apache.maven.model.Scm#getTag()
     */
    @ParameterizedTest
    @CsvSource({"HEAD, HEAD", "project-1.0, project-1.0"})
    void testResolveTag(String tag, String expected) {
        assertEquals(expected, scmTranslator.resolveTag(tag));
    }

    @ParameterizedTest
    @CsvSource(
            value = {"url, url, NULL", "url, url, tagBase"},
            nullValues = "NULL")
    void testTranslateTagUrl(String url, String expected, String tagBase) {
        assertEquals(expected, scmTranslator.translateTagUrl(url, "tag", tagBase));
    }

    @ParameterizedTest
    @CsvSource(
            value = {"url, url, NULL", "url, url, tagBase"},
            nullValues = "NULL")
    void testTranslateBranchUrl(String url, String expected, String tagBase) {
        assertEquals(expected, scmTranslator.translateBranchUrl(url, "branchName", tagBase));
    }
}
