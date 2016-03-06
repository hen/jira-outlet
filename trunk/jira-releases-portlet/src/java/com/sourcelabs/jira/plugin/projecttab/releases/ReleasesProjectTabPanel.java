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
package com.sourcelabs.jira.plugin.projecttab.releases;

import com.atlassian.jira.web.action.ProjectActionSupport;
import com.atlassian.jira.plugin.projectpanel.impl.GenericProjectTabPanel;
import org.ofbiz.core.entity.GenericValue;

import com.atlassian.jira.security.JiraAuthenticationContext;

import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.customfields.converters.DateConverter;
import com.atlassian.jira.issue.fields.NavigableField;
import com.atlassian.jira.issue.index.DocumentConstants;
import com.atlassian.jira.issue.search.*;
import com.atlassian.jira.issue.search.parameters.lucene.*;
import com.atlassian.jira.util.map.EasyMap;
import com.atlassian.jira.web.bean.PagerFilter;
import org.apache.log4j.Category;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ReleasesProjectTabPanel extends GenericProjectTabPanel {

    private static final Category log = Category.getInstance(ReleasesProjectTabPanel.class);

    public ReleasesProjectTabPanel(JiraAuthenticationContext context) {
        super(context);
    }

    public boolean showPanel(ProjectActionSupport support, GenericValue project) {
        // TODO: look to see if this project has any releases (archived or not)
        return true;
    }

}
