package com.atlassian.labs.speakeasy.ui.jira;

import com.atlassian.jira.plugin.profile.OptionalUserProfilePanel;
import com.atlassian.jira.plugin.profile.ViewProfilePanel;
import com.atlassian.jira.plugin.profile.ViewProfilePanelModuleDescriptor;
import com.atlassian.labs.speakeasy.UnauthorizedAccessException;
import com.atlassian.labs.speakeasy.ui.UserProfileRenderer;
import com.opensymphony.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import webwork.action.ServletActionContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringWriter;

/**
 *
 */
public class SpeakeasyViewProfilePanel implements ViewProfilePanel, OptionalUserProfilePanel
{
    private final UserProfileRenderer renderer;
    private final static Logger log = LoggerFactory.getLogger(SpeakeasyViewProfilePanel.class);

    public SpeakeasyViewProfilePanel(UserProfileRenderer renderer)
    {
        this.renderer = renderer;
    }

    public void init(ViewProfilePanelModuleDescriptor viewProfilePanelModuleDescriptor)
    {
    }

    public String getHtml(User user)
    {
        HttpServletRequest req = ServletActionContext.getRequest();
        HttpServletResponse resp = ServletActionContext.getResponse();
        StringWriter writer = new StringWriter();
        try
        {
            renderer.render(req, resp, writer, false);
        }
        catch (UnauthorizedAccessException e)
        {
            writer.write("Unauthorized access: " + e.getMessage());
        }
        catch (IOException e)
        {
            writer.write("Unable to render panel: " + e.getMessage());
            log.error("Error rendering speakeasy panel", e);
        }
        return writer.toString();
    }

    public boolean showPanel(User profileUser, User currentUser)
    {
        return renderer.shouldRender(currentUser.getName());
    }
}
