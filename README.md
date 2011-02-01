# Implementation of full document OCR based on Olena & Tesseract

This addon need the Olena command line utility to analyse digital document
available in common picture formats (e.g. png, tif, gif, jpeg, ...).

Olena's development and support for document analysis was funded as part of the
Scribo (http://scribo.ws) R&D project. This work is still under progress (as of
january 2011) and hence require some manual non trivial build setup.


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

    $ wget http://www.lrde.epita.fr/dload/olena/snapshots/next-build-test/olena-1.0a-snapshot-next-build-test.tar.bz2
    $ tar jxvf olena-1.0a-*.tar.bz2
    $ cd olena-1.0a/
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
