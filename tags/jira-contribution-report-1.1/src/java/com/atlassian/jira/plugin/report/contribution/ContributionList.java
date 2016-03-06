package com.sourcelabs.jira.plugin.report.contribution;

import java.util.*;

public class ContributionList extends ArrayList {

    private int authorCount;
    private int attachmentCount;
    private int commentCount;
    private int reportedIssuesCount;

    public void setAuthorCount(int authorCount) { this.authorCount = authorCount; }
    public int getAuthorCount() { return this.authorCount; }

    public void incrAttachmentCount() { this.attachmentCount++; }
    public int getAttachmentCount() { return this.attachmentCount; }

    public void incrCommentCount() { this.commentCount++; }
    public int getCommentCount() { return this.commentCount; }

    public void incrReportedIssuesCount() { this.reportedIssuesCount++; }
    public int getReportedIssuesCount() { return this.reportedIssuesCount; }

}
