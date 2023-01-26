package org.goplanit.assignment.ltm.sltm;

import java.util.Iterator;

import org.goplanit.algorithms.shortest.ShortestSearchType;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.id.IdAble;

/**
 * 
 * Common Interface for bushes
 * 
 * @author markr
 *
 */
public interface Bush extends IdAble {

  /**
   * Collect an iterator over topologically sorted bush in origin-destination or destination-origin direction. Depending on the derived bush implementation this might require
   * inverting the iteration direction. Hence it is an abstract method here
   * 
   * @param originDestinationDirection when true, iterator runs topological order from origin towards destinatino, when false, they other way around
   * @return iterator over topologically ordered bush vertices
   */
  public abstract Iterator<? extends DirectedVertex> getTopologicalIterator(boolean originDestinationDirection);

  /**
   * determine the search type supported by the bush based on the underlying dag's construction, i.e., a destination-based dag results in All-To-One, whereas an origin based dag
   * results in One-To-All searches.
   * 
   * @return shortest search type compatible with this bush implementation
   */
  public abstract ShortestSearchType getShortestSearchType();

  /**
   * {@inheritDoc}
   */
  public abstract Bush clone();

  /**
   * {@inheritDoc}
   */
  public abstract Bush deepClone();
}
