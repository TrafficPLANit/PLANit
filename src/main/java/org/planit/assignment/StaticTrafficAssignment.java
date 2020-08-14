package org.planit.assignment;

import java.util.Calendar;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Logger;

import org.planit.output.adapter.OutputTypeAdapter;
import org.planit.output.enums.OutputType;
import org.planit.time.TimePeriod;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.misc.LoggingUtils;
import org.planit.utils.network.physical.Mode;

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
    executeTimePeriod(timePeriod, demands.getRegisteredModesForTimePeriod(timePeriod));
    LOGGER.info(LoggingUtils.createRunIdPrefix(getId()) + String.format("run time: %d milliseconds", startTime.getTimeInMillis() - initialStartTime.getTimeInMillis()));
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
   * Execute equilibration over all time periods and modes
   *
   * @throws PlanItException thrown if there is an error
   */
  @Override
  public void executeEquilibration() throws PlanItException {
    // perform assignment per period - per mode
    final Collection<TimePeriod> timePeriods = demands.timePeriods.asSortedSetByStartTime();
    LOGGER.info(LoggingUtils.createRunIdPrefix(getId()) + "total time periods: " + timePeriods.size());
    for (final TimePeriod timePeriod : timePeriods) {
      LOGGER.info(LoggingUtils.createRunIdPrefix(getId()) + LoggingUtils.createTimePeriodPrefix(timePeriod.getExternalId(), timePeriod.getId()) + timePeriod.toString());
      executeTimePeriod(timePeriod);
    }
  }

}
