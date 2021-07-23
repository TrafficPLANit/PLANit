package org.planit.zoning.modifier.event.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.planit.graph.modifier.event.RemoveSubGraphEvent;
import org.planit.graph.modifier.event.RemoveSubGraphVertexEvent;
import org.planit.utils.event.EventType;
import org.planit.utils.graph.Vertex;
import org.planit.utils.graph.directed.DirectedVertex;
import org.planit.utils.graph.modifier.event.DirectedGraphModificationEvent;
import org.planit.utils.graph.modifier.event.DirectedGraphModifierListener;
import org.planit.utils.graph.modifier.event.GraphModificationEvent;
import org.planit.utils.zoning.Connectoid;
import org.planit.utils.zoning.DirectedConnectoid;
import org.planit.utils.zoning.UndirectedConnectoid;
import org.planit.zoning.Zoning;

/**
 * A listener designed to deal with the situation that sub graphs are removed from the network which leaves connectoids in the zoning that reference these sub graph elements
 * invalid. This listener should be used to register on the DirectedGraphModifier before any call to GraphModifier.removeSubGraph in which case callbacks are triggered which allow
 * this instance to update the connectoids, i.e., remove any affected connectoids that are no longer valid.
 * 
 * @author markr
 */
public class UpdateConnectoidsOnVertexRemovalHandler implements DirectedGraphModifierListener {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(UpdateConnectoidsOnVertexRemovalHandler.class.getCanonicalName());

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
    if (zoning == null) {
      LOGGER.severe(String.format("Zoning is null when initialising in %s, %s invalid", this.getClass().getName()));
    }
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
   * Remove connectoids for the given access vertex
   * 
   * @param vertex to remove connectoids for
   */
  protected void removeConnectoidsWithAccessVertex(Vertex vertex) {
    if (connectoidsByAccessVertex.containsKey(vertex)) {
      ArrayList<Connectoid> connectoids = connectoidsByAccessVertex.get(vertex);
      for (Connectoid connectoid : connectoids) {
        if (connectoid instanceof UndirectedConnectoid) {
          zoning.odConnectoids.remove((UndirectedConnectoid) connectoid);
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
   * finalise subgraph removal for the connectoids by recreating the connectoid ids
   */
  protected void afterSubGraphRemovalComplete() {
    /* We guarantee contiguous ids within the platform for all entities, but due to removal of connectoids this is currently unlikely, so recreate ids if needed */
    if (removedConnectoids) {
      zoning.getZoningModifier().recreateConnectoidIds();
    }

    /* reset for next subgraph removal */
    removedConnectoids = false;
  }

  /**
   * constructor
   * 
   * @param zoning to use
   */
  public UpdateConnectoidsOnVertexRemovalHandler(final Zoning zoning) {
    super();
    this.zoning = zoning;

    /* to minimise lookups, we traverse all connectoids once and index them by their access node */
    initialiseIndices();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EventType[] getKnownSupportedEventTypes() {
    return new EventType[] { RemoveSubGraphVertexEvent.EVENT_TYPE, RemoveSubGraphEvent.EVENT_TYPE };
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onGraphModificationEvent(GraphModificationEvent event) {
    if (event.getType().equals(RemoveSubGraphVertexEvent.EVENT_TYPE)) {
      removeConnectoidsWithAccessVertex(RemoveSubGraphVertexEvent.class.cast(event).getRemovedVertex());
    } else if (event.getType().equals(RemoveSubGraphEvent.EVENT_TYPE)) {
      afterSubGraphRemovalComplete();
    } else {
      LOGGER.warning(String.format("%s invoked for unsupported event", UpdateConnectoidsOnVertexRemovalHandler.class.getName()));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onDirectedGraphModificationEvent(DirectedGraphModificationEvent event) {
    LOGGER.warning(String.format("%s only supports graph modification events", UpdateConnectoidsOnVertexRemovalHandler.class.getName()));
  }

}
