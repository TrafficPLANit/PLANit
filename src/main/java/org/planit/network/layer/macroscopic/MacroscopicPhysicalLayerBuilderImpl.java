package org.planit.network.layer.macroscopic;

import java.util.Map;
import java.util.logging.Logger;

import org.planit.network.layer.physical.LinkImpl;
import org.planit.network.layer.physical.NodeImpl;
import org.planit.network.layer.physical.PhysicalLayerBuilderImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.DirectedEdge;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.EdgeSegments;
import org.planit.utils.graph.Edges;
import org.planit.utils.graph.Vertex;
import org.planit.utils.graph.Vertices;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.planit.utils.network.layer.macroscopic.MacroscopicModeProperties;
import org.planit.utils.network.layer.macroscopic.MacroscopicPhysicalLayerBuilder;
import org.planit.utils.network.layer.physical.Link;

/**
 * Create network entities for a macroscopic simulation model
 * 
 * @author markr
 *
 */
public class MacroscopicPhysicalLayerBuilderImpl implements MacroscopicPhysicalLayerBuilder<NodeImpl, LinkImpl, MacroscopicLinkSegmentImpl> {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(MacroscopicPhysicalLayerBuilderImpl.class.getCanonicalName());

  /** use physical network builder to create all but link segments */
  protected final PhysicalLayerBuilderImpl physicalNetworkBuilder;

  /**
   * constructor
   * 
   * @param groupId to use for id generation
   */
  public MacroscopicPhysicalLayerBuilderImpl(IdGroupingToken groupId) {
    this.physicalNetworkBuilder = new PhysicalLayerBuilderImpl(groupId);
  }

  /**
   * {@inheritDoc}
   */
  public MacroscopicLinkSegmentType createLinkSegmentType(String name, double capacity, double maximumDensity, Map<Mode, MacroscopicModeProperties> modeProperties) {
    return new MacroscopicLinkSegmentTypeImpl(getIdGroupingToken(), name, capacity, maximumDensity, modeProperties);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegmentType createLinkSegmentType(String name, double capacity, double maximumDensity) {
    return new MacroscopicLinkSegmentTypeImpl(getIdGroupingToken(), name, capacity, maximumDensity);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegmentImpl createEdgeSegment(DirectedEdge parentLink, boolean directionAB) throws PlanItException {
    if (parentLink instanceof Link) {
      return new MacroscopicLinkSegmentImpl(getIdGroupingToken(), (Link) parentLink, directionAB);
    }
    throw new PlanItException("passed in parent edge is not of type Link, incompatible with Macroscopic network builder");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NodeImpl createVertex() {
    return physicalNetworkBuilder.createVertex();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LinkImpl createEdge(Vertex nodeA, Vertex nodeB) throws PlanItException {
    return physicalNetworkBuilder.createEdge(nodeA, nodeB);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setIdGroupingToken(IdGroupingToken groupId) {
    physicalNetworkBuilder.setIdGroupingToken(groupId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IdGroupingToken getIdGroupingToken() {
    return physicalNetworkBuilder.getIdGroupingToken();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateIds(EdgeSegments<? extends EdgeSegment> macroscopicinkSegments) {
    physicalNetworkBuilder.recreateIds(macroscopicinkSegments);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateIds(Edges<? extends Edge> links) {
    physicalNetworkBuilder.recreateIds(links);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateIds(Vertices<? extends Vertex> nodes) {
    physicalNetworkBuilder.recreateIds(nodes);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LinkImpl createUniqueCopyOf(LinkImpl linkToCopy) {
    return physicalNetworkBuilder.createUniqueCopyOf(linkToCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegmentImpl createUniqueCopyOf(MacroscopicLinkSegmentImpl linkSegmentToCopy, LinkImpl parentLink) {
    return (MacroscopicLinkSegmentImpl) physicalNetworkBuilder.createUniqueCopyOf(linkSegmentToCopy, parentLink);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegmentType createUniqueCopyOf(MacroscopicLinkSegmentType linkSegmentTypeToCopy) {
    if (linkSegmentTypeToCopy instanceof MacroscopicLinkSegmentTypeImpl) {
      /* shallow copy as is */
      MacroscopicLinkSegmentTypeImpl copy = (MacroscopicLinkSegmentTypeImpl) linkSegmentTypeToCopy.clone();
      /* make unique copy by updating id */
      copy.setId(MacroscopicLinkSegmentTypeImpl.generateMacroscopicLinkSegmentTypeId(getIdGroupingToken()));
      return copy;
    }
    LOGGER.severe("passed in link segment type is not an instance created by this builder, incompatible for creating a copy");
    return null;
  }

}
