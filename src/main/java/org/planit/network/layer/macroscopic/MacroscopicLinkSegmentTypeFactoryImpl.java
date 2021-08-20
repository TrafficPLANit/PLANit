package org.planit.network.layer.macroscopic;

import java.util.Collection;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.ManagedId;
import org.planit.utils.id.ManagedIdEntityFactoryImpl;
import org.planit.utils.network.layer.macroscopic.AccessGroupProperties;
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
  public MacroscopicLinkSegmentType registerUniqueCopyOf(ManagedId entityToCopy) {
    MacroscopicLinkSegmentType newType = createUniqueCopyOf(entityToCopy);
    linkSegmentTypes.register(newType);
    return newType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegmentType registerNew(String name, double capacityPcuPerHour, double maximumDensityPcuPerKm,
      final Collection<AccessGroupProperties> accessGroupProperties) {

    MacroscopicLinkSegmentType linkSegmentType = new MacroscopicLinkSegmentTypeImpl(getIdGroupingToken(), name, capacityPcuPerHour, maximumDensityPcuPerKm, accessGroupProperties);
    linkSegmentTypes.register(linkSegmentType);
    return linkSegmentType;
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
  public MacroscopicLinkSegmentType registerNew(String name, double capacityPcuPerHour, double maximumDensityPcuPerKm, AccessGroupProperties groupAccessProperties) {
    MacroscopicLinkSegmentType linkSegmentType = new MacroscopicLinkSegmentTypeImpl(getIdGroupingToken(), name, capacityPcuPerHour, maximumDensityPcuPerKm, groupAccessProperties);
    linkSegmentTypes.register(linkSegmentType);
    return linkSegmentType;
  }

}
