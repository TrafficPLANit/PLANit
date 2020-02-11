package org.planit.route;

import java.util.Iterator;

import org.planit.output.enums.RouteIdType;
import org.planit.utils.graph.EdgeSegment;

/** Route interface representing a route through the network on edge segment level
 * @author markr
 *
 */
public interface Route {

	/** add an edge segment to the path by appending it
	 * @param edgeSegment
	 * @return true as per Collection.add
	 */
	Boolean addEdgeSegment(EdgeSegment edgeSegment);

	/** Iterator over the available edge segments
	 * @return edgseSegmentIterator
	 */
	Iterator<EdgeSegment> getIterator();

	/**
	 * Outputs this path as a String, appropriate to a specified path output type
	 *
	 * @param pathOutputType  the specified path output type
	 * @return String describing the path
	 */
	public String toString(final RouteIdType pathOutputType);

	/**
	 * Outputs this path as a String, appropriate to a specified path output type

	 * @return String describing the path
	 */
	@Override
	String toString();

}