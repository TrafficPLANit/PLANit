package org.goplanit.zoning.zonetozone;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.goplanit.output.enums.SkimSubOutputType;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.unit.Unit;
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
   * Constructor, the values in the default unit of the skim output type, km for a distance and hours for a time or a
   * cost
   *
   * @param zones            holding the zones in the network
   * @param skimType the skim output type for this skim matrix
   */
  public TransferZoneSkimMatrix(TransferZones zones, SkimSubOutputType skimType) {
    this(zones, skimType, skimType.getDefaultUnit());
  }

  /**
   * Constructor
   *
   * @param zones    holding the zones in the network
   * @param skimType the skim output type for this skim matrix
   * @param unit     of the values, one that fits the skim output type, such as metres for a distance
   */
  public TransferZoneSkimMatrix(TransferZones zones, SkimSubOutputType skimType, Unit unit) {
    super(TransferZoneSkimMatrix.class,
        IdGroupingToken.collectGlobalToken(),
        Double.class,
        zones,
        Array2D.PRIMITIVE32.makeZero(zones.size(), zones.size()),
        unit);
    skimType.verifyUnit(unit);
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
  public @NonNull TransferZoneSkimMatrixIterator iterator() {
    return new TransferZoneSkimMatrixIterator(this);
  }

}
