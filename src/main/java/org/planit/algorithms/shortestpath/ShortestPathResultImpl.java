package org.planit.algorithms.shortestpath;

import java.util.Deque;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.Vertex;
import org.planit.utils.path.DirectedPath;
import org.planit.utils.path.DirectedPathFactory;

/**
 * Class that stores the result of a shortest path execution allowing one to extract paths or cost information
 * 
 * Note that we must traverse a path from an origin to a destination in reversed order to extract the path
 * 
 * @author markr
 *
 */
public class ShortestPathResultImpl implements ShortestPathResult {

  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(ShortestPathResult.class.getCanonicalName());

  /**
   * the costs found by a shortest path run
   */
  protected final double[] vertexMeasuredCost;

  /**
   * the preceding vertex to reach the vertex with the given measured cost
   */
  protected final EdgeSegment[] incomingEdgeSegment;

  /**
   * Constructor only to be used by shortest path algorithms
   * 
   * @param vertexMeasuredCost  measured costs to get to the vertex (by id)
   * @param incomingEdgeSegment the incoming edge segment for each vertex (by id)
   */
  protected ShortestPathResultImpl(double[] vertexMeasuredCost, EdgeSegment[] incomingEdgeSegment) {
    this.vertexMeasuredCost = vertexMeasuredCost;
    this.incomingEdgeSegment = incomingEdgeSegment;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedPath createPath(final DirectedPathFactory pathFactory, Vertex origin, Vertex destination) {
    // path edge segment container
    final Deque<EdgeSegment> pathEdgeSegments = new LinkedList<EdgeSegment>();

    // prep
    int vertexId = (int) destination.getId();
    EdgeSegment previousEdgeSegmentOnPath = incomingEdgeSegment[vertexId];
    final int originVertexId = (int) origin.getId();

    // extract path
    while (originVertexId != vertexId) {
      if (previousEdgeSegmentOnPath == null) {
        /* unable to create path */
        return null;
      }
      pathEdgeSegments.addFirst(previousEdgeSegmentOnPath);
      vertexId = (int) previousEdgeSegmentOnPath.getUpstreamVertex().getId();
      previousEdgeSegmentOnPath = incomingEdgeSegment[vertexId];
    }

    // create path
    return pathFactory.createNew(pathEdgeSegments);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegment getIncomingEdgeSegmentForVertex(Vertex vertex) {
    return incomingEdgeSegment[(int) vertex.getId()];
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getCostToReach(Vertex vertex) {
    return vertexMeasuredCost[(int) vertex.getId()];
  }

}
