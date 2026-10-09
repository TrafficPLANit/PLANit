package org.goplanit.assignment.ltm.sltm.loading;

import org.goplanit.utils.graph.directed.EdgeSegment;

/**
 * Interface to abstract out how turn flows are stored. Allows users to access
 * Network level turn flows based on original entry exit segment pairs and returns
 * the relevant turn flow information
 *
 * @author markr
 */
public interface TurnFlowAccessor {

  public double getTurnFlow(EdgeSegment from, EdgeSegment to);
}
