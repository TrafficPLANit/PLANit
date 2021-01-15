package org.planit.network.macroscopic;

import org.planit.utils.mode.Modes;

/**
 * An infrastructure layer represents the infrastructure suited for a number of modes. This can be in the form of a physical network
 * or by some other (more aggregate) representation. The combination of infrastructure layers can be used to construct an intermodal network.
 * Each layer supports one or more modes
 * 
 * @author markr
 *
 */
public interface InfrastructureLayer {

  /**
   * collect the modes supported by this infrastructure layer
   * 
   * @return the supported modes for at least some part of the available infrastructure
   */
  public Modes getSupportedModes();
  
}
