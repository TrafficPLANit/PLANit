package org.planit.network.layer.macroscopic;

import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegmentTypes;
import org.planit.utils.network.layer.macroscopic.MacroscopicModeProperties;
import org.planit.utils.network.layer.macroscopic.MacroscopicPhysicalLayerBuilder;
import org.planit.utils.wrapper.LongMapWrapper;

/**
 * Implementation of the container interface
 * 
 * @author markr
 *
 */
public class MacroscopicLinkSegmentTypesImpl extends LongMapWrapper<MacroscopicLinkSegmentType> implements MacroscopicLinkSegmentTypes {

  /** the logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(MacroscopicLinkSegmentTypesImpl.class.getCanonicalName());

  /** the builder to use for creating instances of macroscopic link segment types */
  protected final MacroscopicPhysicalLayerBuilder<?, ?, MacroscopicLinkSegment> networkBuilder;

  /**
   * Constructor
   * 
   * @param networkBuilder to use for delegating the creation of macroscopic link segment types to
   */
  public MacroscopicLinkSegmentTypesImpl(MacroscopicPhysicalLayerBuilder<?, ?, MacroscopicLinkSegment> networkBuilder) {
    super(new TreeMap<Long, MacroscopicLinkSegmentType>(), MacroscopicLinkSegmentType::getId);
    this.networkBuilder = networkBuilder;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegmentType createAndRegisterNew(String name, double capacityPcuPerHour, double maximumDensityPcuPerKm, Map<Mode, MacroscopicModeProperties> modeProperties)
      throws PlanItException {

    MacroscopicLinkSegmentType linkSegmentType = networkBuilder.createLinkSegmentType(name, capacityPcuPerHour, maximumDensityPcuPerKm, modeProperties);
    register(linkSegmentType);
    return linkSegmentType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegmentType createAndRegisterNew(String name, double capacityPcuPerHour, double maximumDensityPcuPerKm) throws PlanItException {

    MacroscopicLinkSegmentType linkSegmentType = networkBuilder.createLinkSegmentType(name, capacityPcuPerHour, maximumDensityPcuPerKm);
    register(linkSegmentType);
    return linkSegmentType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegmentType registerUniqueCopyOf(MacroscopicLinkSegmentType linkSegmentTypeToCopy) {
    MacroscopicLinkSegmentType linkSegmentType = networkBuilder.createUniqueCopyOf(linkSegmentTypeToCopy);
    register(linkSegmentType);
    return linkSegmentType;
  }

  /**
   * Retrieve a MacroscopicLinkSegmentType by its xml Id
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
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegmentType getFirst() {
    return iterator().next();
  }

}
