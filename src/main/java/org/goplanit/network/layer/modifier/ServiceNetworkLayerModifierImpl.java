package org.goplanit.network.layer.modifier;

import org.goplanit.graph.directed.UntypedDirectedGraphImpl;
import org.goplanit.graph.modifier.DirectedGraphModifierImpl;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.graph.modifier.DirectedGraphModifier;
import org.goplanit.utils.graph.modifier.event.GraphModifierEventType;
import org.goplanit.utils.graph.modifier.event.GraphModifierListener;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.network.layer.modifier.ServiceNetworkLayerModifier;
import org.goplanit.utils.network.layer.modifier.UntypedDirectedGraphLayerModifier;
import org.goplanit.utils.network.layer.service.ServiceLeg;
import org.goplanit.utils.network.layer.service.ServiceLegSegment;
import org.goplanit.utils.network.layer.service.ServiceNode;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Modifier class for service network layer, added functionality for service networks to:
 * <ul>
 *   <li>remove unmapped service nodes, service legs, and service leg segments</li>
 * </ul>
 *
 * @author markr
 */
public class ServiceNetworkLayerModifierImpl<V extends ServiceNode, E extends ServiceLeg, S extends ServiceLegSegment> extends UntypedNetworkLayerModifierImpl<V, E, S> implements ServiceNetworkLayerModifier<V,E,S> {

  // INNER CLASSES

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(ServiceNetworkLayerModifierImpl.class.getCanonicalName());

  // PUBLIC

  /**
   * Constructor
   *
   * @param graphModifier parent graph modifier
   */
  public ServiceNetworkLayerModifierImpl(final DirectedGraphModifier graphModifier) {
    super(graphModifier);
  }

  /**
   * Constructor
   *
   * @param graph parent graph to base modifier on
   */
  public ServiceNetworkLayerModifierImpl(UntypedDirectedGraphImpl<V, E, S> graph) {
    super(graph);
  }

  /**
   * todo: implement by breaking service leg + update underlying physical link segments by assigning them to either part of broken service leg
   */
  @Override
  public Map<Long, Pair<E, E>> breakAt(List<E> serviceLegsToBreak, V serviceNodeToBreakAt, CoordinateReferenceSystem crs) {
    throw new PlanItRunTimeException("Not yet implemented");
  }

  /**
   * todo: implement by removing dangling service network subnetworks. But unclear what this means. What if service network is dangling but the underlying physical network is not
   * for now not yet supported. If we implement it in the strict sense, we can simply borrow the functionality from super class and remove override
   */
  @Override
  public void removeDanglingSubnetworks(final Integer belowSize, Integer aboveSize, boolean alwaysKeepLargest) throws PlanItException {
    throw new PlanItRunTimeException("Not yet supported");
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public void removeUnmappedServiceNetworkEntities() {
    //TODO: --> the one to implement

    // todo do so via modifier for service networks for which we will create some additional methods on top of the general one based on:
    //  to remove dangling unmapped service nodes, service links(segments) and their related unmapped (portions of) service routes
    //  then update the service network/routing ids accordingly

    //  prune service network and routes for unmatched service nodes (and routes) --> many routes/service nodes/legs might not have been matched due to network
    //  not covering entire GTFS area in which case we should remove them as it does result in an invalid/incomplete routedservices/servicenetwork
    //removeServiceLegSegmentsWithoutPhysicalNetworkMapping();
    //removeServiceLegsWithoutPhysicalNetworkMapping();
    //removeServiceNodesWithoutPhysicalNetworkMapping();
    //todo redo id's due to gaps in numbering
  }
}