/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.ocr.converter;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.convert.plugins.CommandLineBasedConverter;

/**
 * Converter implementation of the OCR wrapping the Olena command-line utility.
 */
public class OcrConverter extends CommandLineBasedConverter {

    @Override
    protected BlobHolder buildResult(List<String> cmdOutput,
            CmdParameters cmdParams) throws ConversionException {
        String outputPath = cmdParams.getParameters().get("targetFilePath");
        File outputFile = new File(outputPath);

        if (!outputFile.exists()) {
            String intputPath = cmdParams.getParameters().get("sourceFilePath");
            String message = String.format(
                    "could not find the results of OCR conversion"
                            + " '%s' for input file '%s'", outputPath,
                    intputPath);
            throw new ConversionException(message);
        }
        Blob blob = new FileBlob(outputFile);
        blob.setFilename(outputFile.getName());
        blob.setMimeType("application/xml");
        return new SimpleCachableBlobHolder(blob);
    }

    @Override
    protected Map<String, Blob> getCmdBlobParameters(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {
        Map<String, Blob> cmdBlobParams = new HashMap<String, Blob>();
        try {
            cmdBlobParams.put("sourceFilePath", blobHolder.getBlob());
        } catch (ClientException e) {
            throw new ConversionException("Unable to get Blob for holder", e);
        }
        return cmdBlobParams;
    }

    @Override
    protected Map<String, String> getCmdStringParameters(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {

        Map<String, String> cmdStringParams = new HashMap<String, String>();

        String baseDir = getTmpDirectory(parameters);
        Path targetFilePath = new Path(baseDir).append("ocr_olena_"
                + System.currentTimeMillis() + ".xml");

        cmdStringParams.put("targetFilePath", targetFilePath.toString());
        return cmdStringParams;
    }

}
