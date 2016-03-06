package com.sourcelabs.jira.plugin;

import com.atlassian.jira.portal.ProjectValuesGenerator;
import java.util.Map;

public class ProjectListGenerator extends ProjectValuesGenerator {

    public Map getValues(Map params) {
        Map m = super.getValues(params);
        m.put("-1", "Select Project:");
        return m;
    }

}
