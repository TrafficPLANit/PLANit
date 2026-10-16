package org.goplanit.network.virtual.physical.conjugate;

import org.goplanit.graph.GraphEntityFactoryImpl;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.virtual.physical.ConnectoidSegment;
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
  public ConjugateConnectoidNode createNew(final ConnectoidSegment original) {
    return new ConjugateConnectoidNodeImpl(original, getIdGroupingToken());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateConnectoidNode registerNew(
          final ConnectoidSegment original, boolean deriveFromOriginal, String xmlIdPostFix) {
    final ConjugateConnectoidNode newEntity = createNew(original);
    newEntity.populateXmlId(deriveFromOriginal, xmlIdPostFix);
    getGraphEntities().register(newEntity);
    return newEntity;
  }

}
