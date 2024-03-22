package org.goplanit.choice.logit;

import org.goplanit.choice.ChoiceModel;
import org.goplanit.utils.arrays.ArrayUtils;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.LoggingUtils;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Bounded MNL choice model implementation for probabilities considering absolute differences in utility.
 * (it has an additive error term, i.e. utility_i = modelled_utility_i + epsilon_i).
 * <p>
 *  It is bounded because
 *  the utility domain can be tweaked with a delta parameter to yield 0/1 probabilities when falling outside the bound in relation
 *  to a reference utility (alternative). Based on Watling et. al. 2018: Stochastic User Equilibrium with a bounded choice model.
 * </p>
 * <p>
 *   Note that the formulation  for obtaining the perceived cost has been first derived here as it was not part of the paper. In addition,
 *   the derivative of the perceived cost towards flow has also been newly derived here.
 * </p>
 *
 * @author markr
 *
 */
public class BoundedMultinomialLogit extends ChoiceModel {

  /** generated UID */
  private static final long serialVersionUID = -7602543264466240409L;

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(BoundedMultinomialLogit.class.getCanonicalName());

  /** Delta determines the bounds of the model, i.e., when delta is 1 and reference cost (best alternative) is 4 then
   * probabilities will range from 0-1 in domain [4-1,4+1]. Outdise of this domain, probabilities will always be 0 */
  private double delta = DEFAULT_DELTA;

  /**
   * Compute exp^(-scale(absolutePathCost + max_across_alts(-general_cost_alt_path_p) - delta)
   *
   * @param absolutePathCost the absolute pathcost of the alternative
   * @param negatedMaxAbsolutePathCost result of max_across_alts(-general_cost_alt_path_p)
   * @return exp^(-scale(absolutePathCost + max_across_alts(-general_cost_alt_path_p) - delta)
   */
  private double computeBoundedPathAlternativeExpValue(double absolutePathCost, double negatedMaxAbsolutePathCost){
    return Math.exp(-getScalingFactor() * (absolutePathCost + negatedMaxAbsolutePathCost - getDelta()));
  }

  /**
   * Compute exp^(-scale(absolutePathCost + max_across_alts(-general_cost_alt_path_p) - delta)-1
   * (do not allow negative values here as these always fall outside of domain, truncate to zero in those cases)
   *
   * @param absolutePathCost the absolute pathcost of the alternative
   * @param negatedMaxAbsolutePathCost result of max_across_alts(-general_cost_alt_path_p)
   * @return exp^(-scale(absolutePathCost + max_across_alts(-general_cost_alt_path_p) - delta)-1
   */
  private double computeBoundedPathAlternativeValue(double absolutePathCost, double negatedMaxAbsolutePathCost){
    return Math.max(0,computeBoundedPathAlternativeExpValue(absolutePathCost, negatedMaxAbsolutePathCost)-1);
  }

  /** Default used for delta. Assuming cost is in hours, a range of 20 hours in cost is covered, making the default
   * near identical to a regular MNL.
   */
  public static final double DEFAULT_DELTA = 20;

  /**
   * Constructor (public access required for reflection purposes)
   *
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public BoundedMultinomialLogit(final IdGroupingToken groupId) {
    super(groupId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double[] computeProbabilities(double[] alternativeCosts) {
    final var numAlternatives = alternativeCosts.length;
    var probabilities = new double[numAlternatives];

    /**********************************************************************************
     * In case of more than one path, calculate the probability for every path via
     *
     *
     *            exp^(-scale(general_cost_current_path + max_across_alts(-general_cost_alt_path_p) - delta)-1
     *          ----------------------------------
     *          SUM_candidates_p: (exp^(-scale(general_cost_path_p + max_across_alts(-general_cost_alt_path_p) - delta)-1)
     * ********************************************************************************/

    /* identify max cost option (costs are positive, so to find max of negated use min)*/
    final double negatedMaxAbsolutePathCost = -alternativeCosts[ArrayUtils.findMinValueIndex(alternativeCosts)];

    /* construct denominator: offset by min cost to avoid floating point overflow errors */
    double denominator = 0.0;
    for(int index = 0; index < numAlternatives; ++index) {
      var path_numerator_calc = computeBoundedPathAlternativeValue(alternativeCosts[index], negatedMaxAbsolutePathCost);
      probabilities[index] = path_numerator_calc; // abuse to avoid computing this again below
      denominator += path_numerator_calc;
    }

