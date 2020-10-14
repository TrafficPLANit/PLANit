package org.planit.network.physical.macroscopic;

import java.util.Map;

import org.planit.network.physical.PhysicalNetworkBuilder;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentType;
import org.planit.utils.network.physical.macroscopic.MacroscopicModeProperties;

/**
 * Create network entities for a macroscopic simulation model using Nodes, Links, and MacroscopicLinkSegments
 * 
 * @author markr
 *
 */
public interface MacroscopicPhysicalNetworkBuilder<N extends Node, L extends Link, MLS extends MacroscopicLinkSegment> extends PhysicalNetworkBuilder<N, L, MLS> {

  /**
   * Create a fully functional macroscopic link segment type instance
   * 
   * @param name           the name of this link type
   * @param capacity       the capacity of this link type
   * @param maximumDensity the maximum density of this link type
   * @param externalId     the external reference number of this link type
   * @param modeProperties the mode properties for each mode along this link
   * @return macroscopicLinkSegmentType the created link segment type
   */
  public MacroscopicLinkSegmentType createLinkSegmentType(String name, double capacity, double maximumDensity, Object externalId,
      Map<Mode, MacroscopicModeProperties> modeProperties);

  /**
   * Create a macroscopic link segment type instance without mode properties
   * 
   * @param name           the name of this link type
   * @param capacity       the capacity of this link type
   * @param maximumDensity the maximum density of this link type
   * @param externalId     the external reference number of this link type
   * @return macroscopicLinkSegmentType the created link segment type
   */
  public MacroscopicLinkSegmentType createLinkSegmentType(String name, double capacity, double maximumDensity, Object externalId);

}
