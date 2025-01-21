package org.goplanit.assignment.ltm.sltm;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.assignment.ltm.sltm.conjugate.StaticLtmConjugateBushStrategy;
import org.goplanit.network.UntypedPhysicalNetwork;
import org.goplanit.network.transport.UntypedTransportModelNetwork;
import org.goplanit.output.adapter.BushLinkOutputTypeAdapterImpl;
import org.goplanit.output.adapter.traits.UntypedBushSegmentsOutputTypeAdapterTrait;
import org.goplanit.output.enums.OutputType;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.GraphEntities;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.network.layer.physical.PhysicalLayer;
import org.goplanit.utils.network.layer.physical.UntypedPhysicalLayer;

import java.util.Collection;
import java.util.Optional;
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

  /**
   * {@inheritDoc}
   */
  @Override
  protected StaticLtm getAssignment() {
    return (StaticLtm) super.getAssignment();
  }

  /**
   * Access to trait
   */
  @Override
  protected StaticLtmBushLinkOutputTypeAdapterTraitImpl getTrait(){
    return (StaticLtmBushLinkOutputTypeAdapterTraitImpl) super.getTrait();
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
    super(outputType,
            trafficAssignment,
            new StaticLtmBushLinkOutputTypeAdapterTraitImpl(trafficAssignment));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<Long> getInfrastructureLayerIdForMode(Mode mode) {
    return getTrait().getInfrastructureLayerIdForMode(mode);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public GraphEntities<? extends EdgeSegment> getLinkSegmentsForLayer(long layerId) {
    // delegate to trait as StaticLTM version may does use default trait implementation as conjugate
    // bushes are not based on regular network link segments but on conjugate segments. Trait takes care
    // of this distinction
    return getTrait().getLinkSegmentsForLayer(layerId);
  }

}
