package org.dogoodthings.ectr.cad;

import com.dscsag.plm.spi.interfaces.gui.PluginFunction;
import com.dscsag.plm.spi.interfaces.gui.PluginFunctionService;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

class PluginFunctionManager implements PluginFunctionService {
  private final Map<String, Supplier<PluginFunction>> map;

  protected PluginFunctionManager(ServiceTool serviceTool) {
    map = new HashMap<>();
    map.put("fnc.org.dogoodthings.provideAllCadFilesInSession", () -> new ProvideAllCadFilesInSession(serviceTool));
    map.put("fnc.org.dogoodthings.downloadAllPictures", () -> new DownloadAllPictures(serviceTool));
  }

  @Override
  public PluginFunction getPluginFunction(String functionName) {
    Supplier<PluginFunction> supplier = map.get(functionName);
    if (supplier != null)
      return supplier.get();
    return null;
  }
}