package org.goplanit.output.adapter;

import org.goplanit.output.adapter.traits.UntypedBushSegmentsOutputTypeAdapterTrait;
import org.goplanit.utils.graph.directed.EdgeSegment;

/**
 * Interface defining the methods required for a bush edge (segment) output adapter, i.e., for edge segments that are
 * part of a bush. For now, these include both conjugate and non-conjugate bushes, so we keep the edge segment as the
 * base class to be universally usable (albeit limited in what can be outputted).
 * 
 * @author markr
 *
 */
public interface BushLinkOutputTypeAdapter
        extends UntypedBushSegmentsOutputTypeAdapterTrait<EdgeSegment>, UntypedEdgeOutputTypeAdapter<EdgeSegment> {

}
