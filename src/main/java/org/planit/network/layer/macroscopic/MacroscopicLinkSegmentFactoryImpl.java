package org.planit.network.layer.macroscopic;

import org.planit.graph.GraphEntityFactoryImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegmentFactory;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegments;
import org.planit.utils.network.layer.physical.Link;

/**
 * Factory for creating link segments on link segments container
 * 
 * @author markr
 */
public class MacroscopicLinkSegmentFactoryImpl extends GraphEntityFactoryImpl<MacroscopicLinkSegment> implements MacroscopicLinkSegmentFactory {

  /**
   * Constructor
   * 
   * @param groupId                 to use
   * @param macroscopicLinkSegments to use
   */
  protected MacroscopicLinkSegmentFactoryImpl(final IdGroupingToken groupId, MacroscopicLinkSegments macroscopicLinkSegments) {
    super(groupId, macroscopicLinkSegments);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegment create(final Link parentLink, final boolean directionAB) throws PlanItException {
    final MacroscopicLinkSegment macroscopicLinkSegment = new MacroscopicLinkSegmentImpl(getIdGroupingToken(), parentLink, directionAB);
    return macroscopicLinkSegment;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegment registerNew(final Link parentLink, final boolean directionAb, boolean registerOnNodeAndLink) throws PlanItException {
    final MacroscopicLinkSegment macroscopicLinkSegment = new MacroscopicLinkSegmentImpl(getIdGroupingToken(), parentLink, directionAb);
    getGraphEntities().register(macroscopicLinkSegment);

    if (registerOnNodeAndLink) {
      parentLink.registerEdgeSegment(macroscopicLinkSegment, directionAb);
      parentLink.getVertexA().addEdgeSegment(macroscopicLinkSegment);
      parentLink.getVertexB().addEdgeSegment(macroscopicLinkSegment);
    }
    return macroscopicLinkSegment;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegment registerNew(Link parentLink, MacroscopicLinkSegmentType type, boolean directionAb, boolean registerOnNodeAndLink) throws PlanItException {
    MacroscopicLinkSegment linkSegment = registerNew(parentLink, directionAb, registerOnNodeAndLink);
    linkSegment.setLinkSegmentType(type);
    return linkSegment;
  }

}
