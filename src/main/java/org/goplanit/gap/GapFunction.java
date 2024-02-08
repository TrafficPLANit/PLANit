package org.goplanit.gap;

import java.io.Serializable;
import java.util.Map;

import org.goplanit.component.PlanitComponent;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * Abstract base class for gap functions
 * 
 * @author markr
 *
 */
public abstract class GapFunction extends PlanitComponent<GapFunction> implements Serializable {

  /** Generated UID */
  private static final long serialVersionUID = -4378123200826871747L;

  /**
   * The stopCriterion to use
   */
  protected final StopCriterion stopCriterion;

  /**
   * short code for link based relative duality gap function type
   */
  public static final String LINK_BASED_RELATIVE_GAP = LinkBasedRelativeDualityGapFunction.class.getCanonicalName();

  /**
   * short code for a norm based gap function type
   */
  public static final String NORM_BASED_GAP = NormBasedGapFunction.class.getCanonicalName();

  /**
   * short code for a path based gap function type
   */
  public static final String PATH_BASED_GAP = PathBasedGapFunction.class.getCanonicalName();

  /**
   * Constructor
   * 
   * @param idToken       to use for the generation of its id
   * @param stopCriterion the StopCriterion object to be used
   */
  public GapFunction(IdGroupingToken idToken, StopCriterion stopCriterion) {
    super(idToken, GapFunction.class);
    this.stopCriterion = stopCriterion;
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public GapFunction(final GapFunction other, boolean deepCopy) {
    super(other, deepCopy);
    this.stopCriterion = deepCopy ? other.stopCriterion.deepClone() : other.stopCriterion;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> collectSettingsAsKeyValueMap() {
    Map<String,String> stopCriterionSettings = getStopCriterion().collectSettingsAsKeyValueMap();    
    return stopCriterionSettings;
  }

  /**
   * Verify if algorithm has converged
   * 
   * @param iterationIndex the index of the current iteration
   * @return true if stopping criterion has been met, false otherwise
   */
  public boolean hasConverged(int iterationIndex) {
    return stopCriterion.hasConverged(getGap(), iterationIndex);
  }

  /**
   * Return the StopCriterion object
   * 
   * @return StopCriterion object being used
   */
  public StopCriterion getStopCriterion() {
    return stopCriterion;
  }

  /**
   * Compute the gap and return it
   * 
   * @return the gap for the current iteration
   */
  public abstract double computeGap();

  /**
   * Returns the last computed gap
   * 
   * @return latest gap
   */
  public abstract double getGap();

  /**
   * Returns the last computed gap capped to given value
   *
   * @param truncateTo value to truncate to if gap exceeds this value
   * @return latest gap truncated
   */
  public double getGap(double truncateTo){
    return Math.min(truncateTo, getGap());
  }

  /**
   * Reset the gap function
   */
  public abstract void reset();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract GapFunction shallowClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract GapFunction deepClone();

}
