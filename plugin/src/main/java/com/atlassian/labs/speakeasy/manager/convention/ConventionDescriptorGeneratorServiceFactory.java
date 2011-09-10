package com.atlassian.labs.speakeasy.manager.convention;

import com.atlassian.jira.permission.PermissionSchemeManager;
import com.atlassian.labs.speakeasy.commonjs.CommonJsModules;
import com.atlassian.labs.speakeasy.commonjs.Module;
import com.atlassian.labs.speakeasy.commonjs.util.JsDoc;
import com.atlassian.labs.speakeasy.descriptor.DescriptorGeneratorManager;
import com.atlassian.labs.speakeasy.descriptor.SpeakeasyWebResourceModuleDescriptor;
import com.atlassian.labs.speakeasy.commonjs.descriptor.SpeakeasyCommonJsModulesDescriptor;
import com.atlassian.labs.speakeasy.descriptor.server.SpeakeasyWebPanelModuleDescriptor;
import com.atlassian.labs.speakeasy.descriptor.webfragment.SpeakeasyWebItemModuleDescriptor;
import com.atlassian.labs.speakeasy.manager.PermissionManager;
import com.atlassian.labs.speakeasy.manager.PluginOperationFailedException;
import com.atlassian.labs.speakeasy.manager.convention.external.ConventionDescriptorGenerator;
import com.atlassian.labs.speakeasy.model.JsonManifest;
import com.atlassian.labs.speakeasy.model.Permission;
import com.atlassian.labs.speakeasy.ringojs.external.ServerSideJsNotEnabledException;
import com.atlassian.plugin.*;
import com.atlassian.plugin.descriptors.UnloadableModuleDescriptorFactory;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.module.ModuleFactory;
import com.atlassian.plugin.osgi.util.OsgiHeaderUtil;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.plugin.webresource.WebResourceModuleDescriptor;
import com.google.common.collect.ComputationException;
import org.apache.commons.lang.Validate;
import org.apache.velocity.test.IntrospectorTestCase2;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import sun.security.krb5.internal.crypto.Des;

import javax.xml.stream.Location;

import java.util.Collections;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.emptySet;

/**
 *
 */
public class ConventionDescriptorGeneratorServiceFactory implements ServiceFactory
{
    private final ModuleFactory moduleFactory;
    private final BundleContext bundleContext;
    private final PluginAccessor pluginAccessor;
    private final HostContainer hostContainer;
    private final DescriptorGeneratorManager descriptorGeneratorManager;
    private final JsonToElementParser jsonToElementParser;
    private final WebResourceManager webResourceManager;
    private final JsonManifestHandler jsonManifestHandler;

    //private final Set<String> trackedPlugins = new CopyOnWriteArraySet<String>();

    public ConventionDescriptorGeneratorServiceFactory(ModuleFactory moduleFactory, final BundleContext bundleContext, final PluginAccessor pluginAccessor, HostContainer hostContainer, DescriptorGeneratorManager descriptorGeneratorManager, JsonToElementParser jsonToElementParser, WebResourceManager webResourceManager, JsonManifestHandler jsonManifestHandler)
    {
        this.moduleFactory = moduleFactory;
        this.bundleContext = bundleContext;
        this.pluginAccessor = pluginAccessor;
        this.hostContainer = hostContainer;
        this.descriptorGeneratorManager = descriptorGeneratorManager;
        this.jsonToElementParser = jsonToElementParser;
        this.webResourceManager = webResourceManager;
        this.jsonManifestHandler = jsonManifestHandler;
    }

    public Object getService(Bundle bundle, ServiceRegistration registration)
    {
        DocumentFactory factory = DocumentFactory.getInstance();
        String pluginKey = OsgiHeaderUtil.getPluginKey(bundle);
        Plugin plugin = pluginAccessor.getPlugin(pluginKey);

        if (bundle.getEntry("atlassian-extension.json") != null)
        {
            JsonManifest mf = jsonManifestHandler.read(plugin);
            if (mf.getScreenshot() != null)
            {
                registerScreenshotWebResourceDescriptor(bundle, factory, plugin, mf.getScreenshot());
            }
        }

        if (bundle.getEntry("js/") != null)
        {
            SpeakeasyCommonJsModulesDescriptor descriptor = new SpeakeasyCommonJsModulesDescriptor(moduleFactory, bundleContext, hostContainer, descriptorGeneratorManager, pluginAccessor);

            Element modules = factory.createElement("scoped-modules")
                .addAttribute("key", "modules")
                .addAttribute("location", "js");
            if (bundle.getEntry("css/") != null)
            {
                modules.addElement("dependency").setText("css");
            }

            descriptor.init(plugin, modules);
            bundle.getBundleContext().registerService(ModuleDescriptor.class.getName(), descriptor, null);
        }

        if (bundle.getEntry("server/") != null)
        {
            registerSpeakeasyWebPanels(factory, bundle, plugin);
        }

        if (bundle.getEntry("images/") != null)
        {
            registerSpeakeasyWebResourceDescriptor(bundle, factory, plugin, "images");
        }

        if (bundle.getEntry("css") != null)
        {
            registerSpeakeasyWebResourceDescriptor(bundle, factory, plugin, "css");
        }

        if (bundle.getEntry("ui/web-items.json") != null)
        {
            registerSpeakeasyWebItems(bundle, plugin);
        }

        //trackedPlugins.add(pluginKey);

        return new ConventionDescriptorGenerator()
        {
        };
    }

