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
package com.sourcelabs.jira.plugin.report.contribution;

import java.util.*;

public class ContributionList extends ArrayList {

    private int authorCount;
    private int attachmentCount;
    private int commentCount;
    private int reportedIssuesCount;
    private int assignedIssuesCount;

    public void setAuthorCount(int authorCount) { this.authorCount = authorCount; }
    public int getAuthorCount() { return this.authorCount; }

    public void incrAttachmentCount() { this.attachmentCount++; }
    public int getAttachmentCount() { return this.attachmentCount; }

    public void incrCommentCount() { this.commentCount++; }
    public int getCommentCount() { return this.commentCount; }

    public void incrReportedIssuesCount() { this.reportedIssuesCount++; }
    public int getReportedIssuesCount() { return this.reportedIssuesCount; }

    public void incrAssignedIssuesCount() { this.assignedIssuesCount++; }
    public int getAssignedIssuesCount() { return this.assignedIssuesCount; }

}
