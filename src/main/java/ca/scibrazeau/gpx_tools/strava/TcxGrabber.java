package ca.scibrazeau.gpx_tools.strava;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.DoubleStream;

import ca.scibrazeau.gpx_tools.model.ActivityDurationsAndPowers;
import ca.scibrazeau.gpx_tools.model.TcxSummary;
import ca.scibrazeau.gpx_tools.store.ActivityStore;

@SuppressWarnings("nls")
public class TcxGrabber {


	private final int mMinimumTimeTarget;
	private final int mMinimumTimeOther;
	private final int mMinimumZone;
	private final int[] mUserZones;

	private List<Segment> mSegments;
	private String mDateTime;

	// 765802249
	public TcxGrabber(String userId, long activityId, int targetDurationSeconds, int targetZone, String zones) {

		mMinimumTimeTarget = targetDurationSeconds / 2;
		mMinimumTimeOther = targetDurationSeconds;
		mMinimumZone = targetZone;

		String[] zonesA = zones.split(",");
		mUserZones = new int[zonesA.length];
		for (int i = 0; i < zonesA.length; i++) {
			mUserZones[i] = Integer.parseInt(zonesA[i]);
		}

		ActivityDurationsAndPowers durationsAndPowers = ActivityStore.access().getUserActivityPowerAndDurations(userId, activityId);
		List<Float> powerData = durationsAndPowers.getPowerData();
		List<Float> durationSeconds = durationsAndPowers.getDurationSeconds();

		mDateTime = durationsAndPowers.getStartDateTime();

		fixNullsAtStart(powerData, durationSeconds);
		fixNullsAtEnd(powerData, durationSeconds);

		for (int i = 0; i < durationSeconds.size() - 1; i++) {
			Float thisSec = durationSeconds.get(i);
			durationSeconds.set(i, durationSeconds.get(i + 1) - thisSec);
		}
		durationSeconds.set(durationSeconds.size() - 1, 1f);

		fixNulls(powerData, durationSeconds);

		for (int i = 0; i < powerData.size(); i++) {
			if (durationSeconds.get(i) != 1) {
				int toInsert = (durationSeconds.get(i)).intValue();
				for (int j = 1; j < toInsert; j++) {
					durationSeconds.set(i, 1f);
					powerData.add(i, powerData.get(i));
					durationSeconds.add(i, 1f);
				}
			}
		}


		Segment startSegment = new Segment(powerData);
		mSegments = startSegment.splitInto();
	}

	private void fixNulls(List<Float> powerData, List<Float> durationSeconds) {

		fixNullsInMiddle(powerData, durationSeconds);
	}

	private void fixNullsInMiddle(List<Float> powerData, List<Float> durationSeconds) {
		for (int i = 1; i < powerData.size(); i++) {
			if (powerData.get(i) == null) {
				fixNullsInMiddle(i, powerData, durationSeconds);
			}
		}

	}

	private void fixNullsInMiddle(int startAt, List<Float> powerData, List<Float> durationSeconds) {
		int stopAt = -1;
		for (int i = startAt; i < powerData.size(); i++) {
			if (powerData.get(i) != null) {
				stopAt = i;
				break;
			}
		}
		float totalSecondsMissing = durationSeconds.get(stopAt) - durationSeconds.get(startAt - 1);
		float stopPower = powerData.get(stopAt);
		float startPower = powerData.get(startAt - 1);
		float thisSeconsMissing = durationSeconds.get(startAt) - durationSeconds.get(startAt) - 1;

		for (int i = startAt; i <= stopAt; i++) {
			powerData.set(i, startPower + (stopPower - startPower) * thisSeconsMissing / totalSecondsMissing);
		}
	}

	private void fixNullsAtEnd(List<Float> powerData, List<Float> durationSeconds) {
		while (powerData.size() > 0 && (powerData.get(powerData.size() - 1) == null || powerData.get(powerData.size() - 1) < mUserZones[0]) ) {
			powerData.remove(powerData.size() - 1);
			durationSeconds.remove(durationSeconds.size() - 1);
		}
	}

	private void fixNullsAtStart(List<Float> powerData, List<Float> durationSeconds) {
		while (powerData.size() > 0 && (powerData.get(0) == null || powerData.get(0) < mUserZones[0]))  {
			powerData.remove(0);
			durationSeconds.remove(0);
		}
	}

	private void showZone(int i, int j, double f) {
		String from;
		if (i < 100) {
			from = "   ";
		} else {
			from = Integer.toString(i);
		}
		String to;
		if (j > 999) {
			to = "   ";
		} else {
			to = Integer.toString(j);
		}
		DecimalFormat df = new DecimalFormat("#.#");
		System.out.println(from + " - " + to + ": " + df.format(f / 60f));
	}

