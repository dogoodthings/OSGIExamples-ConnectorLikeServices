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
import com.dscsag.plm.spi.interfaces.services.ServiceResult;
import com.dscsag.plm.spi.interfaces.services.document.key.KeyConverterService;
import com.dscsag.plm.spi.interfaces.services.document.originals.TransferOriginalsRequestBuilder;
import com.dscsag.plm.spi.interfaces.services.document.originals.TransferOriginalsResult;
import com.dscsag.plm.spi.interfaces.services.document.originals.TransferOriginalsService;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class DownloadAllPictures implements PluginFunction {

  private final ServiceTool serviceTool;

  DownloadAllPictures(ServiceTool serviceTool) {
    this.serviceTool = serviceTool;
  }

  @Override
  public PluginResponse actionPerformed(PluginRequest pluginRequest) {
    PluginResponse response = PluginResponseFactory.emptyResponse();
    try {
      TransferOriginalsService transferOriginalsService = serviceTool.getService(TransferOriginalsService.class);
      KeyConverterService keyConverterService = serviceTool.getService(KeyConverterService.class);
      TransferOriginalsRequestBuilder transferOriginalsRequestBuilder = transferOriginalsService.configurationBuilder();
      Set<String> documentTypes = new HashSet<>();
      for (PlmObjectKey plmObjectKey : pluginRequest.getObjects()) {
        if (DocumentDataKeys.TABLE_TYPE.equals(plmObjectKey.getType())) {
          DocumentKey documentKey = keyConverterService.fromPlmObjectKey(plmObjectKey);
          documentTypes.add(documentKey.getType());
          transferOriginalsRequestBuilder.addDocumentKey(documentKey);
        }
      }
      for (String documentType : documentTypes) {
        //we want only originals with workstation application jpg and png for every document type in selection
        transferOriginalsRequestBuilder.addPositiveFilterByDocumentType(documentType, "JPG");
        transferOriginalsRequestBuilder.addPositiveFilterByDocumentType(documentType, "PNG");
        transferOriginalsRequestBuilder.addPositiveFilterByDocumentType(documentType, "GIF");
        transferOriginalsRequestBuilder.addPositiveFilterByDocumentType(documentType, "EPJ");
        transferOriginalsRequestBuilder.addPositiveFilterByDocumentType(documentType, "EPG");
      }

      JFileChooser fileChooser = new JFileChooser();

      fileChooser.setMultiSelectionEnabled(false);
      fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

      if (fileChooser.showDialog(null, "Choose dir") == JFileChooser.APPROVE_OPTION) {
        File targetDirectory = fileChooser.getSelectedFile();
        transferOriginalsRequestBuilder.setTargetDirectory(targetDirectory.getAbsolutePath());
        ServiceResult<TransferOriginalsResult> serviceResult = transferOriginalsService.execute(transferOriginalsRequestBuilder.build());
        if (serviceResult.isCanceled())
          response = PluginResponseFactory.errorResponse("cancelled");
        else if (serviceResult.isFailed())
          response = PluginResponseFactory.errorResponse("failed");
        else {
          TransferOriginalsResult transferResult = serviceResult.get();
          PlmLogger plmLogger = serviceTool.getService(ECTRService.class).getPlmLogger();
          plmLogger.trace("downloaded files: ");
          for (var entry : transferResult.transferOriginalsInfo().entrySet()) {
            plmLogger.trace("  for " + entry.getKey() + ":");
            for (var original : entry.getValue().originals()) {
              plmLogger.trace("    " + original.originalPath());
            }
          }
          Desktop.getDesktop().open(targetDirectory);
        }
      } else
        response = PluginResponseFactory.errorResponse("no directory selected");

    } catch (Exception e) {
      response = PluginResponseFactory.errorResponse(e.getMessage());
    }
    return response;
  }
}