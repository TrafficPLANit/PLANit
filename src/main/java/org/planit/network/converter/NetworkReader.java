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
   * parse the network based on the configuration of the implementing class to yield a PLANit network
   * 
   * @return parsed network
   * @throws PlanItException thrown if error
   */
  public MacroscopicNetwork read() throws PlanItException;

}
