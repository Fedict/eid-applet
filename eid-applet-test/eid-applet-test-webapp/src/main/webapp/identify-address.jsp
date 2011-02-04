<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<html>
<head>
<title>eID Applet Identification Demo</title>
</head>
<body>
<h1>eID Applet Identification Demo</h1>
<script src="https://www.java.com/js/deployJava.js"></script>
<script>
	var attributes = {
		code :'be.fedict.eid.applet.Applet.class',
		archive :'eid-applet-package-${pom.version}.jar',
		width :600,
		height :300,
		mayscript :'true'
	};
	var parameters = {
		TargetPage :'identity-address-result.jsp',
		AppletService :'applet-service-address',
		BackgroundColor :'#ffffff',
		Language : 'nl',
		MessageCallback :'messageCallback',
		MessageCallbackEx : 'messageCallbackEx'
	};
	var version = '1.6';
	deployJava.runApplet(attributes, parameters, version);
</script>
<script>
	function messageCallback(status, message) {
		document.getElementById('appletMessage').innerHTML = '<b>' + status + ': ' + message + '</b>';
	}
	function messageCallbackEx(status, messageId, message) {
		document.getElementById('appletMessageEx').innerHTML = '<b>' + status + ': ' + messageId + ' = ' + message + '</b>';
	}
</script>
<div id="appletMessage">Message placeholder</div>
<div id="appletMessageEx">Message placeholder</div>
</body>
</html>