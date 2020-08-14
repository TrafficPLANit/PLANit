package org.planit.path.choice;

import java.util.logging.Logger;

import org.planit.assignment.TrafficAssignmentComponentFactory;
import org.planit.assignment.TrafficComponentBuilder;
import org.planit.input.InputBuilderListener;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;

/**
 * All path choice instances are built using this or a derived version of this builder
 *
 * @author markr
 *
 */
public abstract class PathChoiceBuilder<T extends PathChoice> extends TrafficComponentBuilder<T> {

  /** the logger */
  protected static final Logger LOGGER = Logger.getLogger(PathChoiceBuilder.class.getCanonicalName());
 
    
  /**
   * Factory method to create the instance of the desired type
   * @return instance of traffic assignment 
   * @throws PlanItException thrown when error
   */
  @SuppressWarnings("unchecked")
  protected T createPathChoiceInstance() throws PlanItException {
    String pathChoiceClassName = getClassToBuild().getClass().getCanonicalName();
    TrafficAssignmentComponentFactory<PathChoice> pathChoiceFactory = new TrafficAssignmentComponentFactory<PathChoice>(pathChoiceClassName);
    final T pathChoice = (T) pathChoiceFactory.create(pathChoiceClassName, new Object[] { getGroupIdToken() });
    PlanItException.throwIf(!(pathChoice instanceof PathChoice), "not a valid path choice type");
    return pathChoice;
  }  
  
  /**
   * call to build and configure all sub components of this builder
   * 
   * @throws PlanItException  thrown if error
   */
  protected abstract void buildSubComponents(T pathChoiceInstance) throws PlanItException;


  // PUBLIC

  /**
   * Constructor
   * 
   * @param path choice class to build
   * @param projectToken           idGrouping token
   * @param inputBuilderListener   the input builder listener
   * @throws PlanItException thrown if error
   */
  protected PathChoiceBuilder(final Class<T> pathChoiceClass, final IdGroupingToken projectToken, InputBuilderListener inputBuilderListener) throws PlanItException {
    super(pathChoiceClass, projectToken, inputBuilderListener);    
  }

  /**
   * Build the path choice
   * 
   * @return path choice instance that is built
   * @throws PlanItException thrown if error
   */
  @Override
  public T build() throws PlanItException {
    // Build the assignment
    T pathChoice = createPathChoiceInstance();
    
    // build the sub components of the path choice as well
    buildSubComponents(pathChoice);

    // perform all delayed calls on the assignment to finalise the build
    getConfigurator().configure(pathChoice);
    
    return pathChoice;
  }

}