    private void registerSpeakeasyWebPanels(DocumentFactory factory, Bundle bundle, Plugin plugin)
    {
        CommonJsModules modules = new CommonJsModules(plugin, bundle, "server/", Collections.<String>emptySet());
        for (Module module : modules.getIterableModules())
        {
            final JsDoc jsDoc = module.getJsDoc();
            if (jsDoc.getAttribute("web-panel") != null)
            {
                String location = jsDoc.getAttribute("location");
                Validate.notNull(location, "@location is required");
                String weight = jsDoc.getAttribute("weight", "1000");

                Element element = factory.createElement("web-panel")
                        .addAttribute("key", "web-panel-" + module.getId())
                        .addAttribute("weight", weight)
                        .addAttribute("location", location)
                        .addAttribute("class", module.getId());

                ModuleDescriptor descriptor = new SpeakeasyWebPanelModuleDescriptor(bundle.getBundleContext(), descriptorGeneratorManager, hostContainer);
                descriptor.init(plugin, element);
                bundle.getBundleContext().registerService(ModuleDescriptor.class.getName(), descriptor, null);
            }
        }
    }

    private void registerSpeakeasyWebItems(Bundle bundle, Plugin plugin)
    {

        try
        {
            for (Element element : jsonToElementParser.createWebItems(plugin.getResourceAsStream("ui/web-items.json")))
            {
                SpeakeasyWebItemModuleDescriptor descriptor = new SpeakeasyWebItemModuleDescriptor(bundleContext, descriptorGeneratorManager, webResourceManager);
                descriptor.init(plugin, element);
                bundle.getBundleContext().registerService(ModuleDescriptor.class.getName(), descriptor, null);
            }
        }
        catch (PluginOperationFailedException e)
        {
            e.setPluginKey(plugin.getKey());
            throw e;
        }
    }

    private void registerScreenshotWebResourceDescriptor(Bundle bundle, DocumentFactory factory, Plugin plugin, String screenshotPath)
    {
        WebResourceModuleDescriptor descriptor = new WebResourceModuleDescriptor(hostContainer);

        Element element = factory.createElement("web-resource")
                .addAttribute("key", "screenshot");

        element.addElement("resource")
                .addAttribute("type", "download")
                .addAttribute("name", "screenshot.png")
                .addAttribute("location", screenshotPath);
        descriptor.init(plugin, element);
        bundle.getBundleContext().registerService(ModuleDescriptor.class.getName(), descriptor, null);
    }

    private void registerSpeakeasyWebResourceDescriptor(Bundle bundle, DocumentFactory factory, Plugin plugin, String type)
    {
        SpeakeasyWebResourceModuleDescriptor descriptor = new SpeakeasyWebResourceModuleDescriptor(hostContainer, bundleContext, descriptorGeneratorManager);

        Element element = factory.createElement("scoped-web-resource")
                .addAttribute("key", type)
                .addAttribute("scan", "/" + type);

        element.addElement("transformation")
                .addAttribute("extension", "css")
                .addElement("transformer")
                    .addAttribute("key", "cssVariables")
                    .addAttribute("imagesModuleKey", plugin.getKey() + ":" + "images-" + bundle.getLastModified())
                    .addAttribute("fullModuleKey", plugin.getKey() + ":" + "css-" + bundle.getLastModified());


        descriptor.init(plugin, element);
        bundle.getBundleContext().registerService(ModuleDescriptor.class.getName(), descriptor, null);
    }

    public void ungetService(Bundle bundle, ServiceRegistration registration, Object service)
    {
        final String pluginKey = OsgiHeaderUtil.getPluginKey(bundle);
//        Plugin plugin = pluginAccessor.getPlugin(pluginKey);
//        if (trackedPlugins.contains(plugin.getKey()))
//        {
//            for (ModuleDescriptor descriptor : plugin.getModuleDescriptors())
//            {
//                if (descriptor instanceof StateAware)
//                {
//                    ((StateAware)descriptor).disabled();
//                }
//            }
//        }
        //trackedPlugins.remove(pluginKey);
    }
}
