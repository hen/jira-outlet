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
package com.sourcelabs.jira.plugin.portlet.poll;

import java.io.Writer;
import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.*;

import  org.ofbiz.core.entity.GenericValue;
import com.opensymphony.user.User;
import com.atlassian.jira.issue.vote.VoteManager;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.ManagerFactory;

import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.cache.CacheManager;
import com.atlassian.jira.issue.index.IssueIndexManager;

import com.atlassian.core.ofbiz.association.AssociationManager;
import com.atlassian.core.ofbiz.CoreFactory;
import com.atlassian.core.user.UserUtils;

import com.atlassian.jira.issue.vote.DefaultVoteManager;

public class VoteServlet extends HttpServlet {

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Writer writer = resp.getWriter();

        // Get the Issue
        String issueKey = req.getParameter("issueKey");
        IssueManager issueManager = ManagerFactory.getIssueManager();

        GenericValue issue = null;
        try {
            issue = issueManager.getIssue(issueKey);
        } catch(org.ofbiz.core.entity.GenericEntityException gee) {
            gee.printStackTrace();
            writer.write("FAIL: ISSUE");
            return;
        }

        if(issue == null) {
            writer.write("FAIL: ISSUE NULL");
            return;
        }

        // Get the User
        HttpSession session = req.getSession();
        User user = (User) session.getAttribute("seraph_defaultauthenticator_user");
        if(user == null) {
            writer.write("FAIL: USER");
            return;
        }

        // We want a VoteManager...it's hard work
        AssociationManager am = CoreFactory.getAssociationManager();
        ApplicationProperties ap = ManagerFactory.getApplicationProperties();
        CacheManager cm = ManagerFactory.getCacheManager();
        IssueIndexManager iim = ManagerFactory.getIndexManager();
        VoteManager voteManager = new DefaultVoteManager(ap, am, cm, iim);

        // toggle the vote
        if(voteManager.hasVoted(user, issue)) {
            voteManager.removeVote(user, issue);
        } else {
            voteManager.addVote(user, issue);
        }

        writer.write("OK");
    }
    
}

