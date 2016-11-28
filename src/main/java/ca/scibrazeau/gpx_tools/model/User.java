package ca.scibrazeau.gpx_tools.model;

import org.apache.commons.lang.StringUtils;

/**
 * Created by pssemr on 2016-11-27.
 */
public class User {
    private String userId;
    private long athleteId;
    private String stravaKey;
    private String confirmKey;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getAthleteId() {
        return athleteId;
    }

    public void setAthleteId(long athleteId) {
        this.athleteId = athleteId;
    }

     public String getStravaKey() {
        return stravaKey;
    }

    public boolean isRegistered() {
        return !StringUtils.isEmpty(stravaKey);
    }

    public void setStravaKey(String stravaKey) {
        this.stravaKey = stravaKey;
    }

    public boolean isPending() {
        return !StringUtils.isEmpty(confirmKey);
    }

    String getCheckKey() {
        return confirmKey;
    }

    void setCheckKey(String checkKey) {
        confirmKey = checkKey;
    }

    public boolean isCheckKeyValid(String confirmGUID) {
        return confirmGUID != null && confirmGUID.equals(confirmKey);
    }

    public void setConfirmKey(String confirmKey) {
        this.confirmKey = confirmKey;
    }
}
