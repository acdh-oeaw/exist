package org.exist.xquery.functions.request;

import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.io.input.UnsynchronizedByteArrayInputStream;
import org.apache.commons.io.output.UnsynchronizedByteArrayOutputStream;
import org.exist.xmldb.UserManagementService;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.exist.http.RESTTest;
import org.exist.xmldb.EXistResource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.BinaryResource;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;

/**
 * @author Adam Retter <adam.retter@googlemail.com>
 */
public class GetDataTest extends RESTTest {

    private final static String CONTAINER_ELEMENT_NAME = "data";
    private final static String XQUERY = wrapInElement("{request:get-data()}");
    private final static String XQUERY_FILENAME = "test-get-data.xql";

    private static Collection root;

    private static String wrapInElement(String value) {
        return value == null || value.length() == 0 ? "<" + CONTAINER_ELEMENT_NAME + "/>" : "<" + CONTAINER_ELEMENT_NAME + ">" + value + "</" + CONTAINER_ELEMENT_NAME + ">";
    }

    @BeforeClass
    public static void beforeClass() throws XMLDBException {
        root = DatabaseManager.getCollection("xmldb:exist://localhost:" + existWebServer.getPort() + "/xmlrpc/db", "admin", "");
        BinaryResource res = (BinaryResource)root.createResource(XQUERY_FILENAME, "BinaryResource");
        ((EXistResource) res).setMimeType("application/xquery");
        res.setContent(XQUERY);
        root.storeResource(res);
        UserManagementService ums = (UserManagementService)root.getService("UserManagementService", "1.0");
        ums.chmod(res, 0777);
    }

    @AfterClass
    public static void afterClass() throws XMLDBException {
        BinaryResource res = (BinaryResource)root.getResource(XQUERY_FILENAME);
        root.removeResource(res);
    }

    @Test
    public void retrieveEmpty() {
        PostMethod post = new PostMethod(getCollectionRootUri() + "/" + XQUERY_FILENAME);

        post.setRequestHeader("Content-Type", "application/octet-stream");

        testRequest(post, wrapInElement("").getBytes());
    }

    @Test
    public void retrieveBinaryHttp09() throws IOException {
        final String testData = "12345";

        final PostMethod post = new PostMethod(getCollectionRootUri() + "/" + XQUERY_FILENAME);
        post.getParams().setVersion(HttpVersion.HTTP_0_9);
        post.setRequestEntity(new ByteArrayRequestEntity(testData.getBytes(UTF_8), "application/octet-stream"));

        final int httpResult = client.executeMethod(post);

        assertEquals(HttpStatus.SC_BAD_REQUEST, httpResult);
    }

    @Test
    public void retrieveBinaryHttp10() {
        final String testData = "12345";

        final PostMethod post = new PostMethod(getCollectionRootUri() + "/" + XQUERY_FILENAME);
        post.getParams().setVersion(HttpVersion.HTTP_1_0);
        post.setRequestEntity(new ByteArrayRequestEntity(testData.getBytes(UTF_8), "application/octet-stream"));

        testRequest(post, wrapInElement(encodeBase64String(testData.getBytes(UTF_8)).trim()).getBytes());
    }

    @Test
    public void retrieveBinaryHttp11() {
        final String testData = "12345";

        final PostMethod post = new PostMethod(getCollectionRootUri() + "/" + XQUERY_FILENAME);
        post.getParams().setVersion(HttpVersion.HTTP_1_1);
        post.setRequestEntity(new ByteArrayRequestEntity(testData.getBytes(UTF_8), "application/octet-stream"));

        testRequest(post, wrapInElement(encodeBase64String(testData.getBytes(UTF_8)).trim()).getBytes());
    }

    @Test
    public void retrieveBinaryHttp11ChunkedTransferEncoding() throws IOException {
        final String testData = "12345";

        final PostMethod post = new PostMethod(getCollectionRootUri() + "/" + XQUERY_FILENAME);
        post.getParams().setVersion(HttpVersion.HTTP_1_1);

        try (final InputStream is = new UnsynchronizedByteArrayInputStream(testData.getBytes(UTF_8))) {
            post.setRequestEntity(new InputStreamRequestEntity(is, "application/octet-stream"));

            testRequest(post, wrapInElement(encodeBase64String(testData.getBytes(UTF_8)).trim()).getBytes());
        }
    }

