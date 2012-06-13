package org.mitre.rhex;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.jdom.JDOMException;
import org.mitre.test.ClientHelper;
import org.mitre.test.Context;
import org.mitre.test.TestException;

import java.io.IOException;

/**
 * <pre>
 * 6.2.1 GET Operation on the Base URL
 *
 * It is RECOMMENDED that the server also offers a web user interface that allows users to access and manipulate the
 * content of the HDR, as permitted by the policies of the system. Selecting between the Atom feed and the user interface
 * can be achieved using standard content negotiation (HTTP Accept header). This is not necessary for systems that are used
 * by non-person entities only.
 *
 * If the Accept header is non-existent, or set to * '/' * or application/atom+xml, the system MUST
 * return the Atom feed. For all other cases the format of the returned resource is left to the implementer.
 *
 * Status Code: 200, 404, 405(*)
 * </pre>
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 2/20/12 10:45 AM
 */
public class BaseUrlGetHtmlAcceptTest extends BaseUrlGetTest {

	@NonNull
	public String getId() {
		return "6.2.1.5";
	}

	@NonNull
	public String getName() {
		return "GET operation on baseURL with text/html Accept header may return HTML document if implemented otherwise must return 405";
	}

	public boolean isRequired() {
		return false; // RECOMMENDED
	}

	protected String getAcceptHeader() {
		return "text/html, application/xhtml+xml";
	}

	// expected response status code 405 or 200
	protected void validateContent(int code, Context context, HttpResponse response) throws TestException, IOException, JDOMException {
		// if response status code not 200 then can only be 405
		if (code == 405) {
			// not implemented is a valid response
			setStatus(StatusEnumType.SUCCESS);
			return;
		} else if (code != 200) {
			// HTML web user interface is a *RECOMMENDED* feature
			// Failed recommendation or success with warning (if optional)
			setStatus(StatusEnumType.FAILED, "Unexpected HTTP response: " + code);
			return;
		}
		final HttpEntity entity = response.getEntity();
		final String contentType = ClientHelper.getContentType(entity);
		// should be text/html or application/xhtml+xml
		if (!MIME_TEXT_HTML.equals(contentType) && !MIME_APPLICATION_XHTML.equals(contentType)) {
			addWarning("Expected text/html content-type but was: " + contentType);
		}
		// GUI HTML representation returned
		// not much requirements on what the recommended HTML content should be like
		// long len = entity.getContentLength();
		// minimum length expected is 43 bytes or negative # if unknown
		// assertTrue(len < 0 || len >= 43, "Expecting valid HTML document for baseURL; returned length was " + len); // or XHTML ??
		setStatus(StatusEnumType.SUCCESS);
	}

}
