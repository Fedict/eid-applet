/*
 * eID Applet Project.
 * Copyright (C) 2008-2009 FedICT.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version
 * 3.0 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, see 
 * http://www.gnu.org/licenses/.
 */

package be.fedict.eid.applet.service;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.eid.applet.service.impl.CleanSessionProtocolStateListener;
import be.fedict.eid.applet.service.impl.HttpServletProtocolContext;
import be.fedict.eid.applet.service.impl.HttpServletRequestHttpReceiver;
import be.fedict.eid.applet.service.impl.HttpServletResponseHttpTransmitter;
import be.fedict.eid.applet.service.impl.handler.AuthenticationDataMessageHandler;
import be.fedict.eid.applet.service.impl.handler.ClientEnvironmentMessageHandler;
import be.fedict.eid.applet.service.impl.handler.ContinueInsecureMessageHandler;
import be.fedict.eid.applet.service.impl.handler.FileDigestsDataMessageHandler;
import be.fedict.eid.applet.service.impl.handler.HandlesMessage;
import be.fedict.eid.applet.service.impl.handler.HelloMessageHandler;
import be.fedict.eid.applet.service.impl.handler.IdentityDataMessageHandler;
import be.fedict.eid.applet.service.impl.handler.MessageHandler;
import be.fedict.eid.applet.service.impl.handler.SignatureDataMessageHandler;
import be.fedict.eid.applet.shared.AbstractProtocolMessage;
import be.fedict.eid.applet.shared.AppletProtocolMessageCatalog;
import be.fedict.eid.applet.shared.annotation.ResponsesAllowed;
import be.fedict.eid.applet.shared.protocol.ProtocolStateMachine;
import be.fedict.eid.applet.shared.protocol.Transport;
import be.fedict.eid.applet.shared.protocol.Unmarshaller;

/**
 * The eID applet service Servlet. This servlet should be used by web
 * applications for secure communication between the Java EE servlet container
 * and the eID applet. This servlet will push attributes within the HTTP session
 * after a successful identification of the browser using via the eID applet.
 * 
 * <p>
 * The attribute that is pushed within the HTTP session per default is:
 * <code>eid.identity</code> of type {@link Identity}.
 * </p>
 * 
 * <p>
 * The address on the eID card can also be requested by setting the optional
 * <code>IncludeAddress</code> <code>init-param</code> to <code>true</code>. The
 * corresponding HTTP session attribute is called <code>eid.address</code> and
 * is of type {@link Address}.
 * </p>
 * 
 * <p>
 * The photo on the eID card can also be requested by setting the optional
 * <code>IncludePhoto</code> <code>init-param</code> to <code>true</code>. The
 * corresponding HTTP session attribute is called <code>eid.photo</code>.
 * </p>
 * 
 * <p>
 * More information on all available init-param configuration parameters is
 * available in the eID Applet developer's guide.
 * </p>
 * 
 * @author fcorneli
 */
