package ca.scibrazeau.gpx_tools.store;

import ca.scibrazeau.gpx_tools.model.StravaToZones;

public class UserStore {
	private static volatile UserStore mStore;

	public static UserStore access() {
		if (mStore == null) {
			mStore = new UserStore();
		}
		return mStore;
	}

	public String getStravaCode(String userId) {
		return "c18014019ea34496c65c279f42a35f9ed7764fca";
	}

	public int[] getZones() {
		return new int[] {
			240,
			290,
			330,
			350,
			380
		};
	}

    public void setLastStravaToZonesParams(String userId, StravaToZones stravaToZones) {
		StoreUtils.save(stravaToZones, userId, "lastStravaToZones");
    }


	public StravaToZones getLastStravaToZonesForUser(String lastUserId) {
		StravaToZones result = StoreUtils.load(StravaToZones.class, lastUserId, "lastStravaToZones");
		return result == null ? new StravaToZones(lastUserId) : result;
	}
}
