package org.dogoodthings.ectr.cad;

import com.dscsag.plm.spi.interfaces.DocumentKey;
import com.dscsag.plm.spi.interfaces.ECTRService;
import com.dscsag.plm.spi.interfaces.gui.PluginFunction;
import com.dscsag.plm.spi.interfaces.gui.PluginRequest;
import com.dscsag.plm.spi.interfaces.gui.PluginResponse;
import com.dscsag.plm.spi.interfaces.gui.PluginResponseFactory;
import com.dscsag.plm.spi.interfaces.logging.PlmLogger;
import com.dscsag.plm.spi.interfaces.objects.PlmObjectKey;
import com.dscsag.plm.spi.interfaces.objects.doc.DocumentDataKeys;
import com.dscsag.plm.spi.interfaces.services.applicationfiles.transfer.TransferApplFilesService;
import com.dscsag.plm.spi.interfaces.services.applicationfiles.uptodate.ProvideApplFileUptodateInfoService;
import com.dscsag.plm.spi.interfaces.services.document.details.ProvideDocumentInfoService;
import com.dscsag.plm.spi.interfaces.services.document.key.KeyConverterService;
import com.dscsag.plm.spi.interfaces.services.document.structure.ProvideDocumentStructureService;

import java.util.HashSet;
import java.util.Set;

public class ProvideAllCadFilesInSession implements PluginFunction {

  private final ServiceTool serviceTool;

  ProvideAllCadFilesInSession(ServiceTool serviceTool) {
    this.serviceTool = serviceTool;
  }

  @Override
  public PluginResponse actionPerformed(PluginRequest pluginRequest) {
    PluginResponse response = PluginResponseFactory.emptyResponse();
    try {
      KeyConverterService keyConverter = serviceTool.getService(KeyConverterService.class);
      for (PlmObjectKey plmObjectKey : pluginRequest.getObjects()) {
        if (DocumentDataKeys.TABLE_TYPE.equals(plmObjectKey.getType())) {
          DocumentKey documentKey = keyConverter.fromPlmObjectKey(plmObjectKey);
          provideAllCadFiles(documentKey);
        }
      }
    } catch (Exception e) {
      response = PluginResponseFactory.errorResponse(e.getMessage());
    }
    return response;
  }

  private void provideAllCadFiles(DocumentKey documentKey) throws Exception {
    PlmLogger logger = serviceTool.getService(ECTRService.class).getPlmLogger();
    logger.trace("resolving structure for " + documentKey);
    Set<DocumentKey> allKeys = resolveStructure(documentKey);
    logger.trace("read metadata for all documents of " + documentKey);
    readMetadata(allKeys);
    logger.trace("uptodate check of all files for " + documentKey);
    uptodateCheck(allKeys);
    logger.trace("providing files for " + documentKey + " in session");
    provideCadFiles(allKeys);
  }

  private void provideCadFiles(Set<DocumentKey> allKeys) throws Exception {
    TransferApplFilesService transferApplFilesService = serviceTool.getService(TransferApplFilesService.class);
    var builder = transferApplFilesService.configurationBuilder();
    for (DocumentKey documentKey : allKeys)
      builder.addDocumentKey(documentKey);
    var result = transferApplFilesService.execute(builder.build());

    if (!result.isFailed() && !result.isCanceled()) {
      PlmLogger logger = serviceTool.getService(ECTRService.class).getPlmLogger();
      var x = result.get();
      for (var c : x.transferApplFileInfo())
        logger.trace("  " + c);
    }
  }

  private void uptodateCheck(Set<DocumentKey> allKeys) throws Exception {
    ProvideApplFileUptodateInfoService uptodateService = serviceTool.getService(ProvideApplFileUptodateInfoService.class);
    var builder = uptodateService.configurationBuilder();
    for (DocumentKey documentKey : allKeys)
      builder.addDocumentKey(documentKey);
    var result = uptodateService.execute(builder.build());
    if (!result.isFailed() && !result.isCanceled()) {
      PlmLogger logger = serviceTool.getService(ECTRService.class).getPlmLogger();
      for (var info : result.get().provideApplFileUptodateInfo()) {
        logger.trace(info.applFileLocalInfo().filename() + " -> " + (info.isUptodate() ? "UPTODATE" : " NOT UPTODATE"));
      }
    }
  }

  private void readMetadata(Set<DocumentKey> allKeys) throws Exception {
    ProvideDocumentInfoService docInfoService = serviceTool.getService(ProvideDocumentInfoService.class);
    var builder = docInfoService.configurationBuilder();
    for (DocumentKey documentKey : allKeys)
      builder.addDocumentKey(documentKey);
    var result = docInfoService.execute(builder.build());
    if (!result.isFailed() && !result.isCanceled()) {
      PlmLogger logger = serviceTool.getService(ECTRService.class).getPlmLogger();
      for (var document : result.get().documents())
        logger.trace(document.documentDetails().masterFilename() + " -> " + document.documentDetails().status());
    }
  }

  private Set<DocumentKey> resolveStructure(DocumentKey documentKey) throws Exception {
    Set<DocumentKey> keys = new HashSet<>();
    keys.add(documentKey);
    ProvideDocumentStructureService documentStructureService = serviceTool.getService(ProvideDocumentStructureService.class);
    var builder = documentStructureService.configurationBuilder()
        .setExplosionScenario("CAD_LOAD").setMultilevel(true)
        .setVersionRuleActive().setTopDocument(documentKey);
    var result = documentStructureService.execute(builder.build());
    if (!result.isFailed() && !result.isCanceled())
      keys.addAll(result.get().children());
    return keys;
  }
}
