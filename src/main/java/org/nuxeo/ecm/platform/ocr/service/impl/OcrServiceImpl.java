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
package org.nuxeo.ecm.platform.ocr.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.api.ConverterCheckResult;
import org.nuxeo.ecm.platform.ocr.service.DocumentStructure;
import org.nuxeo.ecm.platform.ocr.service.ImageRegion;
import org.nuxeo.ecm.platform.ocr.service.MissingCommandLineToolException;
import org.nuxeo.ecm.platform.ocr.service.OcrException;
import org.nuxeo.ecm.platform.ocr.service.OcrService;
import org.nuxeo.ecm.platform.ocr.service.TextRegion;
import org.nuxeo.runtime.api.Framework;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Implementation of the OcrService based on the Onela command line interface.
 */
public class OcrServiceImpl implements OcrService {

    public static final String OLENA_CONVERTER_NAME = "olena_content_in_doc";

    private static final Log log = LogFactory.getLog(OcrServiceImpl.class);

    /**
     * Helper method to check the availability of the Olena command line tool
     * and help build meaningful error messages.
     */
    protected ConverterCheckResult getAvailability(ConversionService service,
            boolean refresh) throws OcrException {
        if (service == null) {
            try {
                service = Framework.getService(ConversionService.class);
            } catch (Exception e) {
                throw new OcrException(
                        "could not access the ConversionService", e);
            }
            if (service == null) {
                throw new OcrException("could not access the ConversionService");
            }
        }
        ConverterCheckResult availability;
        try {
            availability = service.isConverterAvailable(OLENA_CONVERTER_NAME,
                    refresh);
        } catch (ConversionException e) {
            throw new OcrException("failed to check availability of "
                    + OLENA_CONVERTER_NAME, e);
        }
        return availability;
    }

    @Override
    public boolean isEnabled() {
        try {
            return getAvailability(null, true).isAvailable();
        } catch (OcrException e) {
            log.warn("unexpected exception while checking availability for: "
                    + OLENA_CONVERTER_NAME, e);
            return false;
        }
    }

    @Override
    public List<String> extractText(Blob imageBlob) throws OcrException {
        DocumentStructure structure = extractDocumentStructure(imageBlob);
        List<String> aggregateText = new ArrayList<String>();
        for (TextRegion textRegion : structure.getTextRegions()) {
            aggregateText.addAll(textRegion.paragraphs);
        }
        for (ImageRegion imageRegion : structure.getImageRegions()) {
            if (imageRegion.embeddedText != null) {
                aggregateText.add(imageRegion.embeddedText);
            }
        }
        return aggregateText;
    }

    @Override
    public DocumentStructure extractDocumentStructure(Blob imageBlob)
            throws OcrException {
        ConversionService conversionService;
        try {
            conversionService = Framework.getService(ConversionService.class);
        } catch (Exception e) {
            throw new OcrException("could not access the ConversionService", e);
        }
        ConverterCheckResult availability = getAvailability(conversionService,
                false);
        if (!availability.isAvailable()) {
            throw new MissingCommandLineToolException(
                    availability.getErrorMessage() + " "
                            + availability.getInstallationMessage());
        }
        try {
            BlobHolder xmlBlobHolder = conversionService.convert(
                    OLENA_CONVERTER_NAME, new SimpleBlobHolder(imageBlob),
                    new HashMap<String, Serializable>());

            Blob xmlBlob = xmlBlobHolder.getBlob();
            if (xmlBlob == null || xmlBlob.getLength() == 0) {
                throw new OcrException("Unexpected empty XML output for "
                        + OLENA_CONVERTER_NAME);
            }
            log.debug(xmlBlob.getString());
            return parseXml(xmlBlob.getStream());
        } catch (Exception e) {
            throw new OcrException(e.getMessage(), e);
        }
    }

