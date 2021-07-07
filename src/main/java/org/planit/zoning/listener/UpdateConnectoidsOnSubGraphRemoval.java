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
 */
public class UpdateConnectoidsOnSubGraphRemoval extends RemoveSubGraphListenerImpl implements RemoveDirectedSubGraphListener {

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
    super();
    this.zoning = zoning;
    
    /* to minimise lookups, we traverse all connectoids once and index them by their access node */
    initialiseIndices();
  }

  
  /** copy constructor 
   * @param updateConnectoidsOnSubGraphRemoval to copy
   */
  public UpdateConnectoidsOnSubGraphRemoval(final UpdateConnectoidsOnSubGraphRemoval updateConnectoidsOnSubGraphRemoval) {
    super(updateConnectoidsOnSubGraphRemoval);
    this.connectoidsByAccessVertex = updateConnectoidsOnSubGraphRemoval.connectoidsByAccessVertex;
    this.removedConnectoids = updateConnectoidsOnSubGraphRemoval.removedConnectoids;
    this.zoning = updateConnectoidsOnSubGraphRemoval.zoning;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onRemoveSubGraphEdge(Edge edge) {
    /* no action needed, dealt with in vertex removal */
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onRemoveSubGraphVertex(Vertex vertex) {
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
  public void onRemoveSubGraphEdgeSegment(EdgeSegment edgeSegment) {
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
  
  /**
   * {@inheritDoc}
   */  
  @Override
  public UpdateConnectoidsOnSubGraphRemoval clone() {
    return new UpdateConnectoidsOnSubGraphRemoval(this);
  }    

}
