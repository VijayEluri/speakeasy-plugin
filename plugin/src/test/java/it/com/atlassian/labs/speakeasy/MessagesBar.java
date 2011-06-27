package it.com.atlassian.labs.speakeasy;

import com.atlassian.pageobjects.PageBinder;
import com.atlassian.webdriver.AtlassianWebDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class MessagesBar
{
    @FindBy(id = "aui-message-bar")
    private WebElement messageBar;

    @Inject
    protected AtlassianWebDriver driver;

    @Inject
    private PageBinder pageBinder;

    private List<String> getMessages(String className)
    {
        List<String> messages = new ArrayList<String>();
        for (WebElement msg : messageBar.findElements(By.className("aui-message")))
        {
            if (msg.getAttribute("class").contains(className))
            {
                messages.add(msg.getText().trim());
            }
        }
        return messages;
    }

    public SpeakeasyUserPage waitForMessages()
    {
        driver.waitUntilElementIsVisibleAt(By.className("aui-message"), messageBar);
        return pageBinder.bind(SpeakeasyUserPage.class);
    }

    public List<String> getErrorMessages()
    {
        return getMessages("error");
    }

    public List<String> getWarningMessages()
    {
        return getMessages("warning");
    }

    public List<String> getSuccessMessages()
    {
        return getMessages("success");
    }
}
