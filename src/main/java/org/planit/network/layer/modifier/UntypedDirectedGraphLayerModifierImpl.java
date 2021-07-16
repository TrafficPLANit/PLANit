package org.planit.network.layer.modifier;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.graph.UntypedDirectedGraphImpl;
import org.planit.graph.modifier.DirectedGraphModifierImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.GraphEntities;
import org.planit.utils.graph.directed.DirectedEdge;
import org.planit.utils.graph.directed.DirectedVertex;
import org.planit.utils.graph.modifier.DirectedGraphModifier;
import org.planit.utils.graph.modifier.event.GraphModifierEventType;
import org.planit.utils.graph.modifier.event.GraphModifierListener;
import org.planit.utils.network.layer.modifier.UntypedDirectedGraphLayerModifier;

/**
 * Model free network layer consisting of containers for vertices, edges, and edge segments each of which can be typed separately. This network does not contain any transport
 * specific information, hence the qualification "model free".
 *
 * @author markr
 */
public class UntypedDirectedGraphLayerModifierImpl<V extends DirectedVertex, VE extends GraphEntities<V>, E extends DirectedEdge, EE extends GraphEntities<E>, S extends EdgeSegment, SE extends GraphEntities<S>>
    implements UntypedDirectedGraphLayerModifier<V, VE, E, EE, S, SE> {

  // INNER CLASSES

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(UntypedDirectedGraphLayerModifierImpl.class.getCanonicalName());

  /** the graph modifier to use to apply larger modifications */
  protected DirectedGraphModifier graphModifier;

  // PUBLIC

  /**
   * Network Constructor
   *
   * @param graph         parent graph
   * @param graphModifier parent graph modifier
   */
  public UntypedDirectedGraphLayerModifierImpl(final DirectedGraphModifier graphModifier) {
    this.graphModifier = graphModifier;
  }

  /**
   * Network Constructor
   *
   * @param graph parent graph to abse modifier on
   */
  public UntypedDirectedGraphLayerModifierImpl(UntypedDirectedGraphImpl<VE, EE, SE> graph) {
    this.graphModifier = new DirectedGraphModifierImpl(graph);
  }

  // Getters - Setters

  /**
   * Break the passed in links by inserting the passed in node in between. After completion the original links remain as (NodeA,NodeToBreakAt), and new links as inserted for
   * (NodeToBreakAt,NodeB).
   * 
   * Underlying link segments (if any) are also updated accordingly in the same manner
   * 
   * @param linksToBreak  the links to break
   * @param nodeToBreakAt the node to break at
   * @param crs           to use to recompute link lengths of broken links
   * @return the broken links for each original link's internal id
   * @throws PlanItException thrown if error
   */
  @Override
  public Map<Long, Set<E>> breakAt(List<E> linksToBreak, V nodeToBreakAt, CoordinateReferenceSystem crs) throws PlanItException {
    return graphModifier.breakEdgesAt(linksToBreak, nodeToBreakAt, crs);
  }

  /**
   * remove any dangling subnetworks below a given size from the network if they exist and subsequently reorder the internal ids if needed. Also remove zoning entities that rely
   * solely on removed dangling network entities
   * 
   * @param belowSize         remove subnetworks below the given size
   * @param aboveSize         remove subnetworks above the given size (typically set to maximum value)
   * @param alwaysKeepLargest when true the largest of the subnetworks is always kept, otherwise not
   * @throws PlanItException thrown if error
   */
  @Override
  public void removeDanglingSubnetworks(final Integer belowSize, Integer aboveSize, boolean alwaysKeepLargest) throws PlanItException {
    /* perform removal */
    graphModifier.removeDanglingSubGraphs(belowSize, aboveSize, alwaysKeepLargest);
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
