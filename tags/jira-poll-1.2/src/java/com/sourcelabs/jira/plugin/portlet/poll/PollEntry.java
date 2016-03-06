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

