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

import java.util.LongSummaryStatistics;
import java.util.Random;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Logger;

/**
 * This class handles the OD demand matrix.
 * 
 * @author gman6028, markr
 *
 */
public class OdDemandMatrix extends OdPrimitiveMatrix<Double> implements OdDemands {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(OdDemandMatrix.class.getCanonicalName());

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
  public void applyStochasticRounding(double upperBound, int seed, boolean logstats) {
    final var rand = new Random(seed);

    LongAdder roundedKeptCount = new LongAdder();
    LongAdder notRoundedKeptCount = new LongAdder();
    LongAdder nonZeroCount = new LongAdder();
    LongAdder totalCount = new LongAdder();
    var stochasticallyRoundUnary = new UnaryFunction<Double>() {

      private double stochasticallyRounded(double arg){
        totalCount.increment();
        if(arg <= 0.0){
          return arg;
        }else if(arg > upperBound){
          nonZeroCount.increment();
          notRoundedKeptCount.increment();
          return arg;
        }else{
          nonZeroCount.increment();
        }

        double draw = rand.nextDouble() * upperBound;
        if(draw < arg ){
          roundedKeptCount.increment();
          return upperBound;
        }else{
          return 0.0;
        }
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

    if(logstats) {
      LOGGER.info(String.format(
              "Stochastic rounding applied - total entries: %d, original non-zero: %d (%.2f%%), new-non-zero: %d (%.2f%%) [not-rounded-kept: %d, rounded-kept: %d,  rounded-zero: %d]",
              totalCount.intValue(),
              nonZeroCount.intValue(),
              nonZeroCount.intValue() / totalCount.doubleValue(),
              notRoundedKeptCount.intValue() + roundedKeptCount.intValue(),
              (notRoundedKeptCount.intValue() + roundedKeptCount.intValue()) / totalCount.doubleValue(),
              notRoundedKeptCount.intValue(),
              roundedKeptCount.intValue(),
              (nonZeroCount.intValue() - notRoundedKeptCount.intValue()) - roundedKeptCount.longValue()));
    }
  }

  @Override
  public double sum() {
    return matrixContainer.aggregateRange(0, matrixContainer.count(), Aggregator.SUM);
  }

}
