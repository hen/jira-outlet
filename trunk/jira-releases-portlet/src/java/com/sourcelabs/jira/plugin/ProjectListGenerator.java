/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sourcelabs.jira.plugin;

import com.atlassian.jira.portal.ProjectValuesGenerator;
import java.util.Map;

public class ProjectListGenerator extends ProjectValuesGenerator {

    public Map getValues(Map params) {
        Map m = super.getValues(params);
        m.put("-1", "Select Project:");
        return m;
    }

}
