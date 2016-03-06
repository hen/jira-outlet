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
package com.sourcelabs.jira.plugin.portlet.currentuser;

import com.atlassian.configurable.ObjectConfigurationException;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.index.DocumentConstants;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.portal.PortletConfiguration;
import com.atlassian.jira.portal.PortletImpl;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.ManagerFactory;
import com.opensymphony.user.User;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.web.bean.PercentageGraphModel;
import com.atlassian.jira.web.bean.PercentageGraphRow;

import com.atlassian.query.Query;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;

import java.util.*;

/**
 * This portlet displays the status of the current user
 */
public class CurrentUserStatusPortlet extends PortletImpl
{

    private static final long DAY_IN_MILLIS = 1000L * 60L * 60L * 24L;
    private static final int WEEK_IN_DAYS = 7;

    // References to managers required for this portlet
    private final SearchProvider searchProvider;
    private final ApplicationProperties applicationProperties;

    public CurrentUserStatusPortlet(JiraAuthenticationContext authenticationContext,
                          SearchProvider searchProvider, 
                          ApplicationProperties applicationProperties) {
        super(authenticationContext);
        this.searchProvider = searchProvider;
        this.applicationProperties = applicationProperties;
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
        JqlClauseBuilder builder = JqlQueryBuilder.newClauseBuilder();
        Date sevenDaysAgo = new Date( System.currentTimeMillis() - WEEK_IN_DAYS * DAY_IN_MILLIS );
        Query query = builder.createdAfter(sevenDaysAgo).and().reporterUser(user.getName()).buildQuery();
        return new Long(searchProvider.searchCount(query, user));
    }

    protected Long getResolvedRecently(User user) throws SearchException {
        JqlClauseBuilder builder = JqlQueryBuilder.newClauseBuilder();
        Date sevenDaysAgo = new Date( System.currentTimeMillis() - WEEK_IN_DAYS * DAY_IN_MILLIS );
        Query query = builder.status(String.valueOf(IssueFieldConstants.RESOLVED_STATUS_ID), String.valueOf(IssueFieldConstants.CLOSED_STATUS_ID)).and().updatedAfter(sevenDaysAgo).and().reporterUser(user.getName()).buildQuery();
        return new Long(searchProvider.searchCount(query, user));
    }

    protected Long getUpdatedRecently(User user) throws SearchException {
        JqlClauseBuilder builder = JqlQueryBuilder.newClauseBuilder();
        Date sevenDaysAgo = new Date( System.currentTimeMillis() - WEEK_IN_DAYS * DAY_IN_MILLIS );
        Query query = builder.updatedAfter(sevenDaysAgo).and().reporterUser(user.getName()).buildQuery();
        return new Long(searchProvider.searchCount(query, user));
    }

    protected Long getOverdue(User user) throws SearchException {
        JqlClauseBuilder builder = JqlQueryBuilder.newClauseBuilder();
        Query query = builder.unresolved().and().dueAfter(new Date()).and().assigneeUser(user.getName()).buildQuery();
        return new Long(searchProvider.searchCount(query, user));
    }

    protected Long getDueSoon(User user) throws SearchException {
        JqlClauseBuilder builder = JqlQueryBuilder.newClauseBuilder();
        Date sevenDaysTime = new Date( System.currentTimeMillis() + WEEK_IN_DAYS * DAY_IN_MILLIS );
        Query query = builder.unresolved().and().dueBetween(new Date(), sevenDaysTime).and().assigneeUser(user.getName()).buildQuery();
        return new Long(searchProvider.searchCount(query, user));
    }

    protected Long getInProgress(User user) throws SearchException {
        JqlClauseBuilder builder = JqlQueryBuilder.newClauseBuilder();
        Query query = builder.unresolved().and().status(""+IssueFieldConstants.INPROGRESS_STATUS_ID).and().assigneeUser(user.getName()).buildQuery();
        return new Long(searchProvider.searchCount(query, user));
    }

    protected PercentageGraphModel getAssignedSummary(User user) throws SearchException {
        return getUserSummary(user, DocumentConstants.ISSUE_ASSIGNEE);
    }
    protected PercentageGraphModel getReportedSummary(User user) throws SearchException {
        return getUserSummary(user, DocumentConstants.ISSUE_AUTHOR);
    }
    protected PercentageGraphModel getUserSummary(User user, String userColumn) throws SearchException {
        JqlClauseBuilder builder = JqlQueryBuilder.newClauseBuilder();
        Query query = builder.assigneeUser(user.getName()).buildQuery();
        long total = searchProvider.searchCount(query, user);
        query = builder.and().unresolved().and().assigneeUser(user.getName()).buildQuery();
        long open = searchProvider.searchCount(query, user);
        PercentageGraphModel model = new PercentageGraphModel();
        if(total != 0) {
            // TODO: i18nize the 3rd field below
            // TODO: JQLize the 4th field below
            model.addRow("009900", total - open, "Resolved Issues", userColumn + "=issue_current_user&status=5&status=6"); //, new UserParameter(userColumn, DocumentConstants.ISSUE_CURRENT_USER).getQueryString() + new ResolutionParameter((List) ManagerFactory.getConstantsManager().getResolutions()).getQueryString());
            model.addRow("CC0000", open, "Open Issues", userColumn + "=issue_current_user&resolution=-1"); //, new UserParameter(userColumn, DocumentConstants.ISSUE_CURRENT_USER).getQueryString() +  ResolutionParameter.UNRESOLVED.getQueryString());
        }
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
