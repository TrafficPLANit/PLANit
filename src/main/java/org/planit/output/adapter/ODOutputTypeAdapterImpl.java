package org.planit.output.adapter;

import java.util.logging.Logger;

import org.planit.exceptions.PlanItException;
import org.planit.od.odmatrix.ODMatrixIterator;
import org.planit.output.enums.ODSkimSubOutputType;
import org.planit.output.enums.OutputType;
import org.planit.output.enums.SubOutputTypeEnum;
import org.planit.output.property.BaseOutputProperty;
import org.planit.output.property.OutputProperty;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.utils.network.physical.Mode;

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
   * Returns the external Id of the destination zone for the current cell in the OD skim matrix
   * 
   * @param odMatrixIterator ODMatrixIterator object containing the required data
   * @return the external Id of the destination zone for the current cell in the OD skim matrix
   * @throws PlanItException thrown if there is an error
   */
  protected Object getDestinationZoneExternalId(ODMatrixIterator odMatrixIterator) throws PlanItException {
    return odMatrixIterator.getCurrentDestination().getExternalId();
  }

  /**
   * Returns the Id of the destination zone for the current cell in the OD skim matrix
   * 
   * @param odMatrixIterator ODMatrixIterator object containing the required data
   * @return the Id of the destination zone for the current cell in the OD skim matrix
   * @throws PlanItException thrown if there is an error
   */
  protected long getDestinationZoneId(ODMatrixIterator odMatrixIterator) throws PlanItException {
    return odMatrixIterator.getCurrentDestination().getId();
  }

  /**
   * Returns the origin zone external Id for the current cell in the OD skim matrix
   * 
   * @param odMatrixIterator ODMatrixIterator object containing the required data
   * @return the origin zone external Id for the current cell in the OD skim matrix
   * @throws PlanItException thrown if there is an error
   */
  protected Object getOriginZoneExternalId(ODMatrixIterator odMatrixIterator) throws PlanItException {
    return odMatrixIterator.getCurrentOrigin().getExternalId();
  }

  /**
   * Returns the origin zone Id for the current cell in the OD skim matrix
   * 
   * @param odMatrixIterator ODMatrixIterator object containing the required data
   * @return the origin zone Id for the current cell in the OD skim matrix
   * @throws PlanItException thrown if there is an error
   */
  protected long getOriginZoneId(ODMatrixIterator odMatrixIterator) throws PlanItException {
    return odMatrixIterator.getCurrentOrigin().getId();
  }

  /**
   * Returns the OD travel cost for the current cell in the OD skim matrix
   * 
   * @param odMatrixIterator   ODMatrixIterator object containing the required data
   * @param timeUnitMultiplier multiplier to convert time durations to hours, minutes or seconds
   * @return the OD travel cost for the current cell in the OD skim matrix
   * @throws PlanItException thrown if there is an error
   */
  protected double getODCost(ODMatrixIterator odMatrixIterator, double timeUnitMultiplier) throws PlanItException {
    return odMatrixIterator.getCurrentValue() * timeUnitMultiplier;
  }

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
  @Override
  public Object getODOutputPropertyValue(OutputProperty outputProperty, ODMatrixIterator odMatrixIterator, Mode mode, TimePeriod timePeriod, double timeUnitMultiplier) {
    try {
      Object obj = getCommonPropertyValue(outputProperty, mode, timePeriod);
      if (obj != null) {
        return obj;
      }
      switch (outputProperty) {
      case DESTINATION_ZONE_EXTERNAL_ID:
        return getDestinationZoneExternalId(odMatrixIterator);
      case DESTINATION_ZONE_ID:
        return getDestinationZoneId(odMatrixIterator);
      case OD_COST:
        return getODCost(odMatrixIterator, timeUnitMultiplier);
      case ORIGIN_ZONE_EXTERNAL_ID:
        return getOriginZoneExternalId(odMatrixIterator);
      case ORIGIN_ZONE_ID:
        return getOriginZoneId(odMatrixIterator);
      default:
        return new PlanItException(
            "Tried to find link property of " + BaseOutputProperty.convertToBaseOutputProperty(outputProperty).getName() + " which is not applicable for OD matrix.");
      }
    } catch (PlanItException e) {
      return e;
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
  public int getIterationIndexForSubOutputType(SubOutputTypeEnum outputTypeEnum) throws PlanItException {
    PlanItException.throwIf(!(outputTypeEnum instanceof ODSkimSubOutputType), "Incorrect outputType enum found when collecting iteration index");

    int iterationIndex = trafficAssignment.getSimulationData().getIterationIndex();
    switch ((ODSkimSubOutputType) outputTypeEnum) {
    case COST:
      // cost is collected through the shortest path in iteration i based on the link costs of
      // iteration i-1, so the od cost
      // is trailing behind
      // one iteration, hence, we must reduce the iteration index by 1
      return iterationIndex - 1;
    case NONE:
      return iterationIndex;
    default:
      throw new PlanItException("Unknown ODSkimOutputType enum encountered when collecting iteration index");
    }
  }

}
