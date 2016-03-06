<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:jira="jelly:com.atlassian.jira.jelly.enterprise.JiraTagLib"
                xmlns:sql="jelly:sql" 
                xmlns:j="jelly:core"
                version="1.0">                      

  <xsl:template match="rss">
   <JiraJelly>
     <xsl:for-each select="channel/item">
       <xsl:variable name="issueType"><xsl:value-of select="type"/></xsl:variable>
       <xsl:variable name="summary"><xsl:value-of select="summary"/></xsl:variable>
       <xsl:variable name="description"><xsl:value-of select="description"/></xsl:variable>
       <xsl:variable name="priority"><xsl:value-of select="priority"/></xsl:variable>
       <xsl:variable name="duedate"><xsl:value-of select="due"/></xsl:variable>
       <xsl:variable name="created"><xsl:value-of select="created"/></xsl:variable>
       <xsl:variable name="updated"><xsl:value-of select="updated"/></xsl:variable>
       <xsl:variable name="environment"><xsl:value-of select="environment"/></xsl:variable>
       <xsl:variable name="components"><xsl:value-of select="component"/></xsl:variable>
       <xsl:variable name="versions"><xsl:value-of select="version"/></xsl:variable>
       <xsl:variable name="fixVersions"><xsl:value-of select="fixVersion"/></xsl:variable>
       <xsl:variable name="reporter"><xsl:value-of select="reporter/@username"/></xsl:variable>
       <xsl:variable name="assignee"><xsl:value-of select="assignee/@username"/></xsl:variable>
   
       <!-- fixVersions, versions, components need to be supported, ie) if multiple input then make a comma separated list -->
       <!-- if no assignee, then put a -1 in -->
       <jira:CreateIssue project-key="PROJECT_KEY"
         issueType="{$issueType}"
         summary="{$summary}"
         description="{$description}"
         priority="{$priority}"
         duedate="{$duedate}"
         created="{$created}"
         updated="{$updated}"
         environment="{$environment}"
         components="{$components}"
         versions="{$versions}"
         fixVersions="{$fixVersions}"
         reporter="{$reporter}"
         assignee="{$assignee}"
         issueKeyVar="key"
       />
       <xsl:variable name="keyvar">${key}</xsl:variable>
   
       <xsl:for-each select="comments/comment">
         <xsl:variable name="commenter"><xsl:value-of select="@author"/></xsl:variable>
         <xsl:variable name="commentCreated"><xsl:value-of select="@created"/></xsl:variable>
         <xsl:variable name="text"><xsl:value-of select="."/></xsl:variable>
         <jira:AddComment issue-key="{$keyvar}"
           commenter="{$commenter}"
           created="{$commentCreated}"
           comment="{$text}"
         />
       </xsl:for-each>

       <xsl:if test="status = 'Closed'">
         <xsl:variable name="resolution"><xsl:value-of select="resolution"/></xsl:variable>
         <jira:TransitionWorkflow key="{$keyvar}" workflowAction="Close issue" resolution="{$resolution}"/>
       </xsl:if>
       <xsl:if test="status = 'Resolved'">
         <xsl:variable name="resolution"><xsl:value-of select="resolution"/></xsl:variable>
         <jira:TransitionWorkflow key="{$keyvar}" workflowAction="Resolve issue" resolution="{$resolution}"/>
       </xsl:if>
       <xsl:if test="status = 'In Progress'">
         <jira:TransitionWorkflow key="{$keyvar}" workflowAction="Start progress" user="{$assignee}"/>
       </xsl:if>

     </xsl:for-each>

   </JiraJelly>
  </xsl:template>
     
  <!-- block unrecognized nodes -->
  <xsl:template match="node()|@*"/>
   
</xsl:stylesheet>
     
