xsltproc create-issues.xsl $1 | sed "s/PROJECT_KEY/$2/g" | \
sed 's/ *[a-zA-Z]*="" */ /g' | \
sed 's/created="..., \([0-9]*\) \([a-zA-Z]*\) \([0-9]*\) \([0-9:]*\)[^"]*"/created="\3-\2-\1 \4.0"/g' | \
sed 's/updated="..., \([0-9]*\) \([a-zA-Z]*\) \([0-9]*\) \([0-9:]*\)[^"]*"/updated="\3-\2-\1 \4.0"/g' | \
sed 's/duedate="..., \([0-9]*\) \([a-zA-Z]*\) \([0-9]*\) \([0-9:]*\)[^"]*"/duedate="\1\/\2\/\3"/g' | \
sed 's/\([a-z]*ted="[0-9]*\)-Jan-/\1-01-/g' | \
sed 's/\([a-z]*ted="[0-9]*\)-Feb-/\1-02-/g' | \
sed 's/\([a-z]*ted="[0-9]*\)-Mar-/\1-03-/g' | \
sed 's/\([a-z]*ted="[0-9]*\)-Apr-/\1-04-/g' | \
sed 's/\([a-z]*ted="[0-9]*\)-May-/\1-05-/g' | \
sed 's/\([a-z]*ted="[0-9]*\)-Jun-/\1-06-/g' | \
sed 's/\([a-z]*ted="[0-9]*\)-Jul-/\1-07-/g' | \
sed 's/\([a-z]*ted="[0-9]*\)-Aug-/\1-08-/g' | \
sed 's/\([a-z]*ted="[0-9]*\)-Sep-/\1-09-/g' | \
sed 's/\([a-z]*ted="[0-9]*\)-Oct-/\1-10-/g' | \
sed 's/\([a-z]*ted="[0-9]*\)-Nov-/\1-11-/g' | \
sed 's/\([a-z]*ted="[0-9]*\)-Dec-/\1-12-/g' | \
sed 's/\([a-z]*ted="[0-9]*-[0-9]*\)-\([0-9]\) /\1-0\2 /g' > $2-issues.jelly

xsltproc create-meta.xsl $1 | sed "s/PROJECT_KEY/$2/g"  > $2-meta.jelly
