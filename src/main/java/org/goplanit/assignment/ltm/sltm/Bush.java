package org.goplanit.assignment.ltm.sltm;

import java.util.Iterator;
import java.util.function.Consumer;

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
   * Collect an iterator over topologically sorted bush from root to leaves.
   *
   * @return iterator over topologically ordered bush vertices
   */
  public abstract Iterator<? extends DirectedVertex> getTopologicalIterator();

  /**
   * Collect an iterator over topologically sorted bush from leaves to root.
   *
   * @return iterator over topologically ordered bush vertices
   */
  public abstract Iterator<? extends DirectedVertex> getInvertedTopologicalIterator();

  /**
   * Traverse a bush in topological order, invert traversal if indicated
   *
   * @param invertIterator when true invert iterator direction
   * @param vertexConsumer to apply to each vertex
   */
  public abstract void forEachTopologicalSortedVertex(boolean invertIterator, Consumer<DirectedVertex> vertexConsumer);

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
  public abstract Bush shallowClone();

  /**
   * {@inheritDoc}
   */
  public abstract Bush deepClone();
}
