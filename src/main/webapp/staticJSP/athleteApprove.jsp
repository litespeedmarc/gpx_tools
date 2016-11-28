<%@ page import="ca.scibrazeau.gpx_tools.api.UserService" %>
<%@ page import="ca.scibrazeau.gpx_tools.model.User" %>
<%@ page import="ca.scibrazeau.gpx_tools.store.StoreUtils" %>
<%@ page import="java.io.File" %>
<%
    String confirmKey = request.getParameter("state");
    String stravaKey = request.getParameter("code");
    UserService userService = new UserService();
    boolean found = false;
    for (File f : StoreUtils.list("users")) {
        if (f.isDirectory()) {
            User u = userService.fetchUser(f.getName());
            if (u != null && u.isCheckKeyValid(confirmKey)) {
                found = true;
                userService.update(f.getName(), confirmKey, stravaKey);
                break;
            }
        }
    }
    if (!found) {
        throw new RuntimeException("There is no request with data " + confirmKey);
    }
%>
<html>
Key Generated.  You may now use the API.
</html>