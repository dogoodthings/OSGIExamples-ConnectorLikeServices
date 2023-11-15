package org.dogoodthings.ectr.cad;

import com.dscsag.plm.spi.interfaces.gui.PluginFunction;
import com.dscsag.plm.spi.interfaces.gui.PluginRequest;
import com.dscsag.plm.spi.interfaces.gui.PluginResponse;

public class ProvideAllCadFilesInSession implements PluginFunction {

  private final ServiceTool serviceTool;

  ProvideAllCadFilesInSession(ServiceTool serviceTool) {
    this.serviceTool = serviceTool;
  }

  @Override
  public PluginResponse actionPerformed(PluginRequest pluginRequest) {
    return null;
  }
}
