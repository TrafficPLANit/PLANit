package org.goplanit.network.layer.modifier;

import org.goplanit.graph.directed.UntypedDirectedGraphImpl;
import org.goplanit.network.ServiceNetwork;
import org.goplanit.network.layer.service.ServiceNetworkLayerImpl;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.network.layer.modifier.ServiceNetworkLayerModifier;
import org.goplanit.utils.network.layer.service.ServiceLeg;
import org.goplanit.utils.network.layer.service.ServiceLegSegment;
import org.goplanit.utils.network.layer.service.ServiceNode;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Modifier class for service network layer, added functionality for service networks to:
 * <ul>
 *   <li>remove unmapped service nodes, service legs, and service leg segments</li>
 * </ul>
 *
 * @author markr
 */
public class ServiceNetworkLayerModifierImpl<V extends ServiceNode, E extends ServiceLeg, S extends ServiceLegSegment>
        extends UntypedNetworkLayerModifierImpl<V, E, S> implements ServiceNetworkLayerModifier<V,E,S> {

  // INNER CLASSES

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(ServiceNetworkLayerModifierImpl.class.getCanonicalName());

  /** the related service network layer */
  private ServiceNetworkLayerImpl serviceNetworkLayer;

  // PUBLIC

  /**
   * Constructor
   *
   * @param serviceNetworkLayer to use
   * @param graph parent graph to base modifier on
   */
  public ServiceNetworkLayerModifierImpl(ServiceNetworkLayerImpl serviceNetworkLayer, UntypedDirectedGraphImpl<V, E, S> graph) {
    super(graph);
    this.serviceNetworkLayer = serviceNetworkLayer;
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
    var graph = this.getUntypedDirectedGraph();
    final String serviceLayerLoggingPrefix = LoggingUtils.serviceNetworkLayerPrefix(this.serviceNetworkLayer.getId());

    LOGGER.info(String.format("%s Removing GTFS based service network elements without a mapping to physical underlying network, likely due to being outside of network bounding box", serviceLayerLoggingPrefix));
    /* identify and remove service nodes without a mapped physical node in the network layer */
    var toBeRemovedServiceNodes = graph.getVertices().stream().filter( v -> !v.hasPhysicalParentNode()).collect(Collectors.toList());
    toBeRemovedServiceNodes.forEach( n -> this.graphModifier.removeVertex(n));
    LOGGER.info(String.format("%s Removed %d service nodes without a mapping to physical network", serviceLayerLoggingPrefix, toBeRemovedServiceNodes.size()));

    /* remove all service legs and service leg segments connected to an already removed service node or lacking a mapping to underlying physical link segments,
     * in which case they are now invalid as well */
    var toBeRemovedServiceLegSegments =
            graph.getEdgeSegments().stream().filter(
                    ls -> ls.getUpstreamServiceNode()==null || ls.getDownstreamServiceNode()==null || !ls.hasPhysicalParentSegments()).collect(Collectors.toList());
    toBeRemovedServiceLegSegments.forEach( ls -> this.graphModifier.removeEdgeSegment(ls));
    LOGGER.info(String.format("%s Removed %d service leg segments without a mapping to physical network",serviceLayerLoggingPrefix, toBeRemovedServiceLegSegments.size()));

    var toBeRemovedServiceLegs =
            graph.getEdges().stream().filter(
                    e -> e.getVertexA()==null || e.getVertexB()==null || !e.hasEdgeSegment()).collect(Collectors.toList() );
    toBeRemovedServiceLegs.forEach( e -> this.graphModifier.removeEdge(e));
    LOGGER.info(String.format("%s Removed %d service legs without a mapping to physical network", serviceLayerLoggingPrefix, toBeRemovedServiceLegs.size()));

    /* recreate managed ids, so they are contiguous again */
    graphModifier.recreateManagedEntitiesIds();

    //todo redo id's due to gaps in numbering + remove routedServices afterwards (not here, needs its own modifier or events to be triggered...!)
  }
}