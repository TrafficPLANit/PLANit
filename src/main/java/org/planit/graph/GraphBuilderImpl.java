package org.planit.graph;

import java.util.logging.Logger;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.Edges;
import org.planit.utils.graph.Vertex;
import org.planit.utils.graph.Vertices;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;

/**
 * Create network entities for a physical network simulation model
 * 
 * @author markr
 *
 */
public class GraphBuilderImpl implements GraphBuilder<Vertex, Edge> {

  private static final Logger LOGGER = Logger.getLogger(GraphBuilderImpl.class.getCanonicalName());

  /** the id group token */
  protected IdGroupingToken groupIdToken;

  /**
   * Constructor
   * 
   * @param groupIdToken to use for creating element ids
   */
  public GraphBuilderImpl(IdGroupingToken groupIdToken) {
    this.groupIdToken = groupIdToken;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Vertex createVertex() {
    return new DirectedVertexImpl(getIdGroupingToken());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Edge createEdge(Vertex vertexA, Vertex vertexB, final double length) throws PlanItException {
    return new EdgeImpl(getIdGroupingToken(), vertexA, vertexB, length);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setIdGroupingToken(IdGroupingToken groupIdToken) {
    this.groupIdToken = groupIdToken;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IdGroupingToken getIdGroupingToken() {
    return this.groupIdToken;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateIds(Edges<? extends Edge> edges) {
    if (edges instanceof EdgesImpl<?, ?>) {
      /* remove gaps by simply resetting and recreating all edge ids */
      IdGenerator.reset(getIdGroupingToken(), Edge.class);

      for (Edge edge : edges) {
        if (edge instanceof EdgeImpl) {
          ((EdgeImpl) edge).setId(EdgeImpl.generateEdgeId(getIdGroupingToken()));
        } else {
          LOGGER.severe(String.format("attempting to reset id on edge (%s) that is not compatible with the edge implementation generated by this builder, ignored",
              edge.getClass().getCanonicalName()));
        }
      }

      ((EdgesImpl<?, ?>) edges).updateIdMapping();
    } else {
      LOGGER.severe("expected the Edges implementation to be compatible with graph builder, this is not the case: unable to correctly remove subnetwork and update ids");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateIds(Vertices<? extends Vertex> vertices) {
    if (vertices instanceof VerticesImpl<?>) {
      /* remove gaps by simply resetting and recreating all vertex ids */
      IdGenerator.reset(getIdGroupingToken(), Vertex.class);

      for (Vertex vertex : vertices) {
        if (vertex instanceof VertexImpl) {
          ((VertexImpl) vertex).setId(VertexImpl.generateVertexId(getIdGroupingToken()));
        } else {
          LOGGER.severe(String.format("attempting to reset id on vertex (%s) that is not compatible with the edge implementation generated by this builder, ignored",
              vertex.getClass().getCanonicalName()));
        }
      }
      ((VerticesImpl<?>) vertices).updateIdMapping();
    } else {
      LOGGER.severe("expected the Vertices implementation to be compatible with graph builder, this is not the case: unable to correctly remove subnetwork and update ids");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Edge createUniqueCopyOf(Edge edgeToCopy) {
    if (edgeToCopy instanceof EdgeImpl) {
      /* shallow copy as is */
      EdgeImpl copy = (EdgeImpl) edgeToCopy.clone();
      /* make unique copy by updating id */
      copy.setId(EdgeImpl.generateEdgeId(getIdGroupingToken()));
      return copy;
    }
    LOGGER.severe("passed in edge is not an instance created by this builder, incompatible for creating a copy");
    return null;
  }

}
