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

import java.util.List;

import org.nuxeo.ecm.core.api.Blob;

/**
 * Service to analyze a blob holding a picture and extract text content.
 */
public interface OcrService {

    /**
     * @return true is the olena third party commandline tool is found on the
     *         system
     */
    public boolean isEnabled();

    /**
     * @return the aggregated text content as extracted by the OCR. Each text
     *         area content is grouped in a single string.
     */
    public List<String> extractText(Blob imageBlob) throws OcrException;

    /**
     * @return the document structure as extracted by the OCR
     */
    public DocumentStructure extractDocumentStructure(Blob imageBlob)
            throws OcrException;
}
