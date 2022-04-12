package org.goplanit.algorithms.shortest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.logging.Logger;

import org.goplanit.graph.directed.acyclic.ACyclicSubGraph;
import org.goplanit.graph.directed.acyclic.ACyclicSubGraphImpl;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
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
public class ShortestBushResultGeneralised implements ShortestBushResult {

  private static final Logger LOGGER = Logger.getLogger(ShortestBushResultGeneralised.class.getCanonicalName());

  /**
   * the costs found by a shortest path run
   */
  protected final double[] vertexMeasuredCost;

  /**
   * the next edge segment(s) to reach the vertex with the given measured cost. If only a single edge segment is present, that is what is stored, otherwise it is a list of edge
   * segments
   */
  protected final Object[] nextEdgeSegments;

  /** number of edge segments in the parent network */
  protected final int numberOfEdgeSegments;

  /** depending on configuration this function collects vertex at desired edge segment extremity */
  protected Function<EdgeSegment, DirectedVertex> getVertexAtExtreme;

  /**
   * Constructor only to be used by shortest path algorithms
   * 
   * @param origin               to use
   * @param vertexMeasuredCost   measured costs to get to the vertex (by id)
   * @param nextEdgeSegments     the found next edge segment for each vertex (by id)
   * @param numberOfEdgeSegments on the parent network
   * @param searchType           used (one-to-all, all-to-one, etc)
   */
  protected ShortestBushResultGeneralised(double[] vertexMeasuredCost, Object[] nextEdgeSegments, int numberOfEdgeSegments, ShortestSearchType searchType) {
    this.vertexMeasuredCost = vertexMeasuredCost;
    this.nextEdgeSegments = nextEdgeSegments;
    this.numberOfEdgeSegments = numberOfEdgeSegments;

    /* search direction for creating paths in opposite direction as compared to shortest bush search itself */
    this.getVertexAtExtreme = ShortestPathSearchUtils.getVertexFromEdgeSegmentLambda(searchType, true /* invert */ );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ACyclicSubGraph createDirectedAcyclicSubGraph(final IdGroupingToken idToken, final DirectedVertex origin, final DirectedVertex destination) {

    var dag = new ACyclicSubGraphImpl(idToken, origin, false /* not inverted */, numberOfEdgeSegments);

    // extract bush from destination -> backwards to origin
    TreeSet<Vertex> openVertices = new TreeSet<Vertex>();
    Set<Vertex> processedVertices = new HashSet<Vertex>();
    openVertices.add(destination);
    while (!openVertices.isEmpty()) {
      var currVertex = openVertices.first();
      openVertices.remove(currVertex);

      int currVertexId = (int) destination.getId();
      var previousEdgeSegmentsOnPath = nextEdgeSegments[currVertexId];
      if (previousEdgeSegmentsOnPath == null) {
        /* unable to continue populating bush */
        return null;
      }

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
      } else if (!currVertex.equals(origin)) {
        LOGGER.warning(String.format("No eligible next segments found for regular vertex (%s) on shortest-bush, this shouldn't happen", currVertex.getXmlId()));
        continue;
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
  public double getCostOf(Vertex vertex) {
    return vertexMeasuredCost[(int) vertex.getId()];
  }

}
