package org.planit.time;

import java.util.logging.Logger;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.ExternalIdable;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;

/**
 * Represents a time period within the day. Used to determine the duration and start time of trips for example We internally adopt seconds as the unit
 * 
 * @author markr
 *
 */
public class TimePeriod implements Comparable<TimePeriod>, ExternalIdable {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(TimePeriod.class.getCanonicalName());

  /**
   * startTime in seconds from midnight 00:00:00
   */
  private final long startTimeSeconds;

  /**
   * Duration in seconds
   */
  private final long durationSeconds;

  /**
   * Object id
   */
  private final long id;

  /**
   * external Id
   */
  private String externalId;

  /**
   * xml Id
   */
  private String xmlId;

  /**
   * Description of this time period
   */
  private final String description;

  /**
   * Constructor
   * 
   * @param groupId          contiguous id generation within this group for instances of this class
   * @param startTimeSeconds start time in seconds from midnight
   * @param durationSeconds  duration in seconds
   * @throws PlanItException thrown if error
   */
  public TimePeriod(IdGroupingToken groupId, long startTimeSeconds, long durationSeconds) throws PlanItException {
    PlanItException.throwIf(durationSeconds > (24.0 * 3600), "Duration more than 24 hours");
    PlanItException.throwIf(startTimeSeconds > (24.0 * 3600), "Start time later than 24 hours");
    this.id = IdGenerator.generateId(groupId, TimePeriod.class);
    this.startTimeSeconds = startTimeSeconds;
    this.durationSeconds = durationSeconds;
    this.description = null;
  }

  /**
   * Constructor
   * 
   * @param groupId          contiguous id generation within this group for instances of this class
   * @param externalId       externalId of this time period
   * @param description      description of this time period
   * @param startTimeSeconds start time of this time period
   * @param durationSeconds  duration of this time period
   * @throws PlanItException thrown if error
   */
  public TimePeriod(IdGroupingToken groupId, String description, long startTimeSeconds, long durationSeconds) throws PlanItException {
    PlanItException.throwIf(durationSeconds > (24.0 * 3600), "Duration more than 24 hours");
    PlanItException.throwIf(startTimeSeconds > (24.0 * 3600), "Start time later than 24 hours");
    this.id = IdGenerator.generateId(groupId, TimePeriod.class);
    this.startTimeSeconds = startTimeSeconds;
    this.durationSeconds = durationSeconds;
    this.description = description;
  }

  /**
   * Convert duration to seconds given start time using the 24-hour clock
   * 
   * @param fourDigitHour start time in 24-hour clock format (four digits exactly)
   * @return duration in seconds
   * @throws PlanItException thrown if the input time is not in the correct format
   */
  public static long convertHoursToSeconds(String fourDigitHour) throws PlanItException {
    long startTime;
    long startTimeHrs;
    long startTimeMins;
    PlanItException.throwIf(fourDigitHour.length() != 4, "Start time must contain exactly four digits");

    try {
      startTime = Integer.parseInt(fourDigitHour);
    } catch (NumberFormatException e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItException("Start time must contain exactly four digits", e);
    }
    PlanItException.throwIf(startTime < 0, "Start time cannot be negative");
    PlanItException.throwIf(startTime > 2400, "Start time cannot be later than 2400");

    startTimeHrs = startTime / 100;
    startTimeMins = startTime % 100;

    PlanItException.throwIf(startTimeMins > 59, "Last two digits of start time cannot exceed 59");

    return (startTimeHrs * 3600) + (startTimeMins * 60);
  }

  /**
   * Convert hours to (whole) seconds
   * 
   * @param hoursFromMidnight the hours from midnight
   * @return secondsFromMidnight the seconds from midnight
   */
  public static long convertHoursToSeconds(double hoursFromMidnight) {
    return (Math.round(hoursFromMidnight * 3600));
  }

  /**
   * Convert seconds to 24h format from midnight to seconds
   * 
   * @param secondsFromMidnight seconds from midnight
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
  public long getStartTimeSeconds() {
    return startTimeSeconds;
  }

  /**
   * Return the duration in seconds
   * 
   * @return duration
   */
  public long getDurationSeconds() {
    return durationSeconds;
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
   * {@inheritDoc}
   */
  @Override
  public String getExternalId() {
    return externalId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setExternalId(String externalId) {
    this.externalId = externalId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getXmlId() {
    return this.xmlId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setXmlId(String xmlId) {
    this.xmlId = xmlId;
  }

  /**
   * Compare this object with another TimePeriod object
   * 
   * Comparison is based on start time and duration
   * 
   * @param o TimePeriod this object is being compared to
   * @return result of comparison
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(TimePeriod o) {
    long startTimeDiff = getStartTimeSeconds() - ((TimePeriod) o).getStartTimeSeconds();
    if (startTimeDiff != 0) {
      return (int) startTimeDiff;
    } else {
      return (int) (getDurationSeconds() - ((TimePeriod) o).getDurationSeconds());
    }
  }

  /**
   * Output this object as a String
   * 
   * @return String containing the value of this TimePeriod
   */
  @Override
  public String toString() {
    long endTime = startTimeSeconds + durationSeconds;
    return String.format("start time: %02d:%02d ", startTimeSeconds / 3600, (startTimeSeconds % 3600) / 60)
        + String.format("- end time: %02d:%02d", endTime / 3600, (endTime % 3600) / 60);
  }

}
