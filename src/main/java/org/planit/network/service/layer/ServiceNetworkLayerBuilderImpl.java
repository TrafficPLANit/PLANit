package org.planit.network.service.layer;

import java.util.logging.Logger;

import org.planit.graph.DirectedGraphBuilderImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.DirectedEdge;
import org.planit.utils.graph.EdgeSegments;
import org.planit.utils.graph.Edges;
import org.planit.utils.graph.Vertex;
import org.planit.utils.graph.Vertices;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.service.ServiceLeg;
import org.planit.utils.network.service.ServiceLegSegment;
import org.planit.utils.network.service.ServiceNetworkLayerBuilder;
import org.planit.utils.network.service.ServiceNode;

/**
 * Create network entities for a physical network simulation model
 * 
 * @author markr
 *
 */
public class ServiceNetworkLayerBuilderImpl implements ServiceNetworkLayerBuilder {

  /** the logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(ServiceNetworkLayerBuilderImpl.class.getCanonicalName());

  /** hold an implementation of directed graph builder to use its overlapping functionality */
  protected DirectedGraphBuilderImpl directedGraphBuilderImpl;

  // Public methods

  /**
   * Constructor
   * 
   * @param tokenId to use for id generation
   */
  public ServiceNetworkLayerBuilderImpl(IdGroupingToken tokenId) {
    this.directedGraphBuilderImpl = new DirectedGraphBuilderImpl(tokenId);
  }

  /**
   * Create a service node. Requires user to set the underlying network node afterwards before the service node can be regarded as valid
   * 
   * @return created empty service node instance
   */
  @Override
  public ServiceNode createVertex() {
    return new ServiceNodeImpl(getIdGroupingToken());
  }

  /**
   * Create a service leg. Requires user to set the underlying network links that determine the actual leg before the leg can be regarded as valid
   */
  @Override
  public ServiceLeg createEdge(Vertex nodeA, Vertex nodeB) throws PlanItException {
    if (nodeA instanceof ServiceNode && nodeB instanceof ServiceNode) {
      return new ServiceLegImpl(getIdGroupingToken(), (ServiceNode) nodeA, (ServiceNode) nodeB);
    } else {
      throw new PlanItException("unable to create service leg, vertices should be of type ServiceNode");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceLegSegment createEdgeSegment(DirectedEdge parentLeg, boolean directionAB) throws PlanItException {
    if (parentLeg instanceof ServiceLeg) {
      return new ServiceLegSegmentImpl(getIdGroupingToken(), (ServiceLeg) parentLeg, directionAB);
    } else {
      throw new PlanItException("unable to create service leg segment, parent should be of type ServiceLeg");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setIdGroupingToken(IdGroupingToken groupToken) {
    directedGraphBuilderImpl.setIdGroupingToken(groupToken);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IdGroupingToken getIdGroupingToken() {
    return directedGraphBuilderImpl.getIdGroupingToken();
  }

  @Override
  public void recreateIds(EdgeSegments<? extends ServiceLegSegment> edgeSegments) {
    // TODO Auto-generated method stub
  }

  @Override
  public ServiceLegSegment createUniqueCopyOf(ServiceLegSegment edgeSegmentToCopy, DirectedEdge newParentEdge) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void recreateIds(Edges<? extends ServiceLeg> edges) {
    // TODO Auto-generated method stub

  }

  @Override
  public void recreateIds(Vertices<? extends ServiceNode> vertices) {
    // TODO Auto-generated method stub

  }

  @Override
  public ServiceLeg createUniqueCopyOf(ServiceLeg edgeToCopy) {
    // TODO Auto-generated method stub
    return null;
  }

}
