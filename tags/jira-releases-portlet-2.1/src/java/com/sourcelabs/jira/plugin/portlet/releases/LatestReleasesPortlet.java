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
public class LatestReleasesPortlet extends PortletImpl
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

    public LatestReleasesPortlet(JiraAuthenticationContext authenticationContext, PermissionManager permissionManager, 
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
            Browser browser = new Browser(this.projectManager, this.constantsManager, this.searchProvider, this.permissionManager, this.versionManager, this.pluginManager);
            params.put("action", browser);

            Long size = portletConfiguration.getLongProperty("size");
            if(size == null) {
                size = new Long(6);
            }

            boolean loggedIn = (user != null);
            params.put("loggedin", new Boolean(loggedIn) );

            Collection projects = this.projectManager.getProjects();
            Iterator itr = projects.iterator();
            TreeMap map = new TreeMap(new Comparator() {
                public int compare(Object o1, Object o2) {
                    return ((Comparable)o2).compareTo(o1);
                }
            });
            while(itr.hasNext()) {
                GenericValue gv = (GenericValue) itr.next();
                List versions = this.versionManager.getVersions(gv.getLong("id"));
                Iterator itr2 = versions.iterator();
                while(itr2.hasNext()) {
                    Version v = (Version) itr2.next();
                    Date d = v.getReleaseDate();
                    if(d != null && v.isReleased() && !v.isArchived()) {
                        map.put(d, v);
                    }
                }
            }
            ArrayList list = new ArrayList(size.intValue());
            Object[] versions = map.values().toArray();
            for(int i=0; i<size.intValue() && i<versions.length; i++) {
                list.add(versions[i]);
            }
            params.put("justReleased", list);
        } catch(ObjectConfigurationException e) {
            e.printStackTrace();
        }

        return params;
    }

}
