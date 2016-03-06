<?xml version="1.0" encoding="ISO-8859-1"?>
<!DOCTYPE xsl:stylesheet [<!ENTITY nbsp "&#160;">]>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://www.w3.org/1999/xhtml"
                version="1.0">                      

  <xsl:template match="item">
<html>
<head>
      <title>[#<xsl:value-of select="key"/>] <xsl:value-of select="summary"/></title>
    <style type="text/css">
BODY{margin:0;font-family:Arial,Sans-Serif,sans-serif;color:black;}
BODY,P,UL,OL,DL,LI,TD,TEXTAREA,INPUT,SELECT,BUTTON,option,optgroup{font-family:Arial,Sans-Serif;font-size:12px;color:black;}
H3{font-weight:bold;font-size:16px;font-family:Arial,Sans-Serif;margin-top:10px;margin-bottom:0;}
    *
    {
        border: 0;
        padding: 0;
    }

    .tableBorder, .grid
    {
        background-color: #fff;
        width: 100%;
        border-collapse: collapse;
    }

    h3.formtitle
    {
    }

    .tableBorder td, .grid td
    {
        vertical-align: top;
        padding: 2px;
        border: 1px solid #cccccc;
        border-collapse: collapse;
    }

.noPadding
{
    padding: 0px !important;
}

h3 .subText
{
    font-size: 60%;
    font-weight: normal;
}

.tabLabel
{
    font-weight: bold;
    border-top: 1px solid #cccccc;
    border-right: 1px solid #cccccc;
    border-left: 1px solid #cccccc;
    padding: 2px;
    border-collapse: collapse;
    display: inline;
}

td.blank
{
    padding: 0;
    margin: 0;
}

.blank td
{
    border: none;
}


#descriptionArea
{
    margin: 0px;
    padding: 2px;
    border: 1px solid #cccccc;
}

body
{
    margin: 0px;
    font-size: 12px;
    font-family: Arial, Sans-Serif, sans-serif;
    color:black;
}

</style>

</head>
<body bgcolor="#ffffff" leftmargin="0" topmargin="0" marginwidth="0" marginheight="0" link="#003366" vlink="#003366" alink="#660000">

<table cellpadding="0" cellspacing="0" border="0" width="100%">
<p style="background-color: red; padding: 8px">This is an exported version of the JIRA issue tracker. Please use the <a href="http://code.google.com/p/osjava/issues/list">Google Code</a> site to open new tickets or report updates to these existing tickets. Feel free to contact the <a href="http://groups.google.com/group/osjava">mailing list</a> with any questions. </p>
</table>

<table class="tableBorder" cellpadding="0" cellspacing="0" border="0" width="100%">
        <xsl:variable name="project">
          <xsl:value-of select="project/@key"/>
        </xsl:variable>
        <tr>
            <td bgcolor="#f0f0f0" width="100%" colspan="2" valign="top">
                                    <h3 class="formtitle">

                                [<xsl:value-of select="key"/>]&nbsp;<xsl:value-of select="summary"/><br/>
                <span class="subText">
                   Created: <xsl:value-of select="created"/>                       &nbsp;Updated: <xsl:value-of select="updated"/>

                                    </span>
                </h3>
            </td>
        </tr>
        <tr>

            <td width="20%"><b>Status:</b></td>
            <td width="80%"><xsl:value-of select="status"/></td>
        </tr>
        <tr>
            <td width="20%"><b>Project:</b></td>
            <td width="80%"><a href="{$project}.html"><xsl:value-of select="project"/></a></td>
        </tr>

                <tr>
                <td><b>Component/s:</b></td>
                <td><xsl:value-of select="component"/></td>
        </tr>
        

                <tr>
                <td><b>Affects Version/s:</b></td>

                <td>
                  <xsl:variable name="version">
                    <xsl:value-of select="version"/>
                  </xsl:variable>
                  <!-- TODO: Need a for loop here someday - currently the data doesn't have dupes -->
                  <a href="../version/{$project}-{$version}.html"><xsl:value-of select="version"/></a>
                </td>
        </tr>
        

                <tr>
                <td><b>Fix Version/s:</b></td>
                <td>
                  <xsl:variable name="fixVersion">
                    <xsl:value-of select="fixVersion"/>
                  </xsl:variable>
                  <!-- TODO: Need a for loop here someday - currently the data doesn't have dupes -->
                  <a href="../version/{$project}-{$fixVersion}.html"><xsl:value-of select="fixVersion"/></a>
                </td>

        </tr>
        
            </table>

    <br />
<table class="grid" cellpadding="0" cellspacing="0" border="0" width="100%">
    <tr>
        <td bgcolor="#f0f0f0" valign="top" width="20%">
            <b>Type:</b>
        </td>

        <td bgcolor="#ffffff" valign="top"  width="30%" >
            <xsl:value-of select="type"/>
        </td>

                    <td bgcolor="#f0f0f0">
                <b>Priority:</b>
            </td>
            <td bgcolor="#ffffff" valign="top" nowrap="nowrap">
                Major
            </td>

        
    </tr>
    <tr>
                        <td bgcolor="#f0f0f0" valign="top" width="20%">
                <b>Reporter:</b>
            </td>
            <td bgcolor="#ffffff" valign="top"  width="30%" >
                                    <xsl:variable name="username">
                                      <xsl:value-of select="reporter/@username"/>
                                    </xsl:variable>
                                    <a href="../user/{$username}.html"><xsl:value-of select="reporter"/></a>
                            </td>

        
                    <td bgcolor="#f0f0f0" width="20%">
                <b>Assignee:</b>
            </td>
            <td bgcolor="#ffffff" valign="top" nowrap="nowrap"  width="30%" >
                                    <xsl:variable name="username">
                                      <xsl:value-of select="assignee/@username"/>
                                    </xsl:variable>
                                    <a href="../user/{$username}.html"><xsl:value-of select="assignee"/></a>
                            </td>
            </tr>
        <tr>

        <td bgcolor="#f0f0f0" width="20%">
            <b>Resolution:</b>
        </td>
        <td bgcolor="#ffffff" valign="top" width="30%" nowrap="nowrap">
                            <xsl:value-of select="resolution"/>
                    </td>
                    <td bgcolor="#ffffff" colspan="2">&nbsp;</td>
        
    </tr>
    
        <tr>

        <td bgcolor="#f0f0f0" width="20%" valign="top">
            <b>Environment:</b>
        </td>

        <td bgcolor="#ffffff" valign="top" colspan="3">
                              <xsl:value-of select="environment"/>
        </td>
    </tr>
    
    </table>



    <br />

        <table class="grid" cellpadding="0" cellspacing="0" border="0" width="100%">
            

        
    


    
    </table>

            <br/>

        <table cellpadding="2" cellspacing="0" border="0" width="100%" align="center">
        <tr>

            <td bgcolor="#bbbbbb" width="1%" nowrap="nowrap" align="center">
                    &nbsp;<font color="#ffffff"><b>Description</b></font>&nbsp;
            </td>
            <td>&nbsp;</td>
        </tr>
        </table>

        <table cellpadding="0" cellspacing="0" border="0" width="100%">
        <tr>

            <td id="descriptionArea">
                    <xsl:value-of select="description" disable-output-escaping="yes"/>
            </td>
        </tr>

<xsl:for-each select="comments/comment">
                        <tr><td bgcolor="#f0f0f0">
                                    <xsl:variable name="author">
                                      <xsl:value-of select="@author"/>
                                    </xsl:variable>
                Comment by  <a href="../user/{$author}.html"><xsl:value-of select="@author"/></a>
                    <font size="-2"> [ <font color="#336699"><xsl:value-of select="@created"/></font> ] </font>

            </td></tr>
            <tr><td bgcolor="#ffffff">
<xsl:value-of select="." disable-output-escaping="yes"/>
            </td></tr>
</xsl:for-each>

        </table>
    
</body>
</html>

  </xsl:template>
</xsl:stylesheet>
