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

