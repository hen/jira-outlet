/*
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
package outlet.jira.plugin.portlet;

import com.atlassian.jira.portal.PortletConfiguration;
import com.atlassian.jira.portal.PortletImpl;
import com.atlassian.jira.security.JiraAuthenticationContext;

import java.util.*;

public class PassthroughPortlet extends PortletImpl {

    public PassthroughPortlet(JiraAuthenticationContext authenticationContext) {
        super(authenticationContext);
    }

    protected Map getVelocityParams(PortletConfiguration portletConfiguration) {
        Map params = new HashMap();
        params.put("portlet", this);  // automatic?
        params.put("portletConfiguration", portletConfiguration);
        Long portletId = portletConfiguration.getId();
        params.put("portletId", portletId);
        return params;
    }

}
