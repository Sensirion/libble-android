package com.sensirion.libsmartgadget;

public interface GadgetNotificationService extends GadgetService {
    /**
     * Subscribe for the notification defined by this service. Notifications will be received via
     * {@link GadgetListener#onGadgetValuesReceived(Gadget, GadgetService, GadgetValue[])}.
     */
    void subscribe();

    /**
     * Unsubscribe from the notification defined by this service.
     */
    void unsubscribe();

    /**
     * Checks if you're already subscribed for the given service notifications.
     *
     * @return true if already subscribed.
     */
    boolean isSubscribed();
}
