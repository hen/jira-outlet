    function createRequestObject-1()
    {
        var ro;
        try
        {
                    // Opera 8.0+, Firefox, Safari
                    ro = new XMLHttpRequest();
            }
        catch (e)
        {
                    // Internet Explorer Browsers
                    try
            {
                            ro = new ActiveXObject("Msxml2.XMLHTTP");
                    }
            catch (e)
            {
                            try
                {
                                    ro = new ActiveXObject("Microsoft.XMLHTTP");
                            }
                catch (e)
                {
                                    // Unable to create a request object
                                    return false;
                            }
                    }
            }
        return ro;
    }


    function toggleVote(portletId, elementId, toggleKey, issueKeys, title, diluteVote)
    {
        var reqUrl = "${req.ContextPath}/secure/RunPortlet.jspa?portletKey=com.sourcelabs.jira.plugin.portlet.poll:poll&issue-keys="+issueKeys+"%3A"+portletId+"%3A"+toggleKey+"&title="+title+"&diluteVote="+diluteVote;

        var elementId = "voteportlet-"+portletId;
        var elem = document.getElementById(elementId);

        var icon = document.getElementById("voteIcon"+toggleKey+portletId);

        var httpReq = createRequestObject();
        httpReq.open('get', reqUrl, true);
        httpReq.onreadystatechange = function() {
            if (httpReq.readyState == 4)
            {
                var response = httpReq.responseText;
                elem.innerHTML = response;
            }
        }
        icon.src = "${req.ContextPath}/images/icons/wait.gif";

        httpReq.send(null);
    }
