package com.enclaveit.mgecontroller.tcp;

import com.enclaveit.mgecontroller.log.MGELog;
import com.enclaveit.mgecontroller.parse.CloudDatabaseHelper;
import com.parse.ParseUser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.text.format.Formatter;

/**
 * This broadcast use to receive event when WIFI is connected. Then update host and port to cloud if
 * it have cloud account.
 * @author hau.v.bui
 */
public class BroadcastDetectWifiSate extends BroadcastReceiver {
    /**
     * Tag.
     */
    private final String tag = "BroadcastDetectWifiSate TCP";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        String action = intent.getAction();
        if (action.equalsIgnoreCase(ConnectivityManager.CONNECTIVITY_ACTION)) {
            if (intent.getExtras() != null) {
                NetworkInfo ni = (NetworkInfo) intent.getExtras().get(
                        ConnectivityManager.EXTRA_NETWORK_INFO);
                if (ni != null && ni.getState() == NetworkInfo.State.CONNECTED) {
                    ConnectivityManager connectivityManager = (ConnectivityManager) context
                            .getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo = connectivityManager
                            .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                    if (networkInfo.isConnectedOrConnecting()) {
                        if (networkInfo.isConnected()) {
                            WifiManager wim = (WifiManager) context
                                    .getSystemService(Context.WIFI_SERVICE);
                            @SuppressWarnings("deprecation")
                            String currentIpAddress = Formatter.formatIpAddress(wim
                                    .getConnectionInfo().getIpAddress());
                            MGELog.uLogI(tag, "Server IP onReceive = " + currentIpAddress);
                            saveToReferenceAndUpToCloud(context);
                        }
                    }
                }
            }
        }
    }

    /**
     * This method used to save host and port in to ShareReferences and upload it to cloud.
     * @param context Current context.
     */
    public final void saveToReferenceAndUpToCloud(final Context context) {
        if (NetWorkTCP.getServerSocket() != null) {
        WifiManager wim = (WifiManager) context
                .getSystemService(android.content.Context.WIFI_SERVICE);
        // String currentIpAddress =
        // Formatter.formatIpAddress(wim.getConnectionInfo().getIpAddress());
        int ipAddress = wim.getConnectionInfo().getIpAddress();
        MGELog.uLogI(tag, "Server IP in int = " + ipAddress);
        String currentIpAddress = String.format("%d.%d.%d.%d", (ipAddress & 0xff),
                (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));

        MGELog.uLogI(tag, "Port chose = " + NetWorkTCP.getServerSocket().getLocalPort());
        MGELog.uLogI(tag, "Server IP = " + currentIpAddress);
        if (ParseUser.getCurrentUser() != null && ParseUser.getCurrentUser().getUsername() != null) {
            CloudDatabaseHelper.createOrUpdatePOTCP(currentIpAddress,
                    String.valueOf(NetWorkTCP.getServerSocket().getLocalPort()));
            saveHostPortToShareReference(context, currentIpAddress
                    + MGEProtocols.MGE_COMPONENT_DELIMITER
                    + NetWorkTCP.getServerSocket().getLocalPort());
        } else {
            MGELog.uLogI(tag, "User = " + ParseUser.getCurrentUser());
        }
        } else {
            MGELog.uLogI(tag, "Server socket is null.");
        }
    }

    /**
     * This method used to save host and port to ShareReferences file.
     * @param hostandport
     *            : host and port open from controller.
     * @param context
     *            Current context.
     */
    private void saveHostPortToShareReference(final Context context, final String hostandport) {
        String keyHostPortInfo = "hostAndPort";
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(context);
        Editor editor = sharedPreferences.edit();
        editor.putString(keyHostPortInfo, hostandport);
        editor.commit();
    }

}
