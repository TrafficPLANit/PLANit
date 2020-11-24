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
   * write a network to the writer's output format.
   * 
   * @param metwork memory model network to write
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
