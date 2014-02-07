
package com.sensirion.libble.peripheral.info;

public abstract class AbstractInfoService {

    protected AbstractInfoService() {
    }

    public abstract String getUUID();

    public abstract String getName();

    public abstract String getCharacteristicName(String uuid);
}
