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

  // PUBLIC

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
    //TODO: CONTINUE HERE --> TEST AND DEBUG IF THIS IS CORRECT!!!! 13/1/2023

    var graph = this.getUntypedDirectedGraph();

    /* identify and remove service nodes without a mapped physical node in the network layer */
    var toBeRemovedServiceNodes = graph.getVertices().stream().filter( v -> !v.hasPhysicalParentNode()).collect(Collectors.toList());
    toBeRemovedServiceNodes.forEach( n -> this.graphModifier.removeVertex(n));

    /* remove all service legs and service leg segments connected to an already removed service node, in which case they are now invalid as well */
    var toBeRemovedServiceLegSegments =
            graph.getEdgeSegments().stream().filter(
                    ls -> ls.getUpstreamServiceNode()==null || ls.getDownstreamServiceNode()==null).collect(Collectors.toList());
    toBeRemovedServiceLegSegments.forEach( ls -> this.graphModifier.removeEdgeSegment(ls));

    var toBeRemovedServiceLegs =
            graph.getEdges().stream().filter(
                    e -> e.getVertexA()==null || e.getVertexB()==null).collect(Collectors.toList());
    toBeRemovedServiceLegs.forEach( e -> this.graphModifier.removeEdge(e));

    /* special case --> can be that service leg segments are connected to valid service nodes, but the leg segment itself is not valid due to missing
     * physical parent segments -> remove as well but log since this seems very unlikely and possible a result of an error upstream */
    toBeRemovedServiceLegSegments = graph.getEdgeSegments().stream().filter( ls -> !ls.hasPhysicalParentSegments()).collect(Collectors.toList());
    if(!toBeRemovedServiceLegSegments.isEmpty()) {
      toBeRemovedServiceLegSegments.forEach(ls -> this.graphModifier.removeEdgeSegment(ls));
      //todo: check connected edges and service nodes have service leg segments correctly removed
    }

    //todo redo id's due to gaps in numbering + remove routedServices afterwards (not here, needs its own modifier or events to be triggered...!)
  }
}