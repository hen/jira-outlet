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
package com.sourcelabs.jira.plugin.portlet.releases;

import com.atlassian.configurable.ObjectConfigurationException;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.fields.NavigableField;
import com.atlassian.jira.issue.index.DocumentConstants;
import com.atlassian.jira.issue.search.SearchRequest;
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

import com.atlassian.query.Query;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;

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
    private final SearchProvider searchProvider;
    private final ApplicationProperties applicationProperties;
    private final VersionManager versionManager;
    private final ProjectManager projectManager;

    public ReleaseStatusPortlet(JiraAuthenticationContext authenticationContext, PermissionManager permissionManager, 
                          SearchProvider searchProvider, ApplicationProperties applicationProperties, 
                          VersionManager versionManager, ProjectManager projectManager) {
        super(authenticationContext);
        this.permissionManager = permissionManager;
        this.searchProvider = searchProvider;
        this.applicationProperties = applicationProperties;
        this.versionManager = versionManager;
        this.projectManager = projectManager;
    }

    // Pass the data required for the portlet display to the view template
    protected Map getVelocityParams(PortletConfiguration portletConfiguration)
    {
        Map params = new HashMap();
        params.put("portlet", this);  // automatic?
        Long portletId = portletConfiguration.getId();
        params.put("portletId", portletId);

        params.put("versionManager", this.versionManager);

        params.put("timeTrackingOn", new Boolean(this.applicationProperties.getOption("jira.option.timetracking")));

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
                params.put("releaseError", "portlet.releasestatus.errors.noparameters");
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
                    if(tmp == null) {
                        throw new RuntimeException("Version ID not found: " + versionIds[i]);
                    }
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
                        params.put("releaseError", "portlet.releasestatus.errors.noVersionInRandomProject");
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
                            params.put("releaseError", "portlet.releasestatus.errors.versionnotinset");
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
                params.put("releaseError", "portlet.releasestatus.errors.nosuchversion");
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
                    message = "portlet.releasestatus.happiness.success";
                    formula = "totalUnresolvedCount = 0";
                } else {
                    double daysToGo = ((double)(version.getReleaseDate().getTime() - System.currentTimeMillis())) / DAY_IN_MILLIS;
                    if(daysToGo < 0) {
                        message = "portlet.releasestatus.happiness.failed";
                        formula = "daysToGo = " + daysToGo;
                    } else {
                        double rate = (resolvedCount.doubleValue() - addedCount.doubleValue()) / WEEK_IN_DAYS;
                        double happiness = daysToGo * rate / totalUnresolvedCount;
                        params.put("happiness", new Double(happiness));
                        message = "portlet.releasestatus.happiness.dead";
                        formula = "daysToGo * rate / totalUnresolvedCount = " + happiness;
                        if(happiness > 1.1) {
                            message = "portlet.releasestatus.happiness.happy";
                        } else
                        if(happiness > 0.9) {
                            message = "portlet.releasestatus.happiness.shrug";
                        } else
                        if(happiness > 0) {
                            message = "portlet.releasestatus.happiness.sad";
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
        JqlClauseBuilder builder = JqlQueryBuilder.newClauseBuilder();
        Date sevenDaysAgo = new Date( System.currentTimeMillis() - WEEK_IN_DAYS * DAY_IN_MILLIS );
        builder = builder.project(projectId).and().fixVersion(""+version.getId()).and().createdAfter(sevenDaysAgo).and().reporterUser(user.getName());
        if(currentUser) {
            builder = builder.and().assigneeUser(user.getName());
        }
        return new Long(searchProvider.searchCount(builder.buildQuery(), user));
    }

    protected Long getResolvedRecently(Version version, User user, Long projectId, boolean currentUser) throws SearchException {
        JqlClauseBuilder builder = JqlQueryBuilder.newClauseBuilder();
        Date sevenDaysAgo = new Date( System.currentTimeMillis() - WEEK_IN_DAYS * DAY_IN_MILLIS );
        builder = builder.project(projectId).and().fixVersion(""+version.getId()).and().status(String.valueOf(IssueFieldConstants.RESOLVED_STATUS_ID), String.valueOf(IssueFieldConstants.CLOSED_STATUS_ID)).and().updatedAfter(sevenDaysAgo).and().reporterUser(user.getName());
        if(currentUser) {
            builder = builder.and().assigneeUser(user.getName());
        }
        return new Long(searchProvider.searchCount(builder.buildQuery(), user));
    }

    protected Long getUpdatedRecently(Version version, User user, Long projectId, boolean currentUser) throws SearchException {
        JqlClauseBuilder builder = JqlQueryBuilder.newClauseBuilder();
        Date sevenDaysAgo = new Date( System.currentTimeMillis() - WEEK_IN_DAYS * DAY_IN_MILLIS );
        builder = builder.project(projectId).and().fixVersion(""+version.getId()).and().updatedAfter(sevenDaysAgo).and().reporterUser(user.getName());
        if(currentUser) {
            builder = builder.and().assigneeUser(user.getName());
        }
        return new Long(searchProvider.searchCount(builder.buildQuery(), user));
    }

    protected Long getOverdue(Version version, User user, Long projectId, boolean currentUser) throws SearchException {
        JqlClauseBuilder builder = JqlQueryBuilder.newClauseBuilder();
        builder = builder.project(projectId).and().fixVersion(""+version.getId()).and().unresolved().and().dueAfter(new Date());
        if(currentUser) {
            builder = builder.and().assigneeUser(user.getName());
        }
        return new Long(searchProvider.searchCount(builder.buildQuery(), user));
    }

    protected Long getDueSoon(Version version, User user, Long projectId, boolean currentUser) throws SearchException {
        JqlClauseBuilder builder = JqlQueryBuilder.newClauseBuilder();
        Date sevenDaysTime = new Date( System.currentTimeMillis() + WEEK_IN_DAYS * DAY_IN_MILLIS );
        builder = builder.project(projectId).and().fixVersion(""+version.getId()).and().unresolved().and().dueBetween(new Date(), sevenDaysTime);
        if(currentUser) {
            builder = builder.and().assigneeUser(user.getName());
        }
        return new Long(searchProvider.searchCount(builder.buildQuery(), user));
    }

    protected Long getInProgress(Version version, User user, Long projectId, boolean currentUser) throws SearchException {
        JqlClauseBuilder builder = JqlQueryBuilder.newClauseBuilder();
        builder = builder.project(projectId).and().fixVersion(""+version.getId()).and().unresolved().and().dueAfter(new Date()).and().status(""+IssueFieldConstants.INPROGRESS_STATUS_ID);
        if(currentUser) {
            builder = builder.and().assigneeUser(user.getName());
        }
        return new Long(searchProvider.searchCount(builder.buildQuery(), user));
    }

    private static String[] splitVersionIds(String versionIdsConf) {
        if("".equals(versionIdsConf)) {
            return new String[0];
        } else {
            return versionIdsConf.split(",");
        }
    }

    protected PercentageGraphModel getSummaryForVersion(Version version, User user, boolean currentUser) throws SearchException {
        JqlClauseBuilder builder = JqlQueryBuilder.newClauseBuilder();
        builder = builder.project(version.getProject().getLong("id")).and().fixVersion(""+version.getId());
        if(currentUser) {
            builder = builder.and().assigneeUser(user.getName());
        }

        long total = searchProvider.searchCount(builder.buildQuery(), user);
        builder = builder.and().unresolved();
        long open = searchProvider.searchCount(builder.buildQuery(), user);
        PercentageGraphModel model = new PercentageGraphModel();
        if(total != 0) {
            String commonQuery = "fixfor="+version.getId()+"&pid="+version.getProject().getLong("id")+"&"; //new FixForParameter(list).getQueryString() + new ProjectParameter(version.getProject().getLong("id")).getQueryString();
            if(currentUser) commonQuery += DocumentConstants.ISSUE_ASSIGNEE + "=" + DocumentConstants.ISSUE_CURRENT_USER;
            String resolvedQuery = commonQuery + "&status=5&status=6"; //new ResolutionParameter((List) ManagerFactory.getConstantsManager().getResolutions()).getQueryString();
            String unresolvedQuery = commonQuery + "&resolution=-1"; // ResolutionParameter.UNRESOLVED.getQueryString();
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
