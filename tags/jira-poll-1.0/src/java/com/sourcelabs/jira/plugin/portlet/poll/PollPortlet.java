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
package com.sourcelabs.jira.plugin.portlet.poll;

import com.atlassian.configurable.ObjectConfigurationException;
import com.atlassian.jira.issue.search.parameters.lucene.*;
import com.atlassian.jira.portal.PortletConfiguration;
import com.atlassian.jira.portal.PortletImpl;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.opensymphony.user.User;
import com.atlassian.jira.issue.vote.VoteManager;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.Issue;

import org.ofbiz.core.entity.GenericValue;

import java.util.*;

/**
 * This portlet wraps a set of issues into a classic poll
 */
public class PollPortlet extends PortletImpl
{

    // References to managers required for this portlet
    private final PermissionManager permissionManager;
    private final IssueManager issueManager;
    private final VoteManager voteManager;

    public PollPortlet(JiraAuthenticationContext authenticationContext, PermissionManager permissionManager, 
                          IssueManager issueManager, VoteManager voteManager) {
        super(authenticationContext);
        this.permissionManager = permissionManager;
        this.issueManager = issueManager;
        this.voteManager = voteManager;
    }

    // Pass the data required for the portlet display to the view template
    protected Map getVelocityParams(PortletConfiguration portletConfiguration)
    {
        Map params = new HashMap();
        params.put("portlet", this); 
        Long portletId = portletConfiguration.getId();
        params.put("portletId", portletId);

        User user = authenticationContext.getUser();

        boolean loggedIn = (this.authenticationContext.getUser() != null);
        params.put("loggedIn", new Boolean(loggedIn) );
        params.put("user", user);

        boolean votingEnabled = this.voteManager.isVotingEnabled();
        params.put("votingEnabled", new Boolean(votingEnabled));
        if(votingEnabled == false) {
            System.err.println("Warning: Voting not enabled");
            return params;
        }

        try {
            String title = portletConfiguration.getTextProperty("title");
            params.put("title", title);

            String issueKeysConf = portletConfiguration.getTextProperty("issue-keys");

            if(issueKeysConf != null && (issueKeysConf.lastIndexOf(":") != -1) ) {
                int idx = issueKeysConf.lastIndexOf(":");
                String toggleKey = issueKeysConf.substring(idx+1);
                issueKeysConf = issueKeysConf.substring(0, idx);
                toggleVote(user, toggleKey);
            }

            if( (portletId.intValue() == -1) && (issueKeysConf != null) && (issueKeysConf.indexOf(":") != -1) ) {
                int idx = issueKeysConf.indexOf(":");
                params.put("portletId", Long.valueOf(issueKeysConf.substring(idx+1)));
                issueKeysConf = issueKeysConf.substring(0, idx);
            }

            params.put("issue-keys", issueKeysConf);

            String[] issueKeys = issueKeysConf.split(",");

            // If issueKeys.length is 1, then get its subtasks
            if(issueKeys.length == 1) {
                Issue issue = this.issueManager.getIssueObject(issueKeys[0]);

                // If issueKeys.length is 1 and title is null, then use issue summary
                if(title == null || title.trim().equals("")) {
                    title = issue.getSummary();
                    params.put("title", title);
                }
            
                Collection subtasks = issue.getSubTaskObjects();
                if(subtasks != null) {
                    issueKeys = new String[subtasks.size()];
                    Iterator itr = subtasks.iterator();
                    int i=0;
                    while(itr.hasNext()) {
                        Issue subTask = (Issue) itr.next();
                        issueKeys[i] = subTask.getKey();
                        i++;
                    }
                }
            }

            List entries = new LinkedList();
            int total = 0;

            // loop over issueKeys and load the Issues
            for(int i=0; i<issueKeys.length; i++) {
                String issueKey = issueKeys[i];
                Issue issue = this.issueManager.getIssueObject(issueKey);

                boolean hasVoted = this.voteManager.hasVoted(user, issue.getGenericValue());

                // look for duplicate votes and reduce them by ratio
                // put each issue and the vote count into params

                // issue.getVotes() looks like it is the count
                // voteManager.getVoters(issue) is then needed for the filtering

                entries.add(new PollEntry(issue, hasVoted) );
                total += issue.getVotes().intValue();
            }

            for(int i=0; i<entries.size(); i++) {
                PollEntry entry = (PollEntry) entries.get(i);
                entry.calculatePercentage(total);
            }

            Collections.sort(entries, new Comparator() {
                public int compare(Object o1, Object o2) {
                    PollEntry i1 = (PollEntry) o1;
                    PollEntry i2 = (PollEntry) o2;
                    return i2.getIssue().getVotes().compareTo(i1.getIssue().getVotes());
                }
            });

            params.put("entries", entries);

        } catch(ObjectConfigurationException e) {
            e.printStackTrace();
        }

        return params;
    }

    private void toggleVote(User user, String issueKey) {
        try {
            GenericValue issue = this.issueManager.getIssue(issueKey);
            if(voteManager.hasVoted(user, issue)) {
                voteManager.removeVote(user, issue);
            } else {
                voteManager.addVote(user, issue);
            }
        } catch(org.ofbiz.core.entity.GenericEntityException gee) {
            gee.printStackTrace();
        }
    }

}