    protected DocumentStructure parseXml(InputStream xmlStream)
            throws OcrException {
        DocumentBuilder builder;
        List<TextRegion> textRegions = new ArrayList<TextRegion>();
        List<ImageRegion> imageRegions = new ArrayList<ImageRegion>();
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse(xmlStream);
            XPath xpath = XPathFactory.newInstance().newXPath();

            // iterate over all the text regions to extract the text content and
            // the global position of the region
            NodeList textRegionNodes = (NodeList) xpath.evaluate(
                    "//text_region", document, XPathConstants.NODESET);
            for (int i = 0; i < textRegionNodes.getLength(); i++) {

                Node textRegionNode = textRegionNodes.item(i);
                NodeList xcoordAttrs = (NodeList) xpath.evaluate(
                        "coords/point/@x", textRegionNode,
                        XPathConstants.NODESET);
                int topLeftX = -1;
                int bottomRightX = -1;
                for (int xi = 0; xi < xcoordAttrs.getLength(); xi++) {
                    int x = Integer.valueOf(((Attr) xcoordAttrs.item(xi)).getValue());
                    if (topLeftX == -1 || topLeftX > x) {
                        topLeftX = x;
                    }
                    if (bottomRightX == -1 || bottomRightX < x) {
                        bottomRightX = x;
                    }
                }

                NodeList ycoordAttrs = (NodeList) xpath.evaluate(
                        "coords/point/@y", textRegionNode,
                        XPathConstants.NODESET);
                int topLeftY = -1;
                int bottomRightY = -1;
                for (int yi = 0; yi < ycoordAttrs.getLength(); yi++) {
                    int y = Integer.valueOf(((Attr) ycoordAttrs.item(yi)).getValue());
                    if (topLeftY == -1 || topLeftY > y) {
                        topLeftY = y;
                    }
                    if (bottomRightY == -1 || bottomRightY < y) {
                        bottomRightY = y;
                    }
                }
                if (topLeftX == -1 || topLeftY == -1 || bottomRightX == -1
                        || bottomRightY == -1 || topLeftX == bottomRightX
                        || topLeftY == bottomRightY) {
                    continue;
                }
                TextRegion textRegion = new TextRegion(topLeftX, topLeftY,
                        bottomRightX, bottomRightY);
                textRegions.add(textRegion);

                NodeList textNodes = (NodeList) xpath.evaluate("line/@text",
                        textRegionNode, XPathConstants.NODESET);

                StringBuilder sb = new StringBuilder();
                for (int k = 0; k < textNodes.getLength(); k++) {
                    Attr textAttr = (Attr) textNodes.item(k);
                    String line = textAttr.getValue();
                    if (line.endsWith("-") || line.endsWith("\u2010")
                            || line.endsWith("\u2011")) {
                        // special handling for hyphens
                        sb.append(line.substring(0, line.length() - 1));
                    } else {
                        sb.append(line);
                        sb.append(" ");
                    }
                }
                String paragraph = sb.toString().trim();
                if (!paragraph.isEmpty()) {
                    // ignore empty paragraphs
                    textRegion.paragraphs.add(paragraph);
                }
            }

            // iterate over all the image regions to extract their global
            // position along with any embedded text
            NodeList imageRegionNodes = (NodeList) xpath.evaluate(
                    "//image_region", document, XPathConstants.NODESET);
            for (int i = 0; i < imageRegionNodes.getLength(); i++) {

                Node imageRegionNode = imageRegionNodes.item(i);
                NodeList xcoordAttrs = (NodeList) xpath.evaluate(
                        "coords/point/@x", imageRegionNode,
                        XPathConstants.NODESET);
                int topLeftX = -1;
                int bottomRightX = -1;
                for (int xi = 0; xi < xcoordAttrs.getLength(); xi++) {
                    int x = Integer.valueOf(((Attr) xcoordAttrs.item(xi)).getValue());
                    if (topLeftX == -1 || topLeftX > x) {
                        topLeftX = x;
                    }
                    if (bottomRightX == -1 || bottomRightX < x) {
                        bottomRightX = x;
                    }
                }

                NodeList ycoordAttrs = (NodeList) xpath.evaluate(
                        "coords/point/@y", imageRegionNode,
                        XPathConstants.NODESET);
                int topLeftY = -1;
                int bottomRightY = -1;
                for (int yi = 0; yi < ycoordAttrs.getLength(); yi++) {
                    int y = Integer.valueOf(((Attr) ycoordAttrs.item(yi)).getValue());
                    if (topLeftY == -1 || topLeftY > y) {
                        topLeftY = y;
                    }
                    if (bottomRightY == -1 || bottomRightY < y) {
                        bottomRightY = y;
                    }
                }
                if (topLeftX > -1 && topLeftY > -1 && bottomRightX > -1
                        && bottomRightY > -1 && topLeftX < bottomRightX
                        && topLeftY < bottomRightY) {
                    ImageRegion imageRegion = new ImageRegion(topLeftX,
                            topLeftY, bottomRightX, bottomRightY);
                    // TODO: extract embedded text if any
                    imageRegions.add(imageRegion);
                }
            }
            return new DocumentStructureImpl(textRegions, imageRegions);
        } catch (ParserConfigurationException e) {
            throw new OcrException(e);
        } catch (SAXException e) {
            throw new OcrException(e);
        } catch (IOException e) {
            throw new OcrException(e);
        } catch (XPathExpressionException e) {
            throw new OcrException(e);
        }
    }

}
