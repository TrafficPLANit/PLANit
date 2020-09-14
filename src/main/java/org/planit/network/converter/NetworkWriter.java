package org.planit.network.converter;

import org.planit.network.physical.macroscopic.MacroscopicNetwork;

/**
 * Interface for classes able to write a PLANit network to disk
 * 
 * @author markr
 *
 */
public interface NetworkWriter {

  /**
   * write a planit network to disk based on the configuration of the implementing class
   * 
   * @return parsed network
   */
  public void write(MacroscopicNetwork network);
}
