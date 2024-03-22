package org.goplanit.choice.logit;

import java.util.Map;
import java.util.logging.Logger;

import org.goplanit.choice.ChoiceModel;
import org.goplanit.choice.weibit.Weibit;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;

/**
 * MNL choice model implementation for probabilities considering absolute differences in utility
 * (it has an additive error term, i.e. utility_i = modelled_utility_i + epsilon_i)
 *
 * @author markr
 *
 */
public class MultinomialLogit extends ChoiceModel {

  /** generated UID */
  private static final long serialVersionUID = -7602543264466240409L;

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(MultinomialLogit.class.getCanonicalName());

  /**
   * Constructor (public access required for reflection purposes)
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public MultinomialLogit(final IdGroupingToken groupId) {
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
     * In case of more than one route, calculate the probability for every route via
     *
     *
     *            exp^(scale * -general_cost_current_path)
     *          ----------------------------------
     *          SUM_candidates_p: (exp^(scale * -general_cost_of_path_p))
     * ********************************************************************************/

    /* identify minimum cost option */
    double minPathCost = Double.MAX_VALUE;
    for(int index = 0; index < numAlternatives; ++index){
      var pathCost = alternativeCosts[index];
      if( pathCost < minPathCost){
        minPathCost = pathCost;
      }
    }

    /* construct denominator: offset by min cost to avoid floating point overflow errors */
    final var scale = getScalingFactor();
    double denominator = 0.0;
    for(int index = 0; index < numAlternatives; ++index) {
      var path_numerator_calc = Math.exp(-(alternativeCosts[index]-minPathCost) * scale);
      probabilities[index] = path_numerator_calc; // abuse to avoid computing this again below
      denominator += path_numerator_calc;
    }

    /* construct probabilities */
    for(int index = 0; index < numAlternatives; ++index) {
      probabilities[index] = probabilities[index] / denominator; // convert to probability
    }
    return probabilities;
  }

  /**
   * For MNL the perceived cost in the context of OD path based demand = abs_cost + 1/scale * ln(demand).
   * However, for demand smaller than 1 this is a problem because it can become negative, so we may transform this by taking the
   * exponent.
   *
   * {@inheritDoc}
   */
  @Override
  public double computePerceivedCost(double[] alternativeCosts, int index, double demand, boolean applyExpTransform) {
    return computePerceivedCost(alternativeCosts[index], demand, applyExpTransform);
  }

  public double computePerceivedCost(double alternativeCost, double demand, boolean applyExpTransform) {

    if(demand <= 0){
      LOGGER.severe("Negative demand found, can't compute perceived cost (always zero), truncating to 10^-12");
      demand = Precision.EPSILON_12;
    }

    // abs_cost + 1/scale * ln(demand) == scale * abs_cost + ln(demand) which may be transformed to
    // exp(scale * abs_cost + ln(demand)) == exp(scale * abs_cost) * demand
    if(!applyExpTransform){
      return alternativeCost + Math.log(demand)/getScalingFactor();
    }else{
      return Math.exp(getScalingFactor() * alternativeCost) * demand;
    }
  }

  /** For MNL we can work out the derivative of perceived cost towards flow when we know the impact of dAbsoluteCost on a flow change as
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
    return computeDPerceivedCostDFlow(dAbsoluteCostDFlows[index], absoluteCosts[index], demand, applyExpTransform);
  }

  public double computeDPerceivedCostDFlow(double dAbsoluteCostDFlow, double absoluteCost, double demand, boolean applyExpTransform) {

    if(demand <= 0){
      LOGGER.severe("Negative demand found, can't compute perceived cost (always zero), truncating to 10^-12");
      demand = Precision.EPSILON_12;
    }

    // abs_cost + 1/scale * ln(demand) == scale * abs_cost + ln(demand) which may be transformed to
    // exp(scale * abs_cost + ln(demand)) == exp(scale * abs_cost) * demand
    //
    // in dPerceivedCost/dDemand form:
    //   d_abs_cost_d_flow + 1/(scale * demand) in untransformed form or when transformed
    //   scale * dAbsoluteCostDFlow * exp(scale * abs_cost) * demand + exp(scale * abs_cost)
    //   (the latter is because of d/dx of e(f(x))*x --> chain rule  --> f(x)'*e(f(x)))+e(f(x)*1
    var scalingFactor = getScalingFactor();
    if(!applyExpTransform){
      return dAbsoluteCostDFlow + 1/(scalingFactor * demand);
    }else{
      var expOfScaleTimesAbsCost = Math.exp(scalingFactor * absoluteCost);
      return scalingFactor * dAbsoluteCostDFlow * expOfScaleTimesAbsCost * demand + expOfScaleTimesAbsCost;
    }
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected MultinomialLogit(MultinomialLogit other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MultinomialLogit shallowClone() {
    return new MultinomialLogit(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MultinomialLogit deepClone() {
    return new MultinomialLogit(this, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    // No internal state (yet), do nothing
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> collectSettingsAsKeyValueMap() {
    return super.collectSettingsAsKeyValueMap();
  }

}
