package org.goplanit.network.layer.macroscopic;

import java.util.function.BiConsumer;
import java.util.logging.Logger;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
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
   * @param mapper to apply in case of deep copy to each original to copy combination (when provided, may be null)
   */
  public MacroscopicLinkSegmentTypesImpl(
          final MacroscopicLinkSegmentTypesImpl other, boolean deepCopy, BiConsumer<MacroscopicLinkSegmentType,MacroscopicLinkSegmentType> mapper) {
    super(other, deepCopy, mapper);
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
    return firstMatch(type -> xmlId.equals(((MacroscopicLinkSegmentType) type).getXmlId()));
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
  public MacroscopicLinkSegmentTypesImpl shallowClone() {
    return new MacroscopicLinkSegmentTypesImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegmentTypesImpl deepClone() {
    return new MacroscopicLinkSegmentTypesImpl(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegmentTypesImpl deepCloneWithMapping(BiConsumer<MacroscopicLinkSegmentType,MacroscopicLinkSegmentType> mapper) {
    return new MacroscopicLinkSegmentTypesImpl(this, true, mapper);
  }

}
