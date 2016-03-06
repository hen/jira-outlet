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
package com.sourcelabs.jira.plugin.portlet.projectlist;

import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.Project;
import com.atlassian.configurable.ObjectConfigurationException;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.portal.PortletConfiguration;
import com.atlassian.jira.portal.PortletImpl;
import com.atlassian.jira.issue.search.SearchRequestManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.opensymphony.user.User;
import org.ofbiz.core.entity.GenericValue;

import java.util.*;

/**
 * This portlet displays a hardcoded list of projects in a drop-down.
 */
public class ProjectListPortlet extends PortletImpl
{

    // References to managers required for this portlet
    private final PermissionManager permissionManager;
    private final ConstantsManager constantsManager;
    private final ProjectManager projectManager;
    private final SearchProvider searchProvider;
    private final ApplicationProperties applicationProperties;
    private final SearchRequestManager searchRequestManager;

    public ProjectListPortlet(JiraAuthenticationContext authenticationContext, PermissionManager permissionManager, 
                          ConstantsManager constantsManager, SearchProvider searchProvider, 
                          ApplicationProperties applicationProperties, SearchRequestManager searchRequestManager,
                          ProjectManager projectManager)  {
        super(authenticationContext);
        this.permissionManager = permissionManager;
        this.constantsManager = constantsManager;
        this.projectManager = projectManager;
        this.searchProvider = searchProvider;
        this.applicationProperties = applicationProperties;
        this.searchRequestManager = searchRequestManager;
    }

    // Pass the data required for the portlet display to the view template
    protected Map getVelocityParams(PortletConfiguration portletConfiguration)
    {
        Map params = new HashMap();
        params.put("portlet", this);  // automatic?
        params.put("portletId", portletConfiguration.getId());

        try {
            String projectRegexp = portletConfiguration.getProperty("projectlist-regexp");

            String projectTitle = portletConfiguration.getProperty("projectlist-title");
            params.put("projectTitle", projectTitle);

            String projectKeysConf = portletConfiguration.getTextProperty("projectlist-keys");
            String[] projectKeys = projectKeysConf.split(",");

            User user = this.authenticationContext.getUser();
            boolean loggedIn = (user != null);
            params.put("loggedin", new Boolean(loggedIn) );
            params.put("admin", new Boolean(this.permissionManager.hasPermission(Permissions.ADMINISTER, authenticationContext.getUser())));

            String includeLinks = portletConfiguration.getProperty("projectlist-includelinks");
            params.put("includeLinks", new Boolean("true".equals(includeLinks)) );

            // find ALL projects to start with. 
            Collection projects = this.permissionManager.getProjects(Permissions.BROWSE, user);
            if(projectKeys != null && projects != null && projectKeys.length != 0) {
              if( !(projectKeys.length == 1 && projectKeys[0].equals("")) ) {
                Map projectMap = new HashMap();
                Iterator iterator = projects.iterator();
                while(iterator.hasNext()) {
                    GenericValue request = (GenericValue) iterator.next();
                    String id = request.getString("key").toString();
                    projectMap.put(id, request);
                }
                projects = new ArrayList();
                for(int i=0; i<projectKeys.length; i++) {
                    String projectKey = projectKeys[i];
                    if(projectMap.containsKey(projectKey)) {
                        projects.add(projectMap.get(projectKey));
                    }
                }
              }
            }

            if(projectRegexp != null && !"".equals(projectRegexp)) {
                Iterator iterator = projects.iterator();
                while(iterator.hasNext()) {
                    GenericValue request = (GenericValue) iterator.next();
                    if(!request.get("name").toString().matches(projectRegexp)) {
                        iterator.remove();
                    }
                }
            }

            // Given the list of projects, now build the list of categories
            // TODO: Sort alphabetically
            ArrayList categories = new ArrayList();
            ArrayList noCategory = new ArrayList();
            HashMap categoryMap = new HashMap();
            Iterator itr = projects.iterator();
            while(itr.hasNext()) {
                GenericValue project = (GenericValue) itr.next();
                GenericValue category = this.projectManager.getProjectCategoryFromProject(project);
                if(category != null) {
                    List list = (List) categoryMap.get(category);
                    if(list == null) {
                        list = new ArrayList();
                        categoryMap.put(category, list);
                    }
                    list.add(project);
                } else {
                  noCategory.add(project);
                }
            }
            categories.addAll(categoryMap.keySet());
            Collections.sort(categories, 
                new java.util.Comparator() {
                    public int compare(Object o1, Object o2) {
                        GenericValue gv1 = (GenericValue) o1;
                        GenericValue gv2 = (GenericValue) o2;
                        return gv1.getString("name").compareTo(gv2.getString("name"));
                    }
                }
            );

            params.put("chosenProjects", projects);
            params.put("chosenCategories", categories);
            params.put("chosenCategoryMap", categoryMap);
            params.put("noCategory", noCategory);
        } catch(ObjectConfigurationException e) {
            e.printStackTrace();
        }
        return params;
    }

}
