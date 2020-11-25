package org.planit.network.physical;

import java.util.Iterator;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.Edges;
import org.planit.utils.graph.Vertex;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.Links;

/**
 * 
 * Links implementation wrapper that simply utilises passed in edges of the desired generic type to delegate registration and creation of its links on
 * 
 * @author markr
 * 
 * @param <L>
 */
public class LinksImpl<L extends Link> implements Links<L> {

  
  /**
   * The edges we use to create and register our links on
   */
  private final Edges<L> edges;

  /**
   * Constructor
   * 
   * @param edges the edges to use to create and register links on
   */
  public LinksImpl(final Edges<L> edges) {
    this.edges = edges;
  }  
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void remove(L link) {
    edges.remove(link);
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public void remove(long linkId) {
    edges.remove(linkId);
  }
  
  /**
   * Create new link to network identified via its id, injecting link length directly
   *
   * @param vertexA           the first node in this link
   * @param vertexB           the second node in this link
   * @param length          the length of this link
   * @param registerOnNodes choice to register new link on the nodes or not
   * @return the created link
   * @throws PlanItException thrown if there is an error
   */
  @Override  
  public L registerNew(final Vertex vertexA, final Vertex vertexB, final double length, boolean registerOnNodes) throws PlanItException {
    return edges.registerNew(vertexA, vertexB, length, registerOnNodes);
  }  

  /**
   * {@inheritDoc}
   */  
  @Override
  public L registerUniqueCopyOf(L edgeToCopy) {
    return edges.registerUniqueCopyOf(edgeToCopy);
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public L get(long id) {
    return edges.get(id);
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public int size() {
    return edges.size();
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public boolean isEmpty() {
    return edges.isEmpty();
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public Iterator<L> iterator() {
    return edges.iterator();
  }



}
