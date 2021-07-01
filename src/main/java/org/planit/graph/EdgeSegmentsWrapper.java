package org.planit.graph;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.DirectedEdge;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.EdgeSegmentFactory;
import org.planit.utils.graph.EdgeSegments;

/**
 * Wrapper around an EdgeSegments instance.
 * 
 * 
 * @author markr
 *
 * @param <ES> edge segments type
 */
public class EdgeSegmentsWrapper<ES extends EdgeSegment> implements EdgeSegments<ES> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(EdgeSegmentsWrapper.class.getCanonicalName());

  /**
   * The edge segments we are wrapping
   */
  private final EdgeSegments<ES> edgeSegments;

  /**
   * Provide access to edge segments
   * 
   * @return edge segments
   */
  protected EdgeSegments<ES> getEdgeSegments() {
    return edgeSegments;
  }

  /**
   * Constructor
   * 
   * @param edgeSegments we are wrapping
   */
  public EdgeSegmentsWrapper(final EdgeSegments<ES> edgeSegments) {
    this.edgeSegments = edgeSegments;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ES remove(ES edgeSegment) {
    return edgeSegments.remove(edgeSegment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void register(DirectedEdge parentEdge, ES edgeSegment, boolean directionAB) throws PlanItException {
    edgeSegments.register(parentEdge, edgeSegment, directionAB);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int size() {
    return edgeSegments.size();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterator<ES> iterator() {
    return edgeSegments.iterator();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ES register(ES value) {
    return edgeSegments.register(value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ES get(Long key) {
    return edgeSegments.get(key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEmpty() {
    return edgeSegments.isEmpty();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<ES> toCollection() {
    return edgeSegments.toCollection();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<ES> copyOfValuesAsSet() {
    return edgeSegments.copyOfValuesAsSet();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ES findFirst(Predicate<ES> valuePredicate) {
    return edgeSegments.findFirst(valuePredicate);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegmentFactory<? extends ES> getFactory() {
    return edgeSegments.getFactory();
  }

}
