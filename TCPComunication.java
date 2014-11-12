package com.enclaveit.mgecontroller.tcp;

import android.content.Context;

import com.enclaveit.mgecontroller.log.MGELog;

/**
 * This class is Present for all communication of controller with user side working with TCP.
 * @author hau.v.bui
 */
public class TCPComunication {

    /**
     * Tag used to print.
     */
    private String tag = "TCPComunication";
    /**
     * Context of current screen.
     */
    private Context context;

    /**
     * Constructor of TCPComunication object.
     * @param contextIn
     *            : Context from previous screen transfer in.
     */
    public TCPComunication(final Context contextIn) {
        context = contextIn;
        tcpComunication = this;
    }

    /**
     * TCP object used to communication.
     */
    public static TCPComunication tcpComunication;
    /**
     * Method used to run TCP thread and create socket.
     * @param contextIn
     *            current context
     */
    public final void callTCP(final Context contextIn) {

        MGELog.uLogI(tag, "Service TCP started.");
        netWorkTCP = new NetWorkTCP(contextIn);
        netWorkTCP.setIsStillRunning(true);
        new Thread(netWorkTCP).start();
        
    }
    
    public NetWorkTCP netWorkTCP;
    /**
     * Method call TCP network run.
     * @param contextIn : Current context application.
     */
    public final void callTCPOnHandler(final Context contextIn){
        MGELog.uLogI(tag, "Service TCP started.");
        netWorkTCP = new NetWorkTCP(contextIn);
        netWorkTCP.setIsStillRunning(true);
        new Thread(netWorkTCP).start();

    }
}