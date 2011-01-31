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
 *     Olivier Grisel
 */
package org.nuxeo.ecm.platform.ocr.listener;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.common.collections.ScopedMap;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLBlob;
import org.nuxeo.ecm.core.utils.BlobsExtractor;
import org.nuxeo.ecm.platform.ocr.annotation.ImageAnnotationHelper;
import org.nuxeo.ecm.platform.ocr.service.DocumentStructure;
import org.nuxeo.ecm.platform.ocr.service.OcrService;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.Literal;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.NodeFactory;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Base default implementation of an asynchronous event listener that runs the
 * image OCR service and convert the results as annotations.
 * 
 * @author ogrisel@nuxeo.com
 */
public class OcrImageAnnotationAsyncListener implements PostCommitEventListener {

    public static final String ANNOTATIONS_GRAPH_NAME = "annotations";

    public static final String HAS_OCR_PROPERTY = "http://www.nuxeo.org/document/hasOcrFor";

    protected static final Log log = LogFactory.getLog(OcrImageAnnotationAsyncListener.class);

    protected static final String ALLREADY_OCRED_FLAG = OcrImageAnnotationAsyncListener.class.getName();

    protected static ThreadLocal<BlobsExtractor> extractor = new ThreadLocal<BlobsExtractor>() {
        protected synchronized BlobsExtractor initialValue() {
            return new BlobsExtractor();
        }
    };

    // to be overridden in derived classes
    protected Set<String> eventNames = new HashSet<String>(Arrays.asList(
            DocumentEventTypes.DOCUMENT_CREATED,
            DocumentEventTypes.DOCUMENT_UPDATED));

    public void handleEvent(EventBundle events) throws ClientException {
        // collect ids of documents to analyze while filtering duplicated doc
        // ids
        Set<DocumentModel> collectedDocuments = new LinkedHashSet<DocumentModel>(
                events.size());
        for (Event event : events) {
            if (!eventNames.contains(event.getName())) {
                continue;
            }
            EventContext ctx = event.getContext();
            if (ctx.hasProperty(ALLREADY_OCRED_FLAG)) {
                // avoid infinite loops with event listeners triggering them
                // selves on the same documents
                continue;
            }
            if (ctx instanceof DocumentEventContext) {
                DocumentEventContext docCtx = (DocumentEventContext) ctx;
                DocumentModel doc = docCtx.getSourceDocument();
                if (doc != null) {
                    ScopedMap contextData = doc.getContextData();
                    contextData.putScopedValue(ScopeType.REQUEST,
                            ALLREADY_OCRED_FLAG, Boolean.TRUE);
                    collectedDocuments.add(doc);
                }
            }
        }
        if (!collectedDocuments.isEmpty()) {
            try {
                OcrService ocrService = Framework.getService(OcrService.class);
                RelationManager relationService = Framework.getService(RelationManager.class);
                Resource hasOcrFor = NodeFactory.createResource(HAS_OCR_PROPERTY);
                Graph annotationGraph = relationService.getGraphByName(ANNOTATIONS_GRAPH_NAME);
                for (DocumentModel doc : collectedDocuments) {
                    Resource docResource = NodeFactory.createResource(String.format(
                            "urn:nuxeo:%s:%s", doc.getRepositoryName(),
                            doc.getId()));
                    Collection<Blob> blobs = getBlobs(doc);
                    for (Blob blob : blobs) {
                        if (!(blob instanceof SQLBlob)) {
                            // this should always be the case with VCS
                            continue;
                        }
                        String digest = ((SQLBlob) blob).getBinary().getDigest();
                        Literal digestLiteral = NodeFactory.createLiteral(digest);
                        Statement stmt = new StatementImpl(docResource,
                                hasOcrFor, digestLiteral);
                        List<Statement> matching = annotationGraph.getStatements(stmt);
                        if (!matching.isEmpty()) {
                            // this document / pair has already been annotated
                            // with the OCR, skip
                            continue;
                        }
                        String mimeType = blob.getMimeType();
                        if (mimeType != null && mimeType.startsWith("image/")) {
                            DocumentStructure structure = ocrService.extractDocumentStructure(blob);
                            ImageAnnotationHelper.saveAsAnnotations(structure,
                                    new DocumentLocationImpl(doc));
                        }
                        annotationGraph.add(Collections.singletonList(stmt));
                    }
                }
            } catch (Exception e) {
                throw new ClientException(e);
            }
        }
    }

    /**
     * Generic method to fetch the list of blobs of a document that are likely
     * to be worth annotating with the OCR service.
     */
    public Collection<Blob> getBlobs(DocumentModel doc) throws ClientException {
        return getSingleBlobFromBlobHolder(doc);
    }

    /**
     * Default implementation for {@code #getBlobs(DocumentModel)} based on the
     * registered blob holder adatper.
     */
    public Collection<Blob> getSingleBlobFromBlobHolder(DocumentModel doc)
            throws ClientException {
        BlobHolder holder = doc.getAdapter(BlobHolder.class);
        if (holder == null) {
            return Collections.emptyList();
        }
        return Collections.singleton(holder.getBlob());
    }

    /**
     * Alternative implementation for {@code #getBlobs(DocumentModel)} based on
     * the registered blob holder adatper. This variant returns all the blobs
     * managed by the blob holder.
     */
    public Collection<Blob> getAllBlobsFromBlobHolder(DocumentModel doc)
            throws ClientException {
        BlobHolder holder = doc.getAdapter(BlobHolder.class);
        if (holder == null) {
            return Collections.emptyList();
        }
        return holder.getBlobs();
    }

    /**
     * Alternative implementation to fetch the blobs, less explicit controllable
     * that the blob holder implementation that could be used as a fallback by
     * deriving this class, overriding the {@code #getBlobs(DocumentModel)}
     * method to point to it instead.
     */
    public Collection<Blob> getAllBlobsFromExtractor(DocumentModel doc)
            throws ClientException {
        return extractor.get().getBlobs(doc);
    }
}
