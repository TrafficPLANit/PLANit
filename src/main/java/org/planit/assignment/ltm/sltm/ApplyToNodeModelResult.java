package org.planit.assignment.ltm.sltm;

import org.ojalgo.array.Array1D;
import org.planit.algorithms.nodemodel.TampereNodeModel;
import org.planit.utils.functionalinterface.TriConsumer;
import org.planit.utils.network.layer.physical.Node;

/**
 * Apply this to the result of a Tampere node model execution for a particular node, it is invoked with the node the model was invoked on, the resulting flow acceptance factors and
 * the TampereNodeModel instance itself.
 * 
 * @author markr
 *
 */
public interface ApplyToNodeModelResult extends TriConsumer<Node, Array1D<Double>, TampereNodeModel> {

}
