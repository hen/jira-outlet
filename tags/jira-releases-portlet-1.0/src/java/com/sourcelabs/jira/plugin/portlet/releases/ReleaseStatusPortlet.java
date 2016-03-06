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
package com.sourcelabs.jira.plugin.portlet.releases;

import com.atlassian.configurable.ObjectConfigurationException;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.fields.NavigableField;
import com.atlassian.jira.issue.index.DocumentConstants;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.issue.search.SearchSort;
import com.atlassian.jira.issue.search.parameters.lucene.*;
import com.atlassian.jira.portal.PortletConfiguration;
import com.atlassian.jira.portal.PortletImpl;
import com.atlassian.jira.issue.search.SearchRequestManager;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.web.bean.FieldVisibilityBean;
import com.atlassian.jira.web.util.IssueTableBean;
import com.atlassian.jira.web.action.browser.Browser;
import com.atlassian.plugin.PluginManager;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.ManagerFactory;
import com.opensymphony.user.User;
import org.ofbiz.core.entity.EntityOperator;
import org.ofbiz.core.entity.GenericValue;
import org.ofbiz.core.entity.GenericEntityException;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.web.bean.PercentageGraphModel;
import com.atlassian.jira.web.bean.PercentageGraphRow;


import java.util.*;

/**
 * This portlet displays the status of a particular version. 
 */
