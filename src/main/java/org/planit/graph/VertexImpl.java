package org.planit.graph;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.opengis.geometry.DirectPosition;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.Vertex;
import org.planit.utils.misc.IdGenerator;
import org.planit.utils.network.physical.Link;

/**
 * Node representation connected to one or more entry and exit links
 *
 * @author markr
 *
 */
public class VertexImpl implements Vertex {

  /** generated UID */
  private static final long serialVersionUID = 2165199386965239623L;

  /**
   * edges of this vertex
   *
   * @author markr
   */
  public class EdgesImpl implements Edges {

    protected Set<Edge> edges = new TreeSet<Edge>();

    /**
     * #{@inheritDoc}
     */
    @Override
    public boolean addEdge(final Edge edge) {
      return edges.add(edge);
    }

    /**
     * #{@inheritDoc}
     */
    @Override
    public boolean removeEdge(final Edge edge) {
      return edges.remove(edge);
    }

    /**
     * #{@inheritDoc}
     */
    @Override
    public Set<Edge> getEdges() {
      return edges;
    }
  }

  /**
   * EdgeSegment container
   *
   * @author markr
   */
  public class EdgeSegmentsImpl implements EdgeSegments {

    /**
     * Edge segments which connect to this vertex
     */
    protected Set<EdgeSegment> edgeSegments = new TreeSet<EdgeSegment>();

    /**
     * #{@inheritDoc}
     */
    @Override
    public boolean addEdgeSegment(final EdgeSegment edgeSegment) {
      return edgeSegments.add(edgeSegment);
    }

    /**
     * #{@inheritDoc}
     */
    @Override
    public boolean removeEdgeSegment(final EdgeSegment edgeSegment) {
      return edgeSegments.remove(edgeSegment);
    }

    /**
     * #{@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
      return edgeSegments.isEmpty();
    }

    /**
     * #{@inheritDoc}
     */
    @Override
    public int getNumberOfEdges() {
      return edgeSegments.size();
    }

    /**
     * Iterator over available edge segments
     */
    @Override
    public Iterator<EdgeSegment> iterator() {
      return edgeSegments.iterator();
    }
  }

  // Protected

  /**
   * Centre point geometry which is coordinate reference system aware
   */
  protected DirectPosition centrePointGeometry;

  /**
   * generate unique node id
   *
   * @return nodeId
   */
  protected static int generateVertexId() {
    return IdGenerator.generateId(Vertex.class);
  }

  /**
   * Unique internal identifier
   */
  protected final long id;

  /**
   * generic input property storage
   */
  protected Map<String, Object> inputProperties = null;

  /**
   * edge container
   */
  protected final Edges edges = new EdgesImpl();

  /**
   * exitEdgeSegmentcontainer
   */
  protected final EdgeSegments exitEdgeSegments = new EdgeSegmentsImpl();

  /**
   * entryEdgeSegment container
   */
  protected final EdgeSegments entryEdgeSegments = new EdgeSegmentsImpl();

  /**
   * Constructor
   */
  protected VertexImpl() {
    this.id = generateVertexId();
  }

  // Public

  /**
   * Collect the geometry of the point location of this vertex
   * 
   * @return direct position reflecting point location
   */
  public DirectPosition getCentrePointGeometry() {
    return centrePointGeometry;
  }

  /**
   * #{@inheritDoc}
   */
  @Override
  public void setCentrePointGeometry(final DirectPosition centrePointGeometry) {
    this.centrePointGeometry = centrePointGeometry;
  }

  // Getters-Setters

  /**
   * #{@inheritDoc}
   */
  @Override
  public long getId() {
    return id;
  }

  /**
   * Add a property from the original input that is not part of the readily
   * available members
   *
   * @param key key (name) of the input property
   * @param value value of the input property
   */
  public void addInputProperty(final String key, final Object value) {
    if (inputProperties == null) {
      inputProperties = new HashMap<String, Object>();
    }
    inputProperties.put(key, value);
  }

  /**
   * Get input property by its key
   *
   * @param key the key of the input property
   * @return value the value of the input property
   */
  public Object getInputProperty(final String key) {
    return inputProperties.get(key);
  }

  /**
   * Compare vertices by their id
   *
   * @param o Vertex object to be compared to this one
   * @return result of comparison
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(final Vertex o) {
    return Long.valueOf(id).compareTo(o.getId());
  }

  /**
   * #{@inheritDoc}
   */
  @Override
  public EdgeSegments getEntryEdgeSegments() {
    return entryEdgeSegments;
  }

  /**
   * #{@inheritDoc}
   */
  @Override
  public EdgeSegments getExitEdgeSegments() {
    return entryEdgeSegments;
  }

  /**
   * #{@inheritDoc}
   */
  @Override
  public Edges getEdges() {
    return edges;
  }

}
