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

import java.io.Serializable;
import java.util.Queue;

import javax.servlet.http.HttpSession;

import net.zschech.gwt.comet.server.CometSession;

public class CometSessionImpl implements CometSession {
	
	private final HttpSession httpSession;
	private final Queue<Serializable> queue;
	private final AsyncServlet async;
	private volatile boolean valid;
	
	public CometSessionImpl(HttpSession httpSession, Queue<Serializable> queue, AsyncServlet async) {
		this.httpSession = httpSession;
		this.queue = queue;
		this.async = async;
		this.valid = true;
	}
	
	@Override
	public void enqueue(Serializable message) {
		queue.add(message);
		enqueued();
	}
	
	@Override
	public void enqueued() {
		async.enqueued(this);
	}
	
	@Override
	public Queue<? extends Serializable> getQueue() {
		return queue;
	}
	
	@Override
	public void invalidate() {
		valid = false;
		async.invalidate(this);
		httpSession.removeAttribute(HTTP_SESSION_KEY);
	}
	
	@Override
	public boolean isValid() {
		return valid;
	}
	
	@Override
	public HttpSession getHttpSession() {
		return httpSession;
	}
}