public class ReleaseStatusPortlet extends PortletImpl
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

    public ReleaseStatusPortlet(JiraAuthenticationContext authenticationContext, PermissionManager permissionManager, 
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
        params.put("portletId", portletConfiguration.getId());

        User user = authenticationContext.getUser();

        try {
            Long versionId = Long.valueOf(portletConfiguration.getProperty("versionid"));
            Version version = this.versionManager.getVersion(versionId);
            params.put("version", version);

            Browser browser = new Browser(this.projectManager, this.constantsManager, this.searchProvider, this.permissionManager, this.versionManager, this.pluginManager);
            browser.setSelectedProject(version.getProject());
            params.put("action", browser);

            PercentageGraphModel pgm = browser.getIssueSummaryByFixForVersion(version);
            List rows = pgm.getRows();
            PercentageGraphRow row = (PercentageGraphRow) rows.get(0);
            int percentage = pgm.getPercentage(row);
            row = (PercentageGraphRow) rows.get(1);
            long totalUnresolvedCount = row.getNumber();
            params.put("percentage", new Integer(percentage));
            params.put("pgm", pgm);

            Long pid = Long.valueOf(version.getProject().getString("id"));
            Long resolvedCount = getResolvedRecently(version, user, pid);
            params.put("resolvedCount", resolvedCount);
            Long addedCount = getAddedRecently(version, user, pid);
            params.put("addedCount", addedCount);
            params.put("overdueCount", getOverdue(version, user, pid));
            params.put("updatedCount", getUpdatedRecently(version, user, pid));
            params.put("dueSoonCount", getDueSoon(version, user, pid));

            boolean loggedIn = (this.authenticationContext.getUser() != null);
            params.put("loggedin", new Boolean(loggedIn) );

            // Happy-Sad
            if(version.getReleaseDate() != null) {
                double daysToGo = ((double)(version.getReleaseDate().getTime() - System.currentTimeMillis())) / DAY_IN_MILLIS;
                double rate = (resolvedCount.doubleValue() - addedCount.doubleValue()) / WEEK_IN_DAYS;
                double happiness = daysToGo * rate / totalUnresolvedCount;
                params.put("happiness", new Double(happiness));
                String formula = "daysToGo * rate / totalUnresolvedCount = " + happiness;
                params.put("formula", formula);
                String message = "portlet.releases.happiness.dead";
                if(happiness > 1.1) {
                    message = "portlet.releases.happiness.happy";
                } else
                if(happiness > 0.9) {
                    message = "portlet.releases.happiness.shrug";
                } else
                if(happiness > 0) {
                    message = "portlet.releases.happiness.sad";
                }
                params.put("happinessMessage", message);
            }

        } catch(SearchException e) {
            e.printStackTrace();
        } catch(NumberFormatException e) {
            e.printStackTrace();
        } catch(ObjectConfigurationException e) {
            e.printStackTrace();
        } catch(Exception e) {  // thankyou Browser
            e.printStackTrace();
        }

        return params;
    }

    protected Long getAddedRecently(Version version, User user, Long projectId) throws SearchException {
        SearchRequest sr = new SearchRequest(user);
        Date sevenDaysAgo = new Date( System.currentTimeMillis() - WEEK_IN_DAYS * DAY_IN_MILLIS );
        sr.addParameter(new AbsoluteDateRangeParameter(DocumentConstants.ISSUE_CREATED, sevenDaysAgo, null));
        sr.addParameter(new ProjectParameter(projectId));
        ArrayList list = new ArrayList(1);
        list.add(version);
        sr.addParameter(new FixForParameter(list));
        return new Long(searchProvider.searchCount(sr, user));
    }

    protected Long getResolvedRecently(Version version, User user, Long projectId) throws SearchException {
        SearchRequest sr = new SearchRequest(user);
        ArrayList statuses = new ArrayList(2);
        statuses.add(String.valueOf(IssueFieldConstants.RESOLVED_STATUS_ID));
        statuses.add(String.valueOf(IssueFieldConstants.CLOSED_STATUS_ID));
        sr.addParameter(new StatusParameter(statuses));
        Date sevenDaysAgo = new Date( System.currentTimeMillis() - WEEK_IN_DAYS * DAY_IN_MILLIS );
        sr.addParameter(new AbsoluteDateRangeParameter(DocumentConstants.ISSUE_UPDATED, sevenDaysAgo, null));
        sr.addParameter(new ProjectParameter(projectId));
        ArrayList list = new ArrayList(1);
        list.add(version);
        sr.addParameter(new FixForParameter(list));
        return new Long(searchProvider.searchCount(sr, user));
    }

    protected Long getUpdatedRecently(Version version, User user, Long projectId) throws SearchException {
        SearchRequest sr = new SearchRequest(user);
        Date sevenDaysAgo = new Date( System.currentTimeMillis() - WEEK_IN_DAYS * DAY_IN_MILLIS );
        sr.addParameter(new AbsoluteDateRangeParameter(DocumentConstants.ISSUE_UPDATED, sevenDaysAgo, null));
        sr.addParameter(new ProjectParameter(projectId));
        ArrayList list = new ArrayList(1);
        list.add(version);
        sr.addParameter(new FixForParameter(list));
        return new Long(searchProvider.searchCount(sr, user));
    }

    protected Long getOverdue(Version version, User user, Long projectId) throws SearchException {
        SearchRequest sr = new SearchRequest(user);
        sr.addParameter(ResolutionParameter.UNRESOLVED);
        sr.addParameter(new AbsoluteDateRangeParameter(DocumentConstants.ISSUE_DUEDATE, null, new Date()));
        sr.addParameter(new ProjectParameter(projectId));
        ArrayList list = new ArrayList(1);
        list.add(version);
        sr.addParameter(new FixForParameter(list));
        return new Long(searchProvider.searchCount(sr, user));
    }

    protected Long getDueSoon(Version version, User user, Long projectId) throws SearchException {
        SearchRequest sr = new SearchRequest(user);
        sr.addParameter(ResolutionParameter.UNRESOLVED);
        Date sevenDaysTime = new Date( System.currentTimeMillis() + WEEK_IN_DAYS * DAY_IN_MILLIS );
        sr.addParameter(new AbsoluteDateRangeParameter(DocumentConstants.ISSUE_DUEDATE, new Date(), sevenDaysTime));
        sr.addParameter(new ProjectParameter(projectId));
        ArrayList list = new ArrayList(1);
        list.add(version);
        sr.addParameter(new FixForParameter(list));
        return new Long(searchProvider.searchCount(sr, user));
    }

}
