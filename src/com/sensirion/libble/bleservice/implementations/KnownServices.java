package com.sensirion.libble.bleservice.implementations;

import com.sensirion.libble.bleservice.implementations.generic_services.BatteryPeripheralService;
import com.sensirion.libble.bleservice.implementations.sensirion.shtc1_smartgadget.HumigadgetConnectionSpeedService;
import com.sensirion.libble.bleservice.implementations.sensirion.shtc1_smartgadget.HumigadgetLoggingService;
import com.sensirion.libble.bleservice.implementations.sensirion.shtc1_smartgadget.HumigadgetRHTNotificationService;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public enum KnownServices {

    /**
     * GENERIC BATTERY SERVICE FOR ALL DEVICES.
     */
    BATTERY_SERVICE(BatteryPeripheralService.class.getSimpleName(), Arrays.asList(BatteryPeripheralService.READ_BATTERY_CHARACTERISTIC_VALUE_NAME), java.lang.Integer.class),

    /**
     * HUMIGADGET RHT NOTIFICATION SERVICE
     */
    HUMIGADGET_RHT_NOTIFICATION_SERVICE(HumigadgetRHTNotificationService.class.getSimpleName(), Arrays.asList(HumigadgetRHTNotificationService.RHT_CHARACTERISTIC_READ_NAME), HumigadgetRHTNotificationService.class),

    /**
     * HUMIGADGET LOGGING SERVICE
     */
    HUMIGADGET_LOGGING_SERVICE(HumigadgetLoggingService.class.getSimpleName(), new LinkedList<String>(), HumigadgetLoggingService.class),

    /**
     * HUMIGADGET CONNECTION SPEED SERVICE
     */
    HUMIGADGET_CONNECTION_SPEED_SERVICE(HumigadgetConnectionSpeedService.class.getSimpleName(), HumigadgetConnectionSpeedService.CHARACTERISTICS_VALUE_LIST, java.lang.Integer.class);

    public final String NAME; //Class name of the service
    public final List<String> NAMED_CHARACTERISTICS; //Characteristics name of the service.
    public final Class<?> TYPE; //Type of return when reading a characteristic

    private KnownServices(final String name, final List<String> characteristicsList, final Class<?> type) {
        NAME = name;
        NAMED_CHARACTERISTICS = characteristicsList;
        TYPE = type;
    }
}