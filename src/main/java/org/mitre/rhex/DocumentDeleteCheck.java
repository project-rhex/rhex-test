package org.mitre.rhex;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.mitre.test.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Test for section document deletion
 *
 * <pre>
 * 6.5 baseURL/sectionpath/documentname
 *
 * 6.5.4 DELETE
 *
 * This operation MAY be implemented. If a DELETE is sent to the document URL,
 * the document is completely deleted. If DELETE is implemented, special
 * precautions should be taken to assure against accidental or malicious
 * deletion.
 *
 * Future requests(*) to the [DELETED] document URL MAY return a status code
 * of 410 or 404, unless the record is restored.
 *
 * NOTE: (*) Requests include all operations: GET, POST, PUT, DELETE
 *
 * Status Code: 204, <B>404, 410</B>, [405]
 * </pre>
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 2/20/12 10:45 AM
 */
public class DocumentDeleteCheck extends BaseTest {

	@NonNull
	@Override
	public String getId() {
		return "6.5.4.2";
	}

	@Override
	public boolean isRequired() {
		// This operation MAY be implemented. If a DELETE is sent to the document URL,
		// the document is completely deleted.
		return false;
	}

	@NonNull
	public String getName() {
		return "GET request to deleted document URL SHOULD return a status code of 404 or 410";
		// Future [GET and/or DELETE?] requests to the section URL MAY return a status code of 410,
		// unless the record is restored.
	}

	@NonNull
	public List<Class<? extends TestUnit>> getDependencyClasses() {
		List<Class<? extends TestUnit>> depends = new ArrayList<Class<? extends TestUnit>>(2);
		depends.add(DocumentCreate.class); // 6.4.2.2
		depends.add(DocumentDelete.class); // 6.5.4.1
		return depends;
	}

	public void execute() throws TestException {
		// pre-conditions: for this test to be executed the prerequisite test BaseUrlRootXml must have passed
		// with 200 HTTP response and valid root.xml content.
		TestUnit baseTest = getDependency(DocumentCreate.class);
		if (baseTest == null) {
			// assertion failed: this should never be null
			log.error("Failed to retrieve prerequisite test: BaseUrlRootXml");
			setStatus(StatusEnumType.SKIPPED, "Failed to retrieve prerequisite test: 6.4.2.2");
			return;
		}
		final DocumentCreate documentCreate = (DocumentCreate) baseTest;
		URI documentURL = documentCreate.getDocumentURL();
		if (documentURL == null) {
			log.error("Failed to retrieve prerequisite test results: DocumentCreate");
			setStatus(StatusEnumType.SKIPPED, "Failed to retrieve prerequisite test results: 6.4.2.2");
			return;
		}
		final Context context = Loader.getInstance().getContext();
		HttpClient client = context.getHttpClient();
		try {
			HttpGet req = new HttpGet(documentURL);
			req.setHeader("Accept", MIME_APPLICATION_XML);
			HttpResponse response = context.executeRequest(client, req);
            int code = response.getStatusLine().getStatusCode();
            boolean dump = false;
			if (code == 410 || code == 404) {
				setStatus(StatusEnumType.SUCCESS);
                if (log.isDebugEnabled()) dump = true;
			} else {
                dump = true;
                setStatus(StatusEnumType.FAILED, "Expected 410 or 404 HTTP status code but was: " + code);
			}
            if (dump) {
                System.out.println("\nGET URL=" + documentURL);
                dumpResponse(req, response, true);
            }
        } catch (IOException e) {
            System.out.println("\nGET URL=" + documentURL);
			throw new TestException(e);
		} finally {
			client.getConnectionManager().shutdown();
		}
	}

}