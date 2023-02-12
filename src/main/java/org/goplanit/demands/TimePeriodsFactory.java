package org.goplanit.demands;

import org.goplanit.time.TimePeriodImpl;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactory;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;
import org.goplanit.utils.time.TimePeriod;

/**
 * Factory class for time periods instances to be registered on its parent container passed in to constructor
 */
public class TimePeriodsFactory extends ManagedIdEntityFactoryImpl<TimePeriod> implements ManagedIdEntityFactory<TimePeriod> {

  /** container to use */
  protected final TimePeriods timePeriods;

  /**
   * Create a newly created instance without registering on the container
   *
   * @param startTimeSeconds start time in seconds from midnight
   * @param durationSeconds duration of time period in seconds
   * @return created time period
   */
  protected TimePeriodImpl createNew(long startTimeSeconds, long durationSeconds) {
    return new TimePeriodImpl(getIdGroupingToken(), startTimeSeconds, durationSeconds);
  }

  /**
   * Constructor
   *
   * @param tokenId              to use
   * @param timePeriods to use
   */
  protected TimePeriodsFactory(final IdGroupingToken tokenId, final TimePeriods timePeriods) {
    super(tokenId);
    this.timePeriods = timePeriods;
  }

  /**
   * register a new entry on the container and return it
   *
   * @param description for the time period
   * @param startTimeSeconds start time in seconds since midnight
   * @param durationSeconds duration in seconds
   */
  public TimePeriod registerNew(String description, long startTimeSeconds, long durationSeconds) {
    var newTimePeriod = new TimePeriodImpl(getIdGroupingToken(), description, startTimeSeconds, durationSeconds);
    timePeriods.register(newTimePeriod);
    return newTimePeriod;
  }

  /**
   * register a new entry on the container and return it
   *
   * @param startTimeSeconds start time in seconds since midnight
   * @param durationSeconds duration in seconds
   * @return created time period
   */
  public TimePeriod registerNew(long startTimeSeconds, long durationSeconds) {
    TimePeriodImpl newInstance = createNew(startTimeSeconds, durationSeconds);
    timePeriods.register(newInstance);
    return newInstance;
  }
}
