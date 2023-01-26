package org.goplanit.demands;

import org.goplanit.time.TimePeriodImpl;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.time.TimePeriod;
import org.goplanit.utils.time.TimePeriodUtils;
import org.goplanit.utils.wrapper.LongMapWrapperImpl;

import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class to register and store time periods for the current demand object
 * todo: conform to managed id container
 *
 * @author garym, markr
 */
public class TimePeriods extends LongMapWrapperImpl<TimePeriod> {

  private final Demands demands;

  /**
   * Constructor
   */
  public TimePeriods(Demands demands) {
    super(new HashMap<>(), TimePeriod::getId);
    this.demands = demands;
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public TimePeriods(TimePeriods other, boolean deepCopy) {
    super(other);
    this.demands = other.demands;

    if(deepCopy){
      clear();
      other.forEach( tp -> register(deepCopy ? tp.deepClone() : tp));
    }
  }

  /**
   * Factory method to create and register a new time period on the demands
   *
   * @param description      the description for this time period
   * @param startTimeSeconds the start time in seconds since midnight (00:00)
   * @param durationSeconds  the duration in seconds since start time
   * @return new time period created
   * @throws PlanItException thrown if start time and/or duration are invalid
   */
  public TimePeriod createAndRegisterNewTimePeriod(String description, long startTimeSeconds, long durationSeconds) throws PlanItException {
    var newTimePeriod = new TimePeriodImpl(demands.getIdGroupingToken(), description, startTimeSeconds, durationSeconds);
    register(newTimePeriod);
    return newTimePeriod;
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
    return findFirst(timePeriod -> xmlId.equals(((TimePeriod) timePeriod).getXmlId()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TimePeriods clone() {
    return new TimePeriods(this, false);
  }

  /**
   * Support deep clone --> once move to managed id this becomes mandatory override
   */
  public TimePeriods deepClone() {
    return new TimePeriods(this, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clear() {
    super.clear();
    IdGenerator.reset(demands.getIdGroupingToken(), TimePeriod.class);
  }
}
