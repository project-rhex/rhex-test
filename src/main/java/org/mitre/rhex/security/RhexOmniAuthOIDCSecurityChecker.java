package org.mitre.rhex.security;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.http.*;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.mitre.test.ClientHelper;
import org.mitre.test.Context;
import org.mitre.test.HttpRequestChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTTP request security handler for authentication via modified OpenID Connect
 * RubyGem implementation.
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 3/30/12 12:51 PM
 */
public class RhexOmniAuthOIDCSecurityChecker implements HttpRequestChecker {

	private static final Logger log = LoggerFactory.getLogger(RhexOmniAuthOIDCSecurityChecker.class);

	private HttpContext localContext;
    private String currentUser;
    private final Map<String, HttpContext> contexts = new HashMap<String,HttpContext>();

    /**
	 * Setups and initializes the HttpRequestChecker
	 *
	 * @param context   Application context
     * @throws IllegalArgumentException if setup/configuration fails
     */
	@Override
	public void setup(Context context) {
		log.debug("XXX: enable OpenID Connect authentication for defaultUser");

		String loginEmail = context.getUserProperty("defaultUser", "email");
		String loginPassword = context.getUserProperty("defaultUser", "password");
        if (StringUtils.isBlank(loginEmail) || StringUtils.isBlank(loginPassword)) {
            throw new IllegalArgumentException("email and password properties are empty or missing for defaultUser");
        }

        setUser(context, Context.DEFAULT_USER, loginEmail, loginPassword);
	}

    @Override
    public String getCurrentUser(Context context) {
        return currentUser;
    }

    /**
     *
     * @param context
     * @param userId
     * @param userEmail
     * @param userPassword
     *
     * @throws IllegalArgumentException if setup/configuration fails
     */
    @Override
    public void setUser(Context context, String userId, String userEmail, String userPassword) {
        if (currentUser != null && currentUser.equals(userEmail)) {
            log.debug("same user context: no change"); // XXX
            return; // user is already active and context is set
        }
        HttpContext httpContext = contexts.get(userEmail);
        if (httpContext != null) {
            //localContext = contexts.get(userEmail);
            //if (localContext != null) {
            log.debug("switch user context: {}", userEmail);
            currentUser = userEmail;
            localContext = httpContext;
            // log.info("local user context: " + localContext.getAttribute("user"));
            return;
        }

        log.info("set user context: " + userEmail);

        final URI uri = context.getPropertyAsURI("loginURL");
        if (uri == null) {
            throw new IllegalArgumentException("loginURL property not defined");
        }

        /*
        Step 1:

        GET auth URL which redirects to authentication endpoint
        GET -> http://rhex.mitre.org:3000/auth/openid_connect
        Host: rhex.mitre.org:3000
        Connection: Keep-Alive
        User-Agent: Apache-HttpClient/4.1.3 (java 1.5)

        redirects to http://rhex.mitre.org:3001/accounts/sign_in
        */

        //HttpContext httpContext = localContext;
        //if (httpContext == null) {
        // Create a local instance of cookie store
        CookieStore cookieStore = new BasicCookieStore();
        // Create local HTTP context
        httpContext = new BasicHttpContext();
        // Bind custom cookie store to the local context
        httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
        //}
        HttpClient client = null;
        URI redirect = null;
        boolean debug = log.isDebugEnabled();
        try {
            client = context.getHttpClient();
            if (client instanceof AbstractHttpClient) {
                log.debug("*** set setRedirectStrategy ***");
                AbstractHttpClient c = (AbstractHttpClient)client;
                c.setRedirectStrategy(new RedirectStrategy() {
                    @Override
                    public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context) throws ProtocolException {
                        return false;
                    }

                    @Override
                    public HttpUriRequest getRedirect(HttpRequest request, HttpResponse response, HttpContext context) throws ProtocolException {
                        return null;
                    }
                });
            }
            log.debug("1.GET auth URL: {}", uri);
            HttpGet req = new HttpGet(uri);
            req.setHeader("Cache-Control", "no-cache");
            // req.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            HttpResponse response = client.execute(req, httpContext);
            // expect 302 response
            // should redirect to something like:
            // http://rhex.mitre.org:3001/authorizations/new?client_id=xx&nonce=xx&redirect_uri=http://rhex.mitre.org:3000/auth/openid_connect/callback&request=xxx&response_type=code&scope=openid
            redirect = ClientHelper.getRedirectURI(response); // getRedirectURI(response);
            if (debug || redirect == null) {
                ClientHelper.dumpResponse(req, response, true);
            }

            if (redirect == null) {
                log.error("failed to get redirect URL");
                return;
            }

        } catch (IOException e) {
            log.error("", e);
            return;
        } finally {
            if (client != null)
                client.getConnectionManager().shutdown();
        }

