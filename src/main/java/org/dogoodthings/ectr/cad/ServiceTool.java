package org.dogoodthings.ectr.cad;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class ServiceTool {
  private final BundleContext context;

  ServiceTool(BundleContext context) {
    this.context = context;
  }

  public <T> T getService(Class<T> clazz) throws Exception {
    ServiceReference<T> serviceRef = context.getServiceReference(clazz);
    if (serviceRef != null)
      return context.getService(serviceRef);
    throw new Exception("Unable to find implementation for service " + clazz.getName());
  }
}
