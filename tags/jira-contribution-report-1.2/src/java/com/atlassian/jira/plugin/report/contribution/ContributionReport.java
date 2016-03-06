package com.sourcelabs.jira.plugin.report.contribution;

import org.ofbiz.core.entity.GenericValue;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.attachment.Attachment;

import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;

import com.atlassian.core.util.DateUtils;
import com.atlassian.jira.issue.index.DocumentConstants;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.issue.search.parameters.lucene.ProjectParameter;
import com.atlassian.jira.issue.search.parameters.lucene.FixForParameter;
import com.atlassian.jira.plugin.report.impl.AbstractReport;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.ParameterUtils;
import com.atlassian.jira.web.action.ProjectActionSupport;
import com.atlassian.jira.web.bean.I18nBean;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.opensymphony.user.EntityNotFoundException;
import com.opensymphony.user.User;
import com.opensymphony.user.UserManager;
import org.apache.log4j.Logger;
import org.ofbiz.core.entity.EntityOperator;

import java.util.*;

public class ContributionReport extends AbstractReport {

    private final ProjectManager projectManager;
    private final VersionManager versionManager;
    private final IssueManager issueManager;
    private final CommentManager commentManager;
    private final UserManager userManager;
    private final PermissionManager permissionManager;
    private final SearchProvider searchProvider;

    public ContributionReport(ProjectManager projectManager, VersionManager versionManager, IssueManager issueManager, CommentManager commentManager, UserManager userManager, PermissionManager permissionManager, SearchProvider searchProvider) {
        this.projectManager = projectManager;
        this.versionManager = versionManager;
        this.issueManager = issueManager;
        this.commentManager = commentManager;
        this.userManager = userManager;
        this.permissionManager = permissionManager;
        this.searchProvider = searchProvider;
    }

    // Generate the report
    public String generateReportHtml(ProjectActionSupport action, Map params) throws Exception {
        User remoteUser = action.getRemoteUser();
        I18nHelper i18nBean = new I18nBean(remoteUser);

        // Retrieve the project parameter
        Long projectId = ParameterUtils.getLongParam(params, "selectedProjectId");
        // Retrieve the version id
        Long versionId = ParameterUtils.getLongParam(params, "versionId");
        // What type of issues to show
        String issueStatus = ParameterUtils.getStringParam(params, "issueStatus");

        Project project = projectManager.getProjectObj(projectId);

        if(!this.permissionManager.hasPermission(Permissions.BROWSE, project, remoteUser)) {
            return descriptor.getHtml("view", new HashMap());  // can I send an error instead??
        }

        Version version = this.versionManager.getVersion(versionId);

        // Retrieve list of issues for this project/version
        SearchRequest sr = new SearchRequest(remoteUser);
        sr.addParameter(new ProjectParameter(project.getId()));
        if(version != null) {
            ArrayList list = new ArrayList(1);
            list.add(version);
            sr.addParameter(new FixForParameter(list));
        }
        // TODO: Move onto PagerFilters and off of this deprecated API
        Collection issues = searchProvider.search(sr, remoteUser);            

        // Using a Map, store user:contributions
        ContributionMap contributions = new ContributionMap(this.userManager, this.permissionManager, project.getGenericValue());
        ContributionList contributionList = new ContributionList();

        Iterator itr = issues.iterator();
        while(itr.hasNext()) {
            GenericValue gv = (GenericValue) itr.next();
            Issue issue = this.issueManager.getIssueObject(gv.getLong("id"));

            if("open".equals(issueStatus)) {
                // Open 1
                // In Progress 3
                // Reopened 4
                // However - do the opposite of closed instead of the above
                if(issue.getStatusObject().getId().equals("5") || 
                   issue.getStatusObject().getId().equals("6")
                ) {
                    continue;
                }
            } else
            if("closed".equals(issueStatus)) {
                // Resolved 5
                // Closed 6
                if(!issue.getStatusObject().getId().equals("5") &&
                   !issue.getStatusObject().getId().equals("6")
                ) {
                    continue;
                }
            }

            // Store reporter
            String reporter = issue.getReporterId();
            if(reporter != null) {
                contributions.getContribution(reporter).addReportedIssue(issue);
                contributionList.incrReportedIssuesCount();
            }

            // Store assignee
            String assignee = issue.getAssigneeId();
            if(assignee != null) {
                contributions.getContribution(assignee).addAssignedIssue(issue);
                contributionList.incrAssignedIssuesCount();
            }

            // Iterate over comments, store commenter
            Collection comments = this.commentManager.getCommentsForUser(issue, remoteUser);
            Iterator comItr = comments.iterator();
            while(comItr.hasNext()) {
                Comment comment = (Comment) comItr.next();
                String author = comment.getAuthor();
                if(author != null) {
                    contributions.getContribution(author).addComment(comment);
                    contributionList.incrCommentCount();
                }
            }

            // Iterate over attachments, store attacher
            Collection attachments = issue.getAttachments();
            Iterator attachItr = attachments.iterator();
            while(attachItr.hasNext()) {
                Attachment attachment = (Attachment) attachItr.next();
                String author = attachment.getAuthor();
                if(author != null) {
                    contributions.getContribution(author).addAttachment(attachment);
                    contributionList.incrAttachmentCount();
                }
            }
        }

        contributionList.setAuthorCount(contributions.keySet().size());

        // Sort contributions by attachment count, then comment count, then reported count.
        contributionList.addAll(contributions.values());
        Collections.sort( contributionList, new Comparator() {
            public int compare(Object o1, Object o2) {
                Contributions c1 = (Contributions) o1;
                Contributions c2 = (Contributions) o2;
                if(c1.getAttachmentCount() > c2.getAttachmentCount()) {
                    return -1;
                } else
                if(c1.getAttachmentCount() < c2.getAttachmentCount()) {
                    return 1;
                } else
                if(c1.getCommentCount() > c2.getCommentCount()) {
                    return -1;
                } else
                if(c1.getCommentCount() < c2.getCommentCount()) {
                    return 1;
                } else
                if(c1.getReportedIssuesCount() > c2.getReportedIssuesCount()) {
                    return -1;
                } else
                if(c1.getReportedIssuesCount() < c2.getReportedIssuesCount()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });

        // Pass the issues to the velocity template
        Map velocityParams = new HashMap();
        velocityParams.put("project", project);
        velocityParams.put("version", version);
        velocityParams.put("contributions", contributionList);

        return descriptor.getHtml("view", velocityParams);
    }

    public void validate(ProjectActionSupport action, Map params) {
        // no validation required; if no version id then it chooses all issues in the project
    }

}

class ContributionMap extends HashMap {

    private UserManager userManager;
    private PermissionManager permissionManager;
    private GenericValue project;

    public ContributionMap(UserManager userManager, PermissionManager permissionManager, GenericValue project) {
        this.userManager = userManager;
        this.permissionManager = permissionManager;
        this.project = project;
    }

    public Contributions getContribution(String authorName) {
        Contributions con = (Contributions) super.get(authorName);
        if(con == null) {
            User author = null;
            try {
                author = this.userManager.getUser(authorName);
            } catch(EntityNotFoundException enfe) {
                // author = null; 
            }
            con = new Contributions(author, author==null?false:isAbleToClose(author));
            if(author == null) {
                con.setBadAuthorName(authorName);
            }
            super.put(authorName, con);
        }
        return con;
    }

    private boolean isAbleToClose(User user) {
        return false; // Turning off the filtering for users who can close issues (ie: only show non-committers)
//        return this.permissionManager.hasPermission(Permissions.CLOSE_ISSUE, this.project, user);
    }

}
