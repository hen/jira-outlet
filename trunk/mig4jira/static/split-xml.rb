#!/usr/bin/ruby

# Usage: ruby split-xml.rb example.xml http://issues.osjava.org/jira jira

require 'rexml/document'
require 'fileutils'
require 'open-uri'

include REXML

class XRef

    attr_reader :key, :node, :summary, :status

    def initialize(key, summary, status, node)
        @key = key
        @node = node
        @summary = summary
        @status = status
    end

    def to_xml(xml)
        xml.add_element('issue', { 'key', key, 'status', status } ).text = summary
    end

    def to_s
        @key
    end

end

jira_xml = ARGV[0]
jira_url = ARGV[1]
local_dir = ARGV[2]

reporter_xref = Hash.new
assignee_xref = Hash.new
comment_xref  = Hash.new
affects_xref  = Hash.new
fixes_xref    = Hash.new
project_xref  = Hash.new

attach_dir = "secure/attachment"
user_dir = "secure/ViewProfile.jspa?name="
browse_dir = "browse"

local_index = "#{local_dir}/index.xml"
local_attach_dir = "#{local_dir}/#{attach_dir}"
local_browse_dir = "#{local_dir}/#{browse_dir}"
local_user_dir   = "#{local_dir}/user"
local_version_dir   = "#{local_dir}/version"

# scrape JIRA project list
project_page = "secure/BrowseProjects.jspa"
projects = Hash.new
open("#{jira_url}/#{project_page}").read.each do |line|
    # pull out <a with /browse/ in it
    if line =~ /\/browse\/([^"]*)">([^<]*)</
        projects[$1] = $2
    end
end

# Write project index
pxml = Document.new
projects_xml = pxml.add_element("projects")
projects.each do |pkey, name|
    projects_xml.add_element("project", { 'key', pkey }).text = name
end
print "Creating: #{local_index}\n"
output = File.new("#{local_index}", "w")
pxml.write( output, 0 )
output << "\n"
output.close()

# parse JIRA configuration
file = File.new(jira_xml)
jira = Document.new(file)
file.close

FileUtils.mkdir_p "#{local_browse_dir}/"

jira.root.elements.each("channel/item") do |item|
    key = item.elements["key"].text
    summary = item.elements["summary"].text
    status = item.elements["status"].text

    # Pull the project key out of the item key
    pkey = key.sub(/-[^-]*$/, "")
    item.add_element("project", {'key', pkey}).text = projects[pkey]

    # Create xml pages for each page to be browsed
    print "Creating: #{local_browse_dir}/#{key}.xml\n"
    output = File.new("#{local_browse_dir}/#{key}.xml", "w")
    item.write( output, 0 )
    output << "\n"
    output.close()

    # Pull the attachments down
    item.elements.each("attachments/attachment") do |attachment|
        FileUtils.mkdir_p "#{local_attach_dir}/#{attachment.attributes['id']}/"

        attachment_url = "#{jira_url}/#{attach_dir}/#{attachment.attributes['id']}/#{attachment.attributes['name']}"

        print "Downloading: #{attachment_url}\n"
        attachment_file = File.new("#{local_attach_dir}/#{attachment.attributes['id']}/#{attachment.attributes['name']}", "w")
        attachment_file.write(open(attachment_url).read)
        attachment_file.close()
    end

    # Generate User cross-reference
    unless item.elements["assignee/@username"].value == "-1"
        (assignee_xref[item.elements["assignee/@username"].value] ||= []) << XRef.new(key, summary, status, item.elements["assignee"])
    end
    unless item.elements["reporter/@username"].value == "-1"
        (reporter_xref[item.elements["reporter/@username"].value] ||= []) << XRef.new(key, summary, status, item.elements["reporter"])
    end
    if item.elements["comments/comment"]
        # BUG: Why is this of the type REXML:Text and not a Node? Had to hack the 'parent' in below.
        item.elements["comments/comment"].each do |comment|
            unless comment.parent.elements["@author"].value == "-1"
                (comment_xref[comment.parent.elements["@author"].value] ||= []) << XRef.new(key, summary, status, comment)
            end
        end
    end

    # Generate Version cross-reference
    if item.elements["version"]
        item.elements["version"].each do |version|
            (affects_xref[pkey + "-" + version.value] ||= []) << XRef.new(key, summary, status, version)
        end
    end
    if item.elements["fixVersion"]
        item.elements["fixVersion"].each do |version|
            (fixes_xref[pkey + "-" + version.value] ||= []) << XRef.new(key, summary, status, version)
        end
    end

    # Generate Project cross-reference
    (project_xref[pkey] ||= []) << XRef.new(key, summary, status, item.elements["project"])

end

# Create User XML
FileUtils.mkdir_p "#{local_user_dir}/"
user_xref = assignee_xref.keys | reporter_xref.keys | comment_xref.keys

user_xref.uniq.each do |login|
    uxml = Document.new
    project = uxml.add_element("user")
    project.add_element("login").text = login
    name = project.add_element("name")

    if comment_xref.has_key?(login)
        issues = project.add_element("commented")
        # For some reason the node is the REXML:Text node
        name.text = comment_xref[login][0].node.parent.elements["@author"]
        comment_xref[login].each do |xref|
            xref.to_xml(issues)
        end
    end

    if reporter_xref.has_key?(login)
        issues = project.add_element("reported")
        name.text = reporter_xref[login][0].node.text
        reporter_xref[login].each do |xref|
            xref.to_xml(issues)
        end
    end

    if assignee_xref.has_key?(login)
        issues = project.add_element("assigned")
        name.text = assignee_xref[login][0].node.text
        assignee_xref[login].each do |xref|
            xref.to_xml(issues)
        end
    end

    print "Creating: #{local_user_dir}/#{login}.xml\n"
    output = File.new("#{local_user_dir}/#{login}.xml", "w")
    uxml.write( output, 0 )
    output << "\n"
    output.close()
end

# Create Version XML
FileUtils.mkdir_p "#{local_version_dir}/"

version_xref = affects_xref.keys | fixes_xref.keys

version_xref.uniq.each do |version|
    vxml = Document.new
    version_xml = vxml.add_element("version")

    if affects_xref.has_key?(version)
        name = affects_xref[version][0].node.value
        issues = version_xml.add_element("affects-issues")
        affects_xref[version].each do |xref|
            xref.to_xml(issues)
        end
    end

    if fixes_xref.has_key?(version)
        name = fixes_xref[version][0].node.value
        issues = version_xml.add_element("fixes-issues")
        fixes_xref[version].each do |xref|
            xref.to_xml(issues)
        end
    end

    version_xml.add_element("name").text = name

    print "Creating: #{local_version_dir}/#{version}.xml\n"
    output = File.new("#{local_version_dir}/#{version}.xml", "w")
    vxml.write( output, 0 )
    output << "\n"
    output.close()
end

# Create Project XML
project_xref.each do |pkey, xrefs|
    pxml = Document.new
    project = pxml.add_element("project")
    project.add_element("key").text = pkey
    project.add_element("name").text = xrefs[0].node.text
    issues = project.add_element("issues")
    xrefs.each do |xref|
        xref.to_xml(issues)
    end
    print "Creating: #{local_browse_dir}/#{pkey}.xml\n"
    output = File.new("#{local_browse_dir}/#{pkey}.xml", "w")
    pxml.write( output, 0 )
    output << "\n"
    output.close()
end
