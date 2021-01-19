package org.planit.network;

import java.util.Collection;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.ExternalIdable;
import org.planit.utils.mode.Mode;

/**
 * An infrastructure layer represents the infrastructure suited for a number of modes. This can be in the form of a physical network or by some other (more aggregate)
 * representation. The combination of infrastructure layers can be used to construct an intermodal network. Each layer supports one or more modes
 * 
 * @author markr
 *
 */
public interface InfrastructureLayer extends ExternalIdable {

  /**
   * register a mode as supported by this layer
   * 
   * @param supportedMode to support
   * @return true when successful false otherwise
   */
  public boolean registerSupportedMode(Mode supportedMode);

  /**
   * register modes as supported by this layer
   * 
   * @param supportedModes to support
   * @return true when successful false otherwise
   */
  public boolean registerSupportedModes(Collection<Mode> supportedModes);

  /**
   * collect the modes supported by this infrastructure layer
   * 
   * @return the supported modes for at least some part of the available infrastructure
   */
  public Collection<Mode> getSupportedModes();

  /**
   * Determine if mode is supported by this layer
   * 
   * @param mode to verify
   * @return true when supporting, false otherwise
   */
  default public boolean supports(Mode mode) {
    return getSupportedModes().contains(mode);
  }

  /**
   * check if the layer is empty of any infrastructure
   * 
   * @return true when empty, false otherwise
   */
  public boolean isEmpty();

  /**
   * invoked by entities inquiring about general information about the layer to display to users
   * 
   * @param prefix optional prefix to include in each line of logging
   */
  public void logInfo(String prefix);

  /**
   * transform all underlying geometries in the layer from the given crs to the new crs
   * 
   * @param fromCoordinateReferenceSystem presumed current crs
   * @param toCoordinateReferenceSystem   to tranform to crs
   * @thrown PlanItException thrown if error
   */
  public void transform(CoordinateReferenceSystem fromcoordinateReferenceSystem, CoordinateReferenceSystem toCoordinateReferenceSystem) throws PlanItException;

}
