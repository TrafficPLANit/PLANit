package org.goplanit.assignment.ltm.sltm.output;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.assignment.common.bush.RootedBush;
import org.goplanit.assignment.ltm.sltm.*;
import org.goplanit.assignment.common.bush.ConjugateDestinationBush;
import org.goplanit.assignment.ltm.sltm.common.StaticLtmType;
import org.goplanit.assignment.ltm.sltm.StaticLtmConjugateBushStrategy;
import org.goplanit.output.adapter.traits.BushNetworkSegmentsOutputTypeAdapterTraitImpl;
import org.goplanit.utils.graph.GraphEntities;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.network.layer.physical.UntypedPhysicalLayer;

import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Adapter trait implementation for bush links
 *
 * @author markr
 *
 */
public class StaticLtmBushLinkOutputTypeAdapterTraitImpl extends BushNetworkSegmentsOutputTypeAdapterTraitImpl {

  /** logger to use */
  private static final Logger LOGGER =
          Logger.getLogger(StaticLtmBushLinkOutputTypeAdapterTraitImpl.class.getCanonicalName());

  /**
   * Constructor
   *
   * @param trafficAssignment the traffic assignment used to provide the data
   */
  public StaticLtmBushLinkOutputTypeAdapterTraitImpl(final TrafficAssignment trafficAssignment) {
    super(trafficAssignment);
  }

  /**
   * Access to sLTM assignment stratgy
   *
   * @return assignment strategy
   */
  @SuppressWarnings("unchecked")
  public StaticLtmBushStrategyBase<?,?,?,?> getBushBasedAssignmentStrategy(){
    return (StaticLtmBushStrategyBase<? extends DirectedVertex, ? extends DirectedEdge,? extends LinkSegment, ?>)
            ((StaticLtm)trafficAssignment).getAssignmentStrategy();
  }

  @Override
  public Optional<Long> getInfrastructureLayerIdForMode(Mode mode) {
    var sltmType = ((StaticLtm)trafficAssignment).getType();
    UntypedPhysicalLayer<?,?,? extends LinkSegment> layer = null;

    var originalLayer = super.getDefaultInfrastructureLayerForMode(mode);
    if(sltmType.equals(StaticLtmType.CONJUGATE_DESTINATION_BUSH_BASED)) {
      // conjugate bushes reside on conjugate network, so use conjugate network instead by matching to
      // mode supporting layer on original network, and using that conjugate layer
      var conjugateLayer =
          ((StaticLtmConjugateBushStrategy) getBushBasedAssignmentStrategy()).getConjugateTransportModelNetwork().
              getInfrastructureNetwork().getLayerByMode(mode);
        return Optional.of(conjugateLayer != null ? conjugateLayer.getId() : null);
    }else{
      LOGGER.severe(String.format(
          "Chosen sLTM type %s not compatible with bush based link results", sltmType));
      return null;
    }
  }

  /**
   * Obtain the link segments for the given layer from the bush compatible network. To obtain just the link segments
   * for a given bush, these need to be filtered by traversing each bush and verifying whether the links are part of
   * that bush.
   *
   * @param layerId to collect link segments for used by the bushes
   * @return bush compatible link segments (not reduced to a specific bush yet)
   */
  @Override
  public GraphEntities<? extends EdgeSegment> getLinkSegmentsForLayer(long layerId) {
    var sltmType = ((StaticLtm)trafficAssignment).getType();
    UntypedPhysicalLayer<?,?,? extends LinkSegment> layer = null;
    if(sltmType.equals(StaticLtmType.CONJUGATE_DESTINATION_BUSH_BASED)) {
      // conjugate bushes reside on conjugate network, so use conjugate network link segments
      return
          ((StaticLtmConjugateBushStrategy) getBushBasedAssignmentStrategy()).getConjugateTransportModelNetwork().
              getInfrastructureNetwork().getTransportLayers().get(layerId).getLinkSegments();
    }else{
      LOGGER.severe(String.format(
          "Chosen sLTM type %s not compatible with bush based link results", sltmType));
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public Set<RootedBush<? extends DirectedVertex, ? extends DirectedEdge, ? extends EdgeSegment>> getBushes() {
    return (Set<RootedBush<? extends DirectedVertex, ? extends DirectedEdge, ? extends EdgeSegment>>)
            getBushBasedAssignmentStrategy().getBushes();
  }

  @Override
  public boolean hasNonZeroFlow(
      RootedBush<? extends DirectedVertex, ? extends DirectedEdge, ? extends EdgeSegment> bush,
      EdgeSegment edgeSegment) {
    var sltmType = ((StaticLtm)trafficAssignment).getType();
    if(sltmType.equals(StaticLtmType.CONJUGATE_DESTINATION_BUSH_BASED)) {
      // conjugate bushes use conjugate edge segments
      return
          ((ConjugateDestinationBush) bush).getSendingFlowPcuH((ConjugateEdgeSegment) edgeSegment) > 0;
    }else{
      LOGGER.severe(String.format(
          "Chosen sLTM type %s not compatible with bush based link result non zero flow check", sltmType));
      return false;
    }
  }
}
