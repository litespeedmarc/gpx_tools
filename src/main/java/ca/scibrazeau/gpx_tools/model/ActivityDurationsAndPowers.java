package ca.scibrazeau.gpx_tools.model;

import java.util.List;

/**
 * Created by pssemr on 2016-11-26.
 */
public class ActivityDurationsAndPowers {

    private final String userId;
    private final long activityId;
    private final String startDateTime;
    private final List<Float> durationSeconds;
    private final List<Float> powerData;

    public ActivityDurationsAndPowers(String userId, long activityId, String s, List<Float> durationSeconds, List<Float> powerData) {
        this.userId = userId;
        this.activityId = activityId;
        this.startDateTime = s;
        this.durationSeconds = durationSeconds;
        this.powerData = powerData;
    }


    public String getUserId() {
        return userId;
    }

    public long getActivityId() {
        return activityId;
    }

    public String getStartDateTime() {
        return startDateTime;
    }

    public List<Float> getDurationSeconds() {
        return durationSeconds;
    }

    public List<Float> getPowerData() {
        return powerData;
    }
}
