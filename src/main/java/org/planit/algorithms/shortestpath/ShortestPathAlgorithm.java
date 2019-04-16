package org.planit.algorithms.shortestpath;

import javax.annotation.Nonnull;

import org.planit.exceptions.PlanItException;
import org.planit.network.EdgeSegment;
import org.planit.network.Vertex;
import org.planit.utils.Pair;

/**
 * An algorithm which calculates the shortest (a.k.a. lowest cost) route to all vertices from a given origin
 * 
 * @author gman6028
 *
 */
public interface ShortestPathAlgorithm {

/**
 * Construct shortest paths from source node to all other nodes in the network based on directed LinkSegment edges
 * 
 * @param currentOrigin            origin vertex of source node
 * @return 									array of pairs containing, for each vertex (array index), the cost to reach the vertex and the link segment it is reached from with the shortest cost.
 * @throws PlanItException		thrown if an error occurs
 */
	public Pair<Double,EdgeSegment>[] executeOneToAll(@Nonnull Vertex currentOrigin) throws PlanItException;
	
}
