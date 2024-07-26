package org.goplanit.assignment;

import java.util.Calendar;
import java.util.Set;
import java.util.logging.Logger;

import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.time.TimePeriod;

/**
 * A static traffic assignment class with some commonalities implemented shared across static assignment implementations
 * 
 * @author markr
 *
 */
public abstract class StaticTrafficAssignment extends TrafficAssignment {

  /**
   * serial UID
   */
  private static final long serialVersionUID = -2600601431539929608L;

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(StaticTrafficAssignment.class.getCanonicalName());

  /** track most recent time period that is being simulated */
  protected TimePeriod mostRecentTimePeriod = null;

  /**
   * Execute the time period for the registered modes
   * 
   * @param timePeriod to execute
   * @param modes      to consider
   * @throws PlanItException thrown if error
   */
  protected abstract void executeTimePeriod(final TimePeriod timePeriod, final Set<Mode> modes) throws PlanItException;

  /**
   * Perform assignment for a given time period
   *
   * @param timePeriod the time period for the current assignment
   * @throws PlanItException thrown if there is an error
   */
  protected void executeTimePeriod(final TimePeriod timePeriod) throws PlanItException {
    Calendar startTime = Calendar.getInstance();
    final Calendar initialStartTime = startTime;
    executeTimePeriod(timePeriod, getDemands().getRegisteredModesForTimePeriod(timePeriod));
    LOGGER.info(LoggingUtils.runIdPrefix(getId()) + String.format("run time: %d milliseconds", startTime.getTimeInMillis() - initialStartTime.getTimeInMillis()));
  }

  /**
   * Constructor
   * 
   * @param groupId for id generation
   */
  protected StaticTrafficAssignment(IdGroupingToken groupId) {
    super(groupId);
  }

  /**
   * Copy Constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a eep copy, shallow copy otherwise
   */
  protected StaticTrafficAssignment(StaticTrafficAssignment other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * Execute equilibration over all time periods and modes
   *
   * @throws PlanItException thrown if there is an error
   */
  @Override
  public void executeEquilibration() throws PlanItException {
    // perform assignment per period - per mode
    final var timePeriods = getDemands().timePeriods.asSortedSetByStartTime();
    LOGGER.info(LoggingUtils.runIdPrefix(getId()) + "total time periods: " + timePeriods.size());
    for (var timePeriod : timePeriods) {
      LOGGER.info(LoggingUtils.runIdPrefix(getId()) + LoggingUtils.timePeriodPrefix(timePeriod) + timePeriod.toString());
      this.mostRecentTimePeriod = timePeriod;
      executeTimePeriod(timePeriod);
    }
  }

  /**
   * Provide access to current/most recently simulated time period
   *
   * @return time period
   */
  @Override
  public TimePeriod getTimePeriod() {
    return this.mostRecentTimePeriod;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract StaticTrafficAssignment shallowClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract StaticTrafficAssignment deepClone();

}
