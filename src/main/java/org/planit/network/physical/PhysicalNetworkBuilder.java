package org.planit.network.physical;

import org.planit.graph.DirectedGraphBuilder;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.Node;

/**
 * builder and modifier for physical networks of given parameterised types.
 * 
 * @author markr
 *
 */
public interface PhysicalNetworkBuilder<N extends Node, L extends Link, LS extends LinkSegment> extends DirectedGraphBuilder<N, L, LS> {

}
