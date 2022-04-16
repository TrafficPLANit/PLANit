package org.goplanit.graph.directed;

import java.util.logging.Logger;

import org.goplanit.utils.graph.directed.ConjugateDirectedEdge;
import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.Pair;

/**
 * Conjugate Edge implementation class connecting two vertices via some geometry. Each edge has one or two underlying edge segments in a particular direction which may carry
 * additional information for each particular direction of the edge.
 *
 * @author markr
 *
 */
public class ConjugateDirectedEdgeImpl extends DirectedEdgeImpl<ConjugateDirectedVertex, ConjugateEdgeSegment> implements ConjugateDirectedEdge {

  // Protected

  /** generated UID */
  private static final long serialVersionUID = -3061186642253968991L;

  /** the logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(ConjugateDirectedEdgeImpl.class.getCanonicalName());

  /**
   * adjacent original directed edges represented by this conjugate
   */
  protected final Pair<DirectedEdge, DirectedEdge> originalEdges;

  /**
   * Constructor
   *
   * @param groupId, contiguous id generation within this group for instances of this class
   * @param vertexA  first conjugate vertex in the link
   * @param vertexB  second conjugate vertex in the link
   */
  protected ConjugateDirectedEdgeImpl(final IdGroupingToken groupId, final ConjugateDirectedVertex vertexA, final ConjugateDirectedVertex vertexB, final DirectedEdge originalEdge1,
      final DirectedEdge originalEdge2) {
    super(groupId, vertexA, vertexB);
    this.originalEdges = Pair.of(originalEdge1, originalEdge2);
  }

  /**
   * Copy Constructor. Edge segments are shallow copied and point to the passed in edge as their parent So additional effort is needed to make the new edge usable
   * 
   * @param conjugateDirectedEdgeImpl to copy
   */
  protected ConjugateDirectedEdgeImpl(ConjugateDirectedEdgeImpl conjugateDirectedEdgeImpl) {
    super(conjugateDirectedEdgeImpl);
    this.originalEdges = conjugateDirectedEdgeImpl.originalEdges.copy();
  }

  // Protected

  /**
   * {@inheritDoc}
   */
  @Override
  public Pair<DirectedEdge, DirectedEdge> getOriginalEdges() {
    return originalEdges;
  }

}
