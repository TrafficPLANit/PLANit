package org.planit.interactor;

import org.djutils.event.EventListenerInterface;

/**
 * Interactor accessee. Each accessee is expected to listen to the relevant request events that
 * match with its signature.
 * For example a LinkVolumeAccessee reacts to INTERACTOR_REQUEST_LINKVOLUMEACCESSEE event types for
 * which someone registered it.
 * Whenever it has found an accessor it is expected to somehow instigate another event that provides
 * itself to the accessor, e.g., in the
 * case of the LinkVolume example, this entails firing an INTERACTOR_REQUEST_LINKVOLUMEACCESSEE
 * event. Note that accessor must be listening for
 * this event type fired from the producer that is being used. If not already, then the accessor
 * should be registered before firing this event otherwise
 * it will be missed.
 * 
 * Further, both for the accessee related event types as well as the accessor related event types,
 * the content consists of the respective accessor or accessee
 * depending on if it is a request for an accessee (then the accessor is provided as content), or
 * when the accessee is provided (then the accessee is the content).
 * This allows the accessor access to the accessee once it has parsed the contents of the response
 * event from the accessee.
 * 
 * 
 * mechanism
 * 
 * @author markr
 *
 */
public interface InteractorAccessee extends EventListenerInterface {

}
