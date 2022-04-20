package org.goplanit.assignment.ltm.sltm;

import java.util.Iterator;

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
}
