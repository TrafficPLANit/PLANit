package org.goplanit.graph.directed;

import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Logger;

import org.goplanit.graph.VertexImpl;
import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * vertex representation connected to one or more entry and exit edges
 *
 * @author markr
 *
 */
public class DirectedVertexImpl<E extends EdgeSegment> extends VertexImpl<Edge> implements DirectedVertex {

  /**
   * Dedicated iterable to provide access to edge segments that are either incoming or outgoing for this vertex
   * 
   * @author markr
   *
   */
  public final class EdgeSegmentIterable<ESI extends EdgeSegment> implements Iterable<ESI> {

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
     * Shallow copy constructor
     * @param other to copy
     */
    private EdgeSegmentIterable(EdgeSegmentIterable other) {
      this(other.incoming);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EdgeSegmentIterator<ESI> iterator() {
      return new EdgeSegmentIterator<>(incoming);
    }

    /**
     * shallow copy
     */
    public EdgeSegmentIterable<ESI> shallowClone(){ return new EdgeSegmentIterable<>(this);}

  }

  /**
   * Iterator for a run over the incoming or outgoing edge segments of this vertex (non-modifiable)
   * 
   * @author markr
   *
   */
  public class EdgeSegmentIterator<ES extends EdgeSegment> implements Iterator<ES> {

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
    private ES nextEdgeSegment;

    /**
     * Constructor
     * 
     * @param incoming flag
     */
    private EdgeSegmentIterator(boolean incoming) {
      this.incoming = incoming;
      edgesIter = DirectedVertexImpl.this.getEdges().iterator();
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
    @SuppressWarnings("unchecked")
    @Override
    public boolean hasNext() {
      if (nextEdgeSegment != null) {
        return true;
      }

      while (edgesIter.hasNext()) {
        var edge = edgesIter.next();
        if (edge.getVertexA() == DirectedVertexImpl.this) {
          nextEdgeSegment = (ES) (incoming ? edge.getEdgeSegmentBa() : edge.getEdgeSegmentAb());
        } else if (edge.getVertexB() == DirectedVertexImpl.this) {
          nextEdgeSegment = (ES) (incoming ? edge.getEdgeSegmentAb() : edge.getEdgeSegmentBa());
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
    public ES next() {
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
  protected final EdgeSegmentIterable<E> entryEdgeSegments;

  /**
   * Exit edge segments which connect to this vertex
   */
  protected final EdgeSegmentIterable<E> exitEdgeSegments;

  /**
   * Constructor
   * 
   * @param groupId, contiguous id generation within this group for instances of this class
   * @param idClazz  to use for generating the internal id
   */
  protected DirectedVertexImpl(final IdGroupingToken groupId, Class<? extends Vertex> idClazz) {
    super(groupId, idClazz);
    this.entryEdgeSegments = new EdgeSegmentIterable<>(true /* incoming */);
    this.exitEdgeSegments = new EdgeSegmentIterable<>(false /* outgoing */);
  }

  /**
   * Constructor. USe default id class for generating id
   * 
   * @param groupId, contiguous id generation within this group for instances of this class
   */
  protected DirectedVertexImpl(final IdGroupingToken groupId) {
    this(groupId, VERTEX_ID_CLASS);
  }

  /**
   * Constructor. Only to be used when not relying on contiguous id generation within enclosing container
   * 
   * @param id to use
   */
  protected DirectedVertexImpl(long id) {
    super(id);
    this.entryEdgeSegments = new EdgeSegmentIterable<>(true /* incoming */);
    this.exitEdgeSegments = new EdgeSegmentIterable<>(false /* outgoing */);
  }

  /**
   * Copy constructor
   * 
   * @param directedVertexImpl to copy
   */
  protected DirectedVertexImpl(DirectedVertexImpl<E> directedVertexImpl, boolean deepCopy) {
    super(directedVertexImpl, deepCopy);

    // container of non-owned references so always clone required
    this.entryEdgeSegments = directedVertexImpl.entryEdgeSegments.shallowClone();
    this.exitEdgeSegments = directedVertexImpl.exitEdgeSegments.shallowClone();
  }

  // Public

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterable<E> getEntryEdgeSegments() {
    return entryEdgeSegments;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterable<E> getExitEdgeSegments() {
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
  public DirectedVertexImpl<E> shallowClone() {
    return new DirectedVertexImpl<>(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedVertexImpl<E> deepClone() {
    return new DirectedVertexImpl<>(this, true);
  }

}
