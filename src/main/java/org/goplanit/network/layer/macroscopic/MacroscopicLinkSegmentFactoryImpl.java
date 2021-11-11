package org.goplanit.network.layer.macroscopic;

import org.goplanit.graph.GraphEntityFactoryImpl;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentFactory;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegments;
import org.goplanit.utils.network.layer.physical.Link;

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
  public MacroscopicLinkSegment create(final Link parentLink, final boolean directionAb) throws PlanItException {
    final MacroscopicLinkSegment macroscopicLinkSegment = new MacroscopicLinkSegmentImpl(getIdGroupingToken(), parentLink, directionAb);
    return macroscopicLinkSegment;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegment registerNew(final Link parentLink, final boolean directionAb, boolean registerOnNodeAndLink) throws PlanItException {
    final MacroscopicLinkSegment macroscopicLinkSegment = create(parentLink, directionAb);
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