	public TcxSummary summarize() {
		TcxSummary result = new TcxSummary();
		result.setDateTime(mDateTime);
		result.setDurationSeconds(getTotalDurationSeconds(1));

		List<TcxSummary.Interval> intervals = new ArrayList<>();
		double restSoFar = 0;
		boolean first = true;
		for (Segment s : mSegments) {
			if (s.getZone() >= mMinimumZone - 1) {
				// ignore first interval if it is too short.
				if (first && s.totalDuration() < mMinimumTimeTarget * 2 - 10) {
					// but rest before gets reset
					restSoFar = 0;
					continue;
				}
				first = false;
				TcxSummary.Interval i = new TcxSummary.Interval();

				i.setDuration((int) s.totalDuration());
				i.setWatts((int) s.getAvgPower());
				i.setRest((int) restSoFar);
				restSoFar = 0;
				intervals.add(i);
			} else if (s.getZone() < 1) {
				restSoFar += s.totalDuration();
			} else {
				restSoFar = 0;
			}
		}
		result.setIntervals(intervals.stream().toArray(ln -> new TcxSummary.Interval[ln]));
		result.setRideAveragePowerWatts(getAveragePower());

		return result;

	}

	public int getTotalDurationSeconds(int minPower) {
		double totalDuration = mSegments.stream()
				.filter(s -> s.getAvgPower() >= minPower)
				.mapToDouble(Segment::totalDuration)
				.sum();
		return (int) totalDuration;
	}

	private int getAveragePower() {
		int totalDuration = getTotalDurationSeconds(1);
		return (int) mSegments.stream()
				.filter(s -> s.getAvgPower() > 0)
				.mapToDouble(s -> s.getAvgPower() * s.totalDuration() / totalDuration).sum();
	}

	public int[] getRealPoints() {
		int numberPoints = getTotalDurationSeconds(0);
		int at = 0;
		int[] result = new int[numberPoints];
		for (Segment s : mSegments) {
			for (double pow : s.mPowers) {
				result[at++] = (int) pow;
			}
		}
		return result;
	}

	public int[] getZonedPoints() {
		int numberPoints = getTotalDurationSeconds(0);
		int at = 0;
		int[] result = new int[numberPoints];
		for (Segment s : mSegments) {
			double avgPow = s.getAvgPower();
			for (double ignored : s.mPowers) {
				result[at++] = (int) avgPow;
			}
		}
		return result;
	}


	public int[] getMatchingZonePoints() {
		int numberPoints = getTotalDurationSeconds(0);
		int at = 0;
		int[] result = new int[numberPoints];
		for (Segment s : mSegments) {
			double avgPow = s.getAvgPower();
			if (avgPow < mUserZones[mMinimumZone - 1 ]) {
				avgPow = 0;
			}
			for (double ignored : s.mPowers) {
				result[at++] = (int) avgPow;
			}
		}
		return result;
	}

	private class Segment {

		private List<Double> mPowers;

		public List<Segment> splitInto() {
			List<Segment> toReturn = splitInto2();

			for (int i = 0; i < toReturn.size() - 1; i++) {
				Segment thisSeg = toReturn.get(i);
				Segment nextSeg = toReturn.get(i + 1);
				int thisZone = Math.min(thisSeg.getZone(), mMinimumZone - 1);
				int nextZone = Math.min(nextSeg.getZone(), mMinimumZone - 1);
				if (thisZone == nextZone) {
					thisSeg.addEnd(nextSeg);
					toReturn.remove(i + 1);
					i--; // re-evaluate this segment
				}
			}
			
			return toReturn;

		}

