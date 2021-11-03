package org.goplanit.gap;

import java.util.logging.Logger;

import org.goplanit.utils.id.IdGroupingToken;

/**
 * Gap function based on the norm, e.g. ||x||_p where p indicates which norm (norm 1, norm 2 etc) and x represents a vector of differences between two values. When averaged
 * (default) we divide the result by the number of elements in the vector. e.g. for the average norm 1 we would compute: 1/|x| * ( (x_1-x_1_alt) + (x_2-x_2_alt) + .... +
 * (x_n-x_n_alt)), whereas for the averaged norm 2 we would do: 1/|x| * sqrt( (x_1-x_1_alt)^2 + (x_2-x_2_alt)^2 + .... + (x_n-x_n_alt)^2) etc.
 * 
 * @author markr
 *
 */
public class NormBasedGapFunction extends GapFunction {

  /** Generated UID */
  private static final long serialVersionUID = 6739949628577467878L;

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(NormBasedGapFunction.class.getCanonicalName());

  /** which norm we are taking, e.g. 1,2,p */
  protected int norm;

  /** indicate if the result is to be averaged */
  protected boolean averaged;

  /**
   * Constructor with defaults for norm and averaged
   * 
   * @param idToken       to use for the generation of its id
   * @param stopCriterion StopCriterion object being used
   */
  public NormBasedGapFunction(final IdGroupingToken idToken, final StopCriterion stopCriterion) {
    this(idToken, stopCriterion, DEFAULT_NORM, DEFAULT_AVERAGED);
  }

  /**
   * Constructor with default for averaged
   * 
   * @param idToken       to use for the generation of its id
   * @param stopCriterion StopCriterion object being used
   * @param norm          to use
   */
  public NormBasedGapFunction(final IdGroupingToken idToken, final StopCriterion stopCriterion, final int norm) {
    this(idToken, stopCriterion, norm, DEFAULT_AVERAGED);
  }

  /**
   * Constructor
   * 
   * @param idToken       to use for the generation of its id
   * @param stopCriterion StopCriterion object being used
   * @param norm          to use
   * @param averaged      to use
   */
  public NormBasedGapFunction(final IdGroupingToken idToken, final StopCriterion stopCriterion, final int norm, final boolean averaged) {
    super(idToken, stopCriterion);
    if (norm < 1) {
      LOGGER.warning(String.format("Invalid norm, reset to default %d", norm));
      this.norm = DEFAULT_NORM;
    } else {
      this.norm = norm;
    }
    this.averaged = averaged;
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   */
  public NormBasedGapFunction(NormBasedGapFunction other) {
    super(other);
    this.averaged = other.averaged;
    this.count = other.count;
    this.gap = other.gap;
    this.measuredValue = other.measuredValue;
    this.norm = other.norm;
  }

  /**
   * Current value as it stands
   */
  protected double measuredValue = 0;

  /**
   * Represents the total entries that were measured so far
   */
  protected double count = 0;

  /**
   * gap
   */
  protected double gap = MAX_GAP;

  /** maximum gap possible */
  public static final double MAX_GAP = Double.POSITIVE_INFINITY;

  /** default norm is set to 1 */
  public static final int DEFAULT_NORM = 1;

  /** default we average the result */
  public static final boolean DEFAULT_AVERAGED = true;

  /**
   * Return the actual system travel time
   * 
   * @return the actual system travel time
   */
  public double getMeasuredValue() {
    return measuredValue;
  }

  /**
   * Increase value by abs(value1-value2)^p, where p is the norm set. Note that every call to this method also increases the count
   * 
   * @param vector1 first value vector
   * @param vector2 second value vector
   */
  public void increaseMeasuredValue(final double[] vector1, final double[] vector2) {
    if (vector1.length != vector2.length) {
      LOGGER.warning("Cannot compute increasedMEaseredValue of NormBasedGapFunction for two vectors when they are of different size");
      return;
    }

    int length = vector1.length;
    for (int index = 0; index < length; ++index) {
      if (norm == 1) {
        measuredValue += Math.abs(vector1[index] - vector2[index]);
      } else {
        measuredValue += Math.pow(vector1[index] - vector2[index], norm);
      }
    }
    count += length;
  }

  /**
   * Increase value by abs(value1-value2)^p, where p is the norm set. Note that every call to this method also increases the count
   * 
   * @param value1 first value
   * @param value2 second value
   */
  public void increaseMeasuredValue(final double value1, final double value2) {
    if (norm == 1) {
      measuredValue += Math.abs(value1 - value2);
    } else {
      measuredValue += Math.pow(value1 - value2, norm);
    }
    ++count;
  }

  /**
   * Reset
   */
  public void reset() {
    this.measuredValue = 0;
    this.count = 0;
    this.gap = MAX_GAP;
  }

  /**
   * Compute the gap
   * 
   * @return the gap for the current iteration
   */
  @Override
  public double computeGap() {
    if (count <= 0) {
      gap = MAX_GAP;
    } else {
      double multiplicationFactor = isAveraged() ? (1.0 / count) : 1;
      gap = multiplicationFactor * Math.pow(measuredValue, 1.0 / norm);
    }
    return getGap();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getGap() {
    return gap;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NormBasedGapFunction clone() {
    return new NormBasedGapFunction(this);
  }

  // GETTERS - SETTERS

  public int getNorm() {
    return norm;
  }

  public void setNorm(int norm) {
    if (measuredValue > 0) {
      LOGGER.warning("IGNORED: Not allowed to change the norm while computing a gap, reset() first");
      return;
    }
    this.norm = norm;
  }

  public boolean isAveraged() {
    return averaged;
  }

  public void setAveraged(boolean averaged) {
    if (measuredValue > 0) {
      LOGGER.warning("IGNORED: Not allowed to change the averaging scheme while computing a gap, reset() first");
      return;
    }
    this.averaged = averaged;
  }

}
