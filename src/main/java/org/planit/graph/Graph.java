package org.planit.graph;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.Vertex;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.physical.Link;

/**
 * 
 * A graph implementation consisting of vertices and edges
 * 
 * @author markr
 *
 */
public class Graph {
  // INNER CLASSES

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(Graph.class.getCanonicalName());

  /**
   * The id of this graph
   */
  private final long id;

  /**
   * Internal class for all Link specific code
   *
   */
  public class Edges implements Iterable<Edge> {

    /**
     * Map to store edges by their Id
     */
    private Map<Long, Edge> edgeMap;

    /**
     * Add edge to the internal container
     *
     * @param edge edgeto be registered in this network
     * @return edge, in case it overrides an existing edge, the removed edge is returned
     */
    protected Edge registerEdge(final Edge edge) {
      return edgeMap.put(edge.getId(), edge);
    }

    /**
     * Constructor
     */
    public Edges() {
      edgeMap = new TreeMap<Long, Edge>();
    }

    /**
     * Iterator over available edges
     * 
     * @return iterator over edges
     */
    public Iterator<Edge> iterator() {
      return edgeMap.values().iterator();
    }

    /**
     * Iterator over available edges in case one knows the edges are of a derived type
     * 
     * @return iterator
     */
    public Iterator<? extends Edge> derivedIterator() {
      return edgeMap.values().iterator();
    }

    /**
     * Create new edge to graph identified via its id
     *
     * @param vertexA the first vertex of this link
     * @param vertexB the second vertex of this link
     * @param length  the length (in km)
     * @return the created edge
     * @throws PlanItException thrown if there is an error
     */
    public Edge registerNewEdge(final Vertex vertexA, final Vertex vertexB, final double length) throws PlanItException {
      final Edge newEdge = graphBuilder.createEdge(vertexA, vertexB, length);
      registerEdge(newEdge);
      return newEdge;
    }

    /**
     * Get edge by id
     *
     * @param id the id of the edge
     * @return the retrieved edge
     */
    public Edge getEdge(final long id) {
      return edgeMap.get(id);
    }

    /**
     * Get the number of edges in the graph
     *
     * @return the number of edges in the graph
     */
    public int getNumberOfEdges() {
      return edgeMap.size();
    }
  }

  /**
   * Internal class for EdgeSegment (directed links)
   *
   */
  public class EdgeSegments implements Iterable<EdgeSegment> {

    /**
     * Map to store edge segments by their Id
     */
    private Map<Long, EdgeSegment> edgeSegmentMap;

    /**
     * Register a link segment on the network
     *
     * @param edgeSegment the link segment to be registered
     * @throws PlanItException thrown if the current link segment external Id has already been assigned
     */
    protected void registerEdgeSegment(final EdgeSegment edgeSegment) throws PlanItException {
      edgeSegmentMap.put(edgeSegment.getId(), edgeSegment);
    }

    /**
     * Constructor
     */
    public EdgeSegments() {
      edgeSegmentMap = new TreeMap<Long, EdgeSegment>();
    }

    /**
     * Iterator over available edge segments
     */
    @Override
    public Iterator<EdgeSegment> iterator() {
      return edgeSegmentMap.values().iterator();
    }

    /**
     * Create edge segment
     *
     * @param parentEdge  the parent edge of this edge segment
     * @param directionAB direction of travel
     * @return the created edge segment
     * @throws PlanItException thrown if there is an error
     */
    public EdgeSegment createEdgeSegment(final Link parentLink, final boolean directionAB) throws PlanItException {
      final EdgeSegment edgeSegment = graphBuilder.createEdgeSegment(parentLink, directionAB);
      return edgeSegment;
    }

