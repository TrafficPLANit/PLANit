package org.goplanit.algorithms.shortest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.goplanit.graph.directed.acyclic.ACyclicSubGraph;
import org.goplanit.graph.directed.acyclic.ACyclicSubGraphImpl;
import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.graph.directed.DirectedVertex;
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
public class ShortestBushResultImpl implements ShortestBushResult {

  private static final Logger LOGGER = Logger.getLogger(ShortestPathResult.class.getCanonicalName());

  /**
   * the costs found by a shortest path run
   */
  protected final double[] vertexMeasuredCost;

  /**
   * the preceding edge segment(s) to reach the vertex with the given measured cost. If only a single edge segment is present, that is what is stored, otherwise it is a list of
   * edge segments
   */
  protected final Object[] incomingEdgeSegments;

  /** number of edge segments in the parent network */
  protected final int numberOfEdgeSegments;

  /** origin of the bush */
  protected final DirectedVertex origin;

  /**
   * Constructor only to be used by shortest path algorithms
   * 
   * @param origin               to use
   * @param vertexMeasuredCost   measured costs to get to the vertex (by id)
   * @param incomingEdgeSegments the incoming edge segment for each vertex (by id)
   * @param numberOfEdgeSegments on the parent network
   */
  protected ShortestBushResultImpl(DirectedVertex origin, double[] vertexMeasuredCost, Object[] incomingEdgeSegments, int numberOfEdgeSegments) {
    this.origin = origin;
    this.vertexMeasuredCost = vertexMeasuredCost;
    this.incomingEdgeSegments = incomingEdgeSegments;
    this.numberOfEdgeSegments = numberOfEdgeSegments;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ACyclicSubGraph createDirectedAcyclicSubGraph(final IdGroupingToken idToken, final DirectedVertex destination) {

    var dag = new ACyclicSubGraphImpl(idToken, numberOfEdgeSegments, origin);

    // extract bush from destination -> backwards to origin
    TreeSet<Vertex> openVertices = new TreeSet<Vertex>();
    Set<Vertex> processedVertices = new HashSet<Vertex>();
    openVertices.add(destination);
    while (!openVertices.isEmpty()) {
      var currVertex = openVertices.first();
      openVertices.remove(currVertex);

      int currVertexId = (int) destination.getId();
      var previousEdgeSegmentsOnPath = incomingEdgeSegments[currVertexId];
      if (previousEdgeSegmentsOnPath == null) {
        /* unable to continue populating bush */
        return null;
      }

      /* add all eligible upstream segments to the dag and register their upstream vertices (if unprocessed) for further processing */
      List<EdgeSegment> eligibleUpstreamEdgeSegments = getIncomingEdgeSegmentsForVertex(currVertex);
      if (!CollectionUtils.nullOrEmpty(eligibleUpstreamEdgeSegments)) {
        for (var edgeSegment : eligibleUpstreamEdgeSegments) {
          dag.addEdgeSegment((EdgeSegment) edgeSegment);
          if (!processedVertices.contains(edgeSegment.getUpstreamVertex())) {
            openVertices.add(edgeSegment.getUpstreamVertex());
          }
        }
      } else if (!currVertex.equals(origin)) {
        LOGGER.warning(String.format("No entry segments found for non-origin vertex (%s) on shortest-bush, this shouldn't happen", currVertex.getXmlId()));
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
  public List<EdgeSegment> getIncomingEdgeSegmentsForVertex(Vertex vertex) {
    var incomingEdgeSegmentsAtVertex = incomingEdgeSegments[(int) vertex.getId()];
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
