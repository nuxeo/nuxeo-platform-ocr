package org.nuxeo.ecm.platform.ocr.service;

/**
 * OcrException to be raised whenever the commandline tool is not available on
 * the system.
 */
public class MissingCommandLineToolException extends OcrException {

    private static final long serialVersionUID = 1L;

    public MissingCommandLineToolException(String message) {
        super(message);
    }

}
