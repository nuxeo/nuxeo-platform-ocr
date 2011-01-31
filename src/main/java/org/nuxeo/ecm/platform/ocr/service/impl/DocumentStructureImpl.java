package org.nuxeo.ecm.platform.ocr.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.platform.ocr.service.DocumentStructure;
import org.nuxeo.ecm.platform.ocr.service.ImageRegion;
import org.nuxeo.ecm.platform.ocr.service.TextRegion;

public class DocumentStructureImpl implements DocumentStructure {

    protected final List<TextRegion> textRegions = new ArrayList<TextRegion>();

    protected final List<ImageRegion> imageRegions = new ArrayList<ImageRegion>();

    public DocumentStructureImpl(List<TextRegion> textRegions,
            List<ImageRegion> imageRegions) {
        this.textRegions.addAll(textRegions);
        this.imageRegions.addAll(imageRegions);
    }

    @Override
    public List<TextRegion> getTextRegions() {
        return new ArrayList<TextRegion>(textRegions);
    }

    @Override
    public List<ImageRegion> getImageRegions() {
        return new ArrayList<ImageRegion>(imageRegions);
    }

}
