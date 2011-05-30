package com.atlassian.labs.speakeasy.model;

import com.atlassian.labs.speakeasy.manager.convention.JsonVendor;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;

/**
 *
 */
@XmlRootElement
public class JsonManifest
{
    public static final String ATLASSIAN_EXTENSION_PATH = "atlassian-extension.json";
    @XmlAttribute
    private String key;

    @XmlAttribute
    private String name;

    @XmlAttribute
    private String description;

    @XmlAttribute
    private String version;

    @XmlElement
    private JsonVendor vendor;

    @XmlElement
    private Map<Integer, String> icons;

    @XmlElement
    private String screenshot;

    public String getKey()
    {
        return key;
    }

    public void setKey(String key)
    {
        this.key = key;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public JsonVendor getVendor()
    {
        return vendor;
    }

    public void setVendor(JsonVendor vendor)
    {
        this.vendor = vendor;
    }

    public Map<Integer, String> getIcons()
    {
        return icons;
    }

    public void setIcons(Map<Integer, String> icons)
    {
        this.icons = icons;
    }

    public String getScreenshot()
    {
        return screenshot;
    }

    public void setScreenshot(String screenshot)
    {
        this.screenshot = screenshot;
    }
}
