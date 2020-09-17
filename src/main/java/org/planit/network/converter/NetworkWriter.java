package org.planit.network.converter;

import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.utils.exceptions.PlanItException;

/**
 * Interface for classes able to write a PLANit network to disk
 * 
 * @author markr
 *
 */
public interface NetworkWriter {
  
  /**
   * Each writer maps the modes of the PLANit network to the writer's output format. This mapping should be made available via this method
   * It is required for any network converter to verify if the planit modes that are expected are compatible/available in the network that is being written
   *   
   * @return the mode mapper
   */
  PlanitMode2WriterModeMapper getModeMapper();  

  /**
   * write a planit network to disk based on the configuration of the implementing class
   * 
   * @return parsed network
   * @throws PlanItException thrown if error
   */
  public void write(MacroscopicNetwork network) throws PlanItException;

  /**
   * collect the way the ids should be mapped
   * 
   * @return the idmapping choice
   */
  public IdMapper getIdMapper();

  /**
   * set the way ids should be mapped
   * 
   * @param idMapper to use
   */
  public void setIdMapper(IdMapper idMapper);
}
