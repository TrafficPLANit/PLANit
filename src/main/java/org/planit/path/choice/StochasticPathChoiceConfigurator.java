package org.planit.path.choice;

import org.planit.od.path.OdPathMatrix;
import org.planit.path.choice.logit.LogitChoiceModel;
import org.planit.path.choice.logit.LogitChoiceModelConfigurator;
import org.planit.path.choice.logit.LogitChoiceModelConfiguratorFactory;
import org.planit.utils.exceptions.PlanItException;

/**
 * Configurator for FixedConnectoidTravelTimeCost implementation
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
    LogitChoiceModelConfigurator<? extends LogitChoiceModel> logitChoiceModelConfigurator = LogitChoiceModelConfiguratorFactory.createConfigurator(logitChoiceModelType);
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
  public void setOdPathMatrix(final OdPathMatrix odPathSet) {
    registerDelayedMethodCall(SET_OD_PATH_MATRIX, odPathSet);
  }
}
