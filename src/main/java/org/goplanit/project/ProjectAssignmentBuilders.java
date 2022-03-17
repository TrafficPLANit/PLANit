package org.goplanit.project;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.goplanit.assignment.TrafficAssignmentBuilder;

/**
 * Container class for registered traffic assignments on PLANit projects
 *
 */
public class ProjectAssignmentBuilders implements Iterable<TrafficAssignmentBuilder<?>> {

  /**
   * The traffic assignment(s) registered on this project
   */
  protected final Set<TrafficAssignmentBuilder<?>> builders = new HashSet<TrafficAssignmentBuilder<?>>();

  /**
   * add traffic assignment
   * 
   * @param trafficAssignmentBuilder to add
   */
  protected void addTrafficAssignmentBuilder(TrafficAssignmentBuilder<?> trafficAssignmentBuilder) {
    builders.add(trafficAssignmentBuilder);
  }

  /**
   * Get the number of traffic assignment
   *
   * @return the number of traffic assignment in the project
   */
  public int size() {
    return builders.size();
  }

  /**
   * Check if assignments have already been registered
   *
   * @return true if registered assignments exist, false otherwise
   */
  public boolean isEmpty() {
    return builders.isEmpty();
  }

  /**
   * Collect the first traffic assignment that is registered (if any). Otherwise return null
   * 
   * @return first traffic assignment that is registered if none return null
   */
  public TrafficAssignmentBuilder<?> getFirst() {
    return isEmpty() ? builders.iterator().next() : null;
  }

  /**
   * iterable over registered traffic assignments
   */
  @Override
  public Iterator<TrafficAssignmentBuilder<?>> iterator() {
    return builders.iterator();
  }

}