package com.example.myapplication;

public class Serialport {
    static {
        System.loadLibrary("serial_port");// 加载 native 库
    }

    public native int openPort(String portName, int buadRate);
    public native int closePort(int fd);
}
