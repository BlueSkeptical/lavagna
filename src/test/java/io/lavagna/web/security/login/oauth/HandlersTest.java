/**
 * This file is part of lavagna.
 *
 * lavagna is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * lavagna is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with lavagna.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.lavagna.web.security.login.oauth;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import io.lavagna.web.security.SecurityConfiguration.SessionHandler;
import io.lavagna.web.security.SecurityConfiguration.User;
import io.lavagna.web.security.SecurityConfiguration.Users;
import io.lavagna.web.security.login.oauth.BitbucketHandler;
import io.lavagna.web.security.login.oauth.GithubHandler;
import io.lavagna.web.security.login.oauth.GoogleHandler;
import io.lavagna.web.security.login.oauth.OAuthResultHandler.OAuthRequestBuilder;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.Api;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.context.WebApplicationContext;

@RunWith(MockitoJUnitRunner.class)
public class HandlersTest {

	@Mock
	private ServiceBuilder sBuilder;
	@Mock
	private SessionHandler sessionHandler;
	@Mock
	private Users users;
	@Mock
	private HttpServletRequest req, req2;
	@Mock
	private HttpServletResponse resp, resp2;

	@Mock
	private OAuthService oauthService;

	@Mock
	private ServletContext servletContext;
	@Mock
	private WebApplicationContext webappContext;
	@Mock
	private OAuthRequestBuilder reqBuilder;
	@Mock
	private OAuthRequest oauthReq;
	@Mock
	private Response oauthRes;
	@Mock
	private User user;

	private MockHttpSession session;
	private MockHttpSession session2;

	private String key = "key";
	private String secret = "secret";
	private String callback = "callback";
	private String errPage = "error";

	private OAuthResultHandler bitbucketHandler;
	private OAuthResultHandler googleHandler;
	private OAuthResultHandler githubHandler;

	@Before
	public void prepare() {

		when(sBuilder.provider(any(Api.class))).thenReturn(sBuilder);
		when(sBuilder.apiKey(any(String.class))).thenReturn(sBuilder);
		when(sBuilder.apiSecret(any(String.class))).thenReturn(sBuilder);
		when(sBuilder.callback(any(String.class))).thenReturn(sBuilder);
		when(sBuilder.scope(any(String.class))).thenReturn(sBuilder);
		when(sBuilder.build()).thenReturn(oauthService);

		session = new MockHttpSession();
		session2 = new MockHttpSession();

		when(req2.getParameter("code")).thenReturn("code");
		when(req2.getParameter("oauth_verifier")).thenReturn("code");

		when(req.getSession()).thenReturn(session);

		when(req2.getSession()).thenReturn(session);
		when(req2.getSession(true)).thenReturn(session2);

		when(req2.getServletContext()).thenReturn(servletContext);
		when(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).thenReturn(webappContext);

		when(reqBuilder.req(any(Verb.class), any(String.class))).thenReturn(oauthReq);
		when(oauthReq.send()).thenReturn(oauthRes);
		when(users.findUserByName(any(String.class), any(String.class))).thenReturn(user);

		bitbucketHandler = BitbucketHandler.FACTORY.build(sBuilder, reqBuilder, key, secret, callback, users, sessionHandler, errPage);
		githubHandler = GithubHandler.FACTORY.build(sBuilder, reqBuilder, key, secret, callback, users, sessionHandler, errPage);
		googleHandler = GoogleHandler.FACTORY.build(sBuilder, reqBuilder, key, secret, callback, users, sessionHandler, errPage);
	}

	@Test
	public void handleBitbucketFlowAuth() throws IOException {
		when(oauthService.getAuthorizationUrl(any(Token.class))).thenReturn("redirect");
		bitbucketHandler.handleAuthorizationUrl(req, resp);
		verify(resp).sendRedirect("redirect");

		when(oauthRes.getBody()).thenReturn("{\"user\" : {\"username\" : \"username\"}}");
		when(users.userExistsAndEnabled("oauth.bitbucket", "username")).thenReturn(true);
		when(users.findUserByName("oauth.bitbucket", "username")).thenReturn(user);
		when(req2.getContextPath()).thenReturn("");
		Assert.assertTrue(!session.isInvalid());
		bitbucketHandler.handleCallback(req2, resp2);
		verify(resp2).sendRedirect("/");
		verify(sessionHandler).setUser(user.getId(), user.isAnonymous(), req2, resp2);
		
	}

	@Test
	public void handleGithubFlowAuth() throws IOException {
		when(oauthService.getAuthorizationUrl(null)).thenReturn("redirect");
		githubHandler.handleAuthorizationUrl(req, resp);
		verify(resp).sendRedirect("redirect&state=" + session.getAttribute("EXPECTED_STATE_FOR_oauth.github"));

		when(req2.getParameter("state")).thenReturn((String) session.getAttribute("EXPECTED_STATE_FOR_oauth.github"));

		when(oauthRes.getBody()).thenReturn("{\"login\" : \"login\"}");
		when(users.userExistsAndEnabled("oauth.github", "login")).thenReturn(true);
		when(users.findUserByName("oauth.github", "login")).thenReturn(user);
		when(req2.getContextPath()).thenReturn("");

		Assert.assertTrue(!session.isInvalid());
		githubHandler.handleCallback(req2, resp2);
		verify(resp2).sendRedirect("/");

		verify(sessionHandler).setUser(user.getId(), user.isAnonymous(), req2, resp2);
	}

	@Test
	public void handleGoogleFlowAuth() throws IOException {
		when(oauthService.getAuthorizationUrl(null)).thenReturn("redirect");
		googleHandler.handleAuthorizationUrl(req, resp);
		verify(resp).sendRedirect("redirect&state=" + session.getAttribute("EXPECTED_STATE_FOR_oauth.google"));

		when(req2.getParameter("state")).thenReturn((String) session.getAttribute("EXPECTED_STATE_FOR_oauth.google"));
		when(oauthRes.getBody()).thenReturn("{\"email\" : \"email\", \"email_verified\" : true}");
		when(users.userExistsAndEnabled("oauth.google", "email")).thenReturn(true);
		when(users.findUserByName("oauth.github", "email")).thenReturn(user);
		when(req2.getContextPath()).thenReturn("/context-path");
		
		Assert.assertTrue(!session.isInvalid());
		googleHandler.handleCallback(req2, resp2);
		verify(resp2).sendRedirect("/context-path/");
		
		verify(sessionHandler).setUser(user.getId(), user.isAnonymous(), req2, resp2);
	}
}
