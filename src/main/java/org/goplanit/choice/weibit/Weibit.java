package org.goplanit.choice.weibit;

import org.goplanit.choice.ChoiceModel;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Weibit choice model implementation for probabilities considering ratio of differences in utility
 * (it also has a multilicative error term, i.e. utility_i = modelled_utility_i * epsilon_i)
 *
 * @author markr
 *
 */
public class Weibit extends ChoiceModel {

  /** generated UID */
  private static final long serialVersionUID = -7602543264466240409L;

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(Weibit.class.getCanonicalName());

  /**
   * Constructor (public access required for reflection purposes)
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public Weibit(final IdGroupingToken groupId) {
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
     *            ( 1/-general_cost_current_path)^scale
     *          ----------------------------------
     *          SUM_candidates_p: ((1/-general_cost_of_path_p))^scale
     * ********************************************************************************/

    /* construct denominator: offset by min cost to avoid floating point overflow errors */
    final var scale = getScalingFactor();
    double denominator = 0.0;
    for(int index = 0; index < numAlternatives; ++index) {
      var path_numerator_calc = Math.pow(1/-alternativeCosts[index], scale);
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
   * For Weibit the perceived cost in the context of OD path based demand = ln(abs_cost) + 1/scale * ln(demand).
   * However, for demand and/or cost smaller than 1 this is a problem because the result can become negative, so we may transform this by taking the
   * exponent.
   *
   * {@inheritDoc}
   */
  @Override
  public double computePerceivedCost(double absoluteCost, double demand, boolean applyExpTransform) {
    if(demand < Precision.EPSILON_12 && !applyExpTransform){
      LOGGER.warning("No demand, applying dummy demand of 1 --> consider using exp transformed version instead which can deal with this");
      demand = 1;
    }


    // ln(abs_cost) + 1/scale * ln(demand) == which may be transformed to
    // exp(ln(abs_cost) + 1/scale * ln(demand)) = exp(ln(abs_cost) * (exp(ln(demand))^1/scale  = abs_cost * demand^(1/scale)
    if(!applyExpTransform){
      if(demand < 1){
        LOGGER.severe("No demand below 1 possible for non-transformed perceived cost, applying dummy demand of 1 --> DO NOT USE IN PRODUCTION switch to exp transformed");
        demand = 1;
      }
      return Math.log(absoluteCost) + 1/getScalingFactor() * Math.log(demand);
    }else{
      if(demand < 1){
        // todo current exp transform does not solve issue since it increases in size between 0-1 --> requires multiplication by scaling factor to avoid this
        // todo: this before led to incorrect calculation, probably bug, so when working revisit and update to include this approach again
        // todo: UPDATE DERIVATIVE method below accordingly to keep consistent
        LOGGER.warning("exp transformed solution does not cope with small demand, switch-off demand component of cost, DO NOT USE IN PRODUCTION");
        return absoluteCost;
      }
      return absoluteCost * Math.pow(demand, 1/getScalingFactor());
    }
  }

  /** For Weibit we can work out the derivative of perceived cost towards flow when we know the impact of dAbsoluteCost on a flow change as
   *  well as the absolute cost itself. We support an exp transformation as well to allow for small values of demand.
   *
   *
   * @param dAbsoluteCostDFlow derivative of absolute cost towards flow
   * @param absoluteCost absolute cost itself
   * @param demand demand related to the logit model (usually path specific demand for example)
   * @param applyExpTransform when true consider exp transform of formulation, otherwise not
   * @return perceived dCost/dflow
   */
  @Override
  public double computeDPerceivedCostDFlow(double dAbsoluteCostDFlow, double absoluteCost, double demand, boolean applyExpTransform) {
    if(demand < Precision.EPSILON_12 && !applyExpTransform){
      LOGGER.warning("No demand for dPerceivedCost/dFlow, applying dummy demand of 1 --> consider using exp transformed version instead which can deal with this");
      demand = 1;
    }

    // ln(abs_cost) + 1/scale * ln(demand)  which may be exp transformed to
    // abs_cost * demand^(1/scale)
    //
    // in dPerceivedCost/dDemand form:
    //   1/scale*d_abs_cost_d_flow + 1/(demand) in untransformed form or when transformed
    //   abs_cost^scale * demand
    var scalingFactor = getScalingFactor();
    if(!applyExpTransform){
      //                            f = ln(1/absoluteCost), g = 1/scale * ln(demand)
      // sum of derivatives rule:   d(f + g) = d(f) + d(g), and
      //                              d(f) --> chain rule d(f) = d(ln(h)) = 1/h * d(h) --> 1/abs_cost * dAbsoluteCostDFlow
      //                              d(g)  = 1/scale * d(ln(demand) = 1/scale * 1/demand = 1/(scale * demand)
      //                            f' + g' =
      return dAbsoluteCostDFlow/absoluteCost + 1/(scalingFactor * demand);
    }else{
      if(demand < 1){
        // todo current exp transform does not solve issue since it increases in size between 0-1 --> requires multiplication by scaling factor to avoid this
        // todo: this before led to incorrect calculation, probably bug, so when working revisit and update to include this approach again
        // todo: UPDATE DERIVATIVE method below accordingly to keep consistent
        // so d(exp(ln(abs_cost))) is what is left after exp transforming the original function when setting demand portion to 0 --> we get d_abs_cost
        LOGGER.warning("exp transformed solution does not cope with demand <1 yet, use only non-demand component of cost for derivative, DO NOT USE IN PRODUCTION");
        return dAbsoluteCostDFlow;
      }
      // abs_cost * demand^(1/scale)
      //                f = abs_cost, g = demand^(1/scale)
      //                f' = dAbsoluteCostDFlow, g' = (1/scale)*demand^((1/scale)-1)
      // chain rule:    f' * g + f * g'
      return dAbsoluteCostDFlow * Math.pow(demand, 1/getScalingFactor()) + absoluteCost * (1/getScalingFactor() * Math.pow(demand, (1/getScalingFactor())-1));
    }
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected Weibit(Weibit other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Weibit shallowClone() {
    return new Weibit(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Weibit deepClone() {
    return new Weibit(this, true);
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
