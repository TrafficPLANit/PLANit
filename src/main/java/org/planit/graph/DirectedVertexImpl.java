package org.planit.graph;

import java.util.Set;
import java.util.TreeSet;

import org.planit.utils.exceptions.PlanItException;
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

  // Public

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<EdgeSegment> getEntryEdgeSegments() {
    return this.entryEdgeSegments;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<EdgeSegment> getExitEdgeSegments() {
    return this.exitEdgeSegments;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean addEdgeSegment(final EdgeSegment edgeSegment) throws PlanItException {
    if (edgeSegment.getUpstreamVertex().getId() == getId()) {
      return exitEdgeSegments.add(edgeSegment);
    } else if (edgeSegment.getDownstreamVertex().getId() == getId()) {
      return entryEdgeSegments.add(edgeSegment);
    }
    throw new PlanItException(String.format("edge segment %s (id:%d) does not have this vertex %s (%d) on either end", edgeSegment.getExternalId(), edgeSegment.getId()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removeEdgeSegment(final EdgeSegment edgeSegment) throws PlanItException {
    if (edgeSegment.getUpstreamVertex().getId() == getId()) {
      return exitEdgeSegments.remove(edgeSegment);
    } else if (edgeSegment.getDownstreamVertex().getId() == getId()) {
      return entryEdgeSegments.remove(edgeSegment);
    }
    throw new PlanItException(String.format("edge segment %s (id:%d) does not have this vertex %s (%d) on either end", edgeSegment.getExternalId(), edgeSegment.getId()));
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

}
