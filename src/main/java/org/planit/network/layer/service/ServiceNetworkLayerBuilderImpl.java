package org.planit.network.layer.service;

import java.util.logging.Logger;

import org.planit.graph.DirectedGraphBuilderImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.Edges;
import org.planit.utils.graph.Vertex;
import org.planit.utils.graph.Vertices;
import org.planit.utils.graph.directed.DirectedEdge;
import org.planit.utils.graph.directed.EdgeSegments;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.service.ServiceLeg;
import org.planit.utils.network.layer.service.ServiceLegSegment;
import org.planit.utils.network.layer.service.ServiceNetworkLayerBuilder;
import org.planit.utils.network.layer.service.ServiceNode;

/**
 * Create network entities for a service network layer
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

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateIds(EdgeSegments<? extends ServiceLegSegment> legSegments) {
    /* no service leg segment specific ids yet, so delegate directly to underlying graph builder, if this changes, add functionality here */
    directedGraphBuilderImpl.recreateIds(legSegments);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateIds(Edges<? extends ServiceLeg> legs) {
    /* no service leg specific ids yet, so delegate directly to underlying graph builder, if this changes, add functionality here */
    directedGraphBuilderImpl.recreateIds(legs);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateIds(Vertices<? extends ServiceNode> serviceNodes) {
    /* no service node specific ids yet, so delegate directly to underlying graph builder, if this changes, add functionality here */
    directedGraphBuilderImpl.recreateIds(serviceNodes);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceLeg createUniqueCopyOf(ServiceLeg serviceLegToCopy) {
    /* no service leg specific ids yet, so delegate directly to underlying graph builder, if this changes, add functionality here */
    return (ServiceLeg) directedGraphBuilderImpl.createUniqueCopyOf(serviceLegToCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceLegSegment createUniqueCopyOf(ServiceLegSegment serviceLegSegmentToCopy, DirectedEdge newParentLeg) {
    /* no service leg specific ids yet, so delegate directly to underlying graph builder, if this changes, add functionality here */
    return (ServiceLegSegment) directedGraphBuilderImpl.createUniqueCopyOf(serviceLegSegmentToCopy, newParentLeg);
  }

}
