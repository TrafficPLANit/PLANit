package org.goplanit.od.demand;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.od.OdPrimitiveMatrix;
import org.goplanit.utils.od.OdPrimitiveMatrixIterator;
import org.goplanit.utils.zoning.OdZones;
import org.ojalgo.array.Array2D;
import org.ojalgo.function.UnaryFunction;

/**
 * This class handles the OD demand matrix.
 * 
 * @author gman6028, markr
 *
 */
public class OdDemandMatrix extends OdPrimitiveMatrix<Double> implements OdDemands {

  /**
   * Wrapper around primitive matrix iterator
   * 
   * @author markr
   */
  public class OdDemandMatrixIterator extends OdPrimitiveMatrixIterator<Double> {

    public OdDemandMatrixIterator(final OdDemandMatrix OdDemandMatrix) {
      super(OdDemandMatrix.matrixContents, OdDemandMatrix.zones);
    }
  }

  /**
   * Constructor
   * 
   * @param zones holds the zones defined in the network
   */
  public OdDemandMatrix(OdZones zones) {
    super(OdDemandMatrix.class, IdGroupingToken.collectGlobalToken(), zones, Array2D.PRIMITIVE32.makeZero(zones.size(), zones.size()));
  }

  /**
   * Copy constructor
   * 
   * @param odDemandMatrix to copy
   */
  public OdDemandMatrix(final OdDemandMatrix odDemandMatrix) {
    super(odDemandMatrix);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdDemandMatrixIterator iterator() {
    return new OdDemandMatrixIterator(this);
  }

  /**
   * Multiply all entries with given factor
   * 
   * @param factor to multiply with
   */
  public void multiply(final double factor) {
    var unary = new UnaryFunction<Double>() {
      @Override
      public double invoke(double arg) {
        return arg * factor;
      }

      @Override
      public Double invoke(Double arg) {
        return arg * factor;
      }
    };

    matrixContents.modifyAll(unary);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdDemandMatrix clone() {
    return new OdDemandMatrix(this);
  }

}