		List<Segment> splitInto2() {
			List<Segment> toReturn = new ArrayList<>();

			// can't split any further
			if (totalDuration() <= mMinimumTimeTarget) {
				toReturn.add(this);
				return toReturn;
			}

			// get highest point. Create 3 segments. Left, mid & right
			int maxIndex = getMaxIndex();
			Segment left = subset(0, maxIndex);
			Segment middle = subset(maxIndex, maxIndex + 1);
			Segment right = subset(maxIndex + 1, mPowers.size());

			// move highest from left/right until no more to move from
			// or until move one from left (or right) would change our zone
			while (true) {
				double leftPow = left.getLastPow();
				double rightPow = right.getFirstPow();

				if (leftPow < 0 && rightPow < 0) {
					break;
				}

				boolean addLeft = leftPow > rightPow;
				double powToAdd = addLeft ? leftPow : rightPow;
				double totalPower = middle.getTotalPower();
				double avgPower = totalPower / middle.totalDuration();
				int ourZone = Math.min(mMinimumZone - 1, TcxGrabber.this.getZone(avgPower));

				boolean add;
				int minimumTime;
				if (middle.getZone() >= mMinimumZone - 1) {
					minimumTime = mMinimumTimeTarget;
				} else {
					minimumTime = mMinimumTimeOther;
				}
				if (middle.totalDuration() < minimumTime) {
					add = true;
				} else if (powToAdd >= mUserZones[ourZone]) {
					add = true;
				} else {
					double avg5 = addLeft ? left.tailAverage(5) : right.headAverage(5);
					add = avg5 >= mUserZones[ourZone];
				}

				if (middle.totalDuration() < minimumTime || add) {
					if (addLeft) {
						middle.addStart(leftPow);
						left.trimEnd();
					} else {
						middle.addEnd(rightPow);
						right.trimStart();
					}
				} else {
					break;
				}
			}

			if (!left.isEmpty() && left.totalDuration() <= mMinimumTimeTarget) {
				middle.addStart(left);
			}
			if (!right.isEmpty() && right.totalDuration() <= mMinimumTimeTarget) {
				middle.addEnd(right);
			}

			if (!left.isEmpty()) {
				toReturn.addAll(left.splitInto2());
			}
			toReturn.add(middle);
			if (!right.isEmpty()) {
				toReturn.addAll(right.splitInto2());
			}
			return toReturn;
		}

		private double tailAverage(int n) {
			int size = mPowers.size();
			DoubleStream streamToAverage = powers();
			if (size > n) {
				streamToAverage = streamToAverage.sequential().skip(size - n);
			}
			return streamToAverage.average().orElse(0);
		}
		private double headAverage(int n) {
			return powers().sequential().limit(n).average().orElse(0);
		}

		private void addEnd(Segment right) {
			mPowers.addAll(right.mPowers);
			right.mPowers.clear();
		}

		private void addStart(Segment left) {
			mPowers.addAll(0, left.mPowers);
			left.mPowers.clear();
		}

		private void trimStart() {
			mPowers.remove(0);
		}

		private void trimEnd() {
			mPowers.remove(mPowers.size() - 1);
		}

		private double getTotalPower() {
			return totalPower();
		}

		private void addEnd(double pow) {
			mPowers.add(pow);
		}

		private void addStart(double leftPow) {
			mPowers.add(0, leftPow);
		}

		private double totalPower() {
			return powers().sum();
		}

		private boolean isEmpty() {
			return mPowers.isEmpty();
		}

		private double getFirstPow() {
			return mPowers.isEmpty() ? -1 : mPowers.get(0);
		}

		private double getLastPow() {
			return mPowers.isEmpty() ? -1 : mPowers.get(mPowers.size() - 1);
		}

		private Segment subset(int start, int stop) {
			if (start < 0 || start > mPowers.size()) {
				return new Segment();
			}
			if (stop < 0 || stop > mPowers.size()) {
				return new Segment();
			}
			List<Double> newPowers = mPowers.subList(start, stop);
			return new Segment(newPowers);
		}

		private int getMaxIndex() {
			double maxVal = 0;
			int maxIndex = -1;
			for (int i = 0; i < mPowers.size(); i++) {
				if (mPowers.get(i) >= maxVal) {
					maxVal = mPowers.get(i);
					maxIndex = i;
				}
			}
			return maxIndex;
		}

		private double totalDuration() {
			return mPowers.size();
		}

		private DoubleStream powers() {
			return mPowers.stream().mapToDouble(e -> e);
		}

		public Segment(List powers) {
			mPowers = new ArrayList<>();
			for (int i = 0; i < powers.size(); i++) {
				Object pow = powers.get(i);
				if (pow instanceof Float) {
					mPowers.add(((Float) pow).doubleValue());
				} else {
					mPowers.add((Double) pow);
				}
			}
		}

		public Segment() {
			mPowers = new ArrayList<>();
		}

		public double getAvgPower() {
			return totalPower() / totalDuration();
		}

		public int getZone() {
			return TcxGrabber.this.getZone(getAvgPower());
		}
	}

	private int getZone(double avgPower) {
		int idx = mUserZones.length - 1;
		for (int i = 0; i < mUserZones.length - 1; i++) {
			if (avgPower < mUserZones[i + 1]) {
				idx = i;
				break;
			}
		}
		return idx;
	}

