package org.planit.od.odmatrix.skim;

import org.planit.network.virtual.Zoning;
import org.planit.od.odmatrix.ODMatrix;
import org.planit.output.enums.ODSkimSubOutputType;

/**
 * This class stores an OD Skim matrix.
 * 
 * @author gman6028
 *
 */
public class ODSkimMatrix extends ODMatrix {

  // TODO - We may need to add more overloads of the setValue() method below, if different OD skim
  // types need other
  // arguments to determine their cell value e.g. mode, route length, toll etc

  /**
   * The ODSkimOutputType for this ODSkimMatrix
   */
  private final ODSkimSubOutputType odSkimOutputType;

  /**
   * Constructor
   * 
   * @param zones holding the zones in the network
   * @param odSkimOutputType the skim output type for this OD skim matrix
   */
  public ODSkimMatrix(Zoning.Zones zones, ODSkimSubOutputType odSkimOutputType) {
    super(zones);
    this.odSkimOutputType = odSkimOutputType;
  }

  /**
   * Returns the type of the current OD skim matrix
   * 
   * @return the OD skim matrix type for the current OD skim matrix
   */
  public ODSkimSubOutputType getOdSkimOutputType() {
    return odSkimOutputType;
  }

}
