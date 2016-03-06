package com.sourcelabs.jira.plugin.portlet.filterlist;

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
import com.atlassian.jira.issue.search.parameters.lucene.DateParameter;
import com.atlassian.jira.issue.search.parameters.lucene.ProjectParameter;
import com.atlassian.jira.issue.search.parameters.lucene.StatusParameter;
import com.atlassian.jira.portal.PortletConfiguration;
import com.atlassian.jira.portal.PortletImpl;
import com.atlassian.jira.issue.search.SearchRequestManager;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.web.bean.FieldVisibilityBean;
import com.atlassian.jira.web.util.IssueTableBean;
import com.opensymphony.user.User;
import org.ofbiz.core.entity.EntityOperator;
import org.ofbiz.core.entity.GenericValue;
import org.ofbiz.core.entity.GenericEntityException;

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
    private final SearchRequestManager searchRequestManager;

    public FilterListPortlet(JiraAuthenticationContext authenticationContext, PermissionManager permissionManager, 
                          ConstantsManager constantsManager, SearchProvider searchProvider, 
                          ApplicationProperties applicationProperties, SearchRequestManager searchRequestManager)  {
        super(authenticationContext);
        this.permissionManager = permissionManager;
        this.constantsManager = constantsManager;
        this.searchProvider = searchProvider;
        this.applicationProperties = applicationProperties;
        this.searchRequestManager = searchRequestManager;
    }

    // Pass the data required for the portlet display to the view template
    protected Map getVelocityParams(PortletConfiguration portletConfiguration)
    {
        Map params = new HashMap();
        params.put("portlet", this);  // automatic?

        try {
            String filterTitle = portletConfiguration.getTextProperty("filterlist-title");
            params.put("filterTitle", filterTitle);

            String filterIdsConf = portletConfiguration.getTextProperty("filterlist-ids");
            String[] filterIds = filterIdsConf.split(",");

            params.put("indexing", new Boolean(applicationProperties.getOption(APKeys.JIRA_OPTION_INDEXING)));

            boolean loggedIn = (this.authenticationContext.getUser() != null);
            params.put("loggedin", new Boolean(loggedIn) );

            if(loggedIn) {
                // find ALL filters to start with. somehow.
                List filters = this.searchRequestManager.getVisibleRequests( this.authenticationContext.getUser() );
                if(filterIds != null && filters != null && filterIds.length != 0) {
                    Map filterMap = new HashMap();
                    Iterator iterator = filters.iterator();
                    while(iterator.hasNext()) {
                        GenericValue request = (GenericValue) iterator.next();
                        String id = request.getPrimaryKey().get("id").toString();
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
                params.put("chosenFilters", filters);
            }
        } catch(GenericEntityException e) {
            e.printStackTrace();
        } catch(ObjectConfigurationException e) {
            e.printStackTrace();
        }
        return params;
    }

    public long getCountsForFilter(GenericValue gv) throws GenericEntityException, SearchException {
        Long id = (Long) gv.getPrimaryKey().get("id");
        User user = this.authenticationContext.getUser();
        SearchRequest request = this.searchRequestManager.getRequest( user, id );
        return this.searchProvider.searchCount( request, user );
    }

}
