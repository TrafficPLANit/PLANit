package org.goplanit.assignment;

import org.goplanit.od.skim.OdSkimMatrix;
import org.goplanit.output.configuration.OdOutputTypeConfiguration;
import org.goplanit.output.enums.OdSkimSubOutputType;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.zoning.OdZones;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class to track skim matrices for persistence during/after simulation
 *
 * author gmann, markr
 */
public final class ModalSkimMatrixData {

  /**
   * Stores a skim matrix for each mode and skim output type
   */
  private Map<Mode, Map<OdSkimSubOutputType, OdSkimMatrix>> modalSkimMatrixMap;

  /**
   * Constructor. Requires a call to resetAndCreateEmptySkimMatrices before the class can be used
   *
   */
  public ModalSkimMatrixData() {
    this.modalSkimMatrixMap = new HashMap<>();
  }

  /**
   * Copy constructor (shallow copy only)
   *
   * @param simulationData to copy
   */
  protected ModalSkimMatrixData(final ModalSkimMatrixData simulationData) {
    super();
    this.modalSkimMatrixMap = new HashMap<>(simulationData.modalSkimMatrixMap);
  }

  /**
   * Retrieve the skim matrix for a specified mode and skim output type
   *
   * @param odSkimOutputType the specified Skim Output type
   * @param mode             the specified mode
   * @return the skim matrix for the specified mode
   */
  public OdSkimMatrix getOdSkimMatrix(OdSkimSubOutputType odSkimOutputType, Mode mode) {
    if (modalSkimMatrixMap.containsKey(mode)) {
      var skimMatrixMap = modalSkimMatrixMap.get(mode);
      if (skimMatrixMap.containsKey(odSkimOutputType)) {
        return skimMatrixMap.get(odSkimOutputType);
      }
    }
    return null;
  }

  /**
   * Retrieve the Map of OD Skim matrices for all active OD Skim Output Types for a specified mode
   *
   * @param mode the specified mode
   * @return Map of OD Skim matrices for all active OD Skim Output Types, null if none are present for mode
   */
  public Map<OdSkimSubOutputType, OdSkimMatrix> getSkimMatricesByMode(Mode mode) {
    return modalSkimMatrixMap.get(mode);
  }

  /**
   * reset to initial state
   */
  public void reset() {
    modalSkimMatrixMap.entrySet().stream().forEach( e -> reset(e.getKey()));
  }

  /**
   * reset to initial empty state for given mode
   *
   * @param mode to reset for
   */
  public void reset(Mode mode) {
    if(!modalSkimMatrixMap.containsKey(mode)) {
      return;
    }

    modalSkimMatrixMap.get(mode).clear();
  }

  /**
   * Reset the skim matrix to all zeroes for a specified mode for all activated skim output types
   *
   * @param mode                                     the specified mode
   * @param zones                                    Zones object containing all the origin and destination zones
   * @param originDestinationOutputTypeConfiguration configuration to use to create skim matrices for activated types
   */
  public void resetAndCreateEmptySkimMatrices(Mode mode, OdZones zones, OdOutputTypeConfiguration originDestinationOutputTypeConfiguration) {
    reset(mode);
    modalSkimMatrixMap.putIfAbsent(mode, new HashMap<>());

    for (var odSkimOutputType : originDestinationOutputTypeConfiguration.getActiveSubOutputTypes()) {
      OdSkimMatrix odSkimMatrix = new OdSkimMatrix(zones, (OdSkimSubOutputType) odSkimOutputType);
      modalSkimMatrixMap.get(mode).put((OdSkimSubOutputType) odSkimOutputType, odSkimMatrix);
    }
  }

  /**
   * Shallow clone of this instance
   *
   * @return cloned instance
   */
  public ModalSkimMatrixData shallowClone(){
    return new ModalSkimMatrixData(this);
  }

}
