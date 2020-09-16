package org.planit.output.adapter;

import org.planit.assignment.TrafficAssignment;
import org.planit.output.enums.OutputType;
import org.planit.output.enums.SubOutputTypeEnum;
import org.planit.output.property.IterationIndexOutputProperty;
import org.planit.output.property.ModeExternalIdOutputProperty;
import org.planit.output.property.ModeIdOutputProperty;
import org.planit.output.property.OutputProperty;
import org.planit.output.property.RunIdOutputProperty;
import org.planit.output.property.TimePeriodExternalIdOutputProperty;
import org.planit.output.property.TimePeriodIdOutputProperty;
import org.planit.time.TimePeriod;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.mode.Mode;

/**
 * Top-level abstract class which defines the common methods required by all output type adapters
 * 
 * @author gman6028
 *
 */
public abstract class OutputTypeAdapterImpl implements OutputTypeAdapter {

  /**
   * the traffic assignment this output adapter is drawing from
   */
  protected TrafficAssignment trafficAssignment;

  /**
   * The OutputType this OutputTypeAdapter is used for
   */
  protected OutputType outputType;

  /**
   * Returns the value of properties which are common to all output type adapters
   * 
   * @param outputProperty the specified output property
   * @param mode           the current mode
   * @param timePeriod     the current time period
   * @return the value of the specified property, or null if the specified property is not common to all output adapters (or an Exception if an error has occurred)
   */
  protected Object getCommonPropertyValue(OutputProperty outputProperty, Mode mode, TimePeriod timePeriod) {
    try {
      switch (outputProperty) {
      case MODE_EXTERNAL_ID:
        return ModeExternalIdOutputProperty.getModeExternalId(mode);
      case MODE_ID:
        return ModeIdOutputProperty.getModeId(mode);
      case RUN_ID:
        return RunIdOutputProperty.getRunId(trafficAssignment);
      case TIME_PERIOD_EXTERNAL_ID:
        return TimePeriodExternalIdOutputProperty.getTimePeriodExternalId(timePeriod);
      case TIME_PERIOD_ID:
        return TimePeriodIdOutputProperty.getTimePeriodId(timePeriod);
      case ITERATION_INDEX:
        return IterationIndexOutputProperty.getIterationIndex(trafficAssignment);
      default:
        return null;
      }
    } catch (PlanItException e) {
      return e;
    }
  }

  /**
   * Constructor
   * 
   * @param outputType        the OutputType this adapter corresponds to
   * @param trafficAssignment TrafficAssignment object which this adapter wraps
   */
  public OutputTypeAdapterImpl(OutputType outputType, TrafficAssignment trafficAssignment) {
    this.outputType = outputType;
    this.trafficAssignment = trafficAssignment;
  }

  /**
   * Return the output type corresponding to this output adapter
   * 
   * @return the output type corresponding to this output adapter
   */
  public OutputType getOutputType() {
    return outputType;
  }

  /**
   * Default implementation assumes that regular iteration index is used, which in most cases it true, only when for example costs are trailing one iteration behind in case they
   * are only revealed in the next iteration this method should be overridden
   */
  @Override
  public int getIterationIndexForSubOutputType(SubOutputTypeEnum outputTypeEnum) throws PlanItException {
    return trafficAssignment.getIterationIndex();
  }

}
