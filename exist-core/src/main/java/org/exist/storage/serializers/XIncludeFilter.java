/*
 * eXist-db Open Source Native XML Database
 * Copyright (C) 2001 The eXist-db Authors
 *
 * info@exist-db.org
 * http://www.exist-db.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.exist.storage.serializers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.Namespaces;
import org.exist.dom.INodeHandle;
import org.exist.dom.persistent.BinaryDocument;
import org.exist.dom.persistent.DocumentImpl;
import org.exist.dom.QName;
import org.exist.dom.memtree.SAXAdapter;
import org.exist.security.Permission;
import org.exist.security.PermissionDeniedException;
import org.exist.source.DBSource;
import org.exist.source.Source;
import org.exist.source.StringSource;
import org.exist.storage.XQueryPool;
import com.evolvedbinary.j8fu.Either;
import org.exist.util.XMLReaderPool;
import org.exist.util.serializer.AttrList;
import org.exist.util.serializer.Receiver;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.CompiledXQuery;
import org.exist.xquery.Constants;
import org.exist.xquery.Expression;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQuery;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.util.ExpressionDumper;
import org.exist.xquery.value.NodeValue;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceIterator;
import org.exist.xquery.value.Type;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A filter that listens for XInclude elements in the stream
 * of events generated by the {@link org.exist.storage.serializers.Serializer}.
 *
 * XInclude elements are expanded at the position where they were found.
 */
public class XIncludeFilter implements Receiver {

    private static final Logger LOG = LogManager.getLogger(XIncludeFilter.class);

    private static final QName HREF_ATTRIB = new QName("href", XMLConstants.NULL_NS_URI);
    private static final QName XPOINTER_ATTRIB = new QName("xpointer", XMLConstants.NULL_NS_URI);
    private static final String XI_INCLUDE = "include";
    private static final String XI_FALLBACK = "fallback";

    private static class ResourceError {
        private final String message;
        private final Optional<Exception> cause;

        private ResourceError(final String message, final Exception cause) {
            this.message = message;
            this.cause = Optional.ofNullable(cause);
        }

        private ResourceError(final String message) {
            this.message = message;
            this.cause = Optional.empty();
        }
    }

    private Receiver receiver;
    private Serializer serializer;
    private DocumentImpl document = null;
    private String moduleLoadPath = null;
    private Map<String, String> namespaces = new HashMap<>(10);
    private boolean inFallback = false;
    private ResourceError error = null;

    public XIncludeFilter(final Serializer serializer, final Receiver receiver) {
        this.receiver = receiver;
        this.serializer = serializer;
    }

    public XIncludeFilter(final Serializer serializer) {
        this(serializer, null);
    }

    public void setReceiver(final Receiver handler) {
        this.receiver = handler;
    }

    public Receiver getReceiver() {
        return receiver;
    }

    public void setDocument(final DocumentImpl doc) {
        this.document = doc;
        this.inFallback = false;
        this.error = null;
    }

    public void setModuleLoadPath(final String path) {
        this.moduleLoadPath = path;
    }

    @Override
    public void characters(final CharSequence seq) throws SAXException {
        if (!inFallback || error != null) {
            receiver.characters(seq);
        }
    }

    @Override
    public void comment(final char[] ch, final int start, final int length) throws SAXException {
        if (!inFallback || error != null) {
            receiver.comment(ch, start, length);
        }
    }

    @Override
    public void endDocument() throws SAXException {
        receiver.endDocument();
    }

    @Override
    public void endElement(final QName qname) throws SAXException {
        if (Namespaces.XINCLUDE_NS.equals(qname.getNamespaceURI())) {
            if (XI_FALLBACK.equals(qname.getLocalPart())) {
                inFallback = false;
                // clear error
                error = null;
            } else if (XI_INCLUDE.equals(qname.getLocalPart()) && error != null) {
                // found an error, but there was no fallback element.
                // throw the exception now
                final SAXException e = error.cause.map(cause -> new SAXException(error.message, cause)).orElse(new SAXException(error.message));
                error = null;
                throw e;
            }
        } else if (!inFallback || error != null) {
            receiver.endElement(qname);
        }
    }

    @Override
    public void endPrefixMapping(final String prefix) throws SAXException {
        namespaces.remove(prefix);
        receiver.endPrefixMapping(prefix);
    }

    @Override
    public void processingInstruction(final String target, final String data) throws SAXException {
        if (!inFallback || error != null) {
            receiver.processingInstruction(target, data);
        }
    }

    @Override
    public void cdataSection(final char[] ch, final int start, final int len) throws SAXException {
        if (!inFallback || error != null) {
            receiver.cdataSection(ch, start, len);
        }
    }

