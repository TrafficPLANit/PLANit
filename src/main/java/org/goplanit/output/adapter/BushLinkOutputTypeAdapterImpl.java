package org.goplanit.output.adapter;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.output.enums.OutputType;
import org.goplanit.output.property.OutputProperty;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.NetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.network.layer.physical.LinkSegments;
import org.goplanit.utils.time.TimePeriod;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * Top-level abstract class which defines the common methods required by bush link output type adapters.
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
    extends UntypedNetworkLinkOutputTypeAdapterImpl<EdgeSegment> implements BushLinkOutputTypeAdapter {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(BushLinkOutputTypeAdapterImpl.class.getCanonicalName());

  /**
   * Constructor
   *
   * @param outputType        the OutputType this adapter corresponds to
   * @param trafficAssignment TrafficAssignment object which this adapter wraps
   */
  public BushLinkOutputTypeAdapterImpl(OutputType outputType, TrafficAssignment trafficAssignment) {
    super(outputType, trafficAssignment);
  }

}
