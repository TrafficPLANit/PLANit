package org.goplanit.zoning.zonetozone;

import org.goplanit.output.enums.SkimSubOutputType;
import org.ojalgo.array.Array2D;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.zoning.zonetozone.ZoneToZonePrimitiveMatrix;
import org.goplanit.utils.zoning.zonetozone.ZoneToZonePrimitiveMatrixIterator;
import org.goplanit.utils.zoning.OdZones;

/**
 * Thin wrapper storing a skim matrix specifically for od zones.
 * 
 * @author gman6028, markr
 *
 */
public class OdSkimMatrix extends ZoneToZonePrimitiveMatrix<Double> {

  /**
   * Wrapper around primitive matrix iterator
   * 
   * @author markr
   */
  public static class OdSkimMatrixIterator extends ZoneToZonePrimitiveMatrixIterator<Double> {

    public OdSkimMatrixIterator(final OdSkimMatrix matrix) {
      super(matrix.matrixContainer, matrix.zones);
    }
  }

  // TODO - We may need to add more overloads of the setValue() method below, if different OD skim
  // types need other
  // arguments to determine their cell value e.g. mode, path length, toll etc

  /**
   * The ODSkimOutputType for this ODSkimMatrix
   */
  private final SkimSubOutputType odSkimOutputType;

  /**
   * Constructor
   * 
   * @param zones            holding the zones in the network
   * @param odSkimOutputType the skim output type for this OD skim matrix
   */
  public OdSkimMatrix(OdZones zones, SkimSubOutputType odSkimOutputType) {
    super(OdSkimMatrix.class,
        IdGroupingToken.collectGlobalToken(),
        Double.class,
        zones,
        Array2D.PRIMITIVE32.makeZero(zones.size(), zones.size()));
    this.odSkimOutputType = odSkimOutputType;
  }

  /**
   * copy constructor
   * 
   * @param other to copy
   */
  public OdSkimMatrix(final OdSkimMatrix other) {
    super(other, Array2D.PRIMITIVE32);
    this.odSkimOutputType = other.odSkimOutputType;
  }

  /**
   * Returns the type of the current OD skim matrix
   * 
   * @return the OD skim matrix type for the current OD skim matrix
   */
  public SkimSubOutputType getOdSkimOutputType() {
    return odSkimOutputType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdSkimMatrix shallowClone() {
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
