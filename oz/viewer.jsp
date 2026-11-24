<!DOCTYPE html>
<html style="height:100%">
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge"/>
<script src="http://code.jquery.com/jquery-1.8.3.min.js"></script>        
<link rel="stylesheet" href="http://code.jquery.com/ui/1.9.2/themes/base/jquery-ui.css" type="text/css"/>
<script src="http://code.jquery.com/ui/1.9.2/jquery-ui.min.js"></script>
<link rel="stylesheet" href="http://81.181.68.111:8080/ozrviewer/ui.dynatree.css" type="text/css"/>
<script type="text/javascript" src="http://81.181.68.111:8080/ozrviewer/jquery.dynatree.js" charset="utf-8"></script>
<script type="text/javascript" src="http://81.181.68.111:8080/ozrviewer/OZJSViewer.js" charset="utf-8"></script>
<script type="application/json" id="ozData">${dataJson}</script>
</head>
<body style="width:98%;height:98%">
<div id="OZViewer" style="width:98%;height:98%"></div>
<script type="text/javascript" >
	var OZ_REPORT = '<c:out value="${ozrPath}" />';
	var OZ_DATA   = document.getElementById('ozData').textContent;  // JSON 문자열 그대로

   function SetOZParamters_OZViewer(){
       var oz;
       oz = document.getElementById("OZViewer");
       oz.sendToActionScript("connection.servlet","http://81.181.68.111:8080/oz/server");
       oz.sendToActionScript('connection.reportname', OZ_REPORT);
       oz.sendToActionScript('connection.inputjson', OZ_DATA);  
//        oz.sendToActionScript("connection.reportname","category_path/sample.ozr");
       return true;
   }
   start_ozjs("OZViewer","http://81.181.68.111:8080/ozrviewer/");
</script>
</body>
</html>
