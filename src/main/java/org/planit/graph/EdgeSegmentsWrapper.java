package org.planit.graph;

import java.util.Iterator;
import java.util.logging.Logger;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.DirectedEdge;
import org.planit.utils.graph.EdgeSegment;
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
  public ES remove(final long edgeSegmentId) {
    return edgeSegments.remove(edgeSegmentId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ES create(DirectedEdge parent, boolean directionAB) throws PlanItException {
    return edgeSegments.create(parent, directionAB);
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
  public ES get(long id) {
    return edgeSegments.get(id);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ES getByXmlId(String xmlId) {
    return edgeSegments.getByXmlId(xmlId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ES getByExternalId(String xmlId) {
    return edgeSegments.getByExternalId(xmlId);
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
  public ES registerUniqueCopyOf(ES edgeSegmentToCopy, DirectedEdge newParent) {
    return edgeSegments.registerUniqueCopyOf(edgeSegmentToCopy, newParent);
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
  public ES registerNew(DirectedEdge parentEdge, boolean directionAb, boolean registerOnNodeAndLink) throws PlanItException {
    return edgeSegments.registerNew(parentEdge, directionAb, registerOnNodeAndLink);
  }

}
