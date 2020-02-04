package org.planit.path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToLongFunction;

import org.planit.output.enums.PathIdType;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.Vertex;
import org.planit.utils.misc.Pair;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.virtual.Centroid;
import org.planit.utils.network.virtual.ConnectoidSegment;

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
	 * Create the path from an implicit origin to a specified destination, using the vertexPathAndCost array as input
	 * coming from a shortest path algorithm output
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
	public static Path createODPath(final Centroid destination, final Pair<Double, EdgeSegment>[] vertexPathAndCost) {
		long downstreamVertexId = destination.getId();
		EdgeSegment edgeSegment = vertexPathAndCost[(int)downstreamVertexId].getSecond();
		final List<EdgeSegment> pathEdgeSegments = new ArrayList<EdgeSegment>();
		while (edgeSegment != null) {
			pathEdgeSegments.add(edgeSegment);
			downstreamVertexId = edgeSegment.getUpstreamVertex().getId();
			edgeSegment = vertexPathAndCost[(int)downstreamVertexId].getSecond();
		}
		Collections.reverse(pathEdgeSegments);
		return new Path(pathEdgeSegments);
	}

	/**
	 * List containing the edge segments in the path
	 */
	protected final List<EdgeSegment> path;

	/**
	 * Returns the path as a String of comma-separated node Id or external Id values
	 *
	 * @param idGetter lambda function to get the required Id value
	 * @return the path as a String of comma-separated node Id or external Id values
	 */
	private String getNodePathString(final ToLongFunction<Node> idGetter) {
		final StringBuilder builder = new StringBuilder("[");
		for (final EdgeSegment edgeSegment : path) {
			final Vertex vertex = edgeSegment.getUpstreamVertex();
			if (vertex instanceof Node) {
				final Node node = (Node) vertex;
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
	private String getEdgeSegmentPathString(final Function<EdgeSegment, Object> idGetter) {
		final StringBuilder builder = new StringBuilder("[");
		for (final EdgeSegment edgeSegment : path) {
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
	private String getEdgeSegmentPathExternalIdString() {
		return getEdgeSegmentPathString(edgeSegment -> {
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
	private String getEdgeSegmentPathIdString() {
		return getEdgeSegmentPathString(EdgeSegment::getId);
	}

	/**
	 * Output the path as a comma-separated list of node external Id numbers
	 *
	 * @return string of comma-separated list of node external Id numbers
	 */
	private String getNodePathExternalId() {
		return getNodePathString(Node::getExternalId);
	}

	/**
	 * Output the path as a comma-separated list of node Id numbers
	 *
	 * @return string of comma-separated list of node Id numbers
	 */
	private String getNodePathId() {
		return getNodePathString(Node::getId);
	}

	/**
	 * Constructor
	 */
	public Path() {
		this.path = new ArrayList<EdgeSegment>();
	}

	/**
	 * Constructor

	 * @param pathEdgeSegments the path to set (not copied)
	 */
	public Path(final List<EdgeSegment> pathEdgeSegments) {
		this.path = pathEdgeSegments;
	}

	/** add an edge segment to the path by appending it
	 * @param edgeSegment
	 * @return true as per Collection.add
	 */
	public Boolean addEdgeSegment(final EdgeSegment edgeSegment) {
		return this.path.add(edgeSegment);
	}

	/** Iterator over the available edge segments
	 * @return edgseSegmentIterator
	 */
	public Iterator<EdgeSegment> getIterator() {
		return this.path.iterator();
	}

	/**
	 * Outputs this path as a String, appropriate to a specified path output type
	 *
	 * @param pathOutputType  the specified path output type
	 * @return String describing the path
	 */
	public String toString(final PathIdType pathOutputType) {
		switch (pathOutputType) {
		case LINK_SEGMENT_EXTERNAL_ID:
			return getEdgeSegmentPathExternalIdString();
		case LINK_SEGMENT_ID:
			return getEdgeSegmentPathIdString();
		case NODE_EXTERNAL_ID:
			return getNodePathExternalId();
		case NODE_ID:
			return getNodePathId();
		}
		return "";
	}

	/**
	 * Outputs this path as a String, appropriate to a specified path output type

	 * @return String describing the path based on internal segment id
	 */
	@Override
	public String toString() {
		return getEdgeSegmentPathIdString();
	}

}