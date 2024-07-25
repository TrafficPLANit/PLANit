package org.goplanit.algorithms.shortest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.goplanit.graph.directed.acyclic.ACyclicSubGraphImpl;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.graph.directed.acyclic.ACyclicSubGraph;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.CollectionUtils;

/**
 * Class that stores the result of a shortest bush execution allowing one to extract bushes or cost information for a given origin-to-vertex
 * 
 * Note that we construct the bush in reverse order from destination to the origin via all viable paths to construct the bush
 * 
 * @author markr
 *
 */
public class ShortestBushResultGeneralised extends ShortestResultGeneralised implements ShortestBushResult {

  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(ShortestBushResultGeneralised.class.getCanonicalName());

  /**
   * the next edge segment(s) to reach the vertex with the given measured cost. If only a single edge segment is present, that is what is stored, otherwise it is a list of edge
   * segments
   */
  protected final Object[] nextEdgeSegments;

  /** number of edge segments in the parent network */
  protected final int numberOfEdgeSegments;

  /**
   * Constructor only to be used by shortest path algorithms
   * 
   * @param vertexMeasuredCost       measured costs to get to the vertex (by id)
   * @param nextEdgeSegmentsByVertex the found next edge segment for each vertex (by id)
   * @param numberOfEdgeSegments     on the parent network
   * @param searchType               used (one-to-all, all-to-one, etc)
   */
  protected ShortestBushResultGeneralised(double[] vertexMeasuredCost, Object[] nextEdgeSegmentsByVertex, int numberOfEdgeSegments, ShortestSearchType searchType) {
    super(vertexMeasuredCost, searchType);
    this.nextEdgeSegments = nextEdgeSegmentsByVertex;
    this.numberOfEdgeSegments = numberOfEdgeSegments;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ACyclicSubGraph createDirectedAcyclicSubGraph(final IdGroupingToken idToken, final DirectedVertex origin, final DirectedVertex destination) {

    var startEndPair = getStartEndVertexForResultTraversal(origin, destination);
    DirectedVertex startSearchVertex = startEndPair.first();
    DirectedVertex endSearchVertex = startEndPair.second(); // is also root of dag since we always end up back at the root while traversing search result

    var dag = new ACyclicSubGraphImpl(idToken, endSearchVertex, isInverted(), numberOfEdgeSegments);

    // extract bush -> backwards to root
    TreeSet<Vertex> openVertices = new TreeSet<>();
    Set<Vertex> processedVertices = new HashSet<>();
    openVertices.add(startSearchVertex);
    while (!openVertices.isEmpty()) {
      var currVertex = openVertices.first();
      openVertices.remove(currVertex);

      /* add all eligible upstream segments to the dag and register their upstream vertices (if unprocessed) for further processing */
      List<EdgeSegment> eligibleNextEdgeSegments = getNextEdgeSegmentsForVertex(currVertex);
      if (!CollectionUtils.nullOrEmpty(eligibleNextEdgeSegments)) {
        for (var edgeSegment : eligibleNextEdgeSegments) {
          dag.addEdgeSegment((EdgeSegment) edgeSegment);
          DirectedVertex nextVertex = this.getVertexAtExtreme.apply(edgeSegment);
          if (!processedVertices.contains(nextVertex)) {
            openVertices.add(nextVertex);
          }
        }
      }

      processedVertices.add(currVertex);
    }

    return dag;
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public List<EdgeSegment> getNextEdgeSegmentsForVertex(Vertex vertex) {
    var incomingEdgeSegmentsAtVertex = nextEdgeSegments[(int) vertex.getId()];
    if (incomingEdgeSegmentsAtVertex instanceof EdgeSegment) {
      return List.of((EdgeSegment) incomingEdgeSegmentsAtVertex);
    } else {
      return (List<EdgeSegment>) incomingEdgeSegmentsAtVertex;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getCostToReach(Vertex vertex) {
    return vertexMeasuredCost[(int) vertex.getId()];
  }

}
