package org.goplanit.choice.logit;

import java.util.Map;

import org.goplanit.choice.ChoiceModel;
import org.goplanit.utils.id.IdGroupingToken;

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
  public double computePerceivedCost(double absoluteCost, double demand, boolean applyExpTransform) {
    // abs_cost + 1/scale * ln(demand) == scale * abs_cost + ln(demand) which may be transformed to
    // exp(scale * abs_cost + ln(demand)) == exp(scale * abs_cost) * demand)
    if(!applyExpTransform){
      return getScalingFactor() * absoluteCost + Math.log(demand);
    }else{
      return Math.exp(getScalingFactor() * absoluteCost) * demand;
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
