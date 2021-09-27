package org.planit.graph.directed.acyclic;

/**
 * Class to contain the data required on each registered vertex to enable topological sorting on an acyclic sub graph
 * 
 * @author markr
 *
 */
class AcyclicVertexData {

  /** pre visit index, required for topological sorting, see gupta et al. 2008 */
  public long preVisitIndex = 0;

  /** pvisit index, required for topological sorting, see gupta et al. 2008 */
  public long postVisitIndex = 0;
}
