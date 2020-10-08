package org.planit.network.physical.macroscopic;

import java.util.Map;
import java.util.logging.Logger;

import org.planit.network.physical.PhysicalNetworkBuilderImpl;
import org.planit.utils.exceptions.PlanItException;
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
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(MacroscopicPhysicalNetworkBuilderImpl.class.getCanonicalName());

  /** use physical network builder to create all but link segments */
  protected final PhysicalNetworkBuilderImpl physicalNetworkBuilder = new PhysicalNetworkBuilderImpl();

  /**
   * {@inheritDoc}
   */
  public MacroscopicLinkSegmentType createLinkSegmentType(String name, double capacity, double maximumDensity, Object externalId,
      Map<Mode, MacroscopicModeProperties> modeProperties) {
    return new MacroscopicLinkSegmentTypeImpl(getIdGroupingToken(), name, capacity, maximumDensity, externalId, modeProperties);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegmentType createLinkSegmentType(String name, double capacity, double maximumDensity, Object externalId) {
    return new MacroscopicLinkSegmentTypeImpl(getIdGroupingToken(), name, capacity, maximumDensity, externalId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegment createEdgeSegment(Link parentLink, boolean directionAB) throws PlanItException {
    return new MacroscopicLinkSegmentImpl(getIdGroupingToken(), parentLink, directionAB);
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
  public Link createEdge(Vertex nodeA, Vertex nodeB, double length) throws PlanItException {
    return physicalNetworkBuilder.createEdge(nodeA, nodeB, length);
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
  public void recreateIds(EdgeSegments<? extends Link, ? extends MacroscopicLinkSegment> macroscopicinkSegments) {
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

}