    /**
     * Register a edge segment
     *
     * @param parentEdge  the parent edge which specified edge segment will be registered on
     * @param edgeSegment edge segment to be registered
     * @param directionAB direction of travel
     * @throws PlanItException thrown if there is an error
     */
    public void registerEdgeSegment(final Edge parentEdge, final EdgeSegment edgeSegment, final boolean directionAB) throws PlanItException {
      parentEdge.registerEdgeSegment(edgeSegment, directionAB);
      registerEdgeSegment(edgeSegment);
    }

    /**
     * Get edge segment by id
     *
     * @param id id of the edge segment
     * @return retrieved edge Segment
     */
    public EdgeSegment getEdgeSegment(final long id) {
      return edgeSegmentMap.get(id);
    }

    /**
     * Return number of registered edge segments
     *
     * @return number of registered edge segments
     */
    public int getNumberOfEdgeSegments() {
      return edgeSegmentMap.size();
    }

  }

  /**
   * Internal class for all Vertex specific code
   */
  public class Vertices implements Iterable<Vertex> {

    /**
     * Map to store nodes by their Id
     */
    private Map<Long, Vertex> vertexMap;

    /**
     * Add node to the internal container
     *
     * @param node node to be registered in this network
     * @return node, in case it overrides an existing node, the removed node is returned
     */
    protected Vertex registerVertex(final Vertex node) {
      return vertexMap.put(node.getId(), node);
    }

    /**
     * Constructor
     */
    public Vertices() {
      vertexMap = new TreeMap<Long, Vertex>();
    }

    /**
     * Iterator over available vertices
     */
    @Override
    public Iterator<Vertex> iterator() {
      return vertexMap.values().iterator();
    }

    /**
     * Create and register new vertex
     *
     * @return new node created
     */
    public Vertex registerNewNode() {
      final Vertex newVertex = graphBuilder.createVertex();
      registerVertex(newVertex);
      return newVertex;
    }

    /**
     * Create and register new vertex
     *
     * @param externalId the externalId of the vertex
     * @return new vertex created
     */
    public Vertex registerNewVertex(Object externalId) {
      final Vertex newVertex = graphBuilder.createVertex();
      newVertex.setExternalId(externalId);
      registerVertex(newVertex);
      return newVertex;
    }

    /**
     * Return number of registered vertices
     *
     * @return number of registered vertices
     */
    public int getNumberOfVertices() {
      return vertexMap.size();
    }

    /**
     * Find a vertex by its d
     *
     * @param id Id of vertex
     * @return retrieved vertex
     */
    public Vertex getVertexById(final long id) {
      return vertexMap.get(id);
    }

  }

  // Private

  // Protected

  /**
   * Graph builder responsible for constructing all graph related (derived) instances
   */
  protected final GraphBuilder graphBuilder;

  // PUBLIC

  /**
   * internal class instance containing all edges
   */
  public final Edges edges = new Edges();

  /**
   * internal class instance containing all link segments
   */
  public final EdgeSegments edgeSegments = new EdgeSegments();

  /**
   * internal class instance containing all vertices
   */
  public final Vertices vertices = new Vertices();

  /**
   * Graph Constructor
   *
   * @param groupId        contiguous id generation within this group for instances of this class
   * @param networkBuilder the builder to be used to create this network
   */
  public Graph(final IdGroupingToken groupId, final GraphBuilder graphBuilder) {
    this.id = IdGenerator.generateId(groupId, Graph.class);
    this.graphBuilder = graphBuilder;
    this.graphBuilder.setIdGroupingToken(IdGenerator.createIdGroupingToken(this, this.getId()));
  }

  // Getters - Setters

  /**
   * collect the id of this graph
   * 
   * @return graph id
   */
  public long getId() {
    return this.id;
  }

  /**
   * Collect the id grouping token used for all entities registered on the graph, i.e., this network's specific identifier for generating ids unique and contiguous within this
   * network and this network only
   * 
   * @return the graph id grouping token
   */
  public IdGroupingToken getGraphIdGroupingToken() {
    return this.graphBuilder.getIdGroupingToken();
  }

}
