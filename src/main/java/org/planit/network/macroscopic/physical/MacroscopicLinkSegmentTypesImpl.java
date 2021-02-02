package org.planit.network.macroscopic.physical;

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
   * Retrieve a MacroscopicLinkSegmentType by its xml Id
   * 
   * This method is not efficient, since it loops through all the registered types in order to find the required entry. Use get whenever possible instead
   * 
   * @param xmlId the XML Id of the specified MacroscopicLinkSegmentType instance
   * @return the retrieved type, or null if nothing was found
   */
  @Override
  public MacroscopicLinkSegmentType getByXmlId(String xmlId) {
    for (MacroscopicLinkSegmentType macroscopicLinkSegmentType : macroscopicLinkSegmentTypeByIdMap.values()) {
      if (xmlId.equals(macroscopicLinkSegmentType.getXmlId())) {
        return macroscopicLinkSegmentType;
      }
    }
    return null;
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

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegmentType getFirst() {
    return iterator().next();
  }

}