    @Override
    public void startDocument() throws SAXException {
        receiver.startDocument();
    }

    @Override
    public void attribute(final QName qname, final String value) throws SAXException {
        if (!inFallback || error != null) {
            receiver.attribute(qname, value);
        }
    }

    @Override
    public void startElement(final QName qname, final AttrList attribs) throws SAXException {
        if (qname.getNamespaceURI() != null && qname.getNamespaceURI().equals(Namespaces.XINCLUDE_NS)) {
            if (qname.getLocalPart().equals(XI_INCLUDE)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("processing include ...");
                }

                final Optional<ResourceError> maybeResourceError = processXInclude(attribs.getValue(HREF_ATTRIB), attribs.getValue(XPOINTER_ATTRIB));

                if (maybeResourceError.isPresent()) {
                    final ResourceError resourceError = maybeResourceError.get();
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(resourceError.message, resourceError);
                    }
                    error = resourceError;
                }
            } else if (qname.getLocalPart().equals(XI_FALLBACK)) {
                inFallback = true;
            }
        } else if (!inFallback || error != null) {
            //LOG.debug("start: " + qName);
            receiver.startElement(qname, attribs);
        }
    }

    @Override
    public void documentType(final String name, final String publicId, final String systemId)
            throws SAXException {
        receiver.documentType(name, publicId, systemId);
    }

    @Override
    public void highlightText(final CharSequence seq) {
        // not supported with this receiver
    }

    /**
     * @param href     The resource to be xincluded
     * @param xpointer The xpointer
     * @return Optionally a ResourceError if it was not possible to retrieve the resource
     * to be xincluded
     * @throws SAXException              If a SAX processing error occurs
     */
    protected Optional<ResourceError> processXInclude(final String href, String xpointer) throws SAXException {
        if (href == null) {
            throw new SAXException("No href attribute found in XInclude include element");
        }
        // save some settings
        DocumentImpl prevDoc = document;
        boolean createContainerElements = serializer.createContainerElements;
        serializer.createContainerElements = false;

        //The following comments are the basis for possible external documents
        XmldbURI docUri = null;
        try {
            docUri = XmldbURI.xmldbUriFor(href);
            /*
               if(!stylesheetUri.toCollectionPathURI().equals(stylesheetUri)) {
                   externalUri = stylesheetUri.getXmldbURI();
               }
               */
        } catch (final URISyntaxException e) {
            //could be an external URI!
        }

        // parse the href attribute
        LOG.debug("found href=\"{}\"", href);
        //String xpointer = null;
        //String docName = href;

        Map<String, String> params = null;
        DocumentImpl doc = null;
        org.exist.dom.memtree.DocumentImpl memtreeDoc = null;
        boolean xqueryDoc = false;

        if (docUri != null) {
            final String fragment = docUri.getFragment();
            if (!(fragment == null || fragment.length() == 0)) {
                throw new SAXException("Fragment identifiers must not be used in an xinclude href attribute. To specify an xpointer, use the xpointer attribute.");
            }

            // extract possible parameters in the URI
            params = null;
            final String paramStr = docUri.getQuery();
            if (paramStr != null) {
                params = processParameters(paramStr);
                // strip query part
                docUri = XmldbURI.create(docUri.getRawCollectionPath());
            }

            // if docName has no collection specified, assume
            // current collection

            // Patch 1520454 start
            if (!docUri.isAbsolute() && document != null) {
                final String base = document.getCollection().getURI() + "/";
                final String child = "./" + docUri.toString();

                final URI baseUri = URI.create(base);
                final URI childUri = URI.create(child);

                final URI uri = baseUri.resolve(childUri);
                docUri = XmldbURI.create(uri);
            }
            // Patch 1520454 end

            // retrieve the document
            try {
                doc = serializer.broker.getResource(docUri, Permission.READ);
            } catch (final PermissionDeniedException e) {
                return Optional.of(new ResourceError("Permission denied to read XInclude'd resource", e));
            }

            /* Check if the document is a stored XQuery */
            if (doc != null && doc.getResourceType() == DocumentImpl.BINARY_FILE) {
                xqueryDoc = "application/xquery".equals(doc.getMimeType());
            }
        }
        // The document could not be found: check if it points to an external resource
        if (docUri == null || (doc == null && !docUri.isAbsolute())) {
            try {
                URI externalUri = new URI(href);
                final String scheme = externalUri.getScheme();
                // If the URI has no scheme specified,
                // we have to check if it is a relative path, and if yes, try to
                // interpret it relative to the moduleLoadPath property of the current
                // XQuery context.
                if (scheme == null && moduleLoadPath != null) {
                    final String path = externalUri.getSchemeSpecificPart();
                    Path f = Paths.get(path);
                    if (!f.isAbsolute()) {
                        if (moduleLoadPath.startsWith(XmldbURI.XMLDB_URI_PREFIX)) {
                            final XmldbURI parentUri = XmldbURI.create(moduleLoadPath);
                            docUri = parentUri.append(path);
                            doc = (DocumentImpl) serializer.broker.getXMLResource(docUri);
                            if (doc != null && !doc.getPermissions().validate(serializer.broker.getCurrentSubject(), Permission.READ)) {
                                throw new PermissionDeniedException("Permission denied to read XInclude'd resource");
                            }
                        } else {
                            f = Paths.get(moduleLoadPath, path);
                            externalUri = f.toUri();
                        }
                    }
                }
                if (doc == null) {
                    final Either<ResourceError, org.exist.dom.memtree.DocumentImpl> external = parseExternal(externalUri);
                    if (external.isLeft()) {
                        return Optional.of(external.left().get());
                    } else {
                        memtreeDoc = external.right().get();
                    }
                }
            } catch (final PermissionDeniedException e) {
                return Optional.of(new ResourceError("Permission denied on XInclude'd resource", e));
            } catch (final ParserConfigurationException | URISyntaxException e) {
                throw new SAXException("XInclude: failed to parse document at URI: " + href + ": " + e.getMessage(), e);
            }
        }

        /* if document has not been found and xpointer is
               * null, throw an exception. If xpointer != null
               * we retry below and interpret docName as
               * a collection.
               */
        if (doc == null && memtreeDoc == null && xpointer == null) {
            return Optional.of(new ResourceError("document " + docUri + " not found"));
        }

        if (xpointer == null && !xqueryDoc) {
            // no xpointer found - just serialize the doc
            if (memtreeDoc == null) {
                serializer.serializeToReceiver(doc, false);
            } else {
                serializer.serializeToReceiver(memtreeDoc, false);
            }
        } else {
            // process the xpointer or the stored XQuery
            Source source = null;
            final XQueryPool pool = serializer.broker.getBrokerPool().getXQueryPool();
            CompiledXQuery compiled = null;
            try {
                if (xpointer == null) {
                    source = new DBSource(serializer.broker, (BinaryDocument) doc, true);
                } else {
                    xpointer = checkNamespaces(xpointer);
                    source = new StringSource(xpointer);
                }
                final XQuery xquery = serializer.broker.getBrokerPool().getXQueryService();
                XQueryContext context;
                compiled = pool.borrowCompiledXQuery(serializer.broker, source);
                if (compiled == null) {
                    context = new XQueryContext(serializer.broker.getBrokerPool());
                } else {
                    context = compiled.getContext();
                    context.prepareForReuse();
                }
                context.declareNamespaces(namespaces);
                context.declareNamespace("xinclude", Namespaces.XINCLUDE_NS);

                //setup the http context if known
                if (serializer.httpContext != null) {
                    context.setHttpContext(serializer.httpContext);
                }

                //TODO: change these to putting the XmldbURI in, but we need to warn users!
                if (document != null) {
                    context.declareVariable("xinclude:current-doc", document.getFileURI().toString());
                    context.declareVariable("xinclude:current-collection", document.getCollection().getURI().toString());
                }

                if (xpointer != null) {
                    if (doc != null) {
                        context.setStaticallyKnownDocuments(new XmldbURI[]{doc.getURI()});
                    } else if (docUri != null) {
                        context.setStaticallyKnownDocuments(new XmldbURI[]{docUri});
                    }
                }

                // pass parameters as variables
                if (params != null) {
                    for (final Map.Entry<String, String> entry : params.entrySet()) {
                        context.declareVariable(entry.getKey(), entry.getValue());
                    }
                }

                if (compiled == null) {
                    try {
                        compiled = xquery.compile(context, source, xpointer != null);
                    } catch (final IOException e) {
                        throw new SAXException("I/O error while reading query for xinclude: " + e.getMessage(), e);
                    }
                } else {
                    compiled.getContext().updateContext(context);
                    context.getWatchDog().reset();
                }
                LOG.info("xpointer query: {}", ExpressionDumper.dump((Expression) compiled));
                Sequence contextSeq = null;
                if (memtreeDoc != null) {
                    contextSeq = memtreeDoc;
                }

                try {
                    final Sequence seq = xquery.execute(serializer.broker, compiled, contextSeq);

                    if (Type.subTypeOf(seq.getItemType(), Type.NODE)) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("xpointer found: {}", seq.getItemCount());
                        }

                        NodeValue node;
                        for (final SequenceIterator i = seq.iterate(); i.hasNext(); ) {
                            node = (NodeValue) i.nextItem();
                            serializer.serializeToReceiver(node, false);
                        }
                    } else {
                        String val;
                        for (int i = 0; i < seq.getItemCount(); i++) {
                            val = seq.itemAt(i).getStringValue();
                            characters(val);
                        }
                    }
                } finally {
                    context.runCleanupTasks();
                }

            } catch (final XPathException | PermissionDeniedException e) {
                LOG.warn("xpointer error", e);
                throw new SAXException("Error while processing XInclude expression: " + e.getMessage(), e);
            } finally {
                if (compiled != null) {
                    pool.returnCompiledXQuery(source, compiled);
                }
            }
        }
        // restore settings
        document = prevDoc;
        serializer.createContainerElements = createContainerElements;

        return Optional.empty();
    }

    private Either<ResourceError, org.exist.dom.memtree.DocumentImpl> parseExternal(final URI externalUri) throws ParserConfigurationException, SAXException {
        try {
            final URLConnection con = externalUri.toURL().openConnection();
            if (con instanceof HttpURLConnection) {
                final HttpURLConnection httpConnection = (HttpURLConnection) con;
                if (httpConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return Either.Left(new ResourceError("XInclude: unable to retrieve from URI: " + externalUri.toString() + ", server returned response code: " + httpConnection.getResponseCode()));
                }
            }

            // we use eXist's in-memory DOM implementation
            final XMLReaderPool parserPool = serializer.broker.getBrokerPool().getParserPool();
            XMLReader reader = null;
            try (final InputStream is = con.getInputStream()) {
                final InputSource src = new InputSource(is);

                reader = parserPool.borrowXMLReader();
                final SAXAdapter adapter = new SAXAdapter((Expression) null);
                reader.setContentHandler(adapter);
                reader.setProperty(Namespaces.SAX_LEXICAL_HANDLER, adapter);
                reader.parse(src);
                final org.exist.dom.memtree.DocumentImpl doc = adapter.getDocument();
                doc.setDocumentURI(externalUri.toString());
                return Either.Right(doc);
            } finally {
                if (reader != null) {
                    parserPool.returnXMLReader(reader);
                }
            }
        } catch (final IOException e) {
            return Either.Left(new ResourceError("XInclude: unable to retrieve and parse document from URI: " + externalUri.toString(), e));
        }
    }

    @Override
    public void startPrefixMapping(final String prefix, final String uri) throws SAXException {
        namespaces.put(prefix, uri);
        receiver.startPrefixMapping(prefix, uri);
    }

    /**
     * Process xmlns() schema. We process these here, because namespace mappings should
     * already been known when parsing the xpointer() expression.
     */
    private String checkNamespaces(String xpointer) throws XPathException {
        int p0;
        while ((p0 = xpointer.indexOf("xmlns(")) != Constants.STRING_NOT_FOUND) {
            if (p0 < 0) {
                return xpointer;
            }
            final int p1 = xpointer.indexOf(')', p0 + 6);
            if (p1 < 0) {
                throw new XPathException((Expression) null, "expected ) for xmlns()");
            }
            final String mapping = xpointer.substring(p0 + 6, p1);
            xpointer = xpointer.substring(0, p0) + xpointer.substring(p1 + 1);
            final StringTokenizer tok = new StringTokenizer(mapping, "= \t\n");
            if (tok.countTokens() < 2) {
                throw new XPathException((Expression) null, "expected prefix=namespace mapping in " + mapping);
            }
            final String prefix = tok.nextToken();
            final String namespaceURI = tok.nextToken();
            namespaces.put(prefix, namespaceURI);
        }
        return xpointer;
    }

    protected Map<String, String> processParameters(final String args) {
        final Map<String, String> parameters = new HashMap<>();
        int start = 0;
        int end = 0;
        final int l = args.length();
        while ((start < l) && (end < l)) {
            while ((end < l) && (args.charAt(end++) != '='))
                ;
            if (end == l) {
                break;
            }
            String param = args.substring(start, end - 1);
            start = end;
            while ((end < l) && (args.charAt(end++) != '&'))
                ;
            String value;
            if (end == l) {
                value = args.substring(start);
            } else {
                value = args.substring(start, end - 1);
            }
            start = end;
            try {
                param = URLDecoder.decode(param, UTF_8.name());
                value = URLDecoder.decode(value, UTF_8.name());
                LOG.debug("parameter: {} = {}", param, value);
                parameters.put(param, value);
            } catch (final UnsupportedEncodingException e) {
                LOG.warn(e.getMessage(), e);
            }
        }
        return parameters;
    }

    @Override
    public void setCurrentNode(final INodeHandle node) {
        //ignored
    }

    @Override
    public Document getDocument() {
        //ignored
        return null;
    }
}
