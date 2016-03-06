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

            boolean diluteVote = "true".equals(portletConfiguration.getProperty("diluteVote"));
            params.put("diluteVote", new Boolean(diluteVote));

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

            String[] issueKeys = issueKeysConf.split(",\\s*");

            // If issueKeys.length is 1, then get its subtasks
            if(issueKeys.length == 1) {
                Issue issue = this.issueManager.getIssueObject(issueKeys[0]);

                if(issue == null) {
                    return params;
                }

                // If issueKeys.length is 1 and title is null, then use issue summary
                if(title == null || title.trim().equals("")) {
                    if(issue.getSummary() != null) {
                        title = issue.getSummary();
                    }
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
            
            if(diluteVote) {
                Map votersToIssues = new HashMap();

                // Scan all the users first so we can work out what fraction of 
                // 1 point a user can give to an issue
                for(int i=0; i<issueKeys.length; i++) {
                    String issueKey = issueKeys[i];
                    Issue issue = this.issueManager.getIssueObject(issueKey);

                    Collection voters = this.voteManager.getVoters(this.authenticationContext.getLocale(), 
                                                                   issue.getGenericValue());
                    if(voters != null && voters.size() != 0) {
                        Iterator itr = voters.iterator();
                        while(itr.hasNext()) {
                            User voter = (User) itr.next();
                            List issues = (List) votersToIssues.get(voter);
                            if(issues == null) {
                                issues = new LinkedList();
                                votersToIssues.put(voter, issues);
                            }
                            issues.add(issue);
                        }
                    } else {
                        entries.add( new PollEntry(issue, false) );
                    }
                }

                Map issueToPollEntry = new HashMap();

                Iterator itr = votersToIssues.keySet().iterator();
                while(itr.hasNext()) {
                    User voter = (User) itr.next();
                    List issues = (List) votersToIssues.get(voter);

                    // sz should not be 0
                    int sz = issues.size();
                    float voteValue = 1.0F / sz; 

                    for(int i=0; i<sz; i++) {
                        Issue issue = (Issue) issues.get(i);
                        PollEntry entry = (PollEntry) issueToPollEntry.get(issue);
                        if(entry == null) {
                            boolean hasVoted = this.voteManager.hasVoted(user, issue.getGenericValue());
                            entry = new PollEntry(issue, hasVoted);
                            issueToPollEntry.put(issue, entry);
                            entries.add( entry );
                        }
                        entry.addVote(voteValue);
                    }
                }

                total = votersToIssues.keySet().size();
            } else {
                // loop over issueKeys and load the Issues
                for(int i=0; i<issueKeys.length; i++) {
                    String issueKey = issueKeys[i];
                    Issue issue = this.issueManager.getIssueObject(issueKey);

                    boolean hasVoted = this.voteManager.hasVoted(user, issue.getGenericValue());

                    PollEntry entry = new PollEntry(issue, hasVoted);
                    int voteValue = issue.getVotes().intValue();
                    entry.addVote(voteValue);
                    entries.add(entry);
                    total += voteValue;
                }
            }

            for(int i=0; i<entries.size(); i++) {
                PollEntry entry = (PollEntry) entries.get(i);
                entry.calculatePercentage(total);
            }

            Collections.sort(entries, new Comparator() {
                public int compare(Object o1, Object o2) {
                    PollEntry i1 = (PollEntry) o1;
                    PollEntry i2 = (PollEntry) o2;
                    return i2.getVoteCountAsFloat().compareTo(i1.getVoteCountAsFloat());
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
