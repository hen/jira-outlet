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
package com.sourcelabs.jira.plugin.portlet.html;

import com.atlassian.configurable.ObjectConfigurationException;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.portal.PortletConfiguration;
import com.atlassian.jira.portal.PortletImpl;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.opensymphony.user.User;

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
    private final ApplicationProperties applicationProperties;

    public HtmlPortlet(JiraAuthenticationContext authenticationContext, ApplicationProperties applicationProperties) {
        super(authenticationContext);
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
