package org.goplanit.network.virtual.physical.conjugate;

import org.goplanit.graph.GraphEntityFactoryImpl;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.virtual.physical.conjugate.ConjugateConnectoidNode;
import org.goplanit.utils.network.virtual.physical.conjugate.ConjugateConnectoidNodeFactory;
import org.goplanit.utils.network.virtual.physical.conjugate.ConjugateConnectoidNodes;
import org.goplanit.utils.network.virtual.graph.ConnectoidDirectedEdge;

/**
 * Factory for creating conjugate connectoid nodes on container.
 * 
 * @author markr
 */
public class ConjugateConnectoidNodeFactoryImpl
        extends GraphEntityFactoryImpl<ConjugateConnectoidNode> implements ConjugateConnectoidNodeFactory {

  /**
   * Constructor
   * 
   * @param groupId   to use
   * @param container to use
   */
  protected ConjugateConnectoidNodeFactoryImpl(final IdGroupingToken groupId, final ConjugateConnectoidNodes container) {
    super(groupId, container);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateConnectoidNode createNew(final ConnectoidDirectedEdge originalConnectoidEdge) {
    return new ConjugateConnectoidNodeImpl(originalConnectoidEdge, getIdGroupingToken());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateConnectoidNode registerNew(
          final ConnectoidDirectedEdge originalConnectoidEdge, boolean deriveFromOriginalEdge, String xmlIdPostFix) {
    final ConjugateConnectoidNode newEntity = createNew(originalConnectoidEdge);
    newEntity.populateXmlId(deriveFromOriginalEdge, xmlIdPostFix);
    getGraphEntities().register(newEntity);
    return newEntity;
  }

}
