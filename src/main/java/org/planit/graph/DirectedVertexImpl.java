package org.planit.graph;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.planit.utils.graph.DirectedVertex;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.id.IdGroupingToken;

/**
 * vertex representation connected to one or more entry and exit edges
 *
 * @author markr
 *
 */
public class DirectedVertexImpl extends VertexImpl implements DirectedVertex {

  /** generated UID */
  private static final long serialVersionUID = 2165199386965239623L;

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(DirectedVertexImpl.class.getCanonicalName());

  // Protected

  /**
   * Entry edge segments which connect to this vertex
   */
  protected final Set<EdgeSegment> entryEdgeSegments = new TreeSet<EdgeSegment>();

  /**
   * Exit edge segments which connect to this vertex
   */
  protected final Set<EdgeSegment> exitEdgeSegments = new TreeSet<EdgeSegment>();

  /**
   * Constructor
   * 
   * @param groupId, contiguous id generation within this group for instances of this class
   */
  protected DirectedVertexImpl(final IdGroupingToken groupId) {
    super(groupId);
  }

  /**
   * Copy constructor
   * 
   * @param directedVertexImpl to copy
   */
  protected DirectedVertexImpl(DirectedVertexImpl directedVertexImpl) {
    super(directedVertexImpl);
    entryEdgeSegments.addAll(directedVertexImpl.getEntryEdgeSegments());
    exitEdgeSegments.addAll(directedVertexImpl.getExitEdgeSegments());

  }

  // Public

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<EdgeSegment> getEntryEdgeSegments() {
    return Collections.unmodifiableSet(this.entryEdgeSegments);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<EdgeSegment> getExitEdgeSegments() {
    return Collections.unmodifiableSet(this.exitEdgeSegments);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean addEdgeSegment(final EdgeSegment edgeSegment) {

    if (edgeSegment.getUpstreamVertex().getId() == getId()) {
      return exitEdgeSegments.add(edgeSegment);
    } else if (edgeSegment.getDownstreamVertex().getId() == getId()) {
      return entryEdgeSegments.add(edgeSegment);
    }
    LOGGER.warning(String.format("edge segment %s (id:%d) does not have this vertex %s (%d) on either end", edgeSegment.getExternalId(), edgeSegment.getId()));
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removeEdgeSegment(final EdgeSegment edgeSegment) {
    boolean removed = false;
    removed = exitEdgeSegments.remove(edgeSegment);
    if (!removed) {
      removed = entryEdgeSegments.remove(edgeSegment);
    }
    return removed;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removeEntryEdgeSegment(EdgeSegment edgeSegment) {
    return entryEdgeSegments.remove(edgeSegment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removeExitEdgeSegment(EdgeSegment edgeSegment) {
    return exitEdgeSegments.remove(edgeSegment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasExitEdgeSegments() {
    return exitEdgeSegments.isEmpty();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasEntryEdgeSegments() {
    return entryEdgeSegments.isEmpty();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getNumberOfEntryEdgeSegments() {
    return entryEdgeSegments.size();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getNumberOfExitEdgeSegments() {
    return exitEdgeSegments.size();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedVertexImpl clone() {
    return new DirectedVertexImpl(this);
  }

}
