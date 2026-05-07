package org.goplanit.zoning.zonetozone;

import org.goplanit.output.enums.SkimSubOutputType;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.zoning.TransferZones;
import org.goplanit.utils.zoning.zonetozone.ZoneToZonePrimitiveMatrix;
import org.goplanit.utils.zoning.zonetozone.ZoneToZonePrimitiveMatrixIterator;
import org.ojalgo.array.Array2D;

/**
 * Thin wrapper storing a skim matrix specifically for transfer zones.
 * 
 * @author gman6028, markr
 *
 */
public class TransferZoneSkimMatrix extends ZoneToZonePrimitiveMatrix<Double> {

  /**
   * Wrapper around primitive matrix iterator
   *
   * @author markr
   */
  public static class TransferZoneSkimMatrixIterator extends ZoneToZonePrimitiveMatrixIterator<Double> {

    public TransferZoneSkimMatrixIterator(final TransferZoneSkimMatrix matrix) {
      super(matrix.matrixContainer, matrix.zones);
    }
  }

  /**
   * The ODSkimOutputType for this ODSkimMatrix
   */
  private final SkimSubOutputType skimType;

  /**
   * Constructor
   *
   * @param zones            holding the zones in the network
   * @param skimType the skim output type for this skim matrix
   */
  public TransferZoneSkimMatrix(TransferZones zones, SkimSubOutputType skimType) {
    super(TransferZoneSkimMatrix.class,
        IdGroupingToken.collectGlobalToken(),
        Double.class,
        zones,
        Array2D.PRIMITIVE32.makeZero(zones.size(), zones.size()));
    this.skimType = skimType;
  }

  /**
   * copy constructor
   *
   * @param other to copy
   */
  public TransferZoneSkimMatrix(final TransferZoneSkimMatrix other) {
    super(other, Array2D.PRIMITIVE32);
    this.skimType = other.skimType;
  }

  /**
   * Returns the type of the current OD skim matrix
   * 
   * @return the OD skim matrix type for the current OD skim matrix
   */
  public SkimSubOutputType getSkimType() {
    return skimType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransferZoneSkimMatrix shallowClone() {
    return new TransferZoneSkimMatrix(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransferZoneSkimMatrixIterator iterator() {
    return new TransferZoneSkimMatrixIterator(this);
  }

}
