package org.goplanit.demands;

import org.goplanit.utils.id.*;
import org.goplanit.utils.time.TimePeriod;
import org.goplanit.utils.time.TimePeriodUtils;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiConsumer;

/**
 * Class to register and store time periods for the current demand object
 * todo: conform to managed id container
 *
 * @author garym, markr
 */
public final class TimePeriods extends ManagedIdEntitiesImpl<TimePeriod> implements ManagedIdEntities<TimePeriod>{

  /** factory to create instances on this container */
  private final TimePeriodsFactory factory;

  /**
   * Constructor
   *
   * @param tokenId  to use for id generation
   */
  public TimePeriods(final IdGroupingToken tokenId) {
    super(TimePeriod::getId, TimePeriod.TIMEPERIOD_ID_CLASS);
    this.factory = new TimePeriodsFactory(tokenId, this);
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param mapper to apply in case of deep copy to each original to copy combination (when provided, may be null)
   */
  public TimePeriods(TimePeriods other, boolean deepCopy, BiConsumer<TimePeriod, TimePeriod> mapper) {
    super(other, deepCopy, mapper);
    this.factory = new TimePeriodsFactory(other.getFactory().getIdGroupingToken(), this);
  }

  /**
   * Returns a set of all registered time periods sorted by the start time, i.e., the way the time period is comparable
   *
   * @return Set of all registered time periods
   */
  public SortedSet<TimePeriod> asSortedSetByStartTime() {
    SortedSet<TimePeriod> timePeriodSet = new TreeSet<>(TimePeriodUtils.comparatorByStartTime());
    timePeriodSet.addAll(getMap().values());
    return timePeriodSet;
  }

  /**
   * Retrieve a TimePeriod by its xml Id
   * <p>
   * This method is not efficient, since it loops through all the registered time periods in order to find the required time period.
   *
   * @param xmlId the XML Id of the specified time period
   * @return the retrieved time period, or null if no time period was found
   */
  public TimePeriod getByXmlId(final String xmlId) {
    return firstMatch(timePeriod -> xmlId.equals(timePeriod.getXmlId()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TimePeriodsFactory getFactory() {
    return this.factory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TimePeriods shallowClone() {
    return new TimePeriods(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  public TimePeriods deepClone() {
    return new TimePeriods(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TimePeriods deepCloneWithMapping(BiConsumer<TimePeriod, TimePeriod> mapper) {
    return new TimePeriods(this, true, mapper);
  }
}
