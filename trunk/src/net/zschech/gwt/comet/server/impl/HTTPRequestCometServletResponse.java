/*
 * Copyright 2009 Richard Zschech.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package net.zschech.gwt.comet.server.impl;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.zschech.gwt.comet.client.impl.HTTPRequestCometTransport;
import net.zschech.gwt.comet.server.CometServlet;

import com.google.gwt.user.server.rpc.SerializationPolicy;

/**
 * The CometServletResponse for the {@link HTTPRequestCometTransport}
 * 
 * @author Richard Zschech
 */
public class HTTPRequestCometServletResponse extends ManagedStreamCometServletResponseImpl {
	
	private static final int MAX_PADDING_REQUIRED = 256;
	private static final String PADDING_STRING;
	static {
		char[] padding = new char[MAX_PADDING_REQUIRED];
		for (int i = 0; i < padding.length - 2; i++) {
			padding[i] = '*';
		}
		padding[padding.length - 2] = '\r';
		padding[padding.length - 1] = '\n';
		PADDING_STRING = new String(padding);
	}
	
	public HTTPRequestCometServletResponse(HttpServletRequest request, HttpServletResponse response, SerializationPolicy serializationPolicy, CometServlet servlet, AsyncServlet async, int heartbeat) {
		super(request, response, serializationPolicy, servlet, async, heartbeat);
	}
	
	@Override
	public void initiate() throws IOException {
		getResponse().setContentType("text/plain");
		
		super.initiate();
		
		// send connection event to client
		writer.append('!').append(String.valueOf(getHeartbeat())).append("\r\n");
	}
	
	@Override
	protected CharSequence getPadding(int written) {
		if (getRequest().getParameter("padding") != null) {
			// System.out.println("Written " + written);
			int padding = Integer.parseInt(getRequest().getParameter("padding"));
			if (written < padding) {
				StringBuilder result = new StringBuilder(padding - written);
				for (int i = written; i < padding - 2; i++) {
					result.append('*');
				}
				result.append("\r\n");
				return result;
			}
			else {
				return null;
			}
		}
		
		int paddingRequired;
		String userAgent = getRequest().getHeader("User-Agent");
		if (userAgent != null && userAgent.contains("Chrome")) {
			if (getRequest().getScheme().equals("https")) {
				paddingRequired = 64;
			}
			else {
				paddingRequired = 42;
			}
		}
		else {
			paddingRequired = 0;
		}
		
		if (written < paddingRequired) {
			return PADDING_STRING.substring(written);
		}
		else {
			return null;
		}
	}
	
	@Override
	protected void doSendError(int statusCode, String message) throws IOException {
		getResponse().setStatus(statusCode);
		if (message == null) {
			message = String.valueOf(statusCode);
		}
		writer.append(message);
	}
	
	@Override
	protected void doWrite(List<? extends Serializable> messages) throws IOException {
		for (Serializable message : messages) {
			CharSequence string;
			if (message instanceof CharSequence) {
				string = escape((CharSequence) message);
				writer.append(']');
			}
			else {
				string = serialize(message);
			}
			
			writer.append(string).append("\r\n");
		}
	}
	
	@Override
	protected boolean isOverMaxLength(int written) {
		return written > 2 * 1024 * 1024;
	}
	
	@Override
	protected void doHeartbeat() throws IOException {
		writer.append("#\r\n");
	}
	
	@Override
	protected void doTerminate() throws IOException {
		writer.append("?\r\n");
	}
	
	static CharSequence escape(CharSequence string) {
		int length = (string != null) ? string.length() : 0;
		int i = 0;
		loop: while (i < length) {
			char ch = string.charAt(i);
			switch (ch) {
			case '\\':
			case '\n':
			case '\r':
				break loop;
			}
			i++;
		}
		
		if (i == length)
			return string;
		
		StringBuilder str = new StringBuilder(string.length() * 2);
		str.append(string, 0, i);
		while (i < length) {
			char ch = string.charAt(i);
			switch (ch) {
			case '\\':
				str.append("\\\\");
				break;
			case '\n':
				str.append("\\n");
				break;
			case '\r':
				str.append("\\r");
				break;
			default:
				str.append(ch);
			}
			i++;
		}
		return str;
	}
}
