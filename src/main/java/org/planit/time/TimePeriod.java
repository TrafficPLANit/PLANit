package org.planit.time;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.planit.exceptions.PlanItException;
import org.planit.utils.IdGenerator;

/**
 * Represents a time period within the day. Used to determine the duration and
 * start time of trips for example We internally adopt seconds as the unit
 * 
 * @author markr
 *
 */
public class TimePeriod implements Comparable<TimePeriod> {

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(TimePeriod.class.getName());

    /**
     * startTime in seconds from midnight 00:00:00
     */
    private final int startTime;

    /**
     * Duration in seconds
     */
    private final int duration;

    /**
     * Object id
     */
    private final long id;
    
    /**
     * Object external Id
     */
    private final long externalId;

    /**
     * Description of this time period
     */
    private final String description;

    /**
     * Map storing registered time periods by Id
     */
    private static Map<Long, TimePeriod> timePeriods = new HashMap<Long, TimePeriod>();
    
    /**
     * Map storing registered time periods by external Id
     */
    private static Map<Long, TimePeriod> timePeriodsByExternalId = new HashMap<Long, TimePeriod>();

    /**
     * Constructor
     * 
     * @param externalId
     *            externalId of this time period
     * @param startTime
     *            in seconds from midnight
     * @param duration
     *            in seconds
     */

    public TimePeriod(long externalId, int startTime, int duration) {
        this.id = IdGenerator.generateId(TimePeriod.class);
        this.startTime = startTime;
        this.duration = duration;
        description = null;
        timePeriods.put(this.id, this);
        this.externalId = externalId;
        timePeriodsByExternalId.put(this.externalId, this);
    }

    /**
     * Constructor
     * 
     * @param externalId
     *            externalId of this time period
     * @param description
     *            description of this time period
     * @param startTime24hour
     *            start time of this time period
     * @param duration
     *            duration of this time period
     */
    public TimePeriod(long externalId, String description, int startTime, int duration) {
        this.id = IdGenerator.generateId(TimePeriod.class);
        this.externalId = externalId;
        this.startTime = startTime;
        this.duration = duration;
        this.description = description;
        timePeriods.put(this.id, this);
        timePeriodsByExternalId.put(this.externalId, this);
    }
  
    /**
     * Constructor
     * 
     * This constructor uses duration in hours.  This is a double since fractions of an hour are possible.
     * 
     * @param externalId
     *            externalId of this time period
     * @param description
     *            description of this time period
     * @param startTime24hour
     *            start time of this time period
     * @param durationHours
     *            duration of this time period in hours 
     * @throws PlanItException
     *             thrown if duration is longer than 24 hours
     */
    public TimePeriod(long externalId, String description, String startTime24hour, double durationHours) throws PlanItException {
        this.id = IdGenerator.generateId(TimePeriod.class);
        this.externalId = externalId;
        this.description = description;
        this.startTime = convertDurationToSeconds(startTime24hour);
        if (durationHours > 24.0) {
            throw new PlanItException("Duration more than 24 hours");
        }
        this.duration = (int) Math.round(durationHours * 3600.0);
        timePeriods.put(this.id, this);
        timePeriodsByExternalId.put(this.externalId, this);
    }
    
    /**
     * Convert duration to seconds given start time using the 24-hour clock
     * 
     * @param startTime24hour
     *            start time in 24-hour clock format
     * @return duration in seconds
     * @throws PlanItException
     *             thrown if the input time is not in the correct format
     */
    private int convertDurationToSeconds(String startTime24hour) throws PlanItException {
        int startTime;
        int startTimeHrs;
        int startTimeMins;
        if (startTime24hour.length() != 4) {
            throw new PlanItException("Start time must contain exactly four digits");
        }
        try {
            startTime = Integer.parseInt(startTime24hour);
        } catch (NumberFormatException e) {
            throw new PlanItException("Start time must contain exactly four digits");
        }
        if (startTime < 0) {
            throw new PlanItException("Start time cannot be negative");
        }
        if (startTime > 2400) {
            throw new PlanItException("Start time cannot be later than 2400");
        }
        startTimeHrs = startTime % 100;
        startTimeMins = startTime - 24 * startTimeHrs;
        if (startTimeMins > 59) {
            throw new PlanItException("Last two digits of start time cannot exceed 59");
        }
        return (startTimeHrs * 3600) + (startTimeMins * 60);

    }

