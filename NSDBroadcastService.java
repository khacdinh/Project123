package com.enclaveit.mgecontroller.tcp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.IBinder;

import com.enclaveit.mgecontroller.log.MGELog;

/**
 * This broadcast class used for transfer IP and port to user application. If user application can
 * login with controller using public key. Then we will make a handshake communication.
 * @author hau.v.bui
 */
public class NSDBroadcastService extends Service {

    /**
     * Get current NSD service name.
     * @return service name.
     */
    public final String getmServiceName() {
        return mServiceName;
    }

    /**
     * Set current NSD service name.
     * @param mServiceNameInput
     *            new service name.
     */
    public final void setmServiceName(final String mServiceNameInput) {
        this.mServiceName = mServiceNameInput;
    }
    /**
     * Current context.
     */
    private Context context;
    /**
     * Current NSD manager.
     */
    private NsdManager mNsdManager;
    /**
     * Listener of NSD registration.
     */
    private NsdManager.RegistrationListener mRegistrationListener;
    /**
     * Service name.
     */
    private String mServiceName;
    /**
     * Port on Socket being saved.
     */
    private int portSaved = 0;

    /**
     * Constructor get default NSD manager and save current context.
     * @param contextInput
     *            :Current context be saved.
     */
    public NSDBroadcastService(final Context contextInput) {
        MGELog.uLogI(tag, "initialed NsdManager");
        this.context = contextInput;
        mNsdManager = (NsdManager) contextInput.getSystemService(Context.NSD_SERVICE);
    }

    /**
     * Empty constructor.
     */
    public NSDBroadcastService() {
    }

    /**
     * Tag print info.
     */
    private String tag = "TCP NSD";
    /**
     * Service type.
     */
    public static final String SERVICE_TYPE = "_http._udp.";

    /**
     * Initial NSD information and add listener.
     */
    public final void initializeNsd() {
        initializeRegistrationListener();

    }

    /**
     * Method used to register NSD service info on local network.
     * @param port
     *            :Port chose to open.
     */
    public final void registerService(final int port) {
        portSaved = port;
        callForRegisterNSD(port);
    }

    /**
     * Re-register NSD service.
     * @param port for running service.
     */
    private void callForRegisterNSD(final int port) {
        NsdServiceInfo serviceInfo  = new NsdServiceInfo();
        serviceInfo.setPort(port);
        serviceInfo.setServiceName(MGEProtocols.MGE_NSD_NAME);
        serviceInfo.setServiceType(SERVICE_TYPE);
        mNsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
    }

    /**
     * This method used to set listener on Registration NSD service.
     */
    private void initializeRegistrationListener() {
        mRegistrationListener = new NsdManager.RegistrationListener() {

            @Override
            public void onServiceRegistered(final NsdServiceInfo nsdServiceInfo) {
                // Save the service name. Android may have changed it in order to
                // resolve a conflict, so update the name you initially requested
                // with the name Android actually used.
                setmServiceName(nsdServiceInfo.getServiceName());
                MGELog.uLogD(tag, "onServiceRegistered = " + nsdServiceInfo.getServiceName());
            }

            @Override
            public void onRegistrationFailed(final NsdServiceInfo serviceInfo, final int errorCode) {
                // Registration failed! Put debugging code here to determine why.
                MGELog.uLogD(tag, "Registration failed!");
                MGELog.uLogI(tag, "Re - register NSD service again.");
                // callForRegisterNSD(portSaved);
            }

            @Override
            public void onServiceUnregistered(final NsdServiceInfo arg0) {
                // Service has been unregistered. This only happens when you call
                // NsdManager.unregisterService() and pass in this listener.
                MGELog.uLogD(tag, "Service has been unregistered.");
            }

            @Override
            public void onUnregistrationFailed(final NsdServiceInfo serviceInfo, final int errorCode) {
                // Unregistration failed. Put debugging code here to determine why.
                MGELog.uLogD(tag, "Unregistration failed.");
            }
        };
    }


    /**
     * This timer used for re-schedule register NSD service.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        MGELog.uLogI(tag, "On Create service NSD");
    }

    @Override
    public final IBinder onBind(final Intent arg0) {
        MGELog.uLogI(tag, "On bind service NSD");
        return null;
    }

    @Override
    public final void onDestroy() {
        MGELog.uLogI(tag, "On Destroy NSD service.");
        tearDownNsd();
        stopSelf();
        super.onDestroy();
    }

    private void tearDownNsd() {
        if (mNsdManager != null) {
            MGELog.uLogI(tag, "Unregister NSD service.");
            mNsdManager.unregisterService(mRegistrationListener);
        }
    }

}
