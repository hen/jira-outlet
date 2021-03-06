/*
 * Copyright 2007 Henri Yandell
 *
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
package outlet.jira.issueview.csv;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.issue.search.SearchProvider;
import com.opensymphony.user.User;
import com.atlassian.jira.web.component.TableLayoutFactory;
import com.atlassian.jira.web.component.IssueTableLayoutBean;

public class SearchRequestCsvViewCurrentFields extends AbstractSearchRequestCsvView {

    public SearchRequestCsvViewCurrentFields(JiraAuthenticationContext authenticationContext, SearchProvider searchProvider, TableLayoutFactory tableLayoutFactory, CustomFieldManager cfm) {
        super(authenticationContext, searchProvider, tableLayoutFactory, cfm);
    }

    protected IssueTableLayoutBean getLayout(SearchRequest searchRequest, User user) {
        return getTableLayoutFactory().getStandardExcelLayout(searchRequest, user);
    }

}
