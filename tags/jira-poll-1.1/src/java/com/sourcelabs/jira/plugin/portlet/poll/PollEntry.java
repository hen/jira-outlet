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

import com.atlassian.jira.issue.Issue;

public class PollEntry {

    private Issue issue;
    private int percentage;
    private float voteCount;
    private boolean hasVoted;

    public PollEntry(Issue issue, boolean hasVoted) {
        this.issue = issue;
        this.hasVoted = hasVoted;
    }

    public String getVoteCount() { 
        // very nasty evolved hack for 1dp; need to change this all around and see 
        // if the User object has a Locale. If so then need to have this code 
        // return a float and figure out how to deal with that in the velocity
        String txt = "" + ((int) (this.voteCount*10))/10.0F; 
        if(txt.endsWith(".0")) {
            txt = txt.substring(0, txt.length() - 2);
        }
        return txt;
    } 

    Float getVoteCountAsFloat() {
        return new Float(voteCount);
    }

    public void addVote(float voteValue) { this.voteCount += voteValue; }

    public Issue getIssue() { return this.issue; }
    public String getPercentage() { return "" + this.percentage; }
    public String getNonPercentage() { return "" + (100 - this.percentage); }
    public boolean getHasVoted() { return this.hasVoted; }

    public void calculatePercentage(int total) {
        if(total != 0) {
            this.percentage = Math.round( voteCount * 100 / total );
        }
    }
    
}

