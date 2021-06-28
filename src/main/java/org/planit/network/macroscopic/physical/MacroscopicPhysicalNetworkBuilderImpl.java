package org.planit.network.macroscopic.physical;

import java.util.Map;
import java.util.logging.Logger;

import org.planit.network.physical.PhysicalNetworkBuilderImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.DirectedEdge;
import org.planit.utils.graph.EdgeSegments;
import org.planit.utils.graph.Edges;
import org.planit.utils.graph.Vertex;
import org.planit.utils.graph.Vertices;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentType;
import org.planit.utils.network.physical.macroscopic.MacroscopicModeProperties;

/**
 * Create network entities for a macroscopic simulation model
 * 
 * @author markr
 *
 */
public class MacroscopicPhysicalNetworkBuilderImpl implements MacroscopicPhysicalNetworkBuilder<Node, Link, MacroscopicLinkSegment> {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(MacroscopicPhysicalNetworkBuilderImpl.class.getCanonicalName());

  /** use physical network builder to create all but link segments */
  protected final PhysicalNetworkBuilderImpl physicalNetworkBuilder;

  /**
   * constructor
   * 
   * @param groupId to use for id generation
   */
  public MacroscopicPhysicalNetworkBuilderImpl(IdGroupingToken groupId) {
    this.physicalNetworkBuilder = new PhysicalNetworkBuilderImpl(groupId);
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
  public MacroscopicLinkSegment createEdgeSegment(DirectedEdge parentLink, boolean directionAB) throws PlanItException {
    if (parentLink instanceof Link) {
      return new MacroscopicLinkSegmentImpl(getIdGroupingToken(), (Link) parentLink, directionAB);
    }
    throw new PlanItException("passed in parent edge is not of type Link, incompatible with Macroscopic network builder");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Node createVertex() {
    return physicalNetworkBuilder.createVertex();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Link createEdge(Vertex nodeA, Vertex nodeB) throws PlanItException {
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
  public void recreateIds(EdgeSegments<? extends MacroscopicLinkSegment> macroscopicinkSegments) {
    physicalNetworkBuilder.recreateIds(macroscopicinkSegments);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateIds(Edges<? extends Link> links) {
    physicalNetworkBuilder.recreateIds(links);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateIds(Vertices<? extends Node> nodes) {
    physicalNetworkBuilder.recreateIds(nodes);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Link createUniqueCopyOf(Link linkToCopy) {
    return physicalNetworkBuilder.createUniqueCopyOf(linkToCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegment createUniqueCopyOf(MacroscopicLinkSegment linkSegmentToCopy, DirectedEdge parentEdge) {
    return (MacroscopicLinkSegmentImpl) physicalNetworkBuilder.createUniqueCopyOf(linkSegmentToCopy, parentEdge);
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