public class AppletServiceServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final Log LOG = LogFactory
			.getLog(AppletServiceServlet.class);

	private static final Class<? extends MessageHandler<?>>[] MESSAGE_HANDLER_CLASSES = new Class[] {
			IdentityDataMessageHandler.class, HelloMessageHandler.class,
			ClientEnvironmentMessageHandler.class,
			AuthenticationDataMessageHandler.class,
			SignatureDataMessageHandler.class,
			FileDigestsDataMessageHandler.class,
			ContinueInsecureMessageHandler.class };

	private Map<Class<?>, MessageHandler<?>> messageHandlers;

	private Unmarshaller unmarshaller;

	public AppletServiceServlet() {
		super();
		LOG.debug("constructor");
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		LOG.debug("init");

		this.messageHandlers = new HashMap<Class<?>, MessageHandler<?>>();
		for (Class<? extends MessageHandler> messageHandlerClass : MESSAGE_HANDLER_CLASSES) {
			HandlesMessage handlesMessageAnnotation = messageHandlerClass
					.getAnnotation(HandlesMessage.class);
			if (null == handlesMessageAnnotation) {
				throw new ServletException(
						"missing meta-data on message handler");
			}
			Class<? extends AbstractProtocolMessage> protocolMessageClass = handlesMessageAnnotation
					.value();
			MessageHandler<?> messageHandler;
			try {
				messageHandler = messageHandlerClass.newInstance();
			} catch (Exception e) {
				throw new ServletException(
						"cannot create message handler instance");
			}
			this.messageHandlers.put(protocolMessageClass, messageHandler);
		}

		Collection<MessageHandler<?>> messageHandlers = this.messageHandlers
				.values();
		for (MessageHandler<?> messageHandler : messageHandlers) {
			messageHandler.init(config);
		}

		this.unmarshaller = new Unmarshaller(new AppletProtocolMessageCatalog());
	}

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		LOG.debug("doGet");
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		out.println("<html>");
		out.println("<head><title>AppletService</title></head>");
		out.println("<body>");
		out
				.println("<p>The applet service should not be accessed directly.</p>");
		out.println("</body></html>");
		out.close();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		LOG.debug("doPost");

		/*
		 * First retrieve the HTTP headers. The unmarshaller may digest the
		 * body, which makes it impossible to retrieve the headers afterwards.
		 */
		Map<String, String> httpHeaders = new HashMap<String, String>();
		Enumeration<String> headerNamesEnum = request.getHeaderNames();
		while (headerNamesEnum.hasMoreElements()) {
			String headerName = headerNamesEnum.nextElement();
			httpHeaders.put(headerName, request.getHeader(headerName));
		}
		/*
		 * Incoming message unmarshaller.
		 */
		HttpServletRequestHttpReceiver httpReceiver = new HttpServletRequestHttpReceiver(
				request);
		Object transferObject;
		try {
			transferObject = this.unmarshaller.receive(httpReceiver);
		} catch (Exception e) {
			LOG.debug("unmarshaller error: " + e.getMessage(), e);
			throw new RuntimeException("unmarshaller error: " + e.getMessage(),
					e);
		}

		/*
		 * Protocol state checker for incoming message.
		 */
		HttpServletProtocolContext protocolContext = new HttpServletProtocolContext(
				request);
		ProtocolStateMachine protocolStateMachine = new ProtocolStateMachine(
				protocolContext);
		CleanSessionProtocolStateListener cleanSessionProtocolStateListener = new CleanSessionProtocolStateListener(
				request);
		protocolStateMachine
				.addProtocolStateListener(cleanSessionProtocolStateListener);
		protocolStateMachine.checkRequestMessage(transferObject);

		/*
		 * Message dispatcher
		 */
		Class<?> messageClass = transferObject.getClass();
		MessageHandler messageHandler = this.messageHandlers.get(messageClass);
		if (null == messageHandler) {
			throw new ServletException("unsupported message");
		}
		HttpSession session = request.getSession();
		Object responseMessage = messageHandler.handleMessage(transferObject,
				httpHeaders, request, session);

		/*
		 * Check outgoing messages for protocol constraints.
		 */
		ResponsesAllowed responsesAllowedAnnotation = messageClass
				.getAnnotation(ResponsesAllowed.class);
		if (null != responsesAllowedAnnotation) {
			/*
			 * Make sure the message handlers respect the protocol.
			 */
			if (null == responseMessage) {
				throw new ServletException(
						"null response message while @ResponsesAllowed constraint was set");
			}
			Class<?>[] responsesAllowed = responsesAllowedAnnotation.value();
			if (false == isOfClass(responseMessage, responsesAllowed)) {
				throw new ServletException("response message type incorrect");
			}
		}

		/*
		 * Protocol state checker for outgoing message.
		 */
		protocolStateMachine.checkResponseMessage(responseMessage);

		/*
		 * Marshall outgoing message.
		 */
		if (null != responseMessage) {
			HttpServletResponseHttpTransmitter httpTransmitter = new HttpServletResponseHttpTransmitter(
					response);
			Transport.transfer(responseMessage, httpTransmitter);
		}
	}

	private boolean isOfClass(Object object, Class<?>[] classes) {
		for (Class<?> clazz : classes) {
			if (clazz.equals(object.getClass())) {
				return true;
			}
		}
		return false;
	}
}
