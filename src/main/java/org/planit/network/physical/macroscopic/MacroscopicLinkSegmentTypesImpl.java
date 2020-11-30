package org.planit.network.physical.macroscopic;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentType;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentTypes;
import org.planit.utils.network.physical.macroscopic.MacroscopicModeProperties;

/**
 * Implementation of the container interface
 * 
 * @author markr
 *
 */
public class MacroscopicLinkSegmentTypesImpl implements MacroscopicLinkSegmentTypes {

  /** the logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(MacroscopicLinkSegmentTypesImpl.class.getCanonicalName());

  /** the builder to use for creating instances of macroscopic link segment types */
  protected final MacroscopicPhysicalNetworkBuilder<?, ?, MacroscopicLinkSegment> networkBuilder;

  /**
   * Map which stores link segment types by generated Id
   */
  protected Map<Long, MacroscopicLinkSegmentType> macroscopicLinkSegmentTypeByIdMap = new TreeMap<Long, MacroscopicLinkSegmentType>();

  /**
   * Constructor
   * 
   * @param networkBuilder to use for delegating the creation of macroscopic link segment types to
   */
  public MacroscopicLinkSegmentTypesImpl(MacroscopicPhysicalNetworkBuilder<?, ?, MacroscopicLinkSegment> networkBuilder) {
    this.networkBuilder = networkBuilder;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegmentType createAndRegisterNew(String name, double capacityPcuPerHour, double maximumDensityPcuPerKm, Object linkSegmentExternalId,
      Map<Mode, MacroscopicModeProperties> modeProperties) throws PlanItException {

    MacroscopicLinkSegmentType linkSegmentType = networkBuilder.createLinkSegmentType(name, capacityPcuPerHour, maximumDensityPcuPerKm, linkSegmentExternalId, modeProperties);
    register(linkSegmentType);
    return linkSegmentType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegmentType createAndRegisterNew(String name, double capacityPcuPerHour, double maximumDensityPcuPerKm, Object linkSegmentExternalId)
      throws PlanItException {

    MacroscopicLinkSegmentType linkSegmentType = networkBuilder.createLinkSegmentType(name, capacityPcuPerHour, maximumDensityPcuPerKm, linkSegmentExternalId);
    register(linkSegmentType);
    return linkSegmentType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegmentType register(MacroscopicLinkSegmentType linkSegmentType) {
    return macroscopicLinkSegmentTypeByIdMap.put(linkSegmentType.getId(), linkSegmentType);
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
   * {@inheritDoc}
   */
  @Override
  public int size() {
    return macroscopicLinkSegmentTypeByIdMap.size();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegmentType get(long id) {
    return macroscopicLinkSegmentTypeByIdMap.get(id);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<MacroscopicLinkSegmentType> setOf() {
    return Set.copyOf(macroscopicLinkSegmentTypeByIdMap.values());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterator<MacroscopicLinkSegmentType> iterator() {
    return macroscopicLinkSegmentTypeByIdMap.values().iterator();
  }

}
