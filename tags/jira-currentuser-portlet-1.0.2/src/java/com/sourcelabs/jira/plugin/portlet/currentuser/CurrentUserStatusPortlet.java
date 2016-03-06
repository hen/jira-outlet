/*
 * Copyright 2007 SourceLabs, Inc.
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
package com.sourcelabs.jira.plugin.portlet.currentuser;

import com.atlassian.configurable.ObjectConfigurationException;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.index.DocumentConstants;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.issue.search.parameters.lucene.*;
import com.atlassian.jira.portal.PortletConfiguration;
import com.atlassian.jira.portal.PortletImpl;
import com.atlassian.jira.issue.search.SearchRequestManager;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.web.action.browser.Browser;
import com.atlassian.plugin.PluginManager;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.ManagerFactory;
import com.opensymphony.user.User;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.web.bean.PercentageGraphModel;
import com.atlassian.jira.web.bean.PercentageGraphRow;



import java.util.*;

/**
 * This portlet displays the status of the current user
 */
public class CurrentUserStatusPortlet extends PortletImpl
{

    private static final long DAY_IN_MILLIS = 1000L * 60L * 60L * 24L;
    private static final int WEEK_IN_DAYS = 7;

    // References to managers required for this portlet
    private final PermissionManager permissionManager;
    private final ConstantsManager constantsManager;
    private final SearchProvider searchProvider;
    private final ApplicationProperties applicationProperties;
    private final SearchRequestManager searchRequestManager;
    private final VersionManager versionManager;
    private final ProjectManager projectManager;
    private final PluginManager pluginManager;

    public CurrentUserStatusPortlet(JiraAuthenticationContext authenticationContext, PermissionManager permissionManager, 
                          ConstantsManager constantsManager, SearchProvider searchProvider, 
                          ApplicationProperties applicationProperties, SearchRequestManager searchRequestManager,
                          VersionManager versionManager, ProjectManager projectManager, PluginManager pluginManager)  {
        super(authenticationContext);
        this.permissionManager = permissionManager;
        this.constantsManager = constantsManager;
        this.searchProvider = searchProvider;
        this.applicationProperties = applicationProperties;
        this.searchRequestManager = searchRequestManager;
        this.versionManager = versionManager;
        this.projectManager = projectManager;
        this.pluginManager = pluginManager;
    }

    // Pass the data required for the portlet display to the view template
    protected Map getVelocityParams(PortletConfiguration portletConfiguration)
    {
        Map params = new HashMap();
        params.put("portlet", this);  // automatic?
        Long portletId = portletConfiguration.getId();
        params.put("portletId", portletId);

        User user = authenticationContext.getUser();

        boolean loggedIn = (this.authenticationContext.getUser() != null);
        params.put("loggedin", new Boolean(loggedIn) );

        if(!loggedIn) {
            return params;
        }

        params.put("user", user);

        try {
            PercentageGraphModel reportedPgm = getReportedSummary(user);
            params.put("reportedPgm", reportedPgm);
            params.put("totalReported", findSum(reportedPgm));

            PercentageGraphModel assignedPgm = getAssignedSummary(user);
            params.put("assignedPgm", assignedPgm);
            params.put("totalAssigned", findSum(assignedPgm));

            List rows = assignedPgm.getRows();
            long totalAssignedOpen = 0;
            if(rows.size() == 2) {
                totalAssignedOpen = ((PercentageGraphRow)rows.get(1)).getNumber();
            }
            params.put("totalAssignedOpen", new Long(totalAssignedOpen) );

            params.put("resolvedCount", getResolvedRecently(user));
            params.put("addedCount", getAddedRecently(user));
            params.put("overdueCount", getOverdue(user));
            params.put("updatedCount", getUpdatedRecently(user));
            params.put("dueSoonCount", getDueSoon(user));
            params.put("inProgressCount", getInProgress(user));

        } catch(SearchException e) {
            e.printStackTrace();
        } catch(NumberFormatException e) {
            e.printStackTrace();
        }

        return params;
    }

    protected Long getAddedRecently(User user) throws SearchException {
        SearchRequest sr = new SearchRequest(user);
        Date sevenDaysAgo = new Date( System.currentTimeMillis() - WEEK_IN_DAYS * DAY_IN_MILLIS );
        sr.addParameter(new AbsoluteDateRangeParameter(DocumentConstants.ISSUE_CREATED, sevenDaysAgo, null));
        sr.addParameter(new UserParameter(DocumentConstants.ISSUE_AUTHOR, user.getName()));
        return new Long(searchProvider.searchCount(sr, user));
    }

