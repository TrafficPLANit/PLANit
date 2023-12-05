package org.goplanit.path.choice;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.goplanit.assignment.ltm.sltm.StaticLtmDirectedPath;
import org.goplanit.od.path.OdPathMatrix;
import org.goplanit.path.choice.logit.LogitChoiceModel;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.path.SimpleDirectedPath;
import org.w3.xlink.Simple;

/**
 * Stochastic path choice component. Stochasticity is reflected by the fact that the path choice is applied by means of
 * a logit model, to be configured here. Also, due to being  stochastic the paths mey be provided beforehand.
 * The latter is also configured via this class
 *
 * @author markr
 *
 */
public class StochasticPathChoice extends PathChoice {

  /**
   * generated UID
   */
  private static final long serialVersionUID = 6617920674217225019L;

  /**
   * the logger
   */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(StochasticPathChoice.class.getCanonicalName());

  /** scaling factor for MNL, todo: make user configurable in the form of spread */
  private static final double SCALE = 14;

  /**
   * The registered logit choice model
   */
  protected LogitChoiceModel logitChoiceModel = null;

  /**
   * Constructor
   *
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public StochasticPathChoice(IdGroupingToken groupId) {
    super(groupId);
  }

  /**
   * Copy constructor
   *
   * @param other    to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected StochasticPathChoice(final StochasticPathChoice other, boolean deepCopy) {
    super(other, deepCopy);
    this.logitChoiceModel = other.logitChoiceModel; // not owned
  }

  /**
   * Perform the path choice by determining the path probabilities based on path cost and scaling factor
   *
   * @param paths path alternatives to consider
   * @param pathCosts costs of each path in order of collection
   * @return computed probabilities
   */
  public double[] computePathProbabilities(Collection<? extends SimpleDirectedPath> paths, double[] pathCosts) {
    var numPaths = pathCosts.length;
    if(numPaths==1){
      return new double[]{1.0};
    }

    var probabilities = new double[pathCosts.length];

    // TODO: move to logit model + write unit test
    /* ********************************************************************************
     * In case of more than one route, calculate the probability for every route via
     *
     *
     *            e^(-scale * general_cost_current_path)
     *          ----------------------------------
     *          SUM_candidates_p: (e^(-scale * general_cost_of_path_p))
     * ********************************************************************************/

    /* identify minimum cost option */
    double minPathCost = Double.MAX_VALUE;
    for(int index = 0; index < numPaths; ++index){
      var pathCost = pathCosts[index];
      if( pathCost < minPathCost){
        minPathCost = pathCost;
      }
    }

    /* construct denominator: offset by min cost to avoid floating point overflow errors */
    double denominator = 0.0;
    for(int index = 0; index < numPaths; ++index) {
      var path_numerator_calc = Math.exp((pathCosts[index]-minPathCost) * -SCALE);
      probabilities[index] = path_numerator_calc; // abuse to avoid computing this again below
      denominator += path_numerator_calc;
    }

    /* construct probabilities */
    for(int index = 0; index < numPaths; ++index) {
      probabilities[index] = probabilities[index] / denominator; // convert to probability
    }
    return probabilities;
  }

  /**
   * set the chosen logit model
   *
   * @param logitChoiceModel chosen model
   */
  public void setLogitModel(LogitChoiceModel logitChoiceModel) {
    this.logitChoiceModel = logitChoiceModel;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StochasticPathChoice shallowClone() {
    return new StochasticPathChoice(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StochasticPathChoice deepClone() {
    return new StochasticPathChoice(this,true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    logitChoiceModel.reset();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> collectSettingsAsKeyValueMap() {
    return logitChoiceModel.collectSettingsAsKeyValueMap();
  }

}
