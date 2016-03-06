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
package com.sourcelabs.jira.plugin.portlet.randomfilter;

import com.atlassian.jira.portal.portlets.*;
    
import com.atlassian.configurable.ObjectConfigurationException;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.exception.DataAccessException;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.issue.search.SearchRequestManager;
import com.atlassian.jira.portal.PortletConfiguration;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.web.bean.FieldVisibilityBean;
import com.atlassian.jira.web.bean.PagerFilter;
import com.opensymphony.user.User; 
import org.apache.log4j.Category;
import com.atlassian.jira.web.util.IssueTableBean;

import org.apache.commons.lang.math.NumberUtils;

import java.util.*;

public class RandomFilterPortlet extends AbstractRequiresUserPortlet {

    private static final Category log = Category.getInstance(RandomFilterPortlet.class);
    private final SearchRequestManager searchRequestManager;

    protected final ConstantsManager constantsManager;
    protected final SearchProvider searchProvider;

    public RandomFilterPortlet(JiraAuthenticationContext authenticationContext, PermissionManager permissionManager,
                                ConstantsManager constantsManager, SearchProvider searchProvider,
                                ApplicationProperties applicationProperties, SearchRequestManager searchRequestManager)
    {
        super(authenticationContext, permissionManager, applicationProperties);
        this.constantsManager = constantsManager;
        this.searchProvider = searchProvider;
        this.searchRequestManager = searchRequestManager;
    }


    protected String getLinkToSearch(SearchRequest searchRequest, PortletConfiguration portletConfiguration) {
        return "IssueNavigator.jspa?requestId=" + searchRequest.getSearchFilterId() +  "&mode=hide";
    }

    protected String getSearchName(SearchRequest searchRequest) {
        return searchRequest.getName();
    }

    protected String getSearchTypeName() {
        return authenticationContext.getI18nBean().getText("portlet.savedfilter.issues");
    }

    protected String getNoIssuesText() {
        return authenticationContext.getI18nBean().getText("portlet.savedfilter.noissues");
    }

    protected Map getVelocityParams(PortletConfiguration portletConfiguration) {
        Map params = super.getVelocityParams(portletConfiguration);

        try {

            String filterIdStr = portletConfiguration.getProperty("filterid");
            int idx = filterIdStr.indexOf(":");
            String portletIdStr = null;
            if(idx != -1) {
                portletIdStr = filterIdStr.substring(idx + 1);
                filterIdStr = filterIdStr.substring(0, idx);
            }
            Long filterId = new Long(NumberUtils.toInt(filterIdStr));
            params.put("filterid", filterId);
            if(portletIdStr == null) {
                params.put("portletId", portletConfiguration.getId());
            } else {
                params.put("portletId", portletIdStr);
            }

            String numOfEntriesStr = portletConfiguration.getProperty("numofentries");
            String startStr = null;
            idx = numOfEntriesStr.indexOf(":");
            if(idx != -1) {
                startStr = numOfEntriesStr.substring(idx + 1);
                numOfEntriesStr = numOfEntriesStr.substring(0, idx);
            }
            int maxEntryCount = NumberUtils.toInt(numOfEntriesStr);
            params.put("numofentries", new Integer(maxEntryCount));

            User remoteUser = authenticationContext.getUser();
            params.put("user", remoteUser);
            SearchRequest searchRequest = searchRequestManager.getRequest(remoteUser, filterId);
            params.put("searchRequest", searchRequest);
            if (searchRequest != null) {
                int start = 0;
                int total = (int) searchProvider.searchCount(searchRequest, remoteUser);
                if(startStr == null) {
                    start = randomStart(remoteUser, maxEntryCount, searchRequest, total);
                } else {
                    start = NumberUtils.toInt(startStr);
                }
                if(start + maxEntryCount <= total) {
                    params.put("nextStart", new Integer(start + maxEntryCount));
                }
                if(start - maxEntryCount >= 0) {
                    params.put("previousStart", new Integer(start - maxEntryCount));
                } else
                if(start > 0) {
                    params.put("previousStart", new Integer(0));
                }
                SearchResults searchResults = getSearchResults(remoteUser, maxEntryCount, searchRequest, start);
                int totalNumIssues = 0;
                List issues = null;
                int totalCount = 0;
                if (searchResults != null) {
                    totalNumIssues = searchResults.getTotal();
                    issues = searchResults.getIssues();
                    totalCount = searchResults.getEnd();
                }
                if (issues != null && !issues.isEmpty()) {
                    params.put("issues", issues); 
                }
                params.put("displayedIssueCount", new Integer(totalCount));
                params.put("totalNumIssues", new Integer(totalNumIssues));
                params.put("constantsManager", constantsManager);
                params.put("issueBean", new IssueTableBean());
                params.put("fieldVisibility", new FieldVisibilityBean());
                params.put("portlet", this);
                params.put("linkToSearch", getLinkToSearch(searchRequest, portletConfiguration));
                params.put("searchName", getSearchName(searchRequest));
                params.put("searchTypeName", getSearchTypeName());
                params.put("noIssuesText", getNoIssuesText());

                boolean showDescription = "true".equals(portletConfiguration.getProperty("showdescription"));
                params.put("showdescription", new Boolean(showDescription));
                if (showDescription) {
                    params.put("description", searchRequest.getDescription());
                }
            }
        } catch (Exception e) {
            log.error("Could not create velocity parameters " + e.getMessage(),e);
        }

        return params;
    }

    private int randomStart(User remoteUser, int numberOfIssuesToDisplay, SearchRequest sr, int total) {
        try {
            Random rnd = new Random();
            int start = rnd.nextInt( total);
            if(start > total - numberOfIssuesToDisplay) {
                start = total - numberOfIssuesToDisplay;
            }
            return start;
        } catch (Exception e) {
            log.error("Could not get issues", e);
            return 0;
        }
    }

    private SearchResults getSearchResults(User remoteUser, int numberOfIssuesToDisplay, SearchRequest sr, int start) {
        try {
            PagerFilter pagerFilter = new PagerFilter(numberOfIssuesToDisplay);
            pagerFilter.setStart(start);

            return searchProvider.search(sr, remoteUser, pagerFilter);
        } catch (Exception e) {
            log.error("Could not get issues", e);
            return null;
        }
    }

}

/*
class RandomPagerFilter extends PagerFilter {

    public List getRandomSelection(List itemsCol, int sizeToGet) {
        int totSize = itemsCol.size();

        if(totSize < sizeToGet) {
            sizeToGet = totSize;
        }

        List list = new ArrayList(sizeToGet);
        Set seen = new TreeSet();
        Random rnd = new Random();

        while(list.size() < sizeToGet) {
            int num = rnd.nextInt(totSize);
            Object item = itemsCol.get(num);
            if(!seen.contains(item)) {
                seen.add(item);
                list.add(item);
            }
        }

        return list;
    }
}
*/
