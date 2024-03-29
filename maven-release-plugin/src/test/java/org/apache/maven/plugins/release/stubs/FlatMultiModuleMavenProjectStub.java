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
package org.apache.maven.plugins.release.stubs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;

/**
 * <p>Stub for a MavenProject with a flat structure.</p>
 *
 * <p>TODO: shouldn't need to do this, but the "stub" in the harness just throws away values you set.
 * Just overriding the ones I need for this plugin.</p>
 *
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
/*
 * @noinspection ClassNameSameAsAncestorName
 */
public class FlatMultiModuleMavenProjectStub extends org.apache.maven.plugin.testing.stubs.MavenProjectStub {
    public void setDistributionManagement(DistributionManagement distributionManagement) {
        getModel().setDistributionManagement(distributionManagement);
    }

    public Model getModel() {
        Model model = super.getModel();
        if (model == null) {
            model = new Model();
            setModel(model);
        }
        return model;
    }

    public DistributionManagement getDistributionManagement() {
        return getModel().getDistributionManagement();
    }

    public List<String> getModules() {
        List<String> modules = new ArrayList<String>();
        modules.add("../core");
        modules.add("../webapp");
        modules.add("../commons");

        return modules;
    }

    public File getBasedir() {
        return new File("/flat-multi-module/root-project").getAbsoluteFile();
    }

    public Scm getScm() {
        Scm scm = new Scm();
        scm.setConnection("scm:svn:file://localhost/target/svnroot/flat-multi-module/trunk/root-project");

        return scm;
    }

    @Override
    public String getGroupId() {
        return "GROUPID";
    }

    @Override
    public String getArtifactId() {
        return "ARTIFACTID";
    }

    @Override
    public String getVersion() {
        return "VERSION";
    }
}
