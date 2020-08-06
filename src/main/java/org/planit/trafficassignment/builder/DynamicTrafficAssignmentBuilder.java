package org.planit.trafficassignment.builder;

import org.planit.demands.Demands;
import org.planit.input.InputBuilderListener;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.virtual.Zoning;
import org.planit.path.choice.PathChoice;
import org.planit.path.choice.PathChoiceBuilder;
import org.planit.sdinteraction.smoothing.Smoothing;
import org.planit.trafficassignment.DynamicTrafficAssignment;
import org.planit.trafficassignment.TrafficAssignmentComponentFactory;
import org.planit.utils.exceptions.PlanItException;

/**
 * A dynamic traffic assignment builder is assumed to only support capacity constrained traffic assignment instances. It is used to build the traffic assignment instance with the
 * proper configuration settings
 *
 * @author markr
 *
 */
public class DynamicTrafficAssignmentBuilder extends CapacityConstrainedTrafficAssignmentBuilder implements PathChoiceBuilder {

  // needed to allow path choice to register inputbuilder listener on its traffic components
  @SuppressWarnings("unused")
  private final InputBuilderListener trafficComponentCreateListener;

  /** the path choice factory */
  final protected TrafficAssignmentComponentFactory<PathChoice> pathChoiceFactory;

  /**
   * Constructor
   *
   * @param assignment                     the dynamic assignment
   * @param trafficComponentCreateListener the listener for further traffic components that are created by the builder
   * @param demands                        the demands
   * @param zoning                         the zoning
   * @param physicalNetwork                the physical network
   * @throws PlanItException thrown if there is an exception
   */
  public DynamicTrafficAssignmentBuilder(final DynamicTrafficAssignment assignment, final InputBuilderListener trafficComponentCreateListener, final Demands demands,
      final Zoning zoning, final PhysicalNetwork physicalNetwork) throws PlanItException {
    super(assignment, trafficComponentCreateListener, demands, zoning, physicalNetwork);
    this.trafficComponentCreateListener = trafficComponentCreateListener;
    pathChoiceFactory = new TrafficAssignmentComponentFactory<PathChoice>(PathChoice.class);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PathChoice createAndRegisterPathChoice(final String pathChoiceType) throws PlanItException {
    final PathChoice pathChoice = pathChoiceFactory.create(pathChoiceType, new Object[] { parentAssignment.getIdGroupingtoken() });
    ((DynamicTrafficAssignment) parentAssignment).setPathChoice(pathChoice);
    return pathChoice;
  }

  // PUBLIC FACTORY METHODS

  /**
   * Create and Register smoothing component
   *
   * @param smoothingType the type of smoothing component to be created
   * @return Smoothing object created
   * @throws PlanItException thrown if there is an error
   */
  @Override
  public Smoothing createAndRegisterSmoothing(final String smoothingType) throws PlanItException {
    final Smoothing smoothing = smoothingFactory.create(smoothingType, new Object[] { parentAssignment.getIdGroupingtoken() });
    parentAssignment.setSmoothing(smoothing);
    return smoothing;
  }

}