    // Public static

    /**
     * Store time period by its id and external Id
     * 
     * @param timePeriod
     *            the time period to be stored
     */
    public static void putById(TimePeriod timePeriod) {
        timePeriods.put(timePeriod.getId(), timePeriod);
        timePeriodsByExternalId.put(timePeriod.getExternalId(), timePeriod);
    }
    
    /**
     * Retrieve time period by its id
     * 
     * @param id
     *            the id of the time period to be retrieved
     * @return the TimePeriod object found
     */
    public static TimePeriod getById(long id) {
        return timePeriods.get(id);
    }
    
    /**
     * Retrieve time period by its external id
     * 
     * @param externalId
     *            the external id of the time period to be retrieved
     * @return the TimePeriod object found
     */
   public static TimePeriod getByExternalId(long externalId) {
    	return timePeriodsByExternalId.get(externalId);
    }

    /**
     * Create a time period given its start time and duration in hours
     * 
     * @param externalId
     *            externalId of this time period
     * @param startHour
     *            the starting hour
     * @param durationHour
     *            the duration in hours
     * @return TimePeriod object generated
     */
    public static TimePeriod createTimePeriod24h(long externalId, float startHour, float durationHour) {
        return new TimePeriod(externalId, convertHourToSeconds(startHour), convertHourToSeconds(durationHour));
    }

    /**
     * Create a time period given its start time and duration in seconds
     * 
     * @param externalId
     *            externalId of this time period
     * @param startSeconds
     *            the start time in seconds
     * @param durationSeconds
     *            the duration in seconds
     * @return create TimePeriod object
     */
    public static TimePeriod createTimePeriodSeconds(long externalId, int startSeconds, int durationSeconds) {
        return new TimePeriod(externalId, startSeconds, durationSeconds);
    }

    /**
     * Convert hours in 24h format from midnight to seconds
     * 
     * @param hoursFromMidnight
     *            the hours from midnight
     * @return secondsFromMidnight the seconds from midnight
     */
    public static int convertHourToSeconds(float hoursFromMidnight) {
        return (Math.round(hoursFromMidnight * 3600));
    }

    /**
     * Convert seconds to 24h format from midnight to seconds
     * 
     * @param secondsFromMidnight
     *            seconds from midnight
     * @return hours from midnight
     */
    public static float convertSecondsToHours(int secondsFromMidnight) {
        return ((float) secondsFromMidnight) / 3600;
    }

    // Getters

    /**
     * Return the start time
     * 
     * @return start time
     */
    public int getStartTime() {
        return startTime;
    }

    /**
     * Return the duration in seconds
     * 
     * @return duration
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Return the description
     * 
     * @return description of this TimePeriod
     */
    public String getDescription() {
        return description;
    }

    /**
     * Return the id of this time period
     * 
     * @return id of this TimePeriod
     */
    public long getId() {
        return id;
    }
    
    /**
     * Return the external id of this time period
     * 
     * @return external id of this TimePeriod
     */
   public long getExternalId() {
    	return externalId;
    }

    /**
     * Compare this object with another TimePeriod object
     * 
     * Comparison is based on start time and duration
     * 
     * @param o
     *            TimePeriod this object is being compared to
     * @return result of comparison
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(TimePeriod o) {
        int startTimeDiff = getStartTime() - ((TimePeriod) o).getStartTime();
        if (startTimeDiff != 0) {
            return startTimeDiff;
        } else {
            return getDuration() - ((TimePeriod) o).getDuration();
        }
    }

    /**
     * Output this object as a String
     * 
     * @return String containing the value of this TimePeriod
     */
    @Override
    public String toString() {
        int endTime = startTime + duration;
        return "start time: "
                + String.format("%02d:%02d:%02d", startTime / 3600, (startTime / 60) % 3600, startTime % 60)
                + " end time:" + String.format("%02d:%02d:%02d", endTime / 3600, (endTime / 60) % 3600, endTime % 60);
    }

}