    protected Long getResolvedRecently(User user) throws SearchException {
        SearchRequest sr = new SearchRequest(user);
        ArrayList statuses = new ArrayList(2);
        statuses.add(String.valueOf(IssueFieldConstants.RESOLVED_STATUS_ID));
        statuses.add(String.valueOf(IssueFieldConstants.CLOSED_STATUS_ID));
        sr.addParameter(new StatusParameter(statuses));
        Date sevenDaysAgo = new Date( System.currentTimeMillis() - WEEK_IN_DAYS * DAY_IN_MILLIS );
        sr.addParameter(new AbsoluteDateRangeParameter(DocumentConstants.ISSUE_UPDATED, sevenDaysAgo, null));
        sr.addParameter(new UserParameter(DocumentConstants.ISSUE_AUTHOR, user.getName()));
        return new Long(searchProvider.searchCount(sr, user));
    }

    protected Long getUpdatedRecently(User user) throws SearchException {
        SearchRequest sr = new SearchRequest(user);
        Date sevenDaysAgo = new Date( System.currentTimeMillis() - WEEK_IN_DAYS * DAY_IN_MILLIS );
        sr.addParameter(new AbsoluteDateRangeParameter(DocumentConstants.ISSUE_UPDATED, sevenDaysAgo, null));
        sr.addParameter(new UserParameter(DocumentConstants.ISSUE_AUTHOR, user.getName()));
        return new Long(searchProvider.searchCount(sr, user));
    }

    protected Long getOverdue(User user) throws SearchException {
        SearchRequest sr = new SearchRequest(user);
        sr.addParameter(ResolutionParameter.UNRESOLVED);
        sr.addParameter(new AbsoluteDateRangeParameter(DocumentConstants.ISSUE_DUEDATE, null, new Date()));
        sr.addParameter(new UserParameter(DocumentConstants.ISSUE_ASSIGNEE, user.getName()));
        return new Long(searchProvider.searchCount(sr, user));
    }

    protected Long getDueSoon(User user) throws SearchException {
        SearchRequest sr = new SearchRequest(user);
        sr.addParameter(ResolutionParameter.UNRESOLVED);
        Date sevenDaysTime = new Date( System.currentTimeMillis() + WEEK_IN_DAYS * DAY_IN_MILLIS );
        sr.addParameter(new AbsoluteDateRangeParameter(DocumentConstants.ISSUE_DUEDATE, new Date(), sevenDaysTime));
        sr.addParameter(new UserParameter(DocumentConstants.ISSUE_ASSIGNEE, user.getName()));
        return new Long(searchProvider.searchCount(sr, user));
    }

    protected Long getInProgress(User user) throws SearchException {
        SearchRequest sr = new SearchRequest(user);
        sr.addParameter(ResolutionParameter.UNRESOLVED);
        sr.addParameter(new UserParameter(DocumentConstants.ISSUE_ASSIGNEE, user.getName()));
        sr.addParameter(new StatusParameter("" + IssueFieldConstants.INPROGRESS_STATUS_ID));
        return new Long(searchProvider.searchCount(sr, user));
    }

    protected PercentageGraphModel getAssignedSummary(User user) throws SearchException {
        return getUserSummary(user, DocumentConstants.ISSUE_ASSIGNEE);
    }
    protected PercentageGraphModel getReportedSummary(User user) throws SearchException {
        return getUserSummary(user, DocumentConstants.ISSUE_AUTHOR);
    }
    protected PercentageGraphModel getUserSummary(User user, String userColumn) throws SearchException {
        SearchRequest sr = new SearchRequest(user);
        sr.addParameter(new UserParameter(userColumn, user.getName()));
        long total = searchProvider.searchCount(sr, user);
        sr.addParameter(ResolutionParameter.UNRESOLVED);
        long open = searchProvider.searchCount(sr, user);
        if(total == 0) {
            return new NullPercentageGraphModel();
        }
        PercentageGraphModel model = new PercentageGraphModel();
        // TODO: i18nize the strings below
        model.addRow("009900", total - open, "Resolved Issues", new UserParameter(userColumn, DocumentConstants.ISSUE_CURRENT_USER).getQueryString() + new ResolutionParameter((List) ManagerFactory.getConstantsManager().getResolutions()).getQueryString());
        model.addRow("CC0000", open, "Open Issues", new UserParameter(userColumn, DocumentConstants.ISSUE_CURRENT_USER).getQueryString() +  ResolutionParameter.UNRESOLVED.getQueryString());
        return model;
    }

    private Long findSum(PercentageGraphModel pgm) {
        List rows = pgm.getRows();
        long sum = 0;
        Iterator itr = rows.iterator();
        while(itr.hasNext()) {
            PercentageGraphRow row = (PercentageGraphRow) itr.next();
            sum += row.getNumber();
        }
        return new Long(sum);
    }
}

class NullPercentageGraphModel extends PercentageGraphModel {
    public NullPercentageGraphModel() { super(); }
    public void addRow(String colour, long number, String description, String statuses) { throw new UnsupportedOperationException("Not supported"); }
    public int getPercentage(PercentageGraphRow row) { return 0; }
    public List getRows() { return new ArrayList(); }
}
