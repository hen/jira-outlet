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
import com.atlassian.plugin.PluginManager;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.ManagerFactory;
import com.opensymphony.user.User;
import org.ofbiz.core.entity.EntityOperator;
import org.ofbiz.core.entity.GenericValue;
import org.ofbiz.core.entity.GenericEntityException;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.web.bean.PercentageGraphModel;
import com.atlassian.jira.web.bean.PercentageGraphRow;

import org.apache.commons.lang.StringUtils;


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
        Long portletId = portletConfiguration.getId();
        params.put("portletId", portletId);

        params.put("versionManager", this.versionManager);

        User user = authenticationContext.getUser();
        params.put("user", user);

        boolean loggedIn = (this.authenticationContext.getUser() != null);
        params.put("loggedin", new Boolean(loggedIn) );

        try {
            String projectIdConf = portletConfiguration.getProperty("projectid");
            if("".equals(projectIdConf) || "-1".equals(projectIdConf)) {
                projectIdConf = null;
                boolean randomProject = "true".equals(portletConfiguration.getProperty("randomproject"));
                if(randomProject) {
                    projectIdConf = getRandomProjectId();
                }
            }
            String defaultUserType = portletConfiguration.getProperty("defaultUserType");
            String versionIdConf = portletConfiguration.getProperty("versionid");
            if("".equals(versionIdConf)) {
                versionIdConf = null;
            }
            String versionIdsConf = portletConfiguration.getTextProperty("versionlist");
            if("".equals(versionIdsConf)) {
                versionIdsConf = null;
            }

            if(projectIdConf == null && versionIdConf == null && versionIdsConf == null) {
                params.put("releaseError", "portlet.releases.errors.noparameters");
                return params;
            }

            if( (portletId.intValue() == -1) && (versionIdsConf != null) && (versionIdsConf.indexOf(":") != -1) ) {
                params.put("portletId", Long.valueOf(StringUtils.substringAfter(versionIdsConf, ":")) );
                versionIdsConf = StringUtils.substringBefore(versionIdsConf, ":");
            }

            Long projectId = null;
            if(projectIdConf != null) {
                projectId = Long.valueOf(projectIdConf);
                Project project = this.projectManager.getProjectObj(projectId);
                if(!this.permissionManager.hasPermission(Permissions.BROWSE, project.getGenericValue(), user)) {
                    params.put("projectVisible", Boolean.FALSE);
                    return params;
                }
                params.put("project", project);
                if(versionIdsConf == null) {
                    Collection versions = project.getVersions();
                    Iterator itr = versions.iterator();
                    StringBuffer buffer = new StringBuffer();
                    while(itr.hasNext()) {
                        Version version = (Version) itr.next();
                        buffer.append(version.getId());
                        if(itr.hasNext()) {
                            buffer.append(",");
                        }
                    }
                    versionIdsConf = buffer.toString();
                }
            }

            Version version = null;
            if(versionIdConf == null) {
                String[] versionIds = splitVersionIds(versionIdsConf);
                for(int i=0; i<versionIds.length; i++) {
                    Version tmp = this.versionManager.getVersion(Long.valueOf(versionIds[i]));
                    if(!tmp.isReleased() && !tmp.isArchived()) {
                        version = tmp;
                        versionIdConf = version.getId().toString();
                        break;
                    }
                }
                if(version == null) {
                    if(versionIdConf == null && versionIds.length > 0) {
                        versionIdConf = versionIds[0];
                        Long versionId = Long.valueOf(versionIdConf);
                        version = this.versionManager.getVersion(versionId);
                    } else {
                        params.put("releaseError", "portlet.releases.errors.noVersionInRandomProject");
                        return params;
                    }
                }
            } else {
                Long versionId = Long.valueOf(versionIdConf);
                versionIdConf = versionId.toString();
                version = this.versionManager.getVersion(versionId);
            }

            if(versionIdsConf != null) {
                String[] versionIds = splitVersionIds(versionIdsConf);

                if(versionIds.length != 0) {
                    versionIdsConf = StringUtils.join(versionIds, "%2C");
                    params.put("versionlist", versionIdsConf);
    
                    String previousVersion = null;
                    String nextVersion = null;
                    for(int i=0; i<versionIds.length; i++) {
                        if(i != versionIds.length - 1) {
                            nextVersion = versionIds[i + 1];
                        } else {
                            nextVersion = null;
                        }
                        if(versionIdConf.equals(versionIds[i])) {
                            break;
                        }
                        if(i != versionIds.length - 1) {
                            previousVersion = versionIds[i];
                        } else {
                            params.put("releaseError", "portlet.releases.errors.versionnotinset");
                            return params;
                        }
                    }

                    if(previousVersion != null) {
                        params.put("previousVersion", this.versionManager.getVersion(Long.valueOf(previousVersion)));
                    }
                    if(nextVersion != null) {
                        params.put("nextVersion", this.versionManager.getVersion(Long.valueOf(nextVersion)));
                    }
                }
            }

            // TODO: Check nextVersion and previousVersion for permissions. Less important than 
            //       the checking for version being visible above as this really means the portlet 
            //       is misconfigured.
            if(version == null) {
                params.put("releaseError", "portlet.releases.errors.nosuchversion");
                return params;
            }

            if(!this.permissionManager.hasPermission(Permissions.BROWSE, version.getProject(), user)) {
                params.put("projectVisible", Boolean.FALSE);
                return params;
            } else {
                params.put("projectVisible", Boolean.TRUE);
            }
            params.put("version", version);

            boolean currentUser = "current".equals(defaultUserType);
            if("adaptive".equals(defaultUserType)) {
                PercentageGraphModel pgm = getSummaryForVersion(version, user, false);
                if(pgm != null) {
                    List rows = pgm.getRows();
                    if(rows.size() >= 2) {
                        PercentageGraphRow row = (PercentageGraphRow) rows.get(0);
                        int percentage = pgm.getPercentage(row);
                        if(percentage > 49 && percentage < 95) {
                            currentUser = true;
                        }
                    }
                }
            }
            if(!loggedIn) {
                currentUser = false;
            }
            params.put("showCurrentUser", new Boolean(currentUser));
            if(currentUser) {
                params.put("switchUserType", "all");
            } else {
                params.put("switchUserType", "current");
            }
            params.put("defaultUserType", defaultUserType);


            PercentageGraphModel pgm = getSummaryForVersion(version, user, currentUser);

            List rows = pgm.getRows();
            long totalUnresolvedCount = -1;
            if(rows.size() >= 2) {
                PercentageGraphRow row = (PercentageGraphRow) rows.get(0);
                int percentage = pgm.getPercentage(row);
                long totalCount = row.getNumber();
                row = (PercentageGraphRow) rows.get(1);
                totalUnresolvedCount = row.getNumber();
                totalCount += totalUnresolvedCount;
                params.put("totalCount", new Long(totalCount));
                params.put("percentage", new Integer(percentage));
            }
            params.put("pgm", pgm);

// TODO: Don't do all this if the pgm had no issues?
            Long pid = Long.valueOf(version.getProject().getString("id"));
            Long resolvedCount = getResolvedRecently(version, user, pid, currentUser);
            params.put("resolvedCount", resolvedCount);
            Long addedCount = getAddedRecently(version, user, pid, currentUser);
            params.put("addedCount", addedCount);
            params.put("totalOpenCount", new Long(totalUnresolvedCount));
            params.put("overdueCount", getOverdue(version, user, pid, currentUser));
            params.put("updatedCount", getUpdatedRecently(version, user, pid, currentUser));
            params.put("dueSoonCount", getDueSoon(version, user, pid, currentUser));
            params.put("inProgressCount", getInProgress(version, user, pid, currentUser));

            // Happy-Sad
            if(version.getReleaseDate() != null && totalUnresolvedCount != -1) {
                String message = null;
                String formula = null;
                if(totalUnresolvedCount == 0) {
                    message = "portlet.releases.happiness.success";
                    formula = "totalUnresolvedCount = 0";
                } else {
                    double daysToGo = ((double)(version.getReleaseDate().getTime() - System.currentTimeMillis())) / DAY_IN_MILLIS;
                    if(daysToGo < 0) {
                        message = "portlet.releases.happiness.failed";
                        formula = "daysToGo = " + daysToGo;
                    } else {
                        double rate = (resolvedCount.doubleValue() - addedCount.doubleValue()) / WEEK_IN_DAYS;
                        double happiness = daysToGo * rate / totalUnresolvedCount;
                        params.put("happiness", new Double(happiness));
                        message = "portlet.releases.happiness.dead";
                        formula = "daysToGo * rate / totalUnresolvedCount = " + happiness;
                        if(happiness > 1.1) {
                            message = "portlet.releases.happiness.happy";
                        } else
                        if(happiness > 0.9) {
                            message = "portlet.releases.happiness.shrug";
                        } else
                        if(happiness > 0) {
                            message = "portlet.releases.happiness.sad";
                        }
                    }
                }
                params.put("happinessMessage", message);
                params.put("formula", formula);
            }

        } catch(SearchException e) {
            e.printStackTrace();
        } catch(NumberFormatException e) {
            e.printStackTrace();
        } catch(ObjectConfigurationException e) {
            e.printStackTrace();
        }

        return params;
    }

    protected Long getAddedRecently(Version version, User user, Long projectId, boolean currentUser) throws SearchException {
        SearchRequest sr = new SearchRequest(user);
        Date sevenDaysAgo = new Date( System.currentTimeMillis() - WEEK_IN_DAYS * DAY_IN_MILLIS );
        sr.addParameter(new AbsoluteDateRangeParameter(DocumentConstants.ISSUE_CREATED, sevenDaysAgo, null));
        sr.addParameter(new ProjectParameter(projectId));
        if(currentUser) sr.addParameter(new UserParameter(DocumentConstants.ISSUE_ASSIGNEE, user.getName()));
        ArrayList list = new ArrayList(1);
        list.add(version);
        sr.addParameter(new FixForParameter(list));
        return new Long(searchProvider.searchCount(sr, user));
    }

    protected Long getResolvedRecently(Version version, User user, Long projectId, boolean currentUser) throws SearchException {
        SearchRequest sr = new SearchRequest(user);
        ArrayList statuses = new ArrayList(2);
        statuses.add(String.valueOf(IssueFieldConstants.RESOLVED_STATUS_ID));
        statuses.add(String.valueOf(IssueFieldConstants.CLOSED_STATUS_ID));
        sr.addParameter(new StatusParameter(statuses));
        Date sevenDaysAgo = new Date( System.currentTimeMillis() - WEEK_IN_DAYS * DAY_IN_MILLIS );
        sr.addParameter(new AbsoluteDateRangeParameter(DocumentConstants.ISSUE_UPDATED, sevenDaysAgo, null));
        sr.addParameter(new ProjectParameter(projectId));
        if(currentUser) sr.addParameter(new UserParameter(DocumentConstants.ISSUE_ASSIGNEE, user.getName()));
        ArrayList list = new ArrayList(1);
        list.add(version);
        sr.addParameter(new FixForParameter(list));
        return new Long(searchProvider.searchCount(sr, user));
    }

    protected Long getUpdatedRecently(Version version, User user, Long projectId, boolean currentUser) throws SearchException {
        SearchRequest sr = new SearchRequest(user);
        Date sevenDaysAgo = new Date( System.currentTimeMillis() - WEEK_IN_DAYS * DAY_IN_MILLIS );
        sr.addParameter(new AbsoluteDateRangeParameter(DocumentConstants.ISSUE_UPDATED, sevenDaysAgo, null));
        sr.addParameter(new ProjectParameter(projectId));
        if(currentUser) sr.addParameter(new UserParameter(DocumentConstants.ISSUE_ASSIGNEE, user.getName()));
        ArrayList list = new ArrayList(1);
        list.add(version);
        sr.addParameter(new FixForParameter(list));
        return new Long(searchProvider.searchCount(sr, user));
    }

    protected Long getOverdue(Version version, User user, Long projectId, boolean currentUser) throws SearchException {
        SearchRequest sr = new SearchRequest(user);
        sr.addParameter(ResolutionParameter.UNRESOLVED);
        sr.addParameter(new AbsoluteDateRangeParameter(DocumentConstants.ISSUE_DUEDATE, null, new Date()));
        sr.addParameter(new ProjectParameter(projectId));
        if(currentUser) sr.addParameter(new UserParameter(DocumentConstants.ISSUE_ASSIGNEE, user.getName()));
        ArrayList list = new ArrayList(1);
        list.add(version);
        sr.addParameter(new FixForParameter(list));
        return new Long(searchProvider.searchCount(sr, user));
    }

    protected Long getDueSoon(Version version, User user, Long projectId, boolean currentUser) throws SearchException {
        SearchRequest sr = new SearchRequest(user);
        sr.addParameter(ResolutionParameter.UNRESOLVED);
        Date sevenDaysTime = new Date( System.currentTimeMillis() + WEEK_IN_DAYS * DAY_IN_MILLIS );
        sr.addParameter(new AbsoluteDateRangeParameter(DocumentConstants.ISSUE_DUEDATE, new Date(), sevenDaysTime));
        sr.addParameter(new ProjectParameter(projectId));
        if(currentUser) sr.addParameter(new UserParameter(DocumentConstants.ISSUE_ASSIGNEE, user.getName()));
        ArrayList list = new ArrayList(1);
        list.add(version);
        sr.addParameter(new FixForParameter(list));
        return new Long(searchProvider.searchCount(sr, user));
    }

    protected Long getInProgress(Version version, User user, Long projectId, boolean currentUser) throws SearchException {
        SearchRequest sr = new SearchRequest(user);
        sr.addParameter(ResolutionParameter.UNRESOLVED);
        sr.addParameter(new ProjectParameter(projectId));
        if(currentUser) sr.addParameter(new UserParameter(DocumentConstants.ISSUE_ASSIGNEE, user.getName()));
        ArrayList list = new ArrayList(1);
        list.add(version);
        sr.addParameter(new FixForParameter(list));
        sr.addParameter(new StatusParameter("" + IssueFieldConstants.INPROGRESS_STATUS_ID));
        return new Long(searchProvider.searchCount(sr, user));
    }

    private static String[] splitVersionIds(String versionIdsConf) {
        String[] versionIds = null;
        if(versionIdsConf.indexOf(",") != -1) {
            versionIds = versionIdsConf.split(",");
        } else
        if(versionIdsConf.indexOf("%2C") != -1) {
            versionIds = versionIdsConf.split(",");
        }
        if(versionIds == null) {
            versionIds = new String[0];
        }
        return versionIds;
    }

    protected PercentageGraphModel getSummaryForVersion(Version version, User user, boolean currentUser) throws SearchException {
        SearchRequest sr = new SearchRequest(user);
        if(currentUser) sr.addParameter(new UserParameter(DocumentConstants.ISSUE_ASSIGNEE, user.getName()));
        ArrayList list = new ArrayList(1);
        list.add(version);
        sr.addParameter(new FixForParameter(list));
        sr.addParameter(new ProjectParameter(version.getProject().getLong("id")));
        long total = searchProvider.searchCount(sr, user);
        sr.addParameter(ResolutionParameter.UNRESOLVED);
        long open = searchProvider.searchCount(sr, user);
        PercentageGraphModel model = new PercentageGraphModel();
        if(total != 0) {
            String commonQuery = new FixForParameter(list).getQueryString() + new ProjectParameter(version.getProject().getLong("id")).getQueryString();
            if(currentUser) commonQuery += new UserParameter(DocumentConstants.ISSUE_ASSIGNEE, DocumentConstants.ISSUE_CURRENT_USER).getQueryString();
            String resolvedQuery = commonQuery + new ResolutionParameter((List) ManagerFactory.getConstantsManager().getResolutions()).getQueryString();
            String unresolvedQuery = commonQuery +  ResolutionParameter.UNRESOLVED.getQueryString();
            // TODO: i18nize the strings below
            model.addRow("009900", total - open, "Resolved Issues", resolvedQuery);
            model.addRow("CC0000", open, "Open Issues", unresolvedQuery);
        }
        return model;
    }

    protected String getRandomProjectId() {
        Collection projects = this.projectManager.getProjects();
        Random rnd = new Random();
        int idx = rnd.nextInt(projects.size());
        GenericValue project = (GenericValue) (projects.toArray()[idx]);
        return project.getString("id");
    }
}
