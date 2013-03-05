/**
 * Copyright (c) 2000-2012 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.httpservice.internal.http;

import com.liferay.httpservice.HttpServicePropsKeys.Action;
import com.liferay.httpservice.HttpServicePropsKeys;
import com.liferay.httpservice.internal.servlet.BundleServletContext;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.Hashtable;
import java.util.Map;

import javax.servlet.Servlet;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * @author Raymond Augé
 * @author Miguel Pastor
 */
public class ServletTracker
	implements ServiceTrackerCustomizer<Servlet, Servlet> {

	public ServletTracker(HttpSupport httpSupport) {
		_httpSupport = httpSupport;
	}

	public Servlet addingService(ServiceReference<Servlet> serviceReference) {
		BundleContext bundleContext = _httpSupport.getBundleContext();

		Servlet servlet = bundleContext.getService(serviceReference);

		return doAction(serviceReference, servlet, Action.ADDING);
	}

	public void modifiedService(
		ServiceReference<Servlet> serviceReference, Servlet servlet) {

		doAction(serviceReference, servlet, Action.MODIFIED);
	}

	public void removedService(
		ServiceReference<Servlet> serviceReference, Servlet servlet) {

		doAction(serviceReference, servlet, Action.REMOVED);
	}

	protected Servlet doAction(
		ServiceReference<Servlet> serviceReference, Servlet servlet,
		Action action) {

		String alias = GetterUtil.getString(
			serviceReference.getProperty(HttpServicePropsKeys.ALIAS));

		if (Validator.isNull(alias)) {
			return servlet;
		}

		Map<String, String> initParameters = new Hashtable<String, String>();

		if (action != Action.REMOVED) {
			for (String key : serviceReference.getPropertyKeys()) {
				if (key.startsWith(HttpServicePropsKeys.INIT_PREFIX)) {
					String value = GetterUtil.getString(
						serviceReference.getProperty(key));

					initParameters.put(
						key.substring(
							HttpServicePropsKeys.INIT_PREFIX.length()), value);
				}
			}
		}

		Bundle bundle = serviceReference.getBundle();

		try {
			BundleServletContext bundleServletContext =
				_httpSupport.getBundleServletContext(bundle);

			if (action != Action.ADDING) {
				bundleServletContext.unregisterServlet(alias);
			}

			if (action != Action.REMOVED) {
				String contextId = GetterUtil.getString(
					serviceReference.getProperty(
						HttpServicePropsKeys.CONTEXT_ID));

				HttpContext httpContext = _httpSupport.getHttpContext(
					contextId);

				if (httpContext == null) {
					httpContext = bundleServletContext.getHttpContext();
				}

				bundleServletContext.registerServlet(
					alias, servlet, initParameters, httpContext);
			}
		}
		catch (Exception e) {
			_log.error(e);
		}

		return servlet;
	}

	private static Log _log = LogFactoryUtil.getLog(ServletTracker.class);

	private HttpSupport _httpSupport;

}