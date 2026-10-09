package org.goplanit.output.adapter;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.output.enums.OutputType;
import org.goplanit.output.property.OutputProperty;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.network.layer.physical.LinkSegment;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * Top-level abstract class which defines the common methods required by Link output type adapters
 * 
 * @author gman6028, markr
 * @param <LS> type of segment
 */
public abstract class UntypedLinkOutputTypeAdapterImpl<LS extends LinkSegment>
    extends UntypedEdgeOutputTypeAdapterImpl<LS> implements UntypedLinkOutputTypeAdapter<LS> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(UntypedLinkOutputTypeAdapterImpl.class.getCanonicalName());

  /**
   * Constructor
   *
   * @param outputType        the OutputType this adapter corresponds to
   * @param trafficAssignment TrafficAssignment object which this adapter wraps
   */
  public UntypedLinkOutputTypeAdapterImpl(OutputType outputType, TrafficAssignment trafficAssignment) {
    super(outputType, trafficAssignment);
  }

  /**
   * Return the value of a specified output property of a link segment
   * 
   * 
   * @param outputProperty the specified output property
   * @param linkSegment    the specified link segment
   * @return the value of the specified output property (or an Exception message if an error occurs)
   */
  @Override
  public Optional<?> getLinkSegmentOutputPropertyValue(OutputProperty outputProperty, LS linkSegment) {

    Optional<?> result = super.getEdgeSegmentOutputPropertyValue(outputProperty, linkSegment);
    if(result.isPresent()){
      return result;
    }

    try {
      switch (outputProperty.getOutputPropertyType()) {
        case NUMBER_OF_LANES:
          result = getNumberOfLanes(linkSegment);
          break;
        default:
      }

      if (outputProperty.supportsUnitOverride() && outputProperty.isUnitOverride()) {
        result = createConvertedUnitsValue(outputProperty, result);
      }
    } catch (PlanItException e) {
      result = Optional.of(e.getMessage());
    }
    return result;
  }

}