        /*
         * Step 2:
         * Invoke GET on redirected URL at identity provider endpoint
         * GET -> http://rhex.mitre.org:3001/authorizations/new?client_id=xx&nonce=xx&redirect_uri=http://rhex.mitre.org:3000/auth/openid_connect/callback&request=xxx&response_type=code&scope=openid
         */
        String token = null;
        String formAction = "/accounts/sign_in"; // default FORM action for POST
        log.debug("*** 2.GET redirect URL: {}", redirect);
        try {
            client = context.getHttpClient();
            //client = wrapClient(context.getHttpClient());
            client.getParams().setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, true);
            HttpGet req = new HttpGet(redirect);
            req.setHeader("Cache-Control", "no-cache");
            HttpResponse response = client.execute(req, httpContext);
            if (debug) {
                ClientHelper.dumpResponse(req, response, false);
            }
            HttpEntity entity = response.getEntity();
            if (entity != null)
                try {
                    String text = EntityUtils.toString(entity);
                    // <form>...<input name="authenticity_token" type="hidden" value="LHrOI+YLyGQ9Y2vR9QS5wDaGze8j6/krFT6uHIz6f0o=" />
                    Pattern p = Pattern.compile("<input name=\"authenticity_token\".*?value=\"([^\"]+)");
                    Matcher m = p.matcher(text);
                    if (m.find()) {
                        token = m.group(1);
                        //System.out.println("\nXXX: ** MATCH ***" + token);
                        log.trace("Response body:\n{}", text);
                    } else {
                        // no match found
                        log.debug("Response body:\n{}", text);
                    }
                    // <form accept-charset="UTF-8" action="/accounts/sign_in" ...
                    p = Pattern.compile("<form\\s.*?action=\"([^\"]+)");
                    m = p.matcher(text);
                    if (m.find()) {
                        formAction = m.group(1);
                        //if (formAction.startsWith("/"))
                        //formAction = formAction.substring(1);
                        log.trace("form action={}", formAction);
                    }
                } catch(IOException e) {
                    log.warn("", e);
                }
            else
                log.warn("XXX: No body");
        } catch (ParseException e) {
            log.error("", e);
            return;
        } catch (IOException e) {
            log.error("", e);
            return;
        } finally {
            if (client != null)
                client.getConnectionManager().shutdown();
        }

        if (token == null) {
            log.error("failed to get auth token");
            return;
        }

        /*
         Step 3:

         POST to form; e.g., http://rhex.mitre.org:3001/accounts/sign_in
         action="/accounts/sign_in"
         construct POST URL from auth endpoint and submit credentials

         form parameters:

            account[email]	        test@test.com
            account[password]	    password
            account[remember_me]	0
            authenticity_token  	XsV2cW8kHyia6endlijMV37SeeiApdbmPXUi8UvsVJY=
            commit	                Sign in
            utf8	                ?

            utf8=%E2%9C%93&authenticity_token=XsV2cW8kHyia6endlijMV37SeeiApdbmPXUi8UvsVJY%3D&account%5Bemail%5D=test%40test.com&account%5Bpassword%5D=testtest&account%5Bremember_me%5D=0&commit=Sign+in
         */
        final URI signInUrl = redirect.resolve(formAction);
        /*
        try {
            final int port = redirect.getPort();
            signInUrl = port == -1 ? new URI(String.format("%s://%s/%s",
                        redirect.getScheme(), redirect.getHost(), formAction))
                : new URI(String.format("%s://%s:%d/%s",
                        redirect.getScheme(), redirect.getHost(), port, formAction));
        } catch (URISyntaxException e) {
            log.error("", e);
            return;
        }
        */

        log.debug("*** 3.POST auth URL: {}", signInUrl);
        HttpPost httppost = new HttpPost(signInUrl);
        httppost.setHeader("Cache-Control", "no-cache");
        List<NameValuePair> formParams = new ArrayList<NameValuePair>(5);
        formParams.add(new BasicNameValuePair("account[email]", userEmail));
        formParams.add(new BasicNameValuePair("account[password]", userPassword));
        formParams.add(new BasicNameValuePair("authenticity_token", token));
        formParams.add(new BasicNameValuePair("account[remember_me]", "0"));
        formParams.add(new BasicNameValuePair("commit", "Sign in"));
        // formParams.add(new BasicNameValuePair("utf8", "&#x2713;")); // %E2%9C%93
        URI postRedirect = null;
        try {
            httppost.setEntity(new UrlEncodedFormEntity(formParams));
            client = context.getHttpClient();
            /*
            if (client instanceof AbstractHttpClient) {
                log.debug("*** setCredentials ***");
                ((AbstractHttpClient)client).getCredentialsProvider().setCredentials(
                        new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM),
                        new UsernamePasswordCredentials("user", "pass"));
            }
            */
            HttpResponse response = client.execute(httppost, httpContext);
            postRedirect = ClientHelper.getRedirectURI(response);
            if (debug || postRedirect == null) {
                ClientHelper.dumpResponse(httppost, response, true);
            }

            if (postRedirect == null) {
                log.error("failed to get redirect URL");
                return;
            }

        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        } finally {
            if (client != null)
                client.getConnectionManager().shutdown();
        }

        /* Step 4:
         *
         * Expected POST Response: HTTP/1.1 302 Found
         * Location: http://rhex.mitre.org:3001/authorizations/new?client_id=xxx&nonce=xxx&redirect_uri=http://rhex.mitre.org:3000/auth/openid_connect/callback&request=xxx&response_type=code&scope=openid
         * GET redirect URL to verify authentication
         */
        log.debug("*** 4.GET redirect URL: {}", postRedirect);
        URI getRedirect = null;
        try {
            client = context.getHttpClient();
            HttpGet req = new HttpGet(postRedirect);
            req.setHeader("Cache-Control", "no-cache");
            client.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, false);
            HttpResponse response = client.execute(req, httpContext);
            boolean success = response.getStatusLine().getStatusCode() == 302;
            getRedirect = ClientHelper.getRedirectURI(response);

            if (debug || !success) {
                ClientHelper.dumpResponse(req, response, !success);
            }

            if (getRedirect == null) {
                log.error("failed to get redirect URL");
                return;
            }

            // boolean success = response.getStatusLine().getStatusCode() == 200;
            // if HANDLE_REDIRECTS=false then
            // Location: http://rhex-simple.mitre.org:3000/auth/openid_connect/callback?code=6415c21d14d45bf69054b9efca8b36591214c17490f643521cc7b0331f4386ec
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        } finally {
            if (client != null)
                client.getConnectionManager().shutdown();
        }

        log.debug("*** 5.GET redirect URL: {}", getRedirect);
        try {
            client = context.getHttpClient();
            HttpGet req = new HttpGet(getRedirect);
            HttpResponse response = client.execute(req, httpContext);
            boolean success = response.getStatusLine().getStatusCode() == 200;
            if (debug || !success) {
                System.out.println("XXX: 123-1");
                ClientHelper.dumpResponse(req, response, !success);
                System.out.println("XXX: 123-2");
            }
            if (success) {
                log.info("XXX: authentication successful: {}", userEmail);
                saveUriParameters(context, postRedirect, userId);
                saveUriParameters(context, getRedirect, userId);
                currentUser = userEmail;
                this.localContext = httpContext;
                // httpContext.setAttribute("user", userEmail);
                contexts.put(userEmail, httpContext); // save context
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        } finally {
            if (client != null)
                client.getConnectionManager().shutdown();
        }
    }

    private void saveUriParameters(Context context, URI uri, String userId) {
        String query = uri.getQuery();
        if (userId != null && query != null) {
            // if (debug) System.out.println("XXX: params");
            for(String s : query.split("&")) {
                int ind = s.indexOf('=');
                if (ind == -1) continue;
                String name = s.substring(0,ind);
                String value = s.substring(ind + 1);
                if ("client_id".equals(name) || "nonce".equals(name) || "code".equals(name)) {
                    // save client id + nonce in configuration properties
                    context.setProperty(userId + "." + name, value);
                }
                /*
                else if ("request".equals(name)) {
                    byte[] bytes = Base64.decodeBase64(value);
                    if (bytes != null) {
                        value = new String(bytes);
                        System.out.println("\t** XXX: request=" + value);
                    }
                }
                */
                /*
                try {
                    value = new String(bytes);//, "UTF-8");
                    System.out.println("\t** value=" + value);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                */
                // if (debug) System.out.printf("\t%s=%s%n", name, value);
                //if ("nonce".equals(name)) {
                //context.setProperty(userEmail.replaceAll("[^a-zA-Z0-9]+","_") + ".nonce", value);
                //}
            }
        }
        /*
            if (log.isDebugEnabled()) {
                    // log.debug("XXX: params\n\t" + query.replace("&", "\n\t"));
                }
            }
            */
    }

	/**
	 * Wrap <tt>HttpClient.execute()</tt> to pre/post-test HTTP requests for
	 * any server specific implementation handling such as authentication.
	 *
	 * @param context   Application context
	 * @param client   the HttpClient, must never be null
	 * @param req   the request to execute, must never be null
	 *
	 * @return  the response to the request.
	 * @throws java.io.IOException in case of a problem or the connection was aborted
	 * @throws org.apache.http.client.ClientProtocolException in case of an http protocol error
	 */
	@NonNull
	public HttpResponse executeRequest(Context context, HttpClient client, HttpUriRequest req)
			throws IOException
	{
		return client.execute(req, localContext);
	}

}