package org.planit.path.choice;

import java.util.logging.Logger;

import org.planit.component.PlanitComponentFactory;
import org.planit.input.InputBuilderListener;
import org.planit.path.choice.logit.LogitChoiceModel;
import org.planit.utils.builder.Configurator;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;

/**
 * All path choice instances are built using this or a derived version of this builder
 *
 * @author markr
 *
 */
public class StochasticPathChoiceBuilder extends PathChoiceBuilder<StochasticPathChoice> {

  /** the logger */
  protected static final Logger LOGGER = Logger.getLogger(StochasticPathChoiceBuilder.class.getCanonicalName());

  /**
   * {@inheritDoc}
   */
  @Override
  protected Configurator<StochasticPathChoice> createConfigurator() throws PlanItException {
    return new StochasticPathChoiceConfigurator();
  }

  /**
   * create a logit model instance based on passed in configurator
   * 
   * @param configurator for the path choice
   * @return created choice model
   * @throws PlanItException thrown if error
   */
  protected LogitChoiceModel createLogitChoiceModelInstance(StochasticPathChoiceConfigurator configurator) throws PlanItException {
    PlanitComponentFactory<LogitChoiceModel> logitChoiceModelFactory = new PlanitComponentFactory<LogitChoiceModel>(LogitChoiceModel.class);
    logitChoiceModelFactory.addListener(getInputBuilderListener());
    return logitChoiceModelFactory.create(configurator.getLogitModel().getClassTypeToConfigure().getCanonicalName(), new Object[] { getGroupIdToken() });
  }

  /**
   * call to build and configure all sub components of this builder
   * 
   * @param pathChoiceInstance the instance to build on
   * @throws PlanItException thrown if error
   */
  @Override
  protected void buildSubComponents(StochasticPathChoice pathChoiceInstance) throws PlanItException {
    StochasticPathChoiceConfigurator configurator = ((StochasticPathChoiceConfigurator) getConfigurator());

    // build logit model
    if (configurator.getLogitModel() != null) {
      LogitChoiceModel logitModel = createLogitChoiceModelInstance(configurator);
      configurator.getLogitModel().configure(logitModel);
      pathChoiceInstance.setLogitModel(logitModel);
    }
  }

  // PUBLIC

  /**
   * Constructor
   * 
   * @param projectToken         idGrouping token
   * @param inputBuilderListener the input builder listener
   * @throws PlanItException thrown if error
   */
  public StochasticPathChoiceBuilder(final IdGroupingToken projectToken, InputBuilderListener inputBuilderListener) throws PlanItException {
    super(StochasticPathChoice.class, projectToken, inputBuilderListener);
  }

}