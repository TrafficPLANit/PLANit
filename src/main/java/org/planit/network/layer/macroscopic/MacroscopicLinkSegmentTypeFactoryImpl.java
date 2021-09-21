package org.planit.network.layer.macroscopic;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.ManagedIdEntityFactoryImpl;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegmentTypeFactory;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegmentTypes;

/**
 * Factory for creating macroscopic link segment types on link segment types container
 * 
 * @author markr
 */
public class MacroscopicLinkSegmentTypeFactoryImpl extends ManagedIdEntityFactoryImpl<MacroscopicLinkSegmentType> implements MacroscopicLinkSegmentTypeFactory {

  /** container to register new entities on */
  MacroscopicLinkSegmentTypes linkSegmentTypes;

  /**
   * Constructor
   * 
   * @param groupId          to use
   * @param linkSegmentTypes container to use for registering new entities
   */
  protected MacroscopicLinkSegmentTypeFactoryImpl(final IdGroupingToken groupId, MacroscopicLinkSegmentTypes linkSegmentTypes) {
    super(groupId);
    this.linkSegmentTypes = linkSegmentTypes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegmentType registerNew(String name, double capacityPcuPerHour, double maximumDensityPcuPerKm) {
    MacroscopicLinkSegmentType linkSegmentType = new MacroscopicLinkSegmentTypeImpl(getIdGroupingToken(), name, capacityPcuPerHour, maximumDensityPcuPerKm);
    linkSegmentTypes.register(linkSegmentType);
    return linkSegmentType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegmentType registerNew(String name, double capacityPcuPerHour, double maximumDensityPcuPerKm, Mode allowedMode) {
    MacroscopicLinkSegmentType linkSegmentType = registerNew(name, capacityPcuPerHour, maximumDensityPcuPerKm);
    linkSegmentType.setAccessGroupProperties(AccessGroupPropertiesFactory.create(allowedMode));
    return linkSegmentType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegmentType registerNew(String name) {
    MacroscopicLinkSegmentType linkSegmentType = new MacroscopicLinkSegmentTypeImpl(getIdGroupingToken(), name);
    linkSegmentTypes.register(linkSegmentType);
    return linkSegmentType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegmentType registerNewWithCapacity(String name, double capacityPcuPerHour) {
    MacroscopicLinkSegmentType linkSegmentType = new MacroscopicLinkSegmentTypeImpl(getIdGroupingToken(), name, capacityPcuPerHour, null);
    linkSegmentTypes.register(linkSegmentType);
    return linkSegmentType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegmentType registerNewWithMaxDensity(String name, double maximumDensityPcuPerKm) {
    MacroscopicLinkSegmentType linkSegmentType = new MacroscopicLinkSegmentTypeImpl(getIdGroupingToken(), name, null, maximumDensityPcuPerKm);
    linkSegmentTypes.register(linkSegmentType);
    return linkSegmentType;
  }

}
