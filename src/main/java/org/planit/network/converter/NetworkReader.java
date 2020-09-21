package org.planit.network.converter;

import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.utils.exceptions.PlanItException;

/**
 * Interface for classes able to parse networks of some type which are then returned
 * 
 * @author markr
 *
 */
public interface NetworkReader {

  /**
   * Each reader maps the modes of the original reader format to PLANit modes. This mapping should be made available via this method It is required for any network converter to
   * verify if the planit modes that are used are compatible with the planit modes that the writer expects and in turn maps onto the writer's output format modes
   * 
   * @return hte mode mapper
   */
  // ReaderMode2PlanitModeMapper getModeMapper();

  /**
   * parse the network based on the configuration of the implementing class to yield a PLANit network
   * 
   * @return parsed network
   * @throws PlanItException thrown if error
   */
  public MacroscopicNetwork read() throws PlanItException;
}