    /* construct probabilities */
    for(int index = 0; index < numAlternatives; ++index) {
      probabilities[index] = Math.min(1, Math.max(0, probabilities[index] / denominator)); // convert to probability
    }
    return probabilities;
  }

  /**
   * For bounded MNL the perceived cost in the context of OD path based demand is
   * ln(demand) -ln(exp(-scale(absolutePathCost + max_across_alts(-general_cost_alt_path_p) - delta)-1).
   * <p> In the special case the provided demand is zero or the cost of the alternative is such that it falls outside of the bounds
   * an exception is thrown as now suitable replacement value can be computed. This should instead be validated beforehand and this method
   * should not be called</p>
   *
   * {@inheritDoc}
   */
  @Override
  public double computePerceivedCost(double[] alternativeCosts, int index, double demand, boolean applyExpTransform) {

    if(demand <=0){
      LOGGER.severe("No demand, can't compute computeDPerceivedCostDFlow, using dummy value of 10^-12 --> DO NOT USE IN PRODUCTION");
      demand = Precision.EPSILON_12;
    }

    /* identify max cost option (costs are positive, so to find max of negated use min)*/
    final double negatedMaxAbsolutePathCost = -alternativeCosts[ArrayUtils.findMinValueIndex(alternativeCosts)];
    /* compute transformed value representative of bounded model, i.e., exp(-scale(absolutePathCost + max_across_alts(-general_cost_alt_path_p) - delta)-1 */
    final double alternativeCost = alternativeCosts[index];

    double alternativeTransformedValue;
    if( Math.abs(alternativeCost + negatedMaxAbsolutePathCost) > delta){
      // out of bounds -> best guess that we can compute is approaching from inside towards the bound
      alternativeTransformedValue = negatedMaxAbsolutePathCost + delta + Precision.EPSILON_12;
    }else {
      // regular approach
      alternativeTransformedValue = computeBoundedPathAlternativeValue(alternativeCost, negatedMaxAbsolutePathCost);
    }

    // ln(demand) -ln(exp(-scale(absolutePathCost + max_across_alts(-general_cost_alt_path_p) - delta)-1)
    // which may be transformed to
    //    exp(ln(demand) -ln(exp(-scale(absolutePathCost + max_across_alts(-general_cost_alt_path_p) - delta)-1)) ==
    //        fp/(exp(-scale(absolutePathCost + max_across_alts(-general_cost_alt_path_p) - delta)-1)
    if(!applyExpTransform){
      return Math.log(demand)-Math.log(alternativeTransformedValue);
    }else{
      return demand/alternativeTransformedValue;
    }
  }

  /** For bounded MNL we can work out the derivative of perceived cost towards flow when we know the impact of dAbsoluteCost on a flow change as
   *  well as the absolute cost itself. We support an exp transformation as well to allow for small values of demand.
   *
   * @param dAbsoluteCostDFlows derivatives of absolute cost towards flow
   * @param absoluteCosts absolute costs itself
   * @param index of the alternative explored
   * @param demand demand related to the logit model (usually path specific demand for example)
   * @param applyExpTransform when true consider exp transform of formulation, otherwise not
   * @return perceived dCost/dflow
   */
  @Override
  public double computeDPerceivedCostDFlow(double[] dAbsoluteCostDFlows, double[] absoluteCosts, int index, double demand, boolean applyExpTransform) {

    if(demand <=0){
      LOGGER.severe("No demand, can't compute computeDPerceivedCostDFlow, using dummy value of 10^-12 --> DO NOT USE IN PRODUCTION");
      demand = Precision.EPSILON_12;
    }

    // u'= exp(-scale(absolutePathCost + max_across_alts(-general_cost_alt_path_p) - delta)
    // u= u'-1
    int maxValueAlternativeIndex = ArrayUtils.findMinValueIndex(absoluteCosts); // max index based on min value as we negate
    final double negatedMaxAbsolutePathCost = -absoluteCosts[maxValueAlternativeIndex];
    double alternativeAbsolutecost = absoluteCosts[index];
    if( Math.abs(alternativeAbsolutecost - absoluteCosts[maxValueAlternativeIndex]) > delta){
      // out of bounds -> TODO work on best guess

      // ??? other option
      //alternativeAbsolutecost = negatedMaxAbsolutePathCost + delta + Precision.EPSILON_12;
      //alternativeAbsolutecost = negatedMaxAbsolutePathCost;
      return 0;
    }

    final double uPrime = computeBoundedPathAlternativeExpValue(alternativeAbsolutecost, negatedMaxAbsolutePathCost);
    final double u = uPrime - 1;

    // ln(demand) -ln(u) which may be transformed to fp/u with
    //
    // ln(demand) -ln(u) --> in dPerceivedCost/dDemand form:
    //   1/demand - 1/u * dcost/dflow(u) ==
    //    1/demand - 1/u * u` * -scale(dabsPathcost/dflow + dCost/dFlow(max_across_alts(-abscost_alt_path_p)))
    //
    // or fp/u --> in dPerceivedCost/dDemand form:
    //  chain adn product rule multiple times eventually gives
    //    1/u + (demand * -u`/u^2) * -scale(dabsPathcost/dflow + dCost/dFlow(max_across_alts(-abscost_alt_path_p)))
    //
    double temp = -getScalingFactor() *(dAbsoluteCostDFlows[index] + -dAbsoluteCostDFlows[maxValueAlternativeIndex]);
    if(!applyExpTransform){
      return 1/demand - (1/u * uPrime * temp);
    }else{
      return (u-demand*temp*uPrime)/Math.pow(u,2);
    }
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected BoundedMultinomialLogit(BoundedMultinomialLogit other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BoundedMultinomialLogit shallowClone() {
    return new BoundedMultinomialLogit(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BoundedMultinomialLogit deepClone() {
    return new BoundedMultinomialLogit(this, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    // No internal state (yet), do nothing
  }

  public double getDelta() {
    return delta;
  }

  public void setDelta(double delta) {
    this.delta = delta;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> collectSettingsAsKeyValueMap() {
    return super.collectSettingsAsKeyValueMap();
  }

}
