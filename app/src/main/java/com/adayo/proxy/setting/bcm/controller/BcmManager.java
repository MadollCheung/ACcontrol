package com.adayo.proxy.setting.bcm.controller;

/**
 * BcmManager stub — 车身控制模块（座椅加热、车灯等）
 * 真实实现在车机系统框架中，此文件仅编译占位。
 */
public class BcmManager {
    public static final int AREA_DRIVER    = 1;
    public static final int AREA_PASSENGER = 4;
    public static final int AREA_HEADLIGHT = 0;
    public static final int SEAT_HEAT_PROPERTY = 356517131;
    public static final int HEADLIGHT_PROPERTY = 289410088;

    private static BcmManager instance;

    public static BcmManager getInstance() {
        if (instance == null) instance = new BcmManager();
        return instance;
    }

    public int setVehiclePropertyIntValue(int property, int area, int value) {
        throw new RuntimeException("Stub! 真实实现在车机系统框架中");
    }

    public int getVehiclePropertyIntValue(int property, int area) {
        throw new RuntimeException("Stub! 真实实现在车机系统框架中");
    }
}
