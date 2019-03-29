package org.planit.time;

import java.util.HashMap;
import java.util.Map;

import org.planit.utils.IdGenerator;

/**
 * Represents a time period within the day. Used to determine the duration and start time of trips for example
 * We internally adopt seconds as the unit
 * 
 * @author markr
 *
 */
public class TimePeriod implements Comparable<TimePeriod> {
	
	/**
	 * startTime in seconds from midnight 00:00:00
	 */
	private final int startTime;
	
	/**
	 * Duration in seconds
	 */
	private final int duration;
	
	private final long id;
	private final String description;
	
	private static Map<Long, TimePeriod> timePeriods = new HashMap<Long, TimePeriod>();
	
	/**
	 * Constructor
	 * 
	 * @param startTime in seconds from midnight
	 * @param duration in seconds
	 */
	public TimePeriod(int startTime, int duration) {
		this.id = IdGenerator.generateId(TimePeriod.class);
		this.startTime = startTime;
		this.duration = duration;
		description = null;
		timePeriods.put(this.id, this);
	}
	
	public TimePeriod(String description, String startTime24hour, int durationHours) throws Exception {
		this.id = IdGenerator.generateId(TimePeriod.class);
		this.description = description;		
		this.startTime = convertDurationToSeconds(startTime24hour);
		if (durationHours > 24.0) {
			throw new Exception("Duration more than 24 hours");
		}
		this.duration = durationHours * 86400;
		timePeriods.put(this.id, this);
	}
	
	public TimePeriod(long id, String description, String startTime24hour, int durationHours) throws Exception {
		this.id = id;
		this.description = description;		
		this.startTime = convertDurationToSeconds(startTime24hour);
		if (durationHours > 24.0) {
			throw new Exception("Duration more than 24 hours");
		}
		this.duration = durationHours * 86400;
		timePeriods.put(this.id, this);
	}
	
	private int convertDurationToSeconds(String startTime24hour) throws Exception {
		int startTime;
		int startTimeHrs;
		int startTimeMins;
		if (startTime24hour.length() != 4) {
			throw new Exception("Start time must contain exactly four digits");
		}
		try {
			startTime = Integer.parseInt(startTime24hour);
		} catch (NumberFormatException e) {
			throw new Exception("Start time must contain exactly four digits");
		}
		if (startTime < 0) {
			throw new Exception("Start time cannot be negative");
		}
		if (startTime > 2400) {
			throw new Exception("Start time cannot be later than 2400");
		}
		startTimeHrs = startTime % 100;
		startTimeMins = startTime - 24 * startTimeHrs;
		if (startTimeMins > 59) {
			throw new Exception("Last two digits of start time cannot exceed 59");
		}
		return (startTimeHrs * 3600) + (startTimeMins * 60);
		
	}
	
	// Public static
	
	public static void putById(TimePeriod timePeriod) {
		timePeriods.put(timePeriod.getId(), timePeriod);
	}
	
	public static TimePeriod getById(long id) {
		return timePeriods.get(id);
	}
	
	/**
	 * Factory method based on 24 hour input format
	 */
	public static TimePeriod createTimePeriod24h(float startHour, float durationHour) {
		return new TimePeriod(convertHourToSeconds(startHour), convertHourToSeconds(durationHour));
	}
	
	
	/**
	 * Factory method based on seconds from midnight format
	 */
	public static TimePeriod createTimePeriodSeconds(int startSeconds, int durationSeconds) {
		return new TimePeriod(startSeconds, durationSeconds);
	}
	
	/** Convert hours in 24h format from midnight to seconds
	 * @param hoursFromMidnight
	 * @return secondsFromMidnight
	 */
	public static int convertHourToSeconds(float hoursFromMidnight) {
		return (Math.round(hoursFromMidnight*3600));
	}
	
	/** Convert seconds to 24h format from midnight to seconds
	 * @param hoursFromMidnight
	 * @return secondsFromMidnight
	 */
	public static float convertSecondsToHours(int secondsFromMidnight) {
		return ((float)secondsFromMidnight)/3600;
	}
	
	// Getters
	
	public int getStartTime() {
		return startTime;
	}

	public int getDuration() {
		return duration;
	}
	
	public String getDescription() {
		return description;
	}
	
	public long getId() {
		return id;
	}

	/** compare based on start time and duration
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(TimePeriod o) {
		int startTimeDiff = getStartTime()- ((TimePeriod)o).getStartTime();
		if(startTimeDiff != 0) {
			return startTimeDiff;
		} else {
			return getDuration() - ((TimePeriod)o).getDuration();
		}
	}	

	@Override
	public String toString() {
		int endTime = startTime + duration;
		return "start time: "+  
				String.format("%02d:%02d:%02d", startTime/3600, (startTime / 60) % 3600, startTime%60) + 
				" end time:" + 
				String.format("%02d:%02d:%02d", endTime/3600, (endTime / 60) % 3600, endTime%60);
	}	

}