package org.goplanit.network.layer.modifier;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.goplanit.graph.directed.UntypedDirectedGraphImpl;
import org.goplanit.graph.directed.modifier.DirectedGraphModifierImpl;
import org.goplanit.utils.graph.directed.Connectivity;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.UntypedDirectedSubGraph;
import org.goplanit.utils.graph.UntypedDirectedGraph;
import org.goplanit.utils.graph.directed.DirectedGraphUtils;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.graph.modifier.event.GraphModifierEventType;
import org.goplanit.utils.graph.modifier.event.GraphModifierListener;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.network.layer.modifier.UntypedDirectedGraphLayerModifier;
import org.goplanit.utils.graph.directed.BannedMovement;
import org.goplanit.utils.network.layer.physical.Link;

/**
 * Modifier class for model free network layer, generics used to allow derived classes to provide typed versions
 * for containers and content of containers. It wraps a directed graph modifier while allowing the methods to be
 * typed in a more user-friendly way and hide or add additional functionality compared to the raw modifications
 * exposed by the underlying graph modifier.
 *
 * @author markr
 *
 * @param <V> type of vertex
 * @param <E> type of edge
 * @param <S> type of segment
 */
public class UntypedNetworkLayerModifierImpl<V extends DirectedVertex, E extends DirectedEdge, S extends EdgeSegment>
        implements UntypedDirectedGraphLayerModifier<V, E, S> {

  // INNER CLASSES

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(UntypedNetworkLayerModifierImpl.class.getCanonicalName());

  /** the graph modifier to use to apply larger modifications */
  protected DirectedGraphModifierImpl graphModifier;

  /** Access to the underlying graph registered on the modifier
   *
   * @return underlying directed graph */
  protected UntypedDirectedGraphImpl<V,E,S> getUntypedDirectedGraph(){
    return (UntypedDirectedGraphImpl<V, E, S>) graphModifier.getGraph();
  }

  // PUBLIC

  /**
   * Constructor
   *
   * @param graph parent graph to base modifier on
   */
  public UntypedNetworkLayerModifierImpl(final UntypedDirectedGraphImpl<V, E, S> graph) {
    this.graphModifier = new DirectedGraphModifierImpl(graph);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<Long, Pair<E, E>> breakAt(List<E> linksToBreak, V nodeToBreakAt, CoordinateReferenceSystem crs) {
    return graphModifier.breakEdgesAt(linksToBreak, nodeToBreakAt, crs);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<Long, Pair<E, E>> breakAt(
      List<E> linksToBreak,
      V nodeToBreakAt,
      Map<? extends V, List<BannedMovement>> movementsByCentreVertex,
      CoordinateReferenceSystem crs) {
    return graphModifier.breakEdgesAt(linksToBreak, nodeToBreakAt, movementsByCentreVertex, crs);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateManagedIdEntities() {
    graphModifier.recreateManagedEntitiesIds();
  }

  /**
   * remove any dangling subnetworks below a given size from the network if they exist and subsequently reorder
   * the internal ids if needed. Also remove zoning entities that rely solely on removed dangling network entities
   * <p>
   *   Should fire #RecreatedGraphEntitiesManagedIdsEvent after it has been executed
   * </p>
   * 
   * @param belowSize         remove subnetworks below the given size
   * @param aboveSize         remove subnetworks above the given size (typically set to maximum value)
   * @param alwaysKeepLargest when true the largest of the subnetworks is always kept, otherwise not
   * @param recreateManagedIds when true recreate managed id entities so they are contiguous again
   */
  @Override
  public void removeDanglingSubnetworks(
          final Integer belowSize, Integer aboveSize, boolean alwaysKeepLargest, boolean recreateManagedIds) {
    /* perform removal */
    graphModifier.removeDanglingSubGraphs(belowSize, aboveSize, alwaysKeepLargest);
    if(recreateManagedIds) {
      recreateManagedIdEntities();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeDanglingSubnetworks(
      final Integer belowSize,
      Integer aboveSize,
      boolean alwaysKeepLargest,
      boolean recreateManagedIds,
      Predicate<? super S> testEdgeSegment) {
    /* strong by default: infrastructure that cannot be both entered and left is of no use to a route, so treating
     * it as part of the network it hangs off keeps something that can never be travelled. Explicitly requesting
     * the weak notion remains available for callers that want attachment alone to decide */
    removeDanglingSubnetworks(
        belowSize, aboveSize, alwaysKeepLargest, recreateManagedIds, testEdgeSegment, Connectivity.STRONG);
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public void removeDanglingSubnetworks(
      final Integer belowSize,
      Integer aboveSize,
      boolean alwaysKeepLargest,
      boolean recreateManagedIds,
      Predicate<? super S> testEdgeSegment,
      Connectivity connectivity) {

    /* the composed directed graph modifier is bound to the base entity types whereas this layer modifier is
     * generic. Everything registered on the layer's graph is a DirectedVertex, DirectedEdge and EdgeSegment, so
     * viewing them at those types is safe by construction. Confined to these two locals so it is not repeated */
    var baseGraph =
        (UntypedDirectedGraph<DirectedVertex, DirectedEdge, EdgeSegment>) getUntypedDirectedGraph();
    var baseTestEdgeSegment = (Predicate<? super EdgeSegment>) testEdgeSegment;

    /* identification is restricted to the accepted edge segments and applies strict rules, i.e. an edge only
     * counts when all of its segments are accepted and a vertex only when all of its edges are. Entities on the
     * boundary between accepted and rejected parts are therefore left out of the subnetwork and survive removal */
    Function<DirectedVertex, ? extends UntypedDirectedSubGraph<DirectedVertex, DirectedEdge, EdgeSegment>>
        identifySubGraphForVertex;
    if (connectivity.isStrong()) {
      /* partitioned once for the entire layer and then looked up, since the modifier below asks per vertex and
       * partitioning per vertex would be quadratic in the size of the layer */
      var subGraphByVertex = DirectedGraphUtils.identifyStronglyConnectedSubGraphs(
          baseGraph, edge -> true, baseTestEdgeSegment, true);
      identifySubGraphForVertex = subGraphByVertex::get;
    } else {
      identifySubGraphForVertex = v -> DirectedGraphUtils.identifySubGraphForVertex(
          baseGraph, v, edge -> true, baseTestEdgeSegment, true);
    }

    graphModifier.removeDanglingDirectedSubGraphs(
        belowSize, aboveSize, alwaysKeepLargest, identifySubGraphForVertex);

    if(recreateManagedIds) {
      recreateManagedIdEntities();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeEdge(E edge){
    graphModifier.removeEdge(edge);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeEdgeSegment(S edgeSegment){
    graphModifier.removeEdgeSegment(edgeSegment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeVertex(V vertex){
    graphModifier.removeVertex(vertex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(GraphModifierListener listener, GraphModifierEventType eventType) {
    graphModifier.addListener(listener, eventType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(GraphModifierListener listener) {
    graphModifier.addListener(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(GraphModifierListener listener, GraphModifierEventType eventType) {
    graphModifier.removeListener(listener, eventType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(GraphModifierListener listener) {
    graphModifier.removeListener(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeAllListeners() {
    graphModifier.removeAllListeners();
  }

}