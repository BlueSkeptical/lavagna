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
import io.lavagna.model.Key;
import io.lavagna.model.User;
import io.lavagna.service.ConfigurationRepository;
import io.lavagna.service.UserRepository;
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

	private static final String BASE_APPLICATION_URL = "http://localhost/";
	@Mock
	private ServiceBuilder sBuilder;
	@Mock
	private UserRepository usersRep;
	@Mock
	private HttpServletRequest req, req2;
	@Mock
	private HttpServletResponse resp, resp2;
	//
	@Mock
	private ConfigurationRepository configurationRepository;

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

	private BitbucketHandler bitbucketHandler;
	private GoogleHandler googleHandler;
	private GithubHandler githubHandler;

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
		when(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).thenReturn(
				webappContext);
		when(webappContext.getBean(ConfigurationRepository.class)).thenReturn(configurationRepository);
		when(configurationRepository.getValue(Key.BASE_APPLICATION_URL)).thenReturn(BASE_APPLICATION_URL);

		when(reqBuilder.req(any(Verb.class), any(String.class))).thenReturn(oauthReq);
		when(oauthReq.send()).thenReturn(oauthRes);
		when(usersRep.findUserByName(any(String.class), any(String.class))).thenReturn(user);

		bitbucketHandler = new BitbucketHandler(sBuilder, reqBuilder, key, secret, callback, usersRep, errPage);
		githubHandler = new GithubHandler(sBuilder, reqBuilder, key, secret, callback, usersRep, errPage);
		googleHandler = new GoogleHandler(sBuilder, reqBuilder, key, secret, callback, usersRep, errPage);
	}

	@Test
	public void handleBitbucketFlowAuth() throws IOException {
		when(oauthService.getAuthorizationUrl(any(Token.class))).thenReturn("redirect");
		bitbucketHandler.handleAuthorizationUrl(req, resp);
		verify(resp).sendRedirect("redirect");

		when(oauthRes.getBody()).thenReturn("{\"user\" : {\"username\" : \"username\"}}");
		when(usersRep.userExistsAndEnabled("oauth.bitbucket", "username")).thenReturn(true);

		Assert.assertTrue(!session.isInvalid());
		bitbucketHandler.handleCallback(req2, resp2);
		verify(resp2).sendRedirect(BASE_APPLICATION_URL);
		Assert.assertTrue(session.isInvalid());
	}

	@Test
	public void handleGithubFlowAuth() throws IOException {
		when(oauthService.getAuthorizationUrl(null)).thenReturn("redirect");
		githubHandler.handleAuthorizationUrl(req, resp);
		verify(resp).sendRedirect("redirect&state=" + session.getAttribute("EXPECTED_STATE_FOR_oauth.github"));

		when(req2.getParameter("state")).thenReturn((String) session.getAttribute("EXPECTED_STATE_FOR_oauth.github"));

		when(oauthRes.getBody()).thenReturn("{\"login\" : \"login\"}");
		when(usersRep.userExistsAndEnabled("oauth.github", "login")).thenReturn(true);

		Assert.assertTrue(!session.isInvalid());
		githubHandler.handleCallback(req2, resp2);
		verify(resp2).sendRedirect(BASE_APPLICATION_URL);
		Assert.assertTrue(session.isInvalid());
	}

	@Test
	public void handleGoogleFlowAuth() throws IOException {
		when(oauthService.getAuthorizationUrl(null)).thenReturn("redirect");
		googleHandler.handleAuthorizationUrl(req, resp);
		verify(resp).sendRedirect("redirect&state=" + session.getAttribute("EXPECTED_STATE_FOR_oauth.google"));

		when(req2.getParameter("state")).thenReturn((String) session.getAttribute("EXPECTED_STATE_FOR_oauth.google"));
		when(oauthRes.getBody()).thenReturn("{\"email\" : \"email\", \"email_verified\" : true}");
		when(usersRep.userExistsAndEnabled("oauth.google", "email")).thenReturn(true);

		Assert.assertTrue(!session.isInvalid());
		googleHandler.handleCallback(req2, resp2);
		verify(resp2).sendRedirect(BASE_APPLICATION_URL);
		Assert.assertTrue(session.isInvalid());
	}
}
