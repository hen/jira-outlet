echo "Creating project index. "
xsltproc index-html.xsl jira/index.xml > jira/index.html

echo "Creating HTML for issues. "
for i in jira/browse/*-[0-9]*.xml
do
    xsltproc issue-html.xsl $i > `echo $i | sed 's/.xml$/.html/'`
done

echo "Creating HTML for users. "
for i in jira/user/*.xml
do
    xsltproc user-html.xsl $i > `echo $i | sed 's/.xml$/.html/'`
done

echo "Creating HTML for projects. "
for i in `ls -1 jira/browse/*.xml | grep -v '-'`
do
    xsltproc project-html.xsl $i > `echo $i | sed 's/.xml$/.html/'`
done

echo "Creating HTML for versions. "
for i in jira/version/*.xml
do
    xsltproc version-html.xsl $i > `echo $i | sed 's/.xml$/.html/'`
done
