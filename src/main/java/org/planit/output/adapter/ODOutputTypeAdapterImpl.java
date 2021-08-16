package org.planit.output.adapter;

import java.util.Optional;
import java.util.logging.Logger;

import org.planit.assignment.TrafficAssignment;
import org.planit.output.enums.ODSkimSubOutputType;
import org.planit.output.enums.OutputType;
import org.planit.output.enums.SubOutputTypeEnum;
import org.planit.output.property.BaseOutputProperty;
import org.planit.output.property.OutputProperty;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.mode.Mode;
import org.planit.utils.od.OdDataIterator;
import org.planit.utils.time.TimePeriod;

/**
 * Top-level abstract class which defines the common methods required by OD output type adapters
 * 
 * @author gman6028
 *
 */
public abstract class ODOutputTypeAdapterImpl extends OutputTypeAdapterImpl implements ODOutputTypeAdapter {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(ODOutputTypeAdapterImpl.class.getCanonicalName());

  /**
   * Constructor
   * 
   * @param outputType        the output type for the current persistence
   * @param trafficAssignment the traffic assignment used to provide the data
   */
  public ODOutputTypeAdapterImpl(OutputType outputType, TrafficAssignment trafficAssignment) {
    super(outputType, trafficAssignment);
  }

  /**
   * Returns the specified output property values for the current cell in the OD Matrix Iterator
   * 
   * @param outputProperty     the specified output property
   * @param odMatrixIterator   the iterator through the current OD Matrix
   * @param mode               the current mode
   * @param timePeriod         the current time period
   * @param timeUnitMultiplier the multiplier for time units
   * @return the value of the specified property (or an Exception if an error has occurred)
   */
  @SuppressWarnings("unchecked")
  @Override
  public Optional<?> getODOutputPropertyValue(OutputProperty outputProperty, OdDataIterator<?> odIterator, Mode mode, TimePeriod timePeriod, double timeUnitMultiplier) {
    try {
      Optional<?> value = getOutputTypeIndependentPropertyValue(outputProperty, mode, timePeriod);
      if (value.isPresent()) {
        return value;
      }

      switch (outputProperty) {
      case DESTINATION_ZONE_EXTERNAL_ID:
        return ODOutputTypeAdapter.getDestinationZoneExternalId(odIterator);
      case DESTINATION_ZONE_XML_ID:
        return ODOutputTypeAdapter.getDestinationZoneXmlId(odIterator);
      case DESTINATION_ZONE_ID:
        return ODOutputTypeAdapter.getDestinationZoneId(odIterator);
      case OD_COST:
        return ODOutputTypeAdapter.getOdValueMultipliedWith((OdDataIterator<Double>) odIterator, timeUnitMultiplier);
      case ORIGIN_ZONE_EXTERNAL_ID:
        return ODOutputTypeAdapter.getOriginZoneExternalId(odIterator);
      case ORIGIN_ZONE_XML_ID:
        return ODOutputTypeAdapter.getOriginZoneXmlId(odIterator);
      case ORIGIN_ZONE_ID:
        return ODOutputTypeAdapter.getOriginZoneId(odIterator);
      default:
        return Optional
            .of(String.format("Tried to find link property of %s which is not applicable for OD matrix", BaseOutputProperty.convertToBaseOutputProperty(outputProperty).getName()));
      }
    } catch (PlanItException e) {
      return Optional.of(e.getMessage());
    }
  }

  /**
   * ODSkimOutputType.COST: Cost is collected through the shortest path in iteration i based on the link costs of iteration i-1, so the od cost of i-1 are only known once we are in
   * iteration i, hence this information is trailing behind one iteration, and we can only store it in i. Hence, we must reduce the iteration index by 1 to obtain the true
   * iteration index that goes with this information.
   * 
   * all other od information is based on the actual iteration index and will return i
   */
  @Override
  public Optional<Integer> getIterationIndexForSubOutputType(SubOutputTypeEnum outputTypeEnum) throws PlanItException {
    PlanItException.throwIf(!(outputTypeEnum instanceof ODSkimSubOutputType), "Incorrect outputType enum found when collecting iteration index");

    int iterationIndex = trafficAssignment.getIterationIndex();
    switch ((ODSkimSubOutputType) outputTypeEnum) {
    case COST:
      // cost is collected through the shortest path in iteration i based on the link costs of
      // iteration i-1, so the od cost
      // is trailing behind
      // one iteration, hence, we must reduce the iteration index by 1
      return Optional.of(iterationIndex - 1);
    case NONE:
      return Optional.of(iterationIndex);
    default:
      throw new PlanItException("Unknown ODSkimOutputType enum encountered when collecting iteration index");
    }
  }

}
