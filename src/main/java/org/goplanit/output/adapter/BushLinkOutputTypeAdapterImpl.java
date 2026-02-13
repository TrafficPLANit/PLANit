package org.goplanit.output.adapter;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.assignment.common.bush.RootedBush;
import org.goplanit.output.adapter.traits.UntypedBushSegmentsOutputTypeAdapterTrait;
import org.goplanit.output.enums.OutputType;
import org.goplanit.output.property.OutputProperty;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.time.TimePeriod;

import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Top-level class which defines the common methods required by bush link output type adapters.
 * Specifically designed for the use in conjunction with bushes and their link segments.
 * <p>
 *   Currently no additional options compared to the regular network link output type adapter are supported hence
 *   this class only masks its untyped counterpart, when new option are to be made available implement them here.
 * </p>
 *
 * @author markr
 *
 */
public abstract class BushLinkOutputTypeAdapterImpl
        extends UntypedEdgeOutputTypeAdapterImpl<EdgeSegment> implements BushLinkOutputTypeAdapter {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(BushLinkOutputTypeAdapterImpl.class.getCanonicalName());

  /** trait implementation */
  private final UntypedBushSegmentsOutputTypeAdapterTrait<? extends EdgeSegment> bushSegmentsTrait;

  /**
   * Access to trait
   *
   * @return trait
   */
  protected UntypedBushSegmentsOutputTypeAdapterTrait<? extends EdgeSegment> getTrait(){
    return bushSegmentsTrait;
  }

  /**
   * Constructor
   *
   * @param outputType        the OutputType this adapter corresponds to
   * @param trafficAssignment TrafficAssignment object which this adapter wraps
   * @param bushSegmentsTrait trait to provide access to bushes on adapter
   */
  public BushLinkOutputTypeAdapterImpl(
          OutputType outputType,
          TrafficAssignment trafficAssignment,
          UntypedBushSegmentsOutputTypeAdapterTrait<? extends EdgeSegment> bushSegmentsTrait) {
    super(outputType, trafficAssignment);
    this.bushSegmentsTrait = bushSegmentsTrait;
  }

  /**
   * Return the value of a specified output property of a link segment.
   * The DENSITY case should never be called for TraditionalStaticAssignment.
   *
   * @param outputProperty the specified output property
   * @param linkSegment    the specified link segment
   * @param mode           the current mode
   * @param timePeriod     the current time period
   * @return the value of the specified output property (or an Exception message if an error occurs)
   */
  @Override
  public Optional<?> getEdgeSegmentOutputPropertyValue(
          OutputProperty outputProperty, EdgeSegment linkSegment, Mode mode, TimePeriod timePeriod) {

    Optional<?> value = super.getOutputTypeIndependentPropertyValue(outputProperty, mode, timePeriod);
    if (value.isPresent()) {
      return value;
    }

    value = super.getEdgeSegmentOutputPropertyValue(outputProperty, linkSegment);
    if (value.isPresent()) {
      return value;
    }

    return Optional.empty();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<RootedBush<? extends DirectedVertex, ? extends EdgeSegment>> getBushes() {
    return (Set<RootedBush<? extends DirectedVertex, ? extends EdgeSegment>>) bushSegmentsTrait.getBushes();
  }
}
