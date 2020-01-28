package org.planit.od.odpath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToLongFunction;

import org.planit.network.EdgeSegmentImpl;
import org.planit.network.VertexImpl;
import org.planit.network.virtual.ConnectoidSegment;
import org.planit.output.enums.PathIdType;
import org.planit.utils.misc.Pair;
import org.planit.utils.network.EdgeSegment;
import org.planit.utils.network.Vertex;
import org.planit.utils.network.physical.Node;
import org.planit.zoning.Zone;

/**
 * This object creates a path of LinkSegment objects to a specified destination using the vertexPathAndCost object created by the (Dijkstra) Shortest Path Algorithm
 * 
 * The path creation makes use of the fact that the origin pair will have a null EdgeSegment, so there is no need to specify the origin.
 * 
 * @author gman6028
 *
 */
public class Path {
	
	/**
	 * List containing the edge segments in the path
	 */
	
	private List<EdgeSegment> path;
	/**
	 * Create the path from a specified origin to a specified destination, using the vertexPathAndCost array as input
	 * 
	 * This method makes use of the fact that the origin pair in the vertexPathAndCost array has a null EdgeSegment.
	 * It searches through vertexPathAndCost from the destination centroid until it finds a null EdgeSegment, which must 
	 * represent the origin centroid.  This is quicker than doing an instanceof test to determine whether the 
	 * upstream vertex is a physical node.
	 * 
	 * 
	 * @param destination the specified destination zone
	 * @param vertexPathAndCost the vertexPathAndCost array (previously calculated by the traffic assignment)
	 */
	private void createPath(Zone destination, Pair<Double, EdgeSegment>[] vertexPathAndCost) {
		int downstreamVertexId = (int) destination.getCentroid().getId();
		EdgeSegment edgeSegment = vertexPathAndCost[downstreamVertexId].getSecond();
		while (edgeSegment != null) {
			path.add(edgeSegment);
			downstreamVertexId = (int) edgeSegment.getUpstreamVertex().getId();
			edgeSegment = vertexPathAndCost[downstreamVertexId].getSecond();
		} 
		Collections.reverse(path);
	}
	
// Alternative version of the createPath method
//	private void createPath(Zone destination, Pair<Double, EdgeSegment>[] vertexPathAndCost) {
//		for (EdgeSegment edgeSegment = vertexPathAndCost[(int) destination.getCentroid().getId()].getSecond(); edgeSegment != null; edgeSegment = vertexPathAndCost[(int) edgeSegment.getUpstreamVertex().getId()].getSecond()) {
//			path.add(edgeSegment);
//		}
//		Collections.reverse(path);
//	}
	
	/**
	 * Returns the path as a String of comma-separated node Id or external Id values
	 * 
	 * @param idGetter lambda function to get the required Id value
	 * @return the path as a String of comma-separated node Id or external Id values
	 */
	private String getNodePath(ToLongFunction<Node> idGetter) {
		StringBuilder builder = new StringBuilder("[");
		for (EdgeSegment edgeSegment : path) {
			Vertex vertex = edgeSegment.getUpstreamVertex();
			if (vertex instanceof Node) {
				Node node = (Node) vertex;
				builder.append(idGetter.applyAsLong(node));
				if (edgeSegment.getDownstreamVertex() instanceof Node) {
					builder.append(",");
				}
			}
		}
		builder.append("]");
		return new String(builder);
	}

	/**
	 * Returns the path as a String of comma-separated edge segment Id or external Id values
  	 * 
	 * @param idGetter lambda function to get the required Id value
	 * @return the path as a String of comma-separated link segment Id or external Id values
	 */
	private String getEdgeSegmentPath(Function<EdgeSegment, Object>idGetter) {
		StringBuilder builder = new StringBuilder("[");
		for (EdgeSegment edgeSegment : path) {
			builder.append(idGetter.apply(edgeSegment));
			builder.append(",");
		}
		builder.deleteCharAt(builder.length()-1);
		builder.append("]");
		return new String(builder);
	}

	/**
	 * Output the path as a comma-separated list of edge segment external Id numbers
	 * 
	 * @return string of comma-separated list of edge segment external Id numbers
	 */
	private String getEdgeSegmentPathExternalId() {
		return getEdgeSegmentPath(edgeSegment -> {
			if ((edgeSegment instanceof ConnectoidSegment) && !(((ConnectoidSegment) edgeSegment).hasExternalId())){
				return "Connectoid Undefined";
			} 
			return edgeSegment.getExternalId();
		});
	}
	
	/**
	 * Output the path as a comma-separated list of edge segment Id numbers
	 * 
	 * @return string of comma-separated list of edge segment Id numbers
	 */
	private String getEdgeSegmentPathId() {
		return getEdgeSegmentPath(EdgeSegment::getId);
	}
	
	/**
	 * Output the path as a comma-separated list of node external Id numbers
	 * 
	 * @return string of comma-separated list of node external Id numbers
	 */
	private String getNodePathExternalId() {
		return getNodePath(Node::getExternalId);
	}
	
	/**
	 * Output the path as a comma-separated list of node Id numbers
	 * 
	 * @return string of comma-separated list of node Id numbers
	 */
	private String getNodePathId() {
		return getNodePath(Node::getId);
	}

	/**
	 * Constructor
	 * 
	 * @param destination the specified destination zone
	 * @param vertexPathAndCost the vertexPathAndCost array (previously calculated by the traffic assignment)
	 */
	public Path(Zone destination, Pair<Double, EdgeSegment>[] vertexPathAndCost) {
		path = new ArrayList<EdgeSegment>();
		createPath(destination, vertexPathAndCost);
	}
	
	/**
	 * Outputs this path as a String, appropriate to a specified path output type
	 * 
	 * @param pathOutputType  the specified path output type
	 * @return String describing the path
	 */
	public String toString(PathIdType pathOutputType) {
		switch (pathOutputType) {
		case LINK_SEGMENT_EXTERNAL_ID:
			return getEdgeSegmentPathExternalId();
		case LINK_SEGMENT_ID:
			return getEdgeSegmentPathId();
		case NODE_EXTERNAL_ID:
			return getNodePathExternalId();			
		case NODE_ID:
			return getNodePathId();
		}
		return "";
	}
	
}