package com.adayo.proxy.setting.hvac.controller;

/**
 * HvacManager stub — 真实实现在车机系统框架中（com.adayo.app.hvac）
 * App 需以系统签名安装才能调用真实实现。
 * 此文件仅用于编译占位，运行时由车机系统提供真实实现。
 */
public class HvacManager {
    public static final int AREA_DRIVER    = 1;
    public static final int AREA_PASSENGER = 4;
    public static final int BLOW_FACE      = 0;
    public static final int BLOW_FOOT      = 1;
    public static final int BLOW_FACE_FOOT = 2;
    public static final int BLOW_FOOT_WINDOW = 3;
    public static final int SW_OFF = 0;
    public static final int SW_ON  = 1;
    public static final int POWER_OFF = 1;
    public static final int POWER_ON  = 2;

    public static HvacManager getInstance() {
        throw new RuntimeException("Stub! 真实实现在车机系统框架中，App 需以系统签名安装");
    }

    public int getHvacPowerOn()            { return 0; }
    public int getHvacAcOn()               { return 0; }
    public int getHvacAutoOn()             { return 0; }
    public int getHvacRecircOn()           { return 0; }
    public int getHvacFanSpeed()           { return 0; }
    public int getHvacFanDerection()       { return 0; }
    public int getHvacDualOn()             { return 0; }
    public int getHvacOperationMode()      { return 0; }
    public int getHvacDeforstModeStatus()  { return 0; }
    public int getHvacFrontWindowDefroster() { return 0; }
    public int getHvacRearWindowDefroster()  { return 0; }
    public int getHvacHKeyFrontDefrost()   { return 0; }
    public int getHvacHKeyRearDefrost()    { return 0; }
    public int getHvacIonizerStatus()      { return 0; }
    public float getHvacTemperatureSet(int area) { return 0.0f; }
    public float getHvacTemperatureCurrent()     { return 0.0f; }
    public boolean getServiceConnection()        { return false; }

    public int setHvacPowerOn(int v)            { return 0; }
    public int setHvacAcOn(int v)               { return 0; }
    public int setHvacAutoOn(int v)             { return 0; }
    public int setHvacRecircOn(int v)           { return 0; }
    public int setHvacFanSpeed(int v)           { return 0; }
    public int setHvacFanDerection(int v)       { return 0; }
    public int setHvacDualOn(boolean v)         { return 0; }
    public int setHvacOperationMode(int v)      { return 0; }
    public int setHvacDeforstModeStatus(int v)  { return 0; }
    public int setHvacFrontWindowDefroster(int v) { return 0; }
    public int setHvacRearWindowDefroster(int v)  { return 0; }
    public int setHvacHKeyFrontDefrost(int v)   { return 0; }
    public int setHvacHKeyRearDefrost(int v)    { return 0; }
    public int setHvacIonizerStatus(int v)      { return 0; }
    public int setHvacTemperatureSet(int area, float temp) { return 0; }
    public int setVrAcSystemPowerRequest(int v) { return 0; }

    public int registerHvacClientCallback(Object cb, String tag)   { return 0; }
    public int unRegisterHvacClientCallback(Object cb, String tag) { return 0; }
    public int registerHvacConnectStateCallback(Object cb, String tag)   { return 0; }
    public int unRegisterHvacConnectStateCallback(Object cb, String tag) { return 0; }
}
