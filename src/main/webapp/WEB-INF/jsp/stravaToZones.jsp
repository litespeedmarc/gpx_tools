<%@ page import="com.google.gson.GsonBuilder" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html>
<head>
   <title>Strava to Zones</title>
   <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.1.1/jquery.min.js"></script>
</head>
<body>
<h2>Extract Intervals from Strava Activity</h2>
<form:form method="get" modelAttribute="requestParameters">
   <table>
      <thead>
      </thead>
      <tbody>
      <tr>
         <td>Strava Athlete Id:</td>
         <td><form:input path="userId" type="text" name="athleteId" id="athleteId"/></td>
      </tr>
      <tr>
         <td>Strava Activity Id:</td>
         <td><form:input path="activityId" type="text" name="activityId" id="activityId"/></td>
      </tr>
      <tr>
         <td>Minimum Time (seconds):</td>
         <td><form:input path="targetTimeInZone" type="text" name="minTime" id="mineTime"/></td>
      </tr>
      <tr>
         <td>Target Zone</td>
         <td><form:input path="targetZone" type="text" name="targetZone" id="targetZone"/></td>
      </tr>
      <tr>
         <td>Zones</td>
         <td><form:input path="zones" type="text" name="zones" id="zones"/></td>
      </tr>
      <tr>
         <td></td>
         <td><button type="submit">Let's Go</button></td>
      </tr>
      </tbody>
   </table>
</form:form>
  <div class="error">${error}</div>


<c:if test="${not empty summary}">

   <script src="https://code.highcharts.com/highcharts.js"></script>
   <script src="https://code.highcharts.com/modules/exporting.js"></script>


   <div id="container" style="min-width: 310px; height: 400px; margin: 0 auto"></div>

   <script>
       $(function () {
           Highcharts.chart('container', {
               chart: {
                   zoomType: 'x'
               },
               tooltip: {
                   formatter : function() {
                       var dur = 1;
                       for (var i = this.x; i >= 0 && this.series.data[i].y == this.y; i--) {
                           dur++;
                       }
                       for (var i = this.x; i < this.series.data.length && this.series.data[i].y == this.y; i++) {
                           dur++;
                       }
                       var date = new Date(null);
                       date.setSeconds(dur); // specify value for SECONDS here
                       dur = date.toISOString().substr(14, 5);
                       return this.y + " for " + dur;

                   }
               },
               title: {
                   text: 'Intervals vs. Actual Zones',
                   x: -20 //center
               },
               yAxis: {
                   title: {
                       text: 'Power (Watts)'
                   },
                   plotLines: [{
                       value: "${requestParameters.zones}".split(",")[${requestParameters.targetZone} - 1],
                       color: 'green',
                       dashStyle: 'shortdash',
                       width: 2,
                   },
                   ]
               },
               legend: {
                   layout: 'vertical',
                   align: 'right',
                   verticalAlign: 'middle',
                   borderWidth: 0
               },
               series: [{
                   type: 'line',
                   name: 'Actual Power',
                   animation:false,
                   color:'#99b3ff',
                   enableMouseTracking:false,
                   data: <% out.write(new GsonBuilder().create().toJson(request.getAttribute("realPoints"))); %>,
                   lineWidth: .1,
               }, {
                   type: 'line',
                   name: 'Out of Zone',
                   color: '#ff80d5',
                   data: <% out.write(new GsonBuilder().create().toJson(request.getAttribute("zonedPoints"))); %>,
               }, {
                   type: 'column',
                   name: 'In Zone',
                   color: '#ff80d5',
                   borderWidth : 1,
                   borderColor : '#ff80d5',
                   data: <% out.write(new GsonBuilder().create().toJson(request.getAttribute("matchingZonePoints"))); %>,


               }]
           });
       });

                   <%--<%--%>
                        <%--int[] realPoints = (int[]) request.getAttribute("realPoints");--%>
                        <%--StringBuilder str = new StringBuilder(realPoints.length * 7);--%>
                        <%--str.append('[');--%>
                        <%--for (int i = 0; i < realPoints.length; i++) {--%>
                            <%--str.append(i).append(',');--%>
                        <%--}--%>
                        <%--str.append(']');--%>
                        <%--out.write(str.toString());--%>
                   <%--%>--%>

   </script>
<div class="summary">
</div>
</c:if>

</body>
</html>