    @Test
    public void retrieveXmlHttp09() throws IOException {
        final String testData = "<a><b><c>hello</c></b></a>";

        final PostMethod post = new PostMethod(getCollectionRootUri() + "/" + XQUERY_FILENAME);
        post.getParams().setVersion(HttpVersion.HTTP_0_9);
        post.setRequestEntity(new ByteArrayRequestEntity(testData.getBytes(UTF_8), "application/xml"));

        final int httpResult = client.executeMethod(post);

        assertEquals(HttpStatus.SC_BAD_REQUEST, httpResult);
    }

    @Test
    public void retrieveXmlHttp10() {
        final String testData = "<a><b><c>hello</c></b></a>";

        final PostMethod post = new PostMethod(getCollectionRootUri() + "/" + XQUERY_FILENAME);
        post.getParams().setVersion(HttpVersion.HTTP_1_0);
        post.setRequestEntity(new ByteArrayRequestEntity(testData.getBytes(UTF_8), "application/xml"));

        testRequest(post, wrapInElement("\n\t" + testData + "\n").getBytes(), true);
    }

    @Test
    public void retrieveXmlHttp11() throws IOException {
        final String testData = "<a><b><c>hello</c></b></a>";

        final PostMethod post = new PostMethod(getCollectionRootUri() + "/" + XQUERY_FILENAME);
        post.getParams().setVersion(HttpVersion.HTTP_1_1);
        post.setRequestEntity(new ByteArrayRequestEntity(testData.getBytes(UTF_8), "application/xml"));

        testRequest(post, wrapInElement("\n\t" + testData + "\n").getBytes(), true);
    }

    @Test
    public void retrieveXmlHttp11ChunkedTransferEncoding() throws IOException {
        final String testData = "<a><b><c>hello</c></b></a>";

        final PostMethod post = new PostMethod(getCollectionRootUri() + "/" + XQUERY_FILENAME);
        post.getParams().setVersion(HttpVersion.HTTP_1_1);

        try (final InputStream is = new UnsynchronizedByteArrayInputStream(testData.getBytes(UTF_8))) {
            post.setRequestEntity(new InputStreamRequestEntity(is, "application/xml"));

            testRequest(post, wrapInElement("\n\t" + testData + "\n").getBytes(), true);
        }
    }

    @Test
    public void retrieveMalformedXmlFallbackToString() {
        PostMethod post = new PostMethod(getCollectionRootUri() + "/" + XQUERY_FILENAME);

        final String testData = "<a><b></a>";

        post.setRequestHeader("Content-Type", "text/xml");
        post.setRequestEntity(new ByteArrayRequestEntity(testData.getBytes()));

        testRequest(post, wrapInElement(testData.replace("<", "&lt;").replace(">", "&gt;")).getBytes());
    }

    @Test
    public void retrieveString() {
        PostMethod post = new PostMethod(getCollectionRootUri() + "/" + XQUERY_FILENAME);

        final String testData = "12345";

        post.setRequestEntity(new ByteArrayRequestEntity(testData.getBytes()));

        testRequest(post, wrapInElement(testData).getBytes());
    }
    
    private void testRequest(HttpMethod method, byte expectedResponse[]) {
        testRequest(method, expectedResponse, false);
    }
    
    private void testRequest(HttpMethod method, byte expectedResponse[], boolean stripWhitespaceAndFormatting) {
        try {
            int httpResult = client.executeMethod(method);

            assertEquals(HttpStatus.SC_OK, httpResult);

            try (final InputStream is = method.getResponseBodyAsStream();
                    final UnsynchronizedByteArrayOutputStream baos = new UnsynchronizedByteArrayOutputStream()) {
                baos.write(is);

                byte actualResponse[] = baos.toByteArray();
                if(stripWhitespaceAndFormatting) {
                    expectedResponse = new String(expectedResponse).replace("\n", "").replace("\t", "").replace(" ", "").getBytes();
                    actualResponse = new String(actualResponse).replace("\n", "").replace("\t","").replace(" ", "").getBytes();
                }
                assertArrayEquals(expectedResponse, actualResponse);
            }
        } catch(IOException ioe) {
            fail(ioe.getMessage());
        } finally {
            method.releaseConnection();
        }
    }
}
