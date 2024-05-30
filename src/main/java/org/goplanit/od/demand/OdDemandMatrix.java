package org.goplanit.od.demand;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.od.OdPrimitiveMatrix;
import org.goplanit.utils.od.OdPrimitiveMatrixIterator;
import org.goplanit.utils.zoning.OdZones;
import org.ojalgo.OjAlgoUtils;
import org.ojalgo.array.Array2D;
import org.ojalgo.function.UnaryFunction;
import org.ojalgo.function.aggregator.Aggregator;
import org.ojalgo.random.Random1D;

import java.util.Random;

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
      super(OdDemandMatrix.matrixContainer, OdDemandMatrix.zones);
    }
  }

  /**
   * Constructor
   * 
   * @param zones holds the zones defined in the network
   */
  public OdDemandMatrix(OdZones zones) {
    super(OdDemandMatrix.class, IdGroupingToken.collectGlobalToken(), Double.class, zones, Array2D.PRIMITIVE32.makeZero(zones.size(), zones.size()));
  }

  /**
   * Copy constructor
   * 
   * @param odDemandMatrix to copy
   */
  public OdDemandMatrix(final OdDemandMatrix odDemandMatrix) {
    super(odDemandMatrix, Array2D.PRIMITIVE32);
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

    matrixContainer.modifyAll(unary);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdDemandMatrix shallowClone() {
    return new OdDemandMatrix(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdDemandMatrix deepClone() {
    /* primitive wrapper so deep clone and clone are the same */
    return shallowClone();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void applyStochasticRounding(double upperBound, int seed) {
    final var rand = new Random(seed);

    var stochasticallyRoundUnary = new UnaryFunction<Double>() {

      private double stochasticallyRounded(double arg){
        if(arg <= 0.0){
          return arg;
        }else if(arg > upperBound){
          return arg;
        }

        double draw = rand.nextDouble() * upperBound;
        return draw < arg ? upperBound : 0.0;
      }

      @Override
      public double invoke(double arg) {
        return stochasticallyRounded(arg);
      }

      @Override
      public Double invoke(Double arg) {
        return stochasticallyRounded(arg);
      }
    };

    matrixContainer.modifyAll(stochasticallyRoundUnary);
  }

  @Override
  public double sum() {
    return matrixContainer.aggregateRange(0, matrixContainer.count(), Aggregator.SUM);
  }

}
