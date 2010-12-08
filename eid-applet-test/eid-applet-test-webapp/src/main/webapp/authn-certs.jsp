<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<html>
<head>
<title>eID Applet Authentication Demo</title>
</head>
<body>
<h1>eID Applet Authentication Demo</h1>
<p>eID authentication that will also push the eID certificates into
the HTTP session.</p>
<script src="https://www.java.com/js/deployJava.js"></script>
<script>
	var attributes = {
		code : 'be.fedict.eid.applet.Applet.class',
		archive : 'eid-applet.jar',
		width : 600,
		height : 300
	};
	var parameters = {
		TargetPage : 'authn-certs-result.jsp',
		AppletService : 'applet-service-authn-certs',
		BackgroundColor : '#ffffff',
		HideDetailsButton : 'true'
	};
	var version = '1.6';
	deployJava.runApplet(attributes, parameters, version);
</script>
</body>
</html>