/*
 * Copyright (c) 2007, SourceLabs, Inc.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, 
 * are permitted provided that the following conditions are met:
 * 
 *     * Redistributions of source code must retain the above copyright notice, 
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice, 
 *       this list of conditions and the following disclaimer in the documentation 
 *       and/or other materials provided with the distribution.
 *     * Neither the name of SourceLabs nor the names of its contributors 
 *       may be used to endorse or promote products derived from this software 
 *       without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.sourcelabs.jira.plugin.portlet.html;

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
import com.atlassian.jira.web.action.user.PersonalBrowser;
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
 * This portlet displays the status of the current user
 */
public class HtmlPortlet extends PortletImpl
{

    private static final String INTRODUCTION = "introduction";
    private static final String SPECIFIED = "specified";
    private static final String TITLE = "title";
    private static final String NONE = "none";

    // References to managers required for this portlet
    private final PermissionManager permissionManager;
    private final ConstantsManager constantsManager;
    private final SearchProvider searchProvider;
    private final ApplicationProperties applicationProperties;
    private final SearchRequestManager searchRequestManager;
    private final VersionManager versionManager;
    private final ProjectManager projectManager;
    private final PluginManager pluginManager;

    public HtmlPortlet(JiraAuthenticationContext authenticationContext, PermissionManager permissionManager, 
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
        params.put("user", user);

        try {
            String anonymousType = portletConfiguration.getProperty("anonymousType");
            String anonymousText = portletConfiguration.getTextProperty("anonymousText");
            String anonymousTitleType = portletConfiguration.getProperty("anonymousTitleType");
            String anonymousTitleText = portletConfiguration.getProperty("anonymousTitleText");
            String userType = portletConfiguration.getProperty("userType");
            String userText = portletConfiguration.getTextProperty("userText");
            String userTitleType = portletConfiguration.getProperty("userTitleType");
            String userTitleText = portletConfiguration.getProperty("userTitleText");
            String jiraIntro = this.applicationProperties.getText(APKeys.JIRA_INTRODUCTION);
            if(jiraIntro == null) {
                jiraIntro = "";
            }
            String jiraTitle = this.applicationProperties.getDefaultBackedString(APKeys.JIRA_TITLE);
            if(jiraIntro == null) {
                jiraIntro = "";
            }

            if(loggedIn && !NONE.equals(userType)) {
                if(INTRODUCTION.equals(userType)) {
                    params.put("text", jiraIntro);
                } else 
                if(SPECIFIED.equals(userType)) {
                    params.put("text", userText);
                }
                if(!NONE.equals(userTitleType)) {
                    if(TITLE.equals(userTitleType)) {
                        params.put("title", jiraTitle);
                    } else 
                    if(SPECIFIED.equals(userTitleType)) {
                        params.put("title", userTitleText);
                    }
                }
            } else
            if(!loggedIn && !NONE.equals(anonymousType)) {
                if(INTRODUCTION.equals(anonymousType)) {
                    params.put("text", jiraIntro);
                } else 
                if(SPECIFIED.equals(anonymousType)) {
                    params.put("text", anonymousText);
                }
                if(!NONE.equals(anonymousTitleType)) {
                    if(TITLE.equals(anonymousTitleType)) {
                        params.put("title", jiraTitle);
                    } else 
                    if(SPECIFIED.equals(anonymousTitleType)) {
                        params.put("title", anonymousTitleText);
                    }
                }
            }
        } catch(ObjectConfigurationException e) {
            e.printStackTrace();
        }

        return params;
    }


}
