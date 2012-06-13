package org.mitre.hdata.test.tests;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpOptions;
import org.mitre.test.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * 6.2.5 OPTIONS
 *
 * The OPTIONS operation on the baseURL is per [8], section 9.2, intended to return communications options to the clients.
 * Within the context of this specification, OPTIONS is used to indicate which security mechanisms are available for a given
 * baseURL and a list of hData content profiles supported by this implementation. All implementations MUST support
 * OPTIONS on the baseURL of each HDR and return a status code of 200, along with:
 * X-hdata-security, X-hdata-hcp, and X-hdata-extensions HTTP headers. <P>
 *
 * The server MAY include additional HTTP headers. The response SHOULD NOT include an HTTP body. The client
 * MUST NOT use the Max-Forward header when requesting the security mechanisms for a given HDR. <P>
 *
 * Status Code: 200
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 2/20/12 10:45 AM
 */
public class BaseUrlOptions extends BaseTest {

	private static final Logger log = LoggerFactory.getLogger(BaseUrlOptions.class);

	@NonNull
	public String getId() {
		return "6.2.5.1";
	}

	@Override
	public boolean isRequired() {
		return true; // MUST
	}

	@NonNull
	public String getName() {
		return "OPTIONS on HDR baseURL MUST return 200 status code";
	}

	@NonNull
	public List<Class<? extends TestUnit>> getDependencyClasses() {
		return Collections.emptyList(); // none
	}

	public void execute() throws TestException {
		final Context context = Loader.getInstance().getContext();
		HttpClient client = context.getHttpClient();
		try {
			URI baseURL = context.getBaseURL();
			if (log.isDebugEnabled()) System.out.println("\nOPTION URL: " + baseURL);
			HttpOptions req = new HttpOptions(baseURL);
			HttpResponse response = context.executeRequest(client, req);
			int code = response.getStatusLine().getStatusCode();
			if (log.isDebugEnabled()) {
				System.out.println("OPTION Response status=" + code);
				for (Header header : response.getAllHeaders()) {
					System.out.println("\t" + header.getName() + ": " + header.getValue());
				}
			}

			assertEquals(200, code);
			final HttpEntity entity = response.getEntity();
			if (entity != null) {
				// response SHOULD NOT include an HTTP body so entity should be null
				// addWarning("response SHOULD NOT include an HTTP body");
				long len = entity.getContentLength();
				// minimum length expected is 66 bytes or a negative number if unknown
				// assertTrue(len < 0 || len >= 66, "Expecting valid XML document for baseURL/root.xml; returned length was " + len);
				if (log.isDebugEnabled() && len > 0) {
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					try {
						entity.writeTo(bos);
					} catch (IOException e) {
						log.warn("", e);
					}
					System.out.println("content len=" + len + "\n" + bos);
					/*
					<?xml version="1.0" encoding="UTF-8"?>
					<feed xml:lang="en-US" xmlns="http://www.w3.org/2005/Atom">
					*/
				}
			}
			// NOTE: check X-hdata-* header fields in follow-up test
			/*
			if (response.getFirstHeader("X-hdata-security") == null &&
					response.getFirstHeader("X-hdata-hcp") == null &&
						response.getFirstHeader("X-hdata-extensions") == null) {
				setStatus(StatusEnumType.FAILED, "Must set required X-hdata HTTP headers in response");
				return;
			}
			*/
			/*
			6.2.5 OPTIONS

            All implementations MUST support OPTIONS on the baseURL of each HDR and return a status code of 200, along with
            following HTTP headers:

			• The X-hdata-security HTTP header defined in section of this specification. The security mechanisms defined at the
			baseURL are applicable to all child resources, i.e. to the entire HDR.

				X-hdata-security: http://openid.net/connect/

			•  An X-hdata-hcp HTTP header that contains a space separated list of the identifiers of the hData Content Profiles
			supported by this implementation

				X-hdata-hcp: http://projecthdata.org/hcp/greenCDA-CoC

			• The X-hdata-extensions HTTP header contains a space separated list of the identifiers of the hData extensions
			supported by this implementation independent of their presence in the root document at baseURL/root.xml (cf. section
			2.2 in [1] describing the root document format for an explanation of the extensions in a root.xml)

				X-hdata-extensions: http://projecthdata.org/extension/allergy http://projecthdata.org/extension/care-goal http://projecthdata.org/extension/condition
				http://projecthdata.org/extension/encounter http://projecthdata.org/extension/immunization http://projecthdata.org/extension/medical-equipment
				http://projecthdata.org/extension/medication http://projecthdata.org/extension/procedure http://projecthdata.org/extension/result
				http://projecthdata.org/extension/social-history http://projecthdata.org/extension/vital-sign
			*/
			setResponse(response);
			setStatus(StatusEnumType.SUCCESS);		
		} catch (ClientProtocolException e) {
			throw new TestException(e);
		} catch (IOException e) {
			throw new TestException(e);
		} finally {
			client.getConnectionManager().shutdown();
		}
	}

}
