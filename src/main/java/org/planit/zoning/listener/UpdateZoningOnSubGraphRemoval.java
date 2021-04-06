package org.planit.zoning.listener;

import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;

import org.planit.utils.graph.Edge;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.Vertex;
import org.planit.utils.graph.modifier.RemoveDirectedSubGraphListener;
import org.planit.utils.network.physical.Node;
import org.planit.utils.zoning.Connectoid;
import org.planit.utils.zoning.DirectedConnectoid;
import org.planit.utils.zoning.UndirectedConnectoid;
import org.planit.zoning.Zoning;

/**
 * A listener designed to deal with the situation that sub graphs are removed from the network which leaves any zoning elements that reference these sub graph elements invalid.
 * this listener should be used to be registered on the DirectedGraphModifier before any call to {@link GraphModifier.removeSubGraph) in which case callbacks are triggered which
 * allow this instance to update the zoning accordingly by removing all zoning entities that are no longer valid.
 * 
 * @author markr
 *
 * @param <V>  vertex type
 * @param <E>  edge type
 * @param <ES> edge segment type
 */
public class UpdateZoningOnSubGraphRemoval<V extends Vertex, E extends Edge, ES extends EdgeSegment> implements RemoveDirectedSubGraphListener<V, E, ES> {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(UpdateZoningOnSubGraphRemoval.class.getCanonicalName());

  /**
   * zoning to update upon sub graph removal
   */
  protected Zoning zoning;

  /**
   * index all connectoids by access vertex to minimise lookups
   */
  protected Map<Vertex, ArrayList<Connectoid>> connectoidsByAccessVertex;

  /**
   * initialise indices used
   */
  protected void initialiseIndices() {
    for (UndirectedConnectoid connectoid : zoning.odConnectoids) {
      Node accessNode = connectoid.getAccessNode();
      if (accessNode != null) {
        connectoidsByAccessVertex.putIfAbsent(accessNode, new ArrayList<Connectoid>(1));
        connectoidsByAccessVertex.get(accessNode).add(connectoid);
      }
    }
    for (DirectedConnectoid connectoid : zoning.transferConnectoids) {
      Node accessNode = connectoid.getAccessNode();
      if (accessNode != null) {
        connectoidsByAccessVertex.putIfAbsent(accessNode, new ArrayList<Connectoid>(1));
        connectoidsByAccessVertex.get(accessNode).add(connectoid);
      }
    }
  }

  /**
   * constructor
   * 
   * @param zoning to use
   */
  public UpdateZoningOnSubGraphRemoval(Zoning zoning) {
    this.zoning = zoning;

    /* to minimise lookups, we traverse all connectoids once and index them by their access node */
    initialiseIndices();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onRemoveSubGraphEdge(E edge) {
    /* no action needed, dealt with in vertex removal */
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onRemoveSubGraphVertex(V vertex) {
    if (connectoidsByAccessVertex.containsKey(vertex)) {
      ArrayList<Connectoid> connectoids = connectoidsByAccessVertex.get(vertex);
      for (Connectoid connectoid : connectoids) {
        if (connectoid instanceof UndirectedConnectoid) {
          zoning.odConnectoids.remove((UndirectedConnectoid) connectoid);
        } else if (connectoid instanceof DirectedConnectoid) {
          zoning.transferConnectoids.remove((DirectedConnectoid) connectoid);
        } else {
          LOGGER.severe(String.format("unknown connectoid type used on vertex %d", vertex.getId()));
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onRemoveSubGraphEdgeSegment(ES edgeSegment) {
    /* no action needed, dealt with in vertex removal */
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onCompletion() {
    // TODO recreate zoning connectoid ids
    // TODO -> replace zoning modifier with listener as well using the same approach but then for breaking links!!
    // TODO -> remove dangling zones seaprately after all of this -> because due to removal of connectoids we might have those
    // but it is not ideal to do that here. more logical to provide functionality separately
  }

}
