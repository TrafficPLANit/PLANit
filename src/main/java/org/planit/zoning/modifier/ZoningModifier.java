package org.planit.zoning.modifier;

import org.planit.utils.graph.modifier.GraphModifier;

/**
 * Counterpart to its graph equivalent {@link GraphModifier}. Provides methods to make modifications to the zoning on a higher level. Currently mainly methods are provided to keep
 * any references to network entities consistent when modifications are made to the network using the GraphModifier, e.g, break links which can cause references to links or link
 * segments on zoning entities to become invalid.
 * 
 * @author mark
 *
 */
public interface ZoningModifier {

}
