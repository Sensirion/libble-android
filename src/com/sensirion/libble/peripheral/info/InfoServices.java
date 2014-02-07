
package com.sensirion.libble.peripheral.info;

import java.util.HashMap;

public class InfoServices {

    private static HashMap<String, AbstractInfoService> SERVICES = new HashMap<String, AbstractInfoService>();

    static {
        final GattService gapService = new GattService();
        final GapService gattService = new GapService();
        final DeviceInfoService deviceInfoService = new DeviceInfoService();

        SERVICES.put(gapService.getUUID(), gapService);
        SERVICES.put(gattService.getUUID(), gattService);
        SERVICES.put(deviceInfoService.getUUID(), deviceInfoService);
    }

    public static AbstractInfoService getService(String uuid) {
        return SERVICES.get(uuid);
    }
}
