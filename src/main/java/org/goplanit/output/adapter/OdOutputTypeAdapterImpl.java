package org.goplanit.output.adapter;

import java.util.Optional;
import java.util.logging.Logger;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.output.enums.OdSkimSubOutputType;
import org.goplanit.output.enums.OutputType;
import org.goplanit.output.enums.SubOutputTypeEnum;
import org.goplanit.output.property.OutputProperty;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.od.OdDataIterator;
import org.goplanit.utils.time.TimePeriod;

/**
 * Top-level abstract class which defines the common methods required by OD output type adapters
 * 
 * @author gman6028
 *
 */
public abstract class OdOutputTypeAdapterImpl extends OutputTypeAdapterImpl implements OdOutputTypeAdapter {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(OdOutputTypeAdapterImpl.class.getCanonicalName());

  /**
   * Constructor
   * 
   * @param outputType        the output type for the current persistence
   * @param trafficAssignment the traffic assignment used to provide the data
   */
  public OdOutputTypeAdapterImpl(OutputType outputType, TrafficAssignment trafficAssignment) {
    super(outputType, trafficAssignment);
  }

  /**
   * Returns the specified output property values for the current cell in the OD Matrix Iterator
   * 
   * @param outputProperty the specified output property
   * @param odIterator     the iterator through the current OD Matrix
   * @param mode           the current mode
   * @param timePeriod     the current time period
   * @return the value of the specified property (or an Exception if an error has occurred)
   */
  @Override
  public Optional<?> getOdOutputPropertyValue(OutputProperty outputProperty, OdDataIterator<?> odIterator, Mode mode, TimePeriod timePeriod) {
    Optional<?> value = Optional.empty();

    try {
      value = getOutputTypeIndependentPropertyValue(outputProperty, mode, timePeriod);
      if (value.isPresent()) {
        return value;
      }

      switch (outputProperty.getOutputPropertyType()) {
      case DESTINATION_ZONE_EXTERNAL_ID:
        value = OdOutputTypeAdapter.getDestinationZoneExternalId(odIterator);
        break;
      case DESTINATION_ZONE_XML_ID:
        value = OdOutputTypeAdapter.getDestinationZoneXmlId(odIterator);
        break;
      case DESTINATION_ZONE_ID:
        value = OdOutputTypeAdapter.getDestinationZoneId(odIterator);
        break;
      case OD_COST:
        value = OdOutputTypeAdapter.getOdValue(odIterator);
        break;
      case ORIGIN_ZONE_EXTERNAL_ID:
        value = OdOutputTypeAdapter.getOriginZoneExternalId(odIterator);
        break;
      case ORIGIN_ZONE_XML_ID:
        value = OdOutputTypeAdapter.getOriginZoneXmlId(odIterator);
        break;
      case ORIGIN_ZONE_ID:
        value = OdOutputTypeAdapter.getOriginZoneId(odIterator);
        break;
      default:
        throw new PlanItException("Tried to find link property of %s which is not applicable for OD matrix", outputProperty.getName());
      }

      if (outputProperty.supportsUnitOverride() && outputProperty.isUnitOverride()) {
        value = createConvertedUnitsValue(outputProperty, value);
      }
    } catch (PlanItException e) {
      value = Optional.of(e.getMessage());
    }

    return value;
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
    PlanItException.throwIf(!(outputTypeEnum instanceof OdSkimSubOutputType), "Incorrect outputType enum found when collecting iteration index");

    int iterationIndex = getAssignment().getIterationIndex();
    switch ((OdSkimSubOutputType) outputTypeEnum) {
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
