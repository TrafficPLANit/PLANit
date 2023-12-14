package org.goplanit.path.choice;

import java.util.logging.Logger;

import org.goplanit.component.PlanitComponentFactory;
import org.goplanit.input.InputBuilderListener;
import org.goplanit.choice.ChoiceModel;
import org.goplanit.choice.ChoiceModelConfigurator;
import org.goplanit.utils.builder.Configurator;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;

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
   * create a choice model instance based on passed in configurator
   * 
   * @param choiceConfigurator for the choice model that is to be created
   * @return created choice model
   */
  protected ChoiceModel createChoiceModelInstance(ChoiceModelConfigurator<?> choiceConfigurator) {
    PlanitComponentFactory<ChoiceModel> choiceModelFactory = new PlanitComponentFactory<>(ChoiceModel.class);
    choiceModelFactory.addListener(getInputBuilderListener());
    return choiceModelFactory.create(
            choiceConfigurator.getClassTypeToConfigure().getCanonicalName(), new Object[] { getGroupIdToken() });
  }

  /**
   * call to build and configure all sub-components of this builder
   * 
   * @param pathChoiceInstance the instance to build on
   */
  @Override
  protected void buildSubComponents(StochasticPathChoice pathChoiceInstance){
    StochasticPathChoiceConfigurator configurator = ((StochasticPathChoiceConfigurator) getConfigurator());

    // create choice model component
    if (configurator.getChoiceModel() != null) {
      var choiceModel = createChoiceModelInstance(configurator.getChoiceModel());
      configurator.getChoiceModel().configure(choiceModel);
      pathChoiceInstance.setChoiceModel(choiceModel);
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

  /**
   * Constructor
   *
   * @param configurator        the configurator to adopt (copy by reference)
   * @param projectToken         idGrouping token
   * @param inputBuilderListener the input builder listener
   * @throws PlanItException thrown if error
   */
  public StochasticPathChoiceBuilder(final StochasticPathChoiceConfigurator configurator,
                                     final IdGroupingToken projectToken,
                                     InputBuilderListener inputBuilderListener) throws PlanItException {
    this(projectToken, inputBuilderListener);
    setConfigurator(configurator); // override and use this configurator instead of starting from scratch
  }

}