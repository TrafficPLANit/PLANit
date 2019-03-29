package org.planit.event;

public interface InteractorEvent extends Event {


	/** all interactor events are processed by the interactor listener
	 * @see org.planit.event.Event#isProcessedBy(org.planit.event.EventListener)
	 * @return true, if event is an interactor related event
	 */
	@Override
	default public boolean isProcessedBy(EventListener listener) {
		return listener instanceof InteractorListener;
	}

}