package com.sourcelabs.jira.plugin.report.contribution;

import com.opensymphony.user.User;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.attachment.Attachment;

import java.util.*;

public class Contributions {

    private List reportedIssues = new ArrayList();
    private List assignedIssues = new ArrayList();

    private int attachmentCount;
    private Set attachmentIssues = new HashSet();

    private int commentCount;
    private Set commentIssues = new HashSet();

    private User author;
    private boolean ableToClose;
    private String badAuthorName;  // names that don't have Users

    public Contributions(User author, boolean ableToClose) {
        this.author = author;
        this.ableToClose = ableToClose;
    }

    public void addAttachment(Attachment attachment) {
        attachmentCount++;
        attachmentIssues.add(attachment.getIssue());
    }

    public void addComment(Comment comment) {
        commentCount++;
        commentIssues.add(comment.getIssue());
    }

    public void addReportedIssue(Issue issue) {
        reportedIssues.add(issue);
    }

    public void addAssignedIssue(Issue issue) {
        assignedIssues.add(issue);
    }

    public int getCommentCount() { return this.commentCount; }
    public Collection getCommentIssues() { return this.commentIssues; }

    public int getAttachmentCount() { return this.attachmentCount; }
    public Collection getAttachmentIssues() { return this.attachmentIssues; }

    public int getReportedIssuesCount() { return this.reportedIssues.size(); }
    public Collection getReportedIssues() { return this.reportedIssues; }

    public int getAssignedIssuesCount() { return this.assignedIssues.size(); }
    public Collection getAssignedIssues() { return this.assignedIssues; }

    public User getAuthor() { return this.author; }
    public boolean isAbleToClose() { return this.ableToClose; }
    public String getAuthorName() { return this.author == null ? this.badAuthorName : this.author.getName(); }
    public String getAuthorFullName() { return this.author == null ? this.badAuthorName : this.author.getFullName(); }
    public void setBadAuthorName(String name) { this.badAuthorName = name; }

}
