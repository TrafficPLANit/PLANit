package org.goplanit.network.layer.macroscopic;

import java.util.logging.Logger;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentTypeFactory;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentTypes;

/**
 * Implementation of the container interface
 * 
 * @author markr
 *
 */
public class MacroscopicLinkSegmentTypesImpl extends ManagedIdEntitiesImpl<MacroscopicLinkSegmentType> implements MacroscopicLinkSegmentTypes {

  /** the logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(MacroscopicLinkSegmentTypesImpl.class.getCanonicalName());

  /** factory to use */
  private final MacroscopicLinkSegmentTypeFactory linkSegmentTypeFactory;

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public MacroscopicLinkSegmentTypesImpl(final IdGroupingToken groupId) {
    super(MacroscopicLinkSegmentType::getId, MacroscopicLinkSegmentType.MACROSCOPIC_LINK_SEGMENT_TYPE_ID_CLASS);
    this.linkSegmentTypeFactory = new MacroscopicLinkSegmentTypeFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param groupId                to use for creating ids for instances
   * @param linkSegmentTypeFactory the factory to use
   */
  public MacroscopicLinkSegmentTypesImpl(final IdGroupingToken groupId, MacroscopicLinkSegmentTypeFactory linkSegmentTypeFactory) {
    super(MacroscopicLinkSegmentType::getId, MacroscopicLinkSegmentType.MACROSCOPIC_LINK_SEGMENT_TYPE_ID_CLASS);
    this.linkSegmentTypeFactory = linkSegmentTypeFactory;
  }

  /**
   * Copy constructor, also creates new factory with this as its underlying container
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep cpy, shallow copy otherwise
   */
  public MacroscopicLinkSegmentTypesImpl(final MacroscopicLinkSegmentTypesImpl other, boolean deepCopy) {
    super(other, deepCopy);
    this.linkSegmentTypeFactory =
            new MacroscopicLinkSegmentTypeFactoryImpl(other.linkSegmentTypeFactory.getIdGroupingToken(),this);
  }

  /**
   * Retrieve a MacroscopicLinkSegmentType by its XML Id
   * 
   * This method is not efficient, since it loops through all the registered types in order to find the required entry. Use get whenever possible instead
   * 
   * @param xmlId the XML Id of the specified MacroscopicLinkSegmentType instance
   * @return the retrieved type, or null if nothing was found
   */
  @Override
  public MacroscopicLinkSegmentType getByXmlId(String xmlId) {
    return findFirst(type -> xmlId.equals(((MacroscopicLinkSegmentType) type).getXmlId()));
  }

  /**
   * Collect factory for creating (and registering on this container) of macroscopic link segment types
   * 
   * @return factory to use
   */
  @Override
  public MacroscopicLinkSegmentTypeFactory getFactory() {
    return linkSegmentTypeFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegmentTypesImpl clone() {
    return new MacroscopicLinkSegmentTypesImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegmentTypesImpl deepClone() {
    return new MacroscopicLinkSegmentTypesImpl(this, true);
  }

}
