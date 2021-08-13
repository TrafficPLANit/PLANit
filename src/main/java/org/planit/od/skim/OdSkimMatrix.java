package org.planit.od.skim;

import org.ojalgo.array.Array2D;
import org.planit.od.odmatrix.OdPrimitiveMatrix;
import org.planit.od.odmatrix.OdPrimitiveMatrixIterator;
import org.planit.output.enums.ODSkimSubOutputType;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.zoning.OdZones;

/**
 * This class stores an OD Skim matrix.
 * 
 * @author gman6028, markr
 *
 */
public class OdSkimMatrix extends OdPrimitiveMatrix<Double> {

  /**
   * Wrapper around primitive matrix iterator
   * 
   * @author markr
   */
  public class OdSkimMatrixIterator extends OdPrimitiveMatrixIterator<Double> {

    public OdSkimMatrixIterator(final OdSkimMatrix matrix) {
      super(matrix.matrixContents, matrix.zones);
    }
  }

  // TODO - We may need to add more overloads of the setValue() method below, if different OD skim
  // types need other
  // arguments to determine their cell value e.g. mode, path length, toll etc

  /**
   * The ODSkimOutputType for this ODSkimMatrix
   */
  private final ODSkimSubOutputType odSkimOutputType;

  /**
   * Constructor
   * 
   * @param zones            holding the zones in the network
   * @param odSkimOutputType the skim output type for this OD skim matrix
   */
  public OdSkimMatrix(OdZones zones, ODSkimSubOutputType odSkimOutputType) {
    super(OdSkimMatrix.class, IdGroupingToken.collectGlobalToken(), zones, Array2D.PRIMITIVE32.makeZero(zones.size(), zones.size()));
    this.odSkimOutputType = odSkimOutputType;
  }

  /**
   * copy constructor
   * 
   * @param other to copy
   */
  public OdSkimMatrix(final OdSkimMatrix other) {
    super(other);
    this.odSkimOutputType = other.odSkimOutputType;
  }

  /**
   * Returns the type of the current OD skim matrix
   * 
   * @return the OD skim matrix type for the current OD skim matrix
   */
  public ODSkimSubOutputType getOdSkimOutputType() {
    return odSkimOutputType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdSkimMatrix clone() {
    return new OdSkimMatrix(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdSkimMatrixIterator iterator() {
    return new OdSkimMatrixIterator(this);
  }

}