	public static void main(String[] args) {
//		TcxGrabber tcxGrabber = new TcxGrabber("litespeedmarc", 688465052);
//		TcxGrabber tcxGrabber = new TcxGrabber("litespeedmarc", 765221014);
//		TcxGrabber tcxGrabber = new TcxGrabber("litespeedmarc", 765802249);
//		TcxGrabber tcxGrabber = new TcxGrabber("litespeedmarc", 766959565);
//		TcxGrabber tcxGrabber = new TcxGrabber("litespeedmarc", 770090309); // Tuesday
//		TcxGrabber tcxGrabber = new TcxGrabber("litespeedmarc", 771922478);  // Thursday Dec. 10:
//		TcxGrabber tcxGrabber = new TcxGrabber("litespeedmarc", 772736964);  // Friday Dec. 11:
//		TcxGrabber tcxGrabber = new TcxGrabber("litespeedmarc", 773675752);  // Sat Dec. 12:
//		TcxGrabber tcxGrabber = new TcxGrabber("litespeedmarc", 774745277);  // Sun Dec. 13:
//		TcxGrabber tcxGrabber = new TcxGrabber("litespeedmarc", 776707710);  // Tue Dec. 15
//		TcxGrabber tcxGrabber = new TcxGrabber("litespeedmarc", 779300279);  // Fri Dec. 18
//		TcxGrabber tcxGrabber = new TcxGrabber("litespeedmarc", 779942846);  // sat Dec. 18
//		TcxGrabber tcxGrabber = new TcxGrabber("litespeedmarc", 781377549);  // sat Dec. 18
//		TcxGrabber tcxGrabber = new TcxGrabber("litespeedmarc", 784856494);  // thur Dec. 18

		// Thursday.
//		tcxGrabber.showSegments();
//		tcxGrabber.showDetails();
//		tcxGrabber.showTimes();
//		tcxGrabber.showIntervals(12);
	}

	private TcxSummary showIntervals(int maxZones) {
		TcxSummary out = new TcxSummary();
		int[] countZones = new int[mUserZones.length];
		double totalTime = 0;
		for (Segment s : mSegments) {
			countZones[s.getZone()]++;
			totalTime += s.totalDuration();
		}
		double totalAvgPower = 0;
		for (Segment s : mSegments) {
			totalAvgPower += s.getAvgPower() * s.totalDuration() / totalTime;

		}
		out.setRideAveragePowerWatts((int) totalAvgPower);
		int stop = countZones.length - 1;
		int start = stop;
		int total = countZones[countZones.length - 1];
		for (int i = countZones.length - 2; i > 0; i--) {
			if (countZones[i] + total > maxZones) {
				break;
			}
			total += countZones[i];
			stop = i;
		}


		out.setDateTime(mDateTime);
		out.setDurationSeconds((int) totalTime);

		int totalRest = 0;
		for (Segment s : mSegments) {
			TcxSummary.Interval i = new TcxSummary.Interval();
			i.setDuration((int) s.totalDuration());
			i.setRest(totalRest);
			i.setWatts((int) s.getAvgPower());
			if (s.getZone() < 1) {
				totalRest  += s.totalDuration();
			} else {
				totalRest = 0;
			}
		}

		int totalTimeInIntervals = 0;
		for (Segment s : mSegments) {
			if (s.getZone() >= stop) {
				totalTimeInIntervals += s.totalDuration();

			}
		}

		return out;

	}

	private void showTimes() {
		double totalDuration = 0;
		for (Segment s : mSegments) {
			double pow = s.getAvgPower();
//			System.out.println((getZone(pow)+1) + "," + Math.round(pow) +  "," + totalDuration + "," + Math.round(s.totalDuration()));
			totalDuration += s.totalDuration();
			System.out.println((getZone(pow)+1) + "," + Math.round(pow) +  "," + totalDuration + "," + Math.round(s.totalDuration()));
			System.out.println((getZone(pow)+1) + "," + Math.round(pow) +  "," + totalDuration + "," + Math.round(s.totalDuration()));
		}

	}

	private void showDetails() {
		for (Segment s : mSegments) {
			long avgPower = Math.round(s.getAvgPower());
			for (double pow : s.mPowers) {
				System.out.println("1.0," + pow + "," + avgPower);
			}
		}
	}

	private void showSegments() {
		double[] timeInZones = new double[mUserZones.length];
		for (Segment s : mSegments) {
			int zone= getZone(s.getAvgPower());
			timeInZones[zone] += s.totalDuration();
		}

		showZone(0, mUserZones[1], timeInZones[0]);
		for (int i = 1; i < mUserZones.length - 1; i++) {
			showZone(mUserZones[i], mUserZones[i + 1], timeInZones[i]);
		}
		showZone(mUserZones[mUserZones.length - 1], 1000, timeInZones[mUserZones.length - 1]);
	}

}
