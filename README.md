# Implementation of full document OCR based on Olena & Tesseract

This addon need the Olena command line utility to analyse digital document
available in common picture formats (e.g. png, tif, gif, jpeg, ...).

Olena's development and support for document analysis as well as the
integration in Nuxeo through this addon was funded as part of the
Scribo (http://scribo.ws) R&D project.

Olena 2.0 and Tesseract 3 are still not yet packaged by default in most
Linux distributions hence some manual build steps are required.

  <http://www.lrde.epita.fr/cgi-bin/twiki/view/Olena/Olena200>

## Notes

* The quality of the extraction is good only for high resolution
  pictures. For instance photos of a newspaper taken from a mobile
  phone will likely yield unusable output.

* Supporting for PDF files would require an additional step to use
  Apache PDFBox to extract the sizeable pictures from the PDF file to
  pass to Olena / Tesseract. This is not implemented in the current
  version.

## Building the olena command line tool used by the Nuxeo Addon

Here are some instruction to build it under ubuntu & debian linux.

1- Build tesseract 3 by following the official instructions

2- Install the following packaged dependencies:

    $ sudo apt-get install \
      build-essential \
      graphicsmagick-libmagick-dev-compat \
      libmagics++-dev \
      xsltproc \
      fop \
      hevea \
      latex2html \
      autoconf

3- Build Olena itself:

    $ wget http://www.lrde.epita.fr/dload/olena/2.0/olena-2.0.tar.bz2
    $ tar jxvf olena-*.tar.bz2
    $ cd olena-2.0/
    $ mkdir _build
    $ cd _build
    $ ../configure && make
    $ cd scribo/src
    $ make

You should then have a program `content_in_doc`; you can test it with:

    $ ./content_in_doc /path/to/a/picture.png /path/to/result.xml

Install the `content_in_doc` program somewhere in your system path so
that Nuxeo can pick to up to analyze image documents and extract text
annotations.


## Building the Nuxeo Addon itself

Using maven 2.2.1 or later, from root of the `nuxeo-platform-ocr` folder:

    $ mvn install

Then copy the jar `target/nuxeo-platform-ocr-*-SNAPSHOT.jar` into the
`nxserver/bundles` folder of your Nuxeo DM or DAM instance (assuming the default
tomcat package).

## Using the addon

To test the addon, find a high resolution picture of a digitized newspaper or
other text document such as:

  <http://www.google.com/images?as_q=magazine+article&biw=1280>

In Nuxeo DM or DAM import the picture as a new File or Picture
document wait approximately 5s (the OCR is working asynchronously in
the background). Go to the preview tab and have look at the annotated
text areas.

## About Nuxeo

Nuxeo provides a modular, extensible Java-based
[open source software platform for enterprise content
management](http://www.nuxeo.com/en/products/ep)
and packaged applications for [document
management](http://www.nuxeo.com/en/products/document-management),
[digital asset management](http://www.nuxeo.com/en/products/dam) and [case
management](http://www.nuxeo.com/en/products/case-management). Designed
by developers for developers, the Nuxeo platform offers a modern
architecture, a powerful plug-in model and extensive packaging
capabilities for building content applications.

More information on: <http://www.nuxeo.com/>
