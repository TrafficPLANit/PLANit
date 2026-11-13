package org.goplanit.network.layer.modifier.event;

import org.goplanit.utils.event.EventImpl;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.modifier.event.LinkSegmentTypeModificationEvent;
import org.goplanit.utils.network.layer.modifier.event.LinkSegmentTypeModifierEventType;
import org.goplanit.utils.network.layer.modifier.MacroscopicNetworkLayerModifier;

/**
 * Event for when a link segment is given another type, which may change the modes it grants access, for example when a
 * mode's access is withdrawn from it
 *
 * @author markr
 */
public class ChangeLinkSegmentTypeEvent extends EventImpl implements LinkSegmentTypeModificationEvent {

  /** event type fired off when the type of a link segment has changed */
  public static final LinkSegmentTypeModifierEventType EVENT_TYPE =
      new LinkSegmentTypeModifierEventType("MACROSCOPICNETWORKLAYERMODIFIER.LINKSEGMENT.CHANGETYPE");

  /**
   * Constructor
   *
   * @param source modifier of the layer the link segment is part of
   * @param linkSegment whose type has changed
   * @param previousType of the link segment, may be null when it had none
   * @param newType of the link segment
   */
  public ChangeLinkSegmentTypeEvent(MacroscopicNetworkLayerModifier source, MacroscopicLinkSegment linkSegment,
      MacroscopicLinkSegmentType previousType, MacroscopicLinkSegmentType newType) {
    super(EVENT_TYPE, source, new Object[] { linkSegment, previousType, newType });
  }

  /**
   * The link segment whose type has changed
   *
   * @return link segment
   */
  public MacroscopicLinkSegment getLinkSegment() {
    return (MacroscopicLinkSegment) getContent()[0];
  }

  /**
   * The type the link segment had before
   *
   * @return previous type, null when it had none
   */
  public MacroscopicLinkSegmentType getPreviousType() {
    return (MacroscopicLinkSegmentType) getContent()[1];
  }

  /**
   * The type the link segment has now
   *
   * @return new type
   */
  public MacroscopicLinkSegmentType getNewType() {
    return (MacroscopicLinkSegmentType) getContent()[2];
  }
}
