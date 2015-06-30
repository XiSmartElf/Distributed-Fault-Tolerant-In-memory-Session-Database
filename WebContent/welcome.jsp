<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ page session="false" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>EnterServlet</title>
</head>
<body>
	<form method="post" action="EnterServlet">
		<h2>${message}</h2>
		<h3>${message2}</h3>
		<h3>${message3}</h3>
		<div>${message4}</div>				
		<div>${message5}</div>				

		<br/>
		<button type="submit" name="act" value="replace">Replace</button>
		<input style="color:red;"type="text" name="returnName"> 
		<br/>
		<button type="submit" name="act" value="refresh">Refresh</button> 
		<br/>
		<button type="submit" name="act" value="logout">Logout</button>
		
		
		<p>session/cookie name: ${cookieValue} </p>
	</form>

</body>
</html>