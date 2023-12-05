package org.goplanit.path.choice;

import org.goplanit.od.path.OdPathMatrix;
import org.goplanit.path.choice.logit.LogitChoiceModel;
import org.goplanit.path.choice.logit.LogitChoiceModelConfigurator;
import org.goplanit.path.choice.logit.LogitChoiceModelConfiguratorFactory;
import org.goplanit.utils.exceptions.PlanItException;

/**
 * Configurator for StochasticPathChoice implementation
 * 
 * @author markr
 */
public class StochasticPathChoiceConfigurator extends PathChoiceConfigurator<StochasticPathChoice> {
  
  private static final String SET_OD_PATH_MATRIX = "setOdPathMatrix";
    
  /**
   * logit choice model configurator
   */
  protected LogitChoiceModelConfigurator<? extends LogitChoiceModel> logitChoiceModelConfigurator;
  
  /**
   * Constructor
   * 
   */
  protected StochasticPathChoiceConfigurator() {
    super(StochasticPathChoice.class);        
  }
  
  /**
   * create and register the logit model of choice
   * 
   * @param logitChoiceModelType name of the class to be instantiated
   * @return the logit choice model that is registered
   * @throws PlanItException thrown if error
   */
  public LogitChoiceModelConfigurator<? extends LogitChoiceModel> createAndRegisterLogitModel(final String logitChoiceModelType) throws PlanItException {
    this.logitChoiceModelConfigurator = LogitChoiceModelConfiguratorFactory.createConfigurator(logitChoiceModelType);
    return logitChoiceModelConfigurator;
  }
  
  /**
   * Collect the logit model configurator
   * 
   * @return logit model configurator
   */
  public LogitChoiceModelConfigurator<? extends LogitChoiceModel> getLogitModel() {
    return logitChoiceModelConfigurator;
  }

  /**
   * Register a fixed od path set to use in the form of an ODPathMatrix
   *
   * @param odPathSet the fixed od path set in the shape of an od path matrix
   */
  public void setFixedOdPathMatrix(final OdPathMatrix odPathSet) {
    registerDelayedMethodCall(SET_OD_PATH_MATRIX, odPathSet);
  }
}
