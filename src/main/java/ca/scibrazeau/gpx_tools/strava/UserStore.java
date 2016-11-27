package ca.scibrazeau.gpx_tools.strava;

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

}
