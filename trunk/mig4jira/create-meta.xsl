<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:jira="jelly:com.atlassian.jira.jelly.enterprise.JiraTagLib"
                xmlns:sql="jelly:sql" 
                xmlns:j="jelly:core"
                version="1.0">                      

  <xsl:template match="rss">
   <JiraJelly>
     <xsl:for-each select="channel/item/version">
       <xsl:variable name="version"><xsl:value-of select="."/></xsl:variable>
       <jira:AddVersion name="{$version}" project-key="PROJECT_KEY"/>
     </xsl:for-each>
     <xsl:for-each select="channel/item/fixVersion">
       <xsl:variable name="fixVersion"><xsl:value-of select="."/></xsl:variable>
       <jira:AddVersion name="{$fixVersion}" project-key="PROJECT_KEY"/>
     </xsl:for-each>
     <xsl:for-each select="channel/item/component">
       <xsl:variable name="component"><xsl:value-of select="."/></xsl:variable>
       <jira:AddComponent name="{$component}" project-key="PROJECT_KEY"/>
     </xsl:for-each>
   </JiraJelly>
  </xsl:template>
     
  <!-- block unrecognized nodes -->
  <xsl:template match="node()|@*"/>
   
</xsl:stylesheet>
     
