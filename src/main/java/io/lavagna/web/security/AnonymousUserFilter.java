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
package io.lavagna.web.security;

import static org.springframework.web.context.support.WebApplicationContextUtils.getRequiredWebApplicationContext;
import io.lavagna.model.Key;
import io.lavagna.service.ConfigurationRepository;
import io.lavagna.service.UserRepository;
import io.lavagna.web.helper.UserSession;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.context.WebApplicationContext;

public class AnonymousUserFilter extends AbstractBaseFilter {
    
    private ConfigurationRepository configurationRepository;
    private UserRepository userRepository;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        WebApplicationContext ctx = getRequiredWebApplicationContext(filterConfig.getServletContext());
        this.configurationRepository = ctx.getBean(ConfigurationRepository.class);
        this.userRepository = ctx.getBean(UserRepository.class);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        
        Map<Key, String> configuration = configurationRepository.findConfigurationFor(EnumSet.of(Key.SETUP_COMPLETE, Key.ENABLE_ANON_USER));
        if("true".equals(configuration.get(Key.SETUP_COMPLETE))) {
            handleAnonymousUser(configuration, request, response);
        }
        
        chain.doFilter(request, response);
    }
    
    private void handleAnonymousUser(Map<Key, String> configuration, HttpServletRequest req, HttpServletResponse resp) {

        boolean enabled = "true".equals(configuration.get(Key.ENABLE_ANON_USER));

        if (enabled && !UserSession.isUserAuthenticated(req)) {
            UserSession.setUser(userRepository.findUserByName("system", "anonymous"), req, resp, userRepository, false);
        }

        // handle the case when the user is logged as a anonymous user but it's
        // no more enabled
        if (!enabled && UserSession.isUserAuthenticated(req) && UserSession.isUserAnonymous(req)) {
            UserSession.invalidate(req, resp, userRepository);
        }
    }

}
