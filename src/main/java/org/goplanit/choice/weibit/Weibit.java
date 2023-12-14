package org.goplanit.choice.weibit;

import org.goplanit.choice.ChoiceModel;
import org.goplanit.utils.id.IdGroupingToken;

import java.util.Map;

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
