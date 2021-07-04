package org.planit.zoning.listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.planit.utils.graph.Edge;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.Vertex;
import org.planit.utils.graph.directed.DirectedVertex;
import org.planit.utils.graph.modifier.RemoveDirectedSubGraphListener;
import org.planit.utils.graph.modifier.RemoveSubGraphListenerImpl;
import org.planit.utils.zoning.Connectoid;
import org.planit.utils.zoning.DirectedConnectoid;
import org.planit.utils.zoning.UndirectedConnectoid;
import org.planit.zoning.Zoning;

/**
 * A listener designed to deal with the situation that sub graphs are removed from the network which leaves connectoids in the zoning that reference these sub graph elements invalid.
 * This listener should be used to register on the DirectedGraphModifier before any call to GraphModifier.removeSubGraph in which case callbacks are triggered which
 * allow this instance to update the connectoids, i.e., remove any affected connectoids that are no longer valid.
 * 
 * @author markr
 *
 * @param <V>  vertex type
 * @param <E>  edge type
 * @param <ES> edge segment type
 */
public class UpdateConnectoidsOnSubGraphRemoval<V extends Vertex, E extends Edge, ES extends EdgeSegment> extends RemoveSubGraphListenerImpl<V, E> implements RemoveDirectedSubGraphListener<V, E, ES> {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(UpdateConnectoidsOnSubGraphRemoval.class.getCanonicalName());

  /**
   * zoning to update zoning components when needed
   */
  protected final Zoning zoning;

  /**
   * index all connectoids by access vertex to minimise lookups
   */
  protected Map<DirectedVertex, ArrayList<Connectoid>> connectoidsByAccessVertex;
  
  /** track if any connectoids where removed */
  boolean removedConnectoids = false;

  /**
   * initialise indices used
   */
  protected void initialiseIndices() {
    connectoidsByAccessVertex = new HashMap<DirectedVertex, ArrayList<Connectoid>>();
    for (UndirectedConnectoid connectoid : zoning.odConnectoids) {
      DirectedVertex accessVertex = connectoid.getAccessVertex();
      if (accessVertex != null) {
        connectoidsByAccessVertex.putIfAbsent(accessVertex, new ArrayList<Connectoid>(1));
        connectoidsByAccessVertex.get(accessVertex).add(connectoid);
      }
    }
    for (DirectedConnectoid connectoid : zoning.transferConnectoids) {
      DirectedVertex accessVertex = connectoid.getAccessVertex();
      if (accessVertex != null) {
        connectoidsByAccessVertex.putIfAbsent(accessVertex, new ArrayList<Connectoid>(1));
        connectoidsByAccessVertex.get(accessVertex).add(connectoid);
      }
    }
  }

  /**
   * constructor
   * 
   * @param zoning to use
   */
  public UpdateConnectoidsOnSubGraphRemoval(Zoning zoning) {
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
          zoning.odConnectoids.remove((UndirectedConnectoid)connectoid);
          removedConnectoids = true;
        } else if (connectoid instanceof DirectedConnectoid) {
          zoning.transferConnectoids.remove((DirectedConnectoid) connectoid);
          removedConnectoids = true;
        } else {
          LOGGER.severe(String.format("unknown connectoid type used on vertex %d", vertex.getId()));
          continue;
        }
        connectoidsByAccessVertex.remove(connectoid.getAccessVertex());        
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
    /* We guarantee contiguous ids within the platform for all entities, but due to removal of connectoids this is currently unlikely, so recreate ids if needed*/
    if(removedConnectoids) {
      zoning.getZoningModifier().recreateConnectoidIds();
    }
    
    /* reset for next subgraph removal */
    removedConnectoids = false;
  }

}
