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
package org.nuxeo.ecm.platform.ocr.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.platform.commandline.executor.service.CommandLineExecutorComponent;
import org.nuxeo.ecm.platform.ocr.annotation.ImageAnnotationHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestOcrService extends NXRuntimeTestCase {

    private static final Log log = LogFactory.getLog(CommandLineExecutorComponent.class);

    OcrService ocrService;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.convert");
        deployBundle("org.nuxeo.ecm.platform.commandline.executor");
        deployBundle("org.nuxeo.ecm.platform.ocr");
        ocrService = Framework.getService(OcrService.class);
        assertNotNull(ocrService);
    }

    @Test
    public void testServiceEnabled() throws Exception {
        if (!ocrService.isEnabled()) {
            log.warn("Olena commandline is not available: skipping tests");
        }
    }

    @Test
    public void testExtractLenaStructure() throws Exception {
        if (!ocrService.isEnabled()) {
            return;
        }
        InputStream imageStream = getClass().getResourceAsStream(
                "/wikilena.png");
        assertNotNull(imageStream);
        Blob imageBlob = StreamingBlob.createFromStream(imageStream).persist();
        try {
            DocumentStructure structure = ocrService.extractDocumentStructure(imageBlob);

            // integrity checks on the structure of the text regions
            List<TextRegion> textRegions = structure.getTextRegions();
            assertFalse(textRegions.isEmpty());
            assertEquals(271, textRegions.get(0).topLeftX);
            assertEquals(20, textRegions.get(0).topLeftY);
            assertEquals(382, textRegions.get(0).bottomRightX);
            assertEquals(49, textRegions.get(0).bottomRightY);
            assertFalse("No paragraph found in region",
                    textRegions.get(0).paragraphs.isEmpty());
            assertEquals("Lenna", textRegions.get(0).paragraphs.get(0));
            for (TextRegion textRegion : textRegions) {
                assertTrue(String.format("topLeftX=%d >= bottomRightX=%d",
                        textRegion.topLeftX, textRegion.bottomRightX),
                        textRegion.topLeftX < textRegion.bottomRightX);
                assertTrue(String.format("topLeftY=%d >= bottomRightY=%d",
                        textRegion.topLeftY, textRegion.bottomRightY),
                        textRegion.topLeftY < textRegion.bottomRightY);
            }

            // integrity checks on the structure of the image regions
            List<ImageRegion> imageRegions = structure.getImageRegions();
            assertEquals(2, imageRegions.size());
            for (ImageRegion image : imageRegions) {
                assertTrue(String.format("topLeftX=%d >= bottomRightX=%d",
                        image.topLeftX, image.bottomRightX),
                        image.topLeftX < image.bottomRightX);
                assertTrue(String.format("topLeftY=%d >= bottomRightY=%d",
                        image.topLeftY, image.bottomRightY),
                        image.topLeftY < image.bottomRightY);
            }

            // check that the structure can be converted to a nuxeo annotation
            deployBundle("org.nuxeo.ecm.relations.api");
            deployBundle("org.nuxeo.ecm.relations");
            deployBundle("org.nuxeo.ecm.relations.default.config");
            deployBundle("org.nuxeo.ecm.relations.jena");
            deployBundle("org.nuxeo.ecm.platform.annotations.api");
            deployBundle("org.nuxeo.ecm.annotations");
            deployContrib("org.nuxeo.ecm.platform.ocr.test",
                    "OSGI-INF/annotations-relations-contrib.xml");
            deployBundle("org.nuxeo.ecm.annotations.repository");

            ImageAnnotationHelper.saveAsAnnotations(structure,
                    new DocumentLocationImpl("test", new IdRef(
                            "dead-beef-cafe-babe")));
        } catch (MissingCommandLineToolException e) {
            log.warn("olena commandline is not available: skipping test");
        }
    }

    @Test
    public void testExtractLenaText() throws Exception {
        if (!ocrService.isEnabled()) {
            return;
        }
        InputStream imageStream = getClass().getResourceAsStream(
                "/wikilena.png");
        assertNotNull(imageStream);
        Blob imageBlob = StreamingBlob.createFromStream(imageStream).persist();
        try {
            List<String> paragraphs = ocrService.extractText(imageBlob);
            assertFalse(paragraphs.isEmpty());
            assertEquals("Lenna", paragraphs.get(0));
        } catch (MissingCommandLineToolException e) {
            log.warn("olena commandline is not available: skipping test");
        }
    }
}
