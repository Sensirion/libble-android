package com.sensirion.libsmartgadget;

public interface GadgetNotificationService extends GadgetService {
    /**
     * Subscribe for the notification defined by this service. Notifications will be received via
     * {@link GadgetListener#onGadgetHasNewValues(Gadget, GadgetService, GadgetValue[])}.
     *
     * @return true if the subscription call was dispatched successfully.
     */
    boolean subscribe();

    /**
     * Unsubscribe from the notification defined by this service.
     *
     * @return true if the subscription cancel request was dispatched successfully.
     */
    boolean unsubscribe();

    /**
     * Checks if you're already subscribed for the given service notifications.
     *
     * @return true if already subscribed.
     */
    boolean isSubscribed();
}
