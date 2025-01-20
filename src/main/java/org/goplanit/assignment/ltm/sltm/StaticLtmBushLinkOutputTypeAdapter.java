package org.goplanit.assignment.ltm.sltm;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.assignment.ltm.sltm.conjugate.StaticLtmConjugateBushStrategy;
import org.goplanit.network.UntypedPhysicalNetwork;
import org.goplanit.network.transport.UntypedTransportModelNetwork;
import org.goplanit.output.adapter.BushLinkOutputTypeAdapterImpl;
import org.goplanit.output.enums.OutputType;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.GraphEntities;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.network.layer.physical.PhysicalLayer;
import org.goplanit.utils.network.layer.physical.UntypedPhysicalLayer;

import java.util.Collection;
import java.util.logging.Logger;

/**
 * Adapter providing access to the data of the StaticLtm class relevant for bush specific link outputs
 * without exposing the internals of the traffic assignment class itself
 *
 * @author markr
 *
 */
public class StaticLtmBushLinkOutputTypeAdapter extends BushLinkOutputTypeAdapterImpl {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(StaticLtmBushLinkOutputTypeAdapter.class.getCanonicalName());

  @SuppressWarnings("unchecked")
  protected StaticLtmBushStrategyBase<?,?,?> getBushBasedAssignmentStrategy(){
    return (StaticLtmBushStrategyBase<? extends DirectedVertex,?,? extends LinkSegment>)
        getAssignment().getAssignmentStrategy();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected StaticLtm getAssignment() {
    return (StaticLtm) super.getAssignment();
  }

  /**
   * Constructor
   *
   * @param outputType        the output type for the current persistence
   * @param trafficAssignment the traffic assignment used to provide the data
   */
  public StaticLtmBushLinkOutputTypeAdapter(
      final OutputType outputType,
      final TrafficAssignment trafficAssignment) {
    super(outputType, trafficAssignment);
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
  public GraphEntities<? extends LinkSegment> getLinkSegmentsForLayer(long layerId) {
    var sltmType = getAssignment().settings.getSltmType();
    UntypedPhysicalLayer<?,?,? extends LinkSegment> layer = null;
    if(sltmType.equals(StaticLtmType.DESTINATION_BUSH_BASED)){
      // destination based bushes reside on regular network, so collect those link segments
      layer = getAssignment().getTransportNetwork().getInfrastructureNetwork().getTransportLayers().get(layerId);
    }else if(sltmType.equals(StaticLtmType.CONJUGATE_DESTINATION_BUSH_BASED)) {
      // conjugate bushes reside on conjugate network, so use conjugate network link segments
      layer =
          ((StaticLtmConjugateBushStrategy) getBushBasedAssignmentStrategy()).getConjugateTransportModelNetwork().
              getInfrastructureNetwork().getTransportLayers().get(layerId);
    }else{
      LOGGER.severe(String.format(
          "Chosen sLTM type %s not compatible with bush based link results", sltmType));
      return null;
    }
    return layer.getLinkSegments();
  }

  @Override
  public RootedBush<? extends DirectedVertex, ? extends EdgeSegment>[] getBushes() {
    return getBushBasedAssignmentStrategy().getBushes();
  }
}
