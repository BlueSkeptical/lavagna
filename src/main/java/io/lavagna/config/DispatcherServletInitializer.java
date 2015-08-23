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
package io.lavagna.config;

import io.lavagna.web.security.CSFRFilter;
import io.lavagna.web.security.HSTSFilter;
import io.lavagna.web.security.SecurityFilter;

import java.util.Collections;

import javax.servlet.Filter;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.SessionTrackingMode;

import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;
import org.tuckey.web.filters.urlrewrite.gzip.GzipFilter;

public class DispatcherServletInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

	@Override
	protected Class<?>[] getRootConfigClasses() {
		return new Class<?>[] { DataSourceConfig.class,//
				PersistenceAndServiceConfig.class,//
				SchedulingServiceConfig.class,//
				WebSecurityConfig.class};
	}

	@Override
	protected Class<?>[] getServletConfigClasses() {
		return new Class<?>[] { WebConfig.class };
	}

	@Override
	protected String[] getServletMappings() {
		return new String[] { "/" };
	}

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		super.onStartup(servletContext);
		
		//definition order = execution order, the first executed filter is HSTSFilter
		addFilter(servletContext, "HSTSFilter", HSTSFilter.class, "/*");
		
		addFilter(servletContext, "CSFRFilter", CSFRFilter.class, "/*");
		
		addFilter(servletContext, "SecurityFilter", SecurityFilter.class, "/*");
		
		addFilter(servletContext, "ETagFilter", ShallowEtagHeaderFilter.class, "*.js", "*.css",//
                "/", "/project/*", "/admin/*", "/me/",//
                "*.html", "*.woff", "*.eot", "*.svg", "*.ttf");
		
		addFilter(servletContext, "GzipFilter", GzipFilter.class, "*.js", "*.css",//
                "/", "/project/*", "/admin/*", "/me/",//
                "/api/self", "/api/board/*", "/api/project/*");
		
		
		servletContext.setSessionTrackingModes(Collections.singleton(SessionTrackingMode.COOKIE));
		servletContext.getSessionCookieConfig().setHttpOnly(true);
		servletContext.getSessionCookieConfig().setName("LAVAGNA_SESSION_ID");
	}

    private static void addFilter(ServletContext context, String filterName, Class<? extends Filter> filterClass, String... urlPatterns) {
	    javax.servlet.FilterRegistration.Dynamic hstsFilter = context.addFilter(filterName, filterClass);
        hstsFilter.setAsyncSupported(true);
        hstsFilter.addMappingForUrlPatterns(null, false, urlPatterns);
	}

	@Override
	protected void customizeRegistration(Dynamic registration) {

		MultipartConfigElement multipartConfigElement = new MultipartConfigElement("");

		registration.setMultipartConfig(multipartConfigElement);
		registration.setInitParameter("dispatchOptionsRequest", "true");
		registration.setAsyncSupported(true);
	}
}
