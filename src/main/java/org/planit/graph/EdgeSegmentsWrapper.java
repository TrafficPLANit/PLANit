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
 */
public class EdgeSegmentsWrapper implements EdgeSegments {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(EdgeSegmentsWrapper.class.getCanonicalName());

  /**
   * The edge segments we are wrapping
   */
  private final EdgeSegments edgeSegments;

  /**
   * Provide access to edge segments
   * 
   * @return edge segments
   */
  protected EdgeSegments getEdgeSegments() {
    return edgeSegments;
  }

  /**
   * Constructor
   * 
   * @param edgeSegments we are wrapping
   */
  public EdgeSegmentsWrapper(final EdgeSegments edgeSegments) {
    this.edgeSegments = edgeSegments;
  }

  /**
   * Constructor
   * 
   * @param edgeSegments we are wrapping
   */
  public EdgeSegmentsWrapper(final EdgeSegmentsWrapper edgeSegmentsWrapper) {
    this.edgeSegments = edgeSegmentsWrapper.edgeSegments.clone();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegment remove(EdgeSegment edgeSegment) {
    return edgeSegments.remove(edgeSegment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegment remove(long id) {
    return edgeSegments.remove(id);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void register(DirectedEdge parentEdge, EdgeSegment edgeSegment, boolean directionAB) throws PlanItException {
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
  public Iterator<EdgeSegment> iterator() {
    return edgeSegments.iterator();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegment register(EdgeSegment value) {
    return edgeSegments.register(value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegment get(Long key) {
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
  public Collection<EdgeSegment> toCollection() {
    return edgeSegments.toCollection();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<EdgeSegment> copyOfValuesAsSet() {
    return edgeSegments.copyOfValuesAsSet();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegment findFirst(Predicate<EdgeSegment> valuePredicate) {
    return edgeSegments.findFirst(valuePredicate);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegmentFactory getFactory() {
    return edgeSegments.getFactory();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegmentsWrapper clone() {
    return new EdgeSegmentsWrapper(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateIds() {
    edgeSegments.recreateIds();
  }

}
