package org.planit.gap;

import java.io.Serializable;

import org.planit.component.PlanitComponent;
import org.planit.utils.id.IdGroupingToken;

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
   */
  public GapFunction(final GapFunction other) {
    super(other);
    this.stopCriterion = other.stopCriterion.clone();
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
   * Returns the gap for the current iteration
   * 
   * @return gap for current iteration
   */
  public abstract double getGap();

  /**
   * Reset the gap function
   */
  public abstract void reset();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract GapFunction clone();

}
