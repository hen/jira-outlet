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
package outlet.jira.issueview.csv;

import java.io.Writer;
import java.util.List;
import java.util.Collection;
import java.util.Iterator;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.plugin.searchrequestview.*;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.issue.search.SearchException;
import com.opensymphony.user.User;
import com.atlassian.jira.web.component.TableLayoutFactory;
import com.atlassian.jira.web.component.IssueTableLayoutBean;
import com.atlassian.jira.issue.fields.layout.column.ColumnLayoutItem;
import org.ofbiz.core.entity.GenericValue;
import com.atlassian.jira.project.version.Version;

import com.mindprod.csv.CSVWriter;

import org.apache.commons.lang.StringUtils;

public abstract class AbstractSearchRequestCsvView implements SearchRequestView {

    private SearchProvider searchProvider;
    private TableLayoutFactory tableLayoutFactory;
    private JiraAuthenticationContext authenticationContext;
    private CustomFieldManager customFieldManager;

    public AbstractSearchRequestCsvView(JiraAuthenticationContext authenticationContext, SearchProvider searchProvider, TableLayoutFactory tableLayoutFactory, CustomFieldManager cfm) {
        this.authenticationContext = authenticationContext;
        this.searchProvider = searchProvider;
        this.tableLayoutFactory = tableLayoutFactory;
        this.customFieldManager = cfm;
    }

    public void init(SearchRequestViewModuleDescriptor moduleDescriptor) {
    }

    public void writeHeaders(SearchRequest searchRequest, RequestHeaders requestHeaders) {
    }

    public void writeHeaders(SearchRequest searchRequest, RequestHeaders requestHeaders, SearchRequestParams searchRequestParams) {
    }

    abstract protected IssueTableLayoutBean getLayout(SearchRequest searchRequest, User user);

    protected TableLayoutFactory getTableLayoutFactory() {
        return this.tableLayoutFactory;
    }

    public void writeSearchResults(SearchRequest searchRequest, SearchRequestParams searchRequestParams, Writer writer)  {
        try {
            User user = this.authenticationContext.getUser();
            IssueTableLayoutBean layout = getLayout(searchRequest, user);
            CSVWriter csv = new CSVWriter(writer);

            List columns = layout.getColumns();
            Iterator columnItr = columns.iterator();
            while(columnItr.hasNext()) {
                ColumnLayoutItem column = (ColumnLayoutItem) columnItr.next();
                String field = column.getNavigableField().getId();
                if("thumbnail".equals(field)) continue;
                if("workratio".equals(field)) continue;
                if("issuelinks".equals(field)) continue;

                // custom fields
                if(field.startsWith("customfield_")) {
                    field = this.customFieldManager.getCustomFieldObject(field).getName();
                }

                csv.put(field);
            }
            csv.nl();

            SearchResults results = this.searchProvider.search(searchRequest.getQuery(), user, searchRequestParams.getPagerFilter());
            List list = results.getIssues();
            Iterator itr = list.iterator();

            while(itr.hasNext()) {
                Issue issue = (Issue) itr.next();
                GenericValue gv = issue.getGenericValue();
                columnItr = columns.iterator();
                while(columnItr.hasNext()) {
                    ColumnLayoutItem column = (ColumnLayoutItem) columnItr.next();
                    String field = column.getNavigableField().getId();

                    // hacks
                    if("issuekey".equals(field)) field = "key";
                    if("duedate".equals(field)) field = "dueDate";
                    if("issuetype".equals(field)) field = "issueType";
                    if("versions".equals(field)) field = "affectedVersions";
                    if("timeOriginalEstimate".equals(field)) field = "originalEstimate";
                    if("timeEstimate".equals(field)) field = "estimate";
                    if("security".equals(field)) field = "securityLevelId";

                    // DateFormat of the user - figure out how to find it
                    // Find the following:
                    //   thumbnail
                    //   workratio
                    //   issuelinks
                    if("thumbnail".equals(field)) continue;
                    if("workratio".equals(field)) continue;
                    if("issuelinks".equals(field)) continue;

                    String value = null;
                    if(field.startsWith("customfield_")) {
                        // custom fields
                        Object v = issue.getCustomFieldValue(this.customFieldManager.getCustomFieldObject(field));
                        if(v != null) {
                            value = v.toString();
                        }
                    } else
                    if("securityLevelId".equals(field)) {
                        if(issue.getSecurityLevelId() != null) {
                            value = issue.getSecurityLevelId().toString();
                        }
                    } else
                    if("dueDate".equals(field)) {
                        if(issue.getDueDate() != null) {
                            value = issue.getDueDate().toString();
                        }
                    } else
                    if("environment".equals(field)) {
                        if(issue.getEnvironment() != null) {
                            value = issue.getEnvironment();
                        }
                    } else
                    if("description".equals(field)) {
                        if(issue.getDescription() != null) {
                            value = issue.getDescription();
                        }
                    } else
                    if("issueType".equals(field)) {
                        if(issue.getIssueTypeObject() != null) {
                            value = issue.getIssueTypeObject().getName();
                        }
                    } else
                    if("resolution".equals(field)) {
                        if(issue.getResolutionObject() != null) {
                            value = issue.getResolutionObject().getName();
                        }
                    } else
                    if("resolutiondate".equals(field)) {
                        if(issue.getResolutionDate() != null) {
                            value = issue.getResolutionDate().toString();
                        }
                    } else
                    if("affectedVersions".equals(field)) {
                        if(issue.getAffectedVersions() != null) {
                            value = versionsToString(issue.getAffectedVersions());
                        }
                    } else
                    if("fixVersions".equals(field)) {
                        if(issue.getFixVersions() != null) {
                            value = versionsToString(issue.getFixVersions());
                        }
                    } else
                    if("components".equals(field)) {
                        if(issue.getComponents() != null) {
                            value = componentsToString(issue.getComponents());
                        }
                    } else {
                        try {
                            value = gv.get(field).toString();
                        } catch (Exception e) {
                            System.err.println("Unable to get on " + field);
                        }
                    }

                    if(value != null) {
                        csv.put(value);
                    } else {
                        csv.put("");
                    }
                }
                csv.nl();
            }
        } catch(SearchException se) {
            se.printStackTrace();
        }
    }

    public String versionsToString(Collection coll) {
        int i = 0;
        int size = coll.size();
        Object[] versions = new Object[size];
        Iterator itr = coll.iterator();
        while(itr.hasNext()) {
            Version version = (Version) itr.next();
            versions[i] = version.getName();
            i++;
        }
        return StringUtils.join(versions, ",");
    }
 
    public String componentsToString(Collection coll) {
        int i = 0;
        int size = coll.size();
        Object[] components = new Object[size];
        Iterator itr = coll.iterator();
        while(itr.hasNext()) {
            GenericValue component = (GenericValue) itr.next();
            components[i] = component.get("name");
            i++;
        }
        return StringUtils.join(components, ",");
    }
 
}
