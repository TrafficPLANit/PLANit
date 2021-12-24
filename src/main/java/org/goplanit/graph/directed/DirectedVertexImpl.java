package org.goplanit.graph.directed;

import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Logger;

import org.goplanit.graph.VertexImpl;
import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * vertex representation connected to one or more entry and exit edges
 *
 * @author markr
 *
 */
public class DirectedVertexImpl extends VertexImpl implements DirectedVertex {

  /**
   * Dedicated iterable to provide access to edge segments that are either incoming or outgoing for this vertex
   * 
   * @author markr
   *
   */
  public final class EdgeSegmentIterable implements Iterable<EdgeSegment> {

    /** flag indicating incoming or outgoing edge segments to iterate over */
    boolean incoming;

    /**
     * Constructor
     * 
     * @param incoming flag
     */
    private EdgeSegmentIterable(boolean incoming) {
      this.incoming = incoming;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<EdgeSegment> iterator() {
      return new EdgeSegmentIterator(incoming);
    }

  }

  /**
   * Iterator for a run over the incoming or outgoing edge segments of this vertex (non-modifiable)
   * 
   * @author markr
   *
   */
  public class EdgeSegmentIterator implements Iterator<EdgeSegment> {

    /**
     * parent edges iterator to extract information from
     */
    private Iterator<? extends DirectedEdge> edgesIter;

    /**
     * flag inherited from iterable
     */
    private boolean incoming;

    /**
     * tracking of next edge segment (to return)
     */
    private EdgeSegment nextEdgeSegment;

    /**
     * Constructor
     * 
     * @param incoming flag
     */
    private EdgeSegmentIterator(boolean incoming) {
      this.incoming = incoming;
      edgesIter = (Iterator<? extends DirectedEdge>) DirectedVertexImpl.this.getEdges().iterator();
      nextEdgeSegment = null;
      hasNext();
    }

    /**
     * Not supported
     */
    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }


    /**
     * Check if next is available by querying edges for available segment based on their vertex location matching this vertex
     * 
     * @return true when present, false otherwise
     */
    @Override
    public boolean hasNext() {
      if (nextEdgeSegment != null) {
        return true;
      }

      while (edgesIter.hasNext()) {
        var edge = edgesIter.next();
        if (edge.getVertexA() == DirectedVertexImpl.this) {
          nextEdgeSegment = incoming ? edge.getEdgeSegmentBa() : edge.getEdgeSegmentAb();
        } else if (edge.getVertexB() == DirectedVertexImpl.this) {
          nextEdgeSegment = incoming ? edge.getEdgeSegmentAb() : edge.getEdgeSegmentBa();
        } else {
          LOGGER.severe(String.format("Vertex (%s) not present on edge (%s) it holds, this shouldn't happen", DirectedVertexImpl.this.getXmlId(), edge.getXmlId()));
        }

        if (nextEdgeSegment != null) {
          return true;
        }
      }
      return false;
    }

    /**
     * Get next edge segment
     * 
     * @return edge segment found
     */
    @Override
    public EdgeSegment next() {
      var returnEdgeSegment = nextEdgeSegment;
      nextEdgeSegment = null;
      return returnEdgeSegment;
    }

  }

  /** generated UID */
  private static final long serialVersionUID = 2165199386965239623L;

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(DirectedVertexImpl.class.getCanonicalName());

  // Protected

  /**
   * Entry edge segments iterable connected to this vertex
   */
  protected final EdgeSegmentIterable entryEdgeSegments;

  /**
   * Exit edge segments which connect to this vertex
   */
  protected final EdgeSegmentIterable exitEdgeSegments;

  /**
   * Constructor
   * 
   * @param groupId, contiguous id generation within this group for instances of this class
   */
  protected DirectedVertexImpl(final IdGroupingToken groupId) {
    super(groupId);
    this.entryEdgeSegments = new EdgeSegmentIterable(true /* incoming */);
    this.exitEdgeSegments = new EdgeSegmentIterable(false /* outgoing */);
  }

  /**
   * Copy constructor
   * 
   * @param directedVertexImpl to copy
   */
  protected DirectedVertexImpl(DirectedVertexImpl directedVertexImpl) {
    super(directedVertexImpl);
    this.entryEdgeSegments = directedVertexImpl.entryEdgeSegments;
    this.exitEdgeSegments = directedVertexImpl.exitEdgeSegments;
  }

  // Public

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterable<EdgeSegment> getEntryEdgeSegments() {
    return entryEdgeSegments;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterable<EdgeSegment> getExitEdgeSegments() {
    return exitEdgeSegments;
  }


  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public Collection<? extends DirectedEdge> getEdges() {
    return (Collection<? extends DirectedEdge>) super.getEdges();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedVertexImpl clone() {
    return new DirectedVertexImpl(this);
  }

}
