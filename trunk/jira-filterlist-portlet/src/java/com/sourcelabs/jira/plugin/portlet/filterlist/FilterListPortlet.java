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
package com.sourcelabs.jira.plugin.portlet.filterlist;

import com.atlassian.configurable.ObjectConfigurationException;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.portal.PortletConfiguration;
import com.atlassian.jira.portal.PortletImpl;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.opensymphony.user.User;

import com.atlassian.jira.sharing.search.*;
import com.atlassian.jira.sharing.SharedEntityColumn;

import java.util.*;

/**
 * This portlet displays a hardcoded list of filters in a drop-down.
 */
public class FilterListPortlet extends PortletImpl
{

    // References to managers required for this portlet
    private final PermissionManager permissionManager;
    private final ConstantsManager constantsManager;
    private final SearchProvider searchProvider;
    private final ApplicationProperties applicationProperties;
    private final SearchRequestService searchRequestService;

    public FilterListPortlet(JiraAuthenticationContext authenticationContext, PermissionManager permissionManager,
                          ConstantsManager constantsManager, SearchProvider searchProvider,
                          ApplicationProperties applicationProperties, SearchRequestService searchRequestService)  {
        super(authenticationContext);
        this.permissionManager = permissionManager;
        this.constantsManager = constantsManager;
        this.searchProvider = searchProvider;
        this.applicationProperties = applicationProperties;
        this.searchRequestService = searchRequestService;
    }

    // Pass the data required for the portlet display to the view template
    protected Map getVelocityParams(PortletConfiguration portletConfiguration)
    {
        Map params = new HashMap();
        params.put("portlet", this);  // automatic?
        params.put("portletId", portletConfiguration.getId());

        try {
            String onlyFavourites = portletConfiguration.getProperty("filterlist-onlyfavourites");
            String filterRegexp = portletConfiguration.getProperty("filterlist-regexp");

            String filterDisplay = portletConfiguration.getProperty("filterlist-display");
            params.put("displayAsList", new Boolean(!"dropdown".equalsIgnoreCase(filterDisplay)) );

            String filterTitle = portletConfiguration.getProperty("filterlist-title");
            params.put("filterTitle", filterTitle);

            String filterIdsConf = portletConfiguration.getTextProperty("filterlist-ids");
            String[] filterIds = filterIdsConf.split("[, \n]");

            params.put("indexing", new Boolean(applicationProperties.getOption(APKeys.JIRA_OPTION_INDEXING)));

            boolean loggedIn = (this.authenticationContext.getUser() != null);
            params.put("loggedin", new Boolean(loggedIn) );

            String includeLinks = portletConfiguration.getProperty("filterlist-includelinks");
            params.put("includeLinks", new Boolean("true".equals(includeLinks)) );

            String includeCounts = portletConfiguration.getProperty("filterlist-includecounts");
            params.put("includeCounts", new Boolean("true".equals(includeCounts)) );

            if(loggedIn) {
                // find ALL filters to start with.
                User user = this.authenticationContext.getUser();
                Collection filters = null;

                if("true".equals(onlyFavourites)) {
                    filters = this.searchRequestService.getFavouriteFilters( user );
                } else {
                    // This gets all of the filters (up to the hardcoded 400 anyway at which I expect 
                    // performance to have long since become a problem).
                    SharedEntitySearchResult sesr = this.searchRequestService.search( new JiraServiceContextImpl(user), new NullSharedEntitySearchParameters(), 0, 400 );
                    filters = sesr.getResults();
                }

                if(filterIds != null && filters != null && filterIds.length != 0) {
                  if( !(filterIds.length == 1 && filterIds[0].equals("")) ) {
                    Map filterMap = new HashMap();
                    Iterator iterator = filters.iterator();
                    while(iterator.hasNext()) {
                        SearchRequest request = (SearchRequest) iterator.next();
                        String id = request.getId().toString();
                        filterMap.put(id, request);
                    }
                    filters = new ArrayList();
                    for(int i=0; i<filterIds.length; i++) {
                        String filterId = filterIds[i];
                        if(filterMap.containsKey(filterId)) {
                            filters.add(filterMap.get(filterId));
                        }
                    }
                  }
                }

                if(filterRegexp != null && !"".equals(filterRegexp)) {
                    Iterator iterator = filters.iterator();
                    try {
                        while(iterator.hasNext()) {
                            SearchRequest request = (SearchRequest) iterator.next();
                            if(!request.getName().matches(filterRegexp)) {
                                iterator.remove();
                            }
                        }
                    } catch(java.util.regex.PatternSyntaxException pse) {
                        params.put("regexWarning", pse.getMessage());
                    }
                }
                params.put("chosenFilters", filters);
            }
        } catch(ObjectConfigurationException oce) {
            oce.printStackTrace();
        }
        return params;
    }

    public long getCountsForFilter(SearchRequest sr) throws SearchException {
        User user = this.authenticationContext.getUser();
        return this.searchProvider.searchCount( sr.getQuery(), user );
    }

    private class NullSharedEntitySearchParameters implements SharedEntitySearchParameters {
        public String getDescription() { return null; }
        public Boolean getFavourite() { return null; }
        public String getName() { return null; }
        public ShareTypeSearchParameter getShareTypeParameter() { return null; }
        public SharedEntityColumn getSortColumn() { return SharedEntityColumn.NAME; }
        public SharedEntitySearchParameters.TextSearchMode getTextSearchMode() { return null; }
        public String getUserName() { return null; }
        public boolean isAscendingSort() { return true; }
    }

}
