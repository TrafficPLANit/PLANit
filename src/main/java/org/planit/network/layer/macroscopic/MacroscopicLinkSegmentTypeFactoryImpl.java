package org.planit.network.layer.macroscopic;

import java.util.Map;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.ManagedId;
import org.planit.utils.id.ManagedIdEntityFactoryImpl;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegmentTypeFactory;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegmentTypes;
import org.planit.utils.network.layer.macroscopic.MacroscopicModeProperties;

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
  public MacroscopicLinkSegmentType createAndRegisterNew(String name, double capacityPcuPerHour, double maximumDensityPcuPerKm, Map<Mode, MacroscopicModeProperties> modeProperties)
      throws PlanItException {

    MacroscopicLinkSegmentType linkSegmentType = new MacroscopicLinkSegmentTypeImpl(getIdGroupingToken(), name, capacityPcuPerHour, maximumDensityPcuPerKm, modeProperties);
    linkSegmentTypes.register(linkSegmentType);
    return linkSegmentType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegmentType createAndRegisterNew(String name, double capacityPcuPerHour, double maximumDensityPcuPerKm) throws PlanItException {

    MacroscopicLinkSegmentType linkSegmentType = new MacroscopicLinkSegmentTypeImpl(getIdGroupingToken(), name, capacityPcuPerHour, maximumDensityPcuPerKm);
    linkSegmentTypes.register(linkSegmentType);
    return linkSegmentType;
  }

}
