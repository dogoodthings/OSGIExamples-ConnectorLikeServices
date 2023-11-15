package org.dogoodthings.ectr.cad;

import com.dscsag.plm.spi.interfaces.gui.PluginFunctionService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;


public class Activator implements BundleActivator {
  public void start(BundleContext context) throws Exception {
    ServiceTool st = new ServiceTool(context);
    context.registerService(PluginFunctionService.class, new PluginFunctionManager(st), null);
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    // empty
  }
}
