package org.planit.time;

import java.util.logging.Logger;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.time.TimePeriod;

/**
 * Represents a time period within the day. Used to determine the duration and start time of trips for example. We internally adopt seconds as the unit
 * 
 * @author markr
 *
 */
public class TimePeriodImpl implements TimePeriod {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(TimePeriodImpl.class.getCanonicalName());

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
  public TimePeriodImpl(IdGroupingToken groupId, long startTimeSeconds, long durationSeconds) throws PlanItException {
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
   * @param description      description of this time period
   * @param startTimeSeconds start time of this time period
   * @param durationSeconds  duration of this time period
   * @throws PlanItException thrown if error
   */
  public TimePeriodImpl(IdGroupingToken groupId, String description, long startTimeSeconds, long durationSeconds) throws PlanItException {
    PlanItException.throwIf(durationSeconds > (24.0 * 3600), "Duration more than 24 hours");
    PlanItException.throwIf(startTimeSeconds > (24.0 * 3600), "Start time later than 24 hours");
    this.id = IdGenerator.generateId(groupId, TimePeriod.class);
    this.startTimeSeconds = startTimeSeconds;
    this.durationSeconds = durationSeconds;
    this.description = description;
  }

  // Getters

  /**
   * {@inheritDoc}
   */
  @Override
  public long getStartTimeSeconds() {
    return startTimeSeconds;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getDurationSeconds() {
    return durationSeconds;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getDescription() {
    return description;
  }

  /**
   * {@inheritDoc}
   */
  @Override
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
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return idHashCode();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    return idEquals(o);
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
