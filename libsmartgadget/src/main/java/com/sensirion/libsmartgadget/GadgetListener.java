package com.sensirion.libsmartgadget;

import android.support.annotation.NonNull;

public interface GadgetListener {

    /**
     * Callback reporting that the gadget connection and service discovery initiated by the
     * {@link Gadget#connect()} function was successful.
     *
     * @param gadget The gadget the connection was established to.
     */
    void onGadgetConnected(@NonNull Gadget gadget);

    /**
     * Callback reporting that the gadget's connection got lost. This can happen even if
     * {@link Gadget#disconnect()} was not called.
     *
     * @param gadget The gadget to which the connection got lost.
     */
    void onGadgetDisconnected(@NonNull Gadget gadget);

    /**
     * Callback reporting that there were new values received for the given {@link GadgetService}.
     *
     * @param gadget  The gadget the values were sent from.
     * @param service The dedicated service.
     * @param values  the received values.
     */
    void onGadgetValuesReceived(@NonNull Gadget gadget, @NonNull GadgetService service,
                                @NonNull GadgetValue[] values);

    /**
     * Callback reporting that there were new values downloaded from the given
     * {@link GadgetDownloadService}.
     *
     * @param gadget   The gadgets from which the values are coming from.
     * @param service  The dedicated download service.
     * @param values   the received values.
     * @param progress the delivery progress in percent
     */
    void onGadgetDownloadDataReceived(@NonNull Gadget gadget, @NonNull GadgetDownloadService service,
                                      @NonNull GadgetValue[] values, int progress);

}
