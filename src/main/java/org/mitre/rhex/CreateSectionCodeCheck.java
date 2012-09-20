package org.mitre.rhex;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.http.HttpResponse;
import org.mitre.test.BaseTest;
import org.mitre.test.TestException;
import org.mitre.test.TestUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Test for section creation
 *
 * <pre>
 *  6.2 Operations on the Base URL
 *
 *  6.2.2 POST – Parameters:extensionID, path, name
 *
 *  This operation is used to create a new Section at the root of the document.
 *  The request body is of type “application/xwww-form-urlencoded” and MUST contain
 *  the extensionId, path, and name parameters. The extensionId parameter MAY be
 *  a string that is equal to value of one of the registered <extension> nodes of
 *  the root document of the HDR identified by baseURL. The path MUST be a string
 *  that can be used as a URL path segment. If any parameters are incorrect or
 *  not existent, the server MUST return a status code of 400.
 *
 *  The system MUST confirm that there is no other section registered as a
 *  child node that uses the same path name. If there is a collision, the
 *  server MUST return a status code of 409.
 *
 *  If the extensionId is not registered as a valid extension, the server
 *  <B>MUST</B> verify that it can support this extension. If it cannot support
 *  the extension it MUST return a status code of 406. It MAY provide
 *  additional entity information.
 *
 *  If it can support that extension, it MUST register it with the root.xml
 *  of this record. When creating the section resource, the server MUST
 *  update the root document: in the node of the parent section a new child node
 *  must be inserted.
 *
 *  If successful, the server <B>MUST</B> return a 201 status code and
 *  SHOULD include the location of the new section.
 *
 *  The [optional] name parameter MUST be used as the user-friendly name for the
 *  new section.
 *
 * Status Code: <B>201</B>, 400, 406, 409
 * </pre>
 *
 * @author Jason Mathews, MITRE Corp.
 * @see CreateSection
 * Date: 2/20/12 10:45 AM
 */
public class CreateSectionCodeCheck extends BaseTest {

	public CreateSectionCodeCheck() {
		setProperty(CreateSection.class, PROP_KEEP_RESPONSE_BOOL, Boolean.TRUE);
	}

	@NonNull
	public String getId() {
		return "6.2.2.2";
	}

	@Override
	public boolean isRequired() {
		// If successful, the server *MUST* return a 201 status code
		return true;
	}

	@NonNull
	public String getName() {
		return "POST operation on baseURL MUST return a 201 status code";
	}

	@NonNull
	public List<Class<? extends TestUnit>> getDependencyClasses() {
		return Collections.<Class<? extends TestUnit>> singletonList(CreateSection.class); // 6.2.2.1
	}

	public void execute() throws TestException {
		// pre-conditions: for this test to be executed the prerequisite test CreateSection must have passed
		// and the section path was verified to be created and inserted into root.xml of this record
		TestUnit baseTest = getDependency(CreateSection.class);
		if (baseTest == null) {
			// assertion failed: this should never be null
			log.error("Failed to retrieve prerequisite test");
			setStatus(StatusEnumType.SKIPPED, "Failed to retrieve prerequisite test");
			return;
		}
		HttpResponse response = ((CreateSection)baseTest).getResponse();
		if (response == null) {
			log.error("Failed to retrieve prerequisite test results: CreateSection");
			setStatus(StatusEnumType.SKIPPED, "Failed to retrieve prerequisite test results: 6.2.2.1");
			return;
		}
		int code = response.getStatusLine().getStatusCode();
		// TODO: any way to test "SHOULD include the location of the new section" -- check location header field ??
		if (code == 201)
			setStatus(StatusEnumType.SUCCESS);
		else
			setStatus(StatusEnumType.FAILED, "Expected 201 HTTP status code but was: " + code);
	}
}