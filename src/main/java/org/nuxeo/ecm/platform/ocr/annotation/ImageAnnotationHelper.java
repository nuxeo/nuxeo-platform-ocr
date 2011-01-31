package org.nuxeo.ecm.platform.ocr.annotation;

import java.io.ByteArrayInputStream;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.ecm.platform.annotations.api.AnnotationManager;
import org.nuxeo.ecm.platform.annotations.api.AnnotationsService;
import org.nuxeo.ecm.platform.ocr.service.DocumentStructure;
import org.nuxeo.ecm.platform.ocr.service.TextRegion;
import org.nuxeo.runtime.api.Framework;

/**
 * Utility to convert a the TextRegions of an image into a list of RDF
 * annotations suitable for the Nuxeo Annotations web client.
 */
public class ImageAnnotationHelper {

    public static final String ANNOTATION_TEMPLATE = "<?xml version=\"1.0\"?>"
            + "<r:RDF xmlns:a=\"http://www.w3.org/2000/10/annotation-ns#\" xmlns:r=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\""
            + "    xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:h=\"http://www.w3.org/1999/xx/http#\""
            + "    xmlns:nx=\"http://www.nuxeo.org/document/uid/\">"
            + "    <r:Description>"
            + "      <r:type r:resource=\"http://www.w3.org/2000/10/annotation-ns#Annotation\" />"
            + "      <r:type r:resource=\"http://www.w3.org/2000/10/annotationType#Comment\" />"
            + "      <a:annotates r:resource=\"urn:nuxeo:${repo}:${ref}\" /> "
            + "      <a:context>urn:nuxeo:${repo}:${ref}#xpointer(image-range(//img[1],[${topLeftX},${topLeftY}],[${bottomRightX},${bottomRightY}]))</a:context> "
            + "      <a:body r:parseType=\"Literal\">${body}</a:body>"
            + "    </r:Description></r:RDF>";

    public static void saveAsAnnotations(DocumentStructure structure,
            DocumentLocation docLocation) throws Exception {
        AnnotationManager manager = new AnnotationManager();

        AnnotationsService annotationsService = Framework.getService(AnnotationsService.class);
        for (TextRegion textRegion : structure.getTextRegions()) {
            String xmlDescription = ANNOTATION_TEMPLATE;
            xmlDescription = xmlDescription.replace("${repo}",
                    docLocation.getServerName());
            xmlDescription = xmlDescription.replace("${ref}",
                    docLocation.getDocRef().toString());
            String body = StringUtils.join(textRegion.paragraphs, "\n\n");
            body = StringEscapeUtils.escapeXml(body);
            xmlDescription = xmlDescription.replace("${body}", body);
            xmlDescription = xmlDescription.replace("${topLeftX}",
                    String.valueOf(textRegion.topLeftX));
            xmlDescription = xmlDescription.replace("${topLeftY}",
                    String.valueOf(textRegion.topLeftY));
            xmlDescription = xmlDescription.replace("${bottomRightX}",
                    String.valueOf(textRegion.bottomRightX));
            xmlDescription = xmlDescription.replace("${bottomRightY}",
                    String.valueOf(textRegion.bottomRightY));
            Annotation annotation = manager.getAnnotation(new ByteArrayInputStream(
                    xmlDescription.getBytes("UTF-8")));
            annotationsService.addAnnotation(annotation, new UserPrincipal(
                    "OCR", null, false, true), "http://server/");
        }
    }
}
