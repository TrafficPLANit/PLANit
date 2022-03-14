package org.goplanit.assignment.ltm.eltm;

import java.util.Map;

import org.goplanit.assignment.ltm.LtmAssignment;
import org.goplanit.interactor.InteractorAccessor;
import org.goplanit.output.adapter.OutputTypeAdapter;
import org.goplanit.output.enums.OutputType;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * Event based Link Transmission Model implementation (eLTM) for network loading using a capacity constrained (Deterministic) assignment
 *
 * @author markr
 *
 */
public class EventBasedLtm extends LtmAssignment {

  /** generated UID */
  private static final long serialVersionUID = 994316948946768870L;

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public EventBasedLtm(IdGroupingToken groupId) {
    super(groupId);
  }

  /**
   * Copy Constructor
   * 
   * @param eltm to copy
   */
  public EventBasedLtm(EventBasedLtm eltm) {
    super(eltm);
  }

  @Override
  public OutputTypeAdapter createOutputTypeAdapter(OutputType outputType) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void executeEquilibration() throws PlanItException {
    // TODO Auto-generated method stub

  }

  @Override
  public int getIterationIndex() {
    // TODO Auto-generated method stub
    return 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EventBasedLtm clone() {
    return new EventBasedLtm(this);
  }

  @Override
  public Class<? extends InteractorAccessor<?>> getCompatibleAccessor() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    // TODO Auto-generated method stub
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> collectSettingsAsKeyValueMap() {
    return null;
  }

}
