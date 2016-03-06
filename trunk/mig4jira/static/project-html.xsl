<?xml version="1.0" encoding="ISO-8859-1"?>
<!DOCTYPE xsl:stylesheet [<!ENTITY nbsp "&#160;">]>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://www.w3.org/1999/xhtml"
                version="1.0">                      

  <xsl:template match="project">
<html>
<head>
      <title><xsl:value-of select="name"/></title>
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

      <h3><xsl:value-of select="name"/></h3>

<b>Issues</b>
<ul>
<xsl:for-each select="issues/issue">
    <xsl:variable name="issue">
        <xsl:value-of select="@key"/>
    </xsl:variable>
    <li><a href="../browse/{$issue}.html"><xsl:value-of select="@key"/></a> - <xsl:value-of select="."/> (<xsl:value-of select="@status"/>)</li>
</xsl:for-each>
</ul>
    
</body>
</html>

  </xsl:template>
</xsl:stylesheet>
