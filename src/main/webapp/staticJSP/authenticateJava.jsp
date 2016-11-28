<%@ page import="ca.scibrazeau.gpx_tools.api.UserService" %>
<%@ page import="ca.scibrazeau.gpx_tools.model.User" %>
<%@ page import="ca.scibrazeau.gpx_tools.store.ActivityStore" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.util.UUID" %>
<%
    UserService userService = new UserService();

    String confirmKey = UUID.randomUUID().toString().replaceAll("-", "");
    String userId = request.getParameter("userId");
    long athleteId = Long.parseLong(request.getParameter("athleteId"));

    User user = userService.fetchUser(userId);
    if (user == null) {
        user = new User();
        user.setUserId(userId);
        user.setAthleteId(athleteId);
        user.setConfirmKey(confirmKey);
    } else if (user.getAthleteId() != athleteId) {
        throw new IllegalArgumentException("UserId " + userId + " already in use to a different athlete");
    } else {
        user.setConfirmKey(confirmKey);
        user.setStravaKey(null);
    }
    userService.update(user);

    String callback = "http://www.scibrazeau.ca:8080/tps_tools/staticJSP/athleteApprove.jsp";
    URLEncoder.encode(callback, "UTF-8");
    String redirectURL = "https://www.strava.com/oauth/authorize?client_id=" +
            ActivityStore.kStravaClientId +
            "&state=" + confirmKey +
            "&response_type=code" +
            "&redirect_uri=" + callback +
            "&approval_prompt=force";

    response.sendRedirect(redirectURL);
%>