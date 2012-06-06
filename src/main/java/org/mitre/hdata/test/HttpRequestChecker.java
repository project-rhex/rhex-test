package org.mitre.hdata.test;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;

import java.io.IOException;

/**
 * Interface to handle server specific implementation of HttpRequestChecker to
 * perform pre or post actions around HTTP requests such as handling authentication.
 * Implementations of this interface wrap the HttpClient and execute the request object.
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 3/30/12 12:48 PM
 */
public interface HttpRequestChecker {

	/**
	 * Wrap <tt>HttpClient.execute()</tt> to pre/post-test HTTP requests for
	 * any server specific implementation handling such as authentication.
	 *
	 * @param context   Application context
	 * @param client   the HttpClient, must never be null
	 * @param request   the request to execute, must never be null
	 *
	 * @return  the response to the request.
	 * @throws IOException in case of a problem or the connection was aborted
	 * @throws ClientProtocolException in case of an http protocol error
	 */
	@NonNull
	HttpResponse executeRequest(Context context, HttpClient client, HttpUriRequest request)
			throws IOException;

	/**
	 * Setups and initializes the HttpRequestChecker as appropriate.
	 * This is called once before any tests are executed.
	 *
	 * @param context   Application context
     * @throws IllegalArgumentException if setup/configuration fails
	 */
	void setup(Context context);

    /**
     * Set explicit user context by user email and password
     *
     * @param context   Application context
     * @param userId
     * @param userEmail
     * @param userPassword   @throws IllegalArgumentException if setup/configuration fails
     */
    void setUser(Context context, String userId, String userEmail, String userPassword);

    /**
     * Get current active user identity if applicable
     * @param context   Application context
     * @return email address assigned to active user context if applicable otherwise null
     */
    String getCurrentUser(Context context);
}
