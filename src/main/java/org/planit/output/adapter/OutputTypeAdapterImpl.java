package org.planit.output.adapter;

import java.util.Optional;

import org.planit.assignment.TrafficAssignment;
import org.planit.output.enums.OutputType;
import org.planit.output.enums.SubOutputTypeEnum;
import org.planit.output.property.IterationIndexOutputProperty;
import org.planit.output.property.ModeExternalIdOutputProperty;
import org.planit.output.property.ModeIdOutputProperty;
import org.planit.output.property.ModeXmlIdOutputProperty;
import org.planit.output.property.OutputProperty;
import org.planit.output.property.RunIdOutputProperty;
import org.planit.output.property.TimePeriodExternalIdOutputProperty;
import org.planit.output.property.TimePeriodIdOutputProperty;
import org.planit.output.property.TimePeriodXmlIdOutputProperty;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.mode.Mode;
import org.planit.utils.time.TimePeriod;

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
  private TrafficAssignment trafficAssignment;

  /**
   * The OutputType this OutputTypeAdapter is used for
   */
  protected OutputType outputType;

  /**
   * Access the assignment
   * 
   * @return the assignment
   */
  protected TrafficAssignment getAssignment() {
    return trafficAssignment;
  }

  /**
   * Convert the output property value that is assumed to be in the properties default units in the desired units indicated on the property
   * 
   * @param outputProperty   to base conversion on
   * @param unconvertedValue original value in default units
   * @return converted value as optional
   * @throws PlanItException thrown if error
   */
  protected static Optional<?> createConvertedUnitsValue(OutputProperty outputProperty, Optional<?> unconvertedValue) throws PlanItException {
    if (unconvertedValue.isPresent()) {
      return Optional.of(outputProperty.getDefaultUnit().convertTo(outputProperty.getOverrideUnit(), (double) unconvertedValue.get()));
    }
    return unconvertedValue;
  }

  /**
   * Returns the value of properties which are common to all output type adapters
   * 
   * @param outputProperty the specified output property
   * @param mode           the current mode
   * @param timePeriod     the current time period
   * @return the value of the specified property, or null if the specified property is not common to all output adapters (or an Exception message if an error has occurred)
   */
  protected Optional<?> getOutputTypeIndependentPropertyValue(OutputProperty outputProperty, Mode mode, TimePeriod timePeriod) {
    try {
      switch (outputProperty.getOutputPropertyType()) {
      case MODE_EXTERNAL_ID:
        return ModeExternalIdOutputProperty.getModeExternalId(mode);
      case MODE_XML_ID:
        return ModeXmlIdOutputProperty.getModeXmlId(mode);
      case MODE_ID:
        return ModeIdOutputProperty.getModeId(mode);
      case RUN_ID:
        return RunIdOutputProperty.getRunId(trafficAssignment);
      case TIME_PERIOD_EXTERNAL_ID:
        return TimePeriodExternalIdOutputProperty.getTimePeriodExternalId(timePeriod);
      case TIME_PERIOD_XML_ID:
        return TimePeriodXmlIdOutputProperty.getTimePeriodXmlId(timePeriod);
      case TIME_PERIOD_ID:
        return TimePeriodIdOutputProperty.getTimePeriodId(timePeriod);
      case ITERATION_INDEX:
        return IterationIndexOutputProperty.getIterationIndex(trafficAssignment);
      default:
        return Optional.empty();
      }
    } catch (PlanItException e) {
      return Optional.of(e.getMessage());
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
  public Optional<Integer> getIterationIndexForSubOutputType(SubOutputTypeEnum outputTypeEnum) throws PlanItException {
    return Optional.of(trafficAssignment.getIterationIndex());
  }

}
