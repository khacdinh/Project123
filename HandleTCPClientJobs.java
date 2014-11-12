package com.enclaveit.mgecontroller.tcp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.net.ssl.SSLSocket;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

import com.enclaveit.mgecontroller.activities.ConsumerDashboard;
import com.enclaveit.mgecontroller.activities.ConsumerSystemMapDetail;
import com.enclaveit.mgecontroller.activities.ListbaseConsumerRoomDetail;
import com.enclaveit.mgecontroller.dataaccess.DADeviceMap;
import com.enclaveit.mgecontroller.dataaccess.DADeviceScene;
import com.enclaveit.mgecontroller.dataaccess.DADevices;
import com.enclaveit.mgecontroller.dataaccess.DAPowerUsage;
import com.enclaveit.mgecontroller.dataaccess.DAScene;
import com.enclaveit.mgecontroller.dataaccess.DASettings;
import com.enclaveit.mgecontroller.dataaccess.DAToggle;
import com.enclaveit.mgecontroller.database.MGEControllerDbHelper;
import com.enclaveit.mgecontroller.database.entities.DeviceScene;
import com.enclaveit.mgecontroller.database.entities.DeviceType;
import com.enclaveit.mgecontroller.database.entities.Devices;
import com.enclaveit.mgecontroller.database.entities.Scene;
import com.enclaveit.mgecontroller.database.entities.Settings;
import com.enclaveit.mgecontroller.database.entities.Toggle;
import com.enclaveit.mgecontroller.library.MGEPLibrary;
import com.enclaveit.mgecontroller.log.MGELog;
import com.enclaveit.mgecontroller.parse.CloudDatabaseHelper;
import com.enclaveit.mgecontroller.parse.Command;
import com.enclaveit.mgecontroller.parse.CustomAnalytics;
import com.enclaveit.mgecontroller.utils.Constants;
import com.enclaveit.mgecontroller.utils.Files;
import com.enclaveit.mgecontroller.utils.GlobalVariables;
import com.enclaveit.mgecontroller.utils.MapDetailUtils;
import com.mygreenedge.mgelibrary.gepCommon.eChannel;

/**
 * This class used for handle all event when server receive message from EdgeHome.
 * @author hau.v.bui
 */
public class HandleTCPClientJobs implements Runnable {
    /**
     * Current context.
     */
    private static Context contextApp;

    /**
     * Set running activity for receiver.
     * @param context
     *            context of activity
     */
    public static void setActivity(final Context context) {
        contextApp = context;
    }
    /**
     * Variable using for check is client sign out or not.
     */
    private boolean isSignOut = false;
    /**
     * Message received from client.
     */
    private String receivedMessage;
    /**
     * Tool used for read line data on socket.
     */
    private BufferedReader in;
    /**
     * Tool for write line data on socket.
     */
    private PrintWriter out;
    /**
     * Current socket opened.
     */
    private SSLSocket sslSocket;
    /**
     * Current context.
     */
    private Context context;

    private int oderCameToServer = 0;

    private Hashtable<Integer, ServerToConnection> usersList = new Hashtable<Integer, ServerToConnection>();

    private String tag = "HanddleTCPClientJobs TCP";
    /**
     * Directory of the folder in SDCard.
     */
    public static final String APP_DIR_PATH_USER = "EDGEHomeMobile";

    /**
     * Class Command is used to send command to user device.
     */
    public Command commandConfirmation = new Command();
    /**
     * Class CustomAnalytics use for track CON activity then upload to cloud.
     */
    private CustomAnalytics customAnalytics = new CustomAnalytics();
    /**
     * Constructor used for save current socket and context. Also get tool to put data on socket.
     * @param clientSocket
     *            :Current socket available.
     * @param contextCurrent
     *            : Current context.
     * @param message
     *            :Message got from EM.
     * @param outPrint
     *            :Tool used to put data on socket.
     * @throws IOException
     *             : Throws {@link IOException} if initial tool failed.
     */
    public HandleTCPClientJobs(final SSLSocket clientSocket, final Context contextCurrent,
        final String message, final PrintWriter outPrint, BufferedReader bufferedReader,
        final int orderCameIn) throws IOException {
        this.sslSocket = clientSocket;
        this.context = contextCurrent;
        receivedMessage = message;
        out = outPrint;
        in = bufferedReader;
        oderCameToServer = orderCameIn;
        // out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
        // clientSocket.getOutputStream())), true);
        // outputStream = new DataOutputStream(sslSocket.getOutputStream());
        // inputStream = new DataInputStream(sslSocket.getInputStream());
        // bufferedOutputStream = new BufferedOutputStream(outputStream);
    }

    public HandleTCPClientJobs(Context contextInput) {
        this.context = contextInput;
    }

    @Override
    public final void run() {
        try {
            handle();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method used for handle all events some to CON.
     * @throws IOException
     *             If error happen when sending or receiving data failed.
     */
    private void handle() throws IOException {
        MGELog.uLogE(tag, receivedMessage);
     // Database is not available
        if (GlobalVariables.mgeMode == GlobalVariables.INSTALLATION_MODE) {
            // Send command back to EHM.
            sendMessageToAllUserUsingTCP(MGEProtocols.MGE_DEACTIVE_ACTION);
        } else {
            MGEControllerDbHelper helper = MGEControllerDbHelper.getInstance(context);
            SQLiteDatabase db = helper.getWritableDatabase();
            if (receivedMessage.contains(MGEProtocols.MGE_GET_IMAGES)) {

                receivedMessage = receivedMessage.replace(MGEProtocols.MGE_GET_IMAGES
                    + MGEProtocols.MGE_COMPONENT_DELIMITER, "");
                MGELog.uLogI(tag, "DB version of user side = " + receivedMessage);
                String informationsFromUserSide = "";

                prepareFilesToSend(informationsFromUserSide, receivedMessage);

                File fileDestination = new File(Environment.getExternalStorageDirectory()
                    .getAbsolutePath() + Constants.SLASH + APP_DIR_PATH_USER + ".zip");

                long fileSize = fileDestination.length();
                // Replace space to splash of brand image name if any
                String brandImageName = GlobalVariables.getBrandImage();
                if (brandImageName.contains(Constants.SPACE)) {
                    brandImageName = brandImageName.replaceAll(Constants.SPACE, Constants.SLASH);
                }
                // Send message for zip file and zip file size
                String message = Constants.EMPTY_STRING;
                if (brandImageName == null || brandImageName.isEmpty()
                    || brandImageName.equals(Constants.EMPTY_STRING)) {
                    message = MGEProtocols.MGE_GET_IMAGES + MGEProtocols.MGE_COMPONENT_DELIMITER
                        + APP_DIR_PATH_USER + ".zip" + MGEProtocols.MGE_COMPONENT_DELIMITER
                        + fileSize;
                } else {
                    // Send message for zip file, zip file size, brand image and url
                    message = MGEProtocols.MGE_GET_IMAGES + MGEProtocols.MGE_COMPONENT_DELIMITER
                        + APP_DIR_PATH_USER + ".zip" + MGEProtocols.MGE_COMPONENT_DELIMITER
                        + fileSize + MGEProtocols.MGE_COMPONENT_DELIMITER + brandImageName
                        + MGEProtocols.MGE_COMPONENT_DELIMITER + GlobalVariables.getBrandURL();
                }
                sendMessage(message);
                pushFileToSocket(fileDestination.getAbsolutePath());
                // Delete Zip file if it needed.
                File zipFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                    + Constants.SLASH + APP_DIR_PATH_USER + ".zip");
                if (zipFile.exists()) {
                    zipFile.delete();
                }
            } else if (receivedMessage.contains(MGEProtocols.MGE_REQUEST_TURN_ON_OFF_LIGHT)) {
                MGELog.uLogD(tag, "(1) Got message from Sever ON/OFF = " + receivedMessage);
                // Sent to others phones device inside WIFI network.
                sendUpdateStateToOthersDeviceInsideWifiNet(receivedMessage);
                // Update state of Controller itself.
                receivedMessage = receivedMessage.replace(
                    MGEProtocols.MGE_REQUEST_TURN_ON_OFF_LIGHT
                        + MGEProtocols.MGE_COMPONENT_DELIMITER, "");
                // update device first.
                String[] result = receivedMessage.split(MGEProtocols.MGE_COMPONENT_DELIMITER);
                String idOfDevice = result[0];
                String powerState = result[1];
                String brightness = result[2];
                DADevices daDevices = new DADevices();
                Devices deviceInput = daDevices.getByID(Long.parseLong(idOfDevice), db);

                long deviceTypeId = deviceInput.getDeviceType().getId();
                // Track on/off device.
                customAnalytics.trackOnOffDevice(deviceTypeId);
                // Insert toggle
                if (deviceTypeId == DeviceType.DEVICE_TYPE_SWITCH
                    || deviceTypeId == DeviceType.DEVICE_TYPE_GANG_SUB_SWITCH
                    || deviceTypeId == DeviceType.DEVICE_TYPE_WALL_CONTROL_SUB_SWITCH
                    || deviceTypeId == DeviceType.DEVICE_TYPE_DOUBLE_SUB_SWITCH) {
                    Toggle toggle = new Toggle(deviceInput.getId(), Calendar.getInstance()
                        .getTimeInMillis());
                    new DAToggle().add(toggle, context);
                }
                deviceInput.setPowerState(Integer.parseInt(powerState));
                if (deviceInput.getDeviceType().getId() != DeviceType.DEVICE_TYPE_DOOR_LOCK) {
                    deviceInput.setBrightness(Integer.parseInt(brightness));
                    // daDevices.updateBrightness(ConsumerSystemMapDetail.timerContext,
                    // Long.parseLong(idOfDevice), Integer.parseInt(brightness));
                }
                if (!deviceInput.isReadOnly()) {
                    // daDevices.updatePowerState(ConsumerSystemMapDetail.timerContext,
                    // Long.parseLong(idOfDevice), Integer.parseInt(powerState));
                    daDevices.update(ConsumerSystemMapDetail.timerContext, deviceInput);
                    // boolean isEco = false;
                    // if (deviceInput.getTimer() > 0) {
                    // isEco = true;
                    // }
                    // Map base
                    // doActionUpdateDevicePowerSate(deviceInput);
                    // List base
                    doActionUpdateDevicePowerSateListBase(deviceInput, contextApp);
                    // Used for update state with timer.
                    if (Integer.parseInt(powerState) == Devices.POWERSTATE_ON
                        && deviceInput.getTimer() > 0) {
                        ListbaseConsumerRoomDetail.addEcoTimerDevice(deviceInput,
                            deviceInput.getTimer());
                    }
                } else {
                    sendMessageToAllUserUsingTCP(MGEProtocols.MGE_REQUEST_TURN_ON_OFF_LIGHT
                        + MGEProtocols.MGE_COMPONENT_DELIMITER + deviceInput.getId()
                        + MGEProtocols.MGE_COMPONENT_DELIMITER + Devices.POWERSTATE_ON
                        + MGEProtocols.MGE_COMPONENT_DELIMITER + deviceInput.getBrightness());
                    MGELog.uLogD(tag, "Send back power state to EMH." + Devices.POWERSTATE_ON);
                }
                // Send command to update device are login by cloud.
                String templateValuePhoneIDForController = "-1";
                commandConfirmation.sendCommandForChageDeviceStateToCloud(idOfDevice,
                    deviceInput.getPowerState(), deviceInput.getBrightness(),
                    templateValuePhoneIDForController, Constants.COMMAND_MODE_ON_OFF);
                // out.println(MGEProtocols.MGE_RESULT_REQUEST);
                // sendMessage(MGEProtocols.MGE_RESULT_REQUEST);

            } else if (receivedMessage.contains(MGEProtocols.MGE_REQUEST_DIM_THE_LIGHT)) {
                MGELog.uLogD(tag, "(1) Got message from Sever DIM = " + receivedMessage);
                // Sent to others phones device inside WIFI network.
                sendUpdateStateToOthersDeviceInsideWifiNet(receivedMessage);

                // Update controller itself.
                receivedMessage = receivedMessage.replace(MGEProtocols.MGE_REQUEST_DIM_THE_LIGHT
                    + MGEProtocols.MGE_COMPONENT_DELIMITER, "");
                // update device first.
                String[] result = receivedMessage.split(MGEProtocols.MGE_COMPONENT_DELIMITER);
                String idOfDevice = result[0];
                String powerState = result[1];
                String brightness = result[2];
                DADevices daDevices = new DADevices();
                Devices deviceInput = daDevices.getFullByID(Long.parseLong(idOfDevice), db);
                if (!deviceInput.isReadOnly()) {
                    deviceInput.setPowerState(Integer.parseInt(powerState));
                    deviceInput.setBrightness(Integer.parseInt(brightness));
                    daDevices.update(ConsumerSystemMapDetail.timerContext, deviceInput);
                    // boolean isEco = false;
                    // if (deviceInput.getTimer() > 0) {
                    // isEco = true;
                    // }
                    // use for Map view
                    // doActionUpdateDimLevelSate(deviceInput);
                    // use for list view.
                    doActionUpdateDimLevelListBase(deviceInput, contextApp);
                    if(deviceInput.getDeviceType().getId() == DeviceType.DEVICE_TYPE_LIGHT) {
                        // Track dim of device.
                        customAnalytics.trackDimOfLight(deviceInput.getSerial(), deviceInput.getRoom().getName());
                    }
                    // Send command to update device are login by cloud.
                    String templateValuePhoneIDForController = "-1";
                    commandConfirmation.sendCommandForChageDeviceStateToCloud(idOfDevice,
                        deviceInput.getPowerState(), deviceInput.getBrightness(),
                        templateValuePhoneIDForController, Constants.COMMAND_MODE_DIMM);
                    // out.println(MGEProtocols.MGE_RESULT_REQUEST);
                }
            } else if (receivedMessage.contains(MGEProtocols.MGE_REQUEST_APPLY_SCENE)) {
                MGELog.uLogD(tag, "(1) Got message from Sever Scene = " + receivedMessage);
                // Send update to others device phone inside WIFI network.
                sendUpdateStateToOthersDeviceInsideWifiNet(receivedMessage);
                // Update controller itself.

                receivedMessage = receivedMessage.replace(MGEProtocols.MGE_REQUEST_APPLY_SCENE
                    + MGEProtocols.MGE_COMPONENT_DELIMITER, "");
                String[] result = receivedMessage.split(MGEProtocols.MGE_COMPONENT_DELIMITER);
                String scenesId = result[0];
                if (!scenesId.isEmpty()) {
                    ArrayList<DeviceScene> lstDeviceScene = new ArrayList<DeviceScene>();
                    DADeviceScene dADeviceScene = new DADeviceScene();
                    DADevices dADevices = new DADevices();
                    lstDeviceScene = dADeviceScene.getBySceneId(context, Long.parseLong(scenesId));
                    updateDevice(context, Long.parseLong(scenesId));
                    if (contextApp != null && contextApp instanceof ListbaseConsumerRoomDetail) {
                        ((ListbaseConsumerRoomDetail) contextApp).reloadPopupUIScence();
                    }
                    if (contextApp instanceof ConsumerDashboard) {
                        if (((ConsumerDashboard) contextApp).hasWindowFocus()) {
                            ((ConsumerDashboard) contextApp).refreshFMSystemMap();
                        }
                    }
                    // Track times activated scene and upload to cloud.
                    Scene scene = new DAScene().getByID(contextApp, Long.parseLong(scenesId));
                    customAnalytics.trackSceneActivated(scene.getName());
                    // Send command to update device inside are login by cloud.
                    String templatePhoneIdUseForController = "-1";
                    commandConfirmation.sendCommandForApplyScenesToCloud(scenesId,
                        templatePhoneIdUseForController);
                }
            } else if (receivedMessage.contains(MGEProtocols.MGE_GET_POWER_CONSUMPTION)) {
                MGELog.uLogD(tag, "(1) Got message from Sever POWER_CONSUMPTION= "
                    + receivedMessage);
                receivedMessage = receivedMessage.replace(MGEProtocols.MGE_GET_POWER_CONSUMPTION
                    + MGEProtocols.MGE_COMPONENT_DELIMITER, "");
                String deviceID = receivedMessage;
//                getPowerConsumptionFromReal(context, deviceID);
                getPowerConsumption(context, deviceID);
            } else if (receivedMessage.contains(MGEProtocols.MGE_GET_POWER_BRIGHTNESS)) {
                MGELog
                    .uLogD(tag, "(1) Got message from Sever POWER_BRIGHTNESS= " + receivedMessage);
                receivedMessage = receivedMessage.replace(MGEProtocols.MGE_GET_POWER_BRIGHTNESS
                    + MGEProtocols.MGE_COMPONENT_DELIMITER, "");
                String deviceID = receivedMessage;
                getPowerConsumptionAndBrightnessFromReal(context, deviceID);
            } else if (receivedMessage.contains(MGEProtocols.MGE_GET_DB_VERSION)) {
                // Get db version and send it back.
                Settings setting = new DASettings().getByName(context, Constants.DB_VERSION);
                int dbVersion = 0;
                if ((!setting.getValue().equals(Constants.EMPTY_STRING) && (setting.getValue() != null))) {
                    dbVersion = Integer.parseInt(setting.getValue());
                    sendMessage(MGEProtocols.MGE_GET_DB_VERSION
                        + MGEProtocols.MGE_COMPONENT_DELIMITER + dbVersion);
                }
            } else if (receivedMessage.contains(MGEProtocols.MGE_REQUEST_SET_BRIGHTNESS)) {
                String[] result = receivedMessage.split(MGEProtocols.MGE_COMPONENT_DELIMITER);
                String serial = result[1];
                int channel = Integer.parseInt(result[2]);
                int brightness = Integer.parseInt(result[Constants.NUMBER_THREE]);
                // Command type.
                int flagCT = Integer.parseInt(result[Constants.NUMBER_FOUR]);
             // Update DB and reloadUI.
                MapDetailUtils.handleCommandSetBrightnessFromEDGEhome(contextApp,
                        serial, channel, brightness, flagCT);
            } else {
                MGELog.uLogI(tag, "Unknow message = " + receivedMessage);
                MGELog.uLogI(tag, "Attacker IP = " + sslSocket.getInetAddress().toString());
            }
        }
    }
    //Map base doActionUpdateDimLevel
//    private void doActionUpdateDimLevel(final Devices deviceInput, Context context) {
//        if (ConsumerSystemMapDetail.timerContext != null
//            && ConsumerSystemMapDetail.timerContext instanceof ConsumerSystemMapDetail) {
//            ((ConsumerSystemMapDetail) ConsumerSystemMapDetail.timerContext).reloadPopupUI(
//                deviceInput, true, true, false);
//        } else if (ConsumerSystemMapDetail.timerContext != null
//            && ConsumerSystemMapDetail.timerContext instanceof ConsumerDashboard) {
//            MapDetailUtils.updateAssociatedDatabase(deviceInput, true, true,
//                ConsumerSystemMapDetail.timerContext, null, true, false);
//            ((ConsumerDashboard) (ConsumerSystemMapDetail.timerContext)).refreshFMSystemMap();
//        } else {
//            MapDetailUtils.updateAssociatedDatabase(deviceInput, true, true,
//                ConsumerSystemMapDetail.timerContext, null, true, false);
//        }
//    }
    //List base doActionUpdateDimLevelListbase
    /**
     * Update dim level in list base room detail
     * @param deviceInput device need to update.
     * @param context current context app.
     */
    private void doActionUpdateDimLevelListBase(final Devices deviceInput, Context context) {
        if (context != null
            && context instanceof ListbaseConsumerRoomDetail) {
            ((ListbaseConsumerRoomDetail) context).reloadUI(deviceInput);
        } else if (context != null
            && context instanceof ConsumerDashboard) {
            MapDetailUtils.updateAssociatedDatabase(deviceInput, true, true,
                context, null, true, false, false);
            ((ConsumerDashboard) (context)).refreshFMSystemMap();
        } else {
            MapDetailUtils.updateAssociatedDatabase(deviceInput, true, true,
                ConsumerSystemMapDetail.timerContext, null, true, false, false);
        }
    }
    
    //Map base doActionUpdateDevicePowerSate
//    private void doActionUpdateDevicePowerSate(Devices deviceInput){
//        if (ConsumerSystemMapDetail.timerContext != null
//            && ConsumerSystemMapDetail.timerContext instanceof ConsumerSystemMapDetail) {
//            ((ConsumerSystemMapDetail) ConsumerSystemMapDetail.timerContext).reloadPopupUI(
//                deviceInput, true, false, false);
//            // Update toggle number
//            if (((ConsumerSystemMapDetail)
//                    (ConsumerSystemMapDetail.timerContext)).getPopupHistorical() != null) {
//                ((ConsumerSystemMapDetail)
//                        (ConsumerSystemMapDetail.timerContext))
//                        .getPopupHistorical().updateToggle();
//            }
//        } else if (ConsumerSystemMapDetail.timerContext != null
//            && ConsumerSystemMapDetail.timerContext instanceof ConsumerDashboard) {
//            MapDetailUtils.updateAssociatedDatabase(deviceInput, false, true,
//                ConsumerSystemMapDetail.timerContext, null, true, false);
//            ((ConsumerDashboard) (ConsumerSystemMapDetail.timerContext))
//                .refreshFMSystemMap();
//        } else {
//            MapDetailUtils.updateAssociatedDatabase(deviceInput, false, true,
//                ConsumerSystemMapDetail.timerContext, null, true, false);
//        }
//    }
    //List base doActionUpdateDevicePowerSateListBase
    /**
     * Update powerstate in list base room detail
     * @param deviceInput device need to update.
     * @param context current context app.
     */
    private void doActionUpdateDevicePowerSateListBase(final Devices deviceInput, final Context context){
        if (context != null && context instanceof ListbaseConsumerRoomDetail) {
            ((ListbaseConsumerRoomDetail) context).reloadUI(deviceInput);
            // Update toggle number
            ((ListbaseConsumerRoomDetail) context).reloadToggled(deviceInput.getId());
        } else if (context != null && context instanceof ConsumerDashboard) {
            MapDetailUtils.updateAssociatedDatabase(deviceInput, false, true, context, null, true,
                false, false);
            ((ConsumerDashboard) (context)).refreshFMSystemMap();
        } else {
            MapDetailUtils.updateAssociatedDatabase(deviceInput, false, true,
                ConsumerSystemMapDetail.timerContext, null, true, false, false);
        }
    }
    /**
     * This method used to get power consumption of real device and send to update to current user.
     * @param contextInput
     *            Context of current screen.
     * @param deviceIDIn
     *            Id of device.
     */
    private void getPowerConsumptionFromReal(final Context contextInput, final String deviceIDIn) {
        // Write the code to get power state and brightness from real bellow here.
        MGEControllerDbHelper helper = MGEControllerDbHelper.getInstance(contextInput);
        SQLiteDatabase db = helper.getReadableDatabase();
        DADevices daDevices = new DADevices();
        Devices device = daDevices.getByID(Long.valueOf(deviceIDIn).longValue(),
            db);

        String deviceSerial = daDevices.getDeviceSerial(device.getId(), db).getSerial();
        eChannel channel = eChannel.fromInteger(device.getChannel());

        MGEPLibrary.getInstance(context).getPowerByChannel(deviceSerial, channel);
    }

    /**
     * This method is used to get power consumption of device and send back to EHM.
     * @param contextInput
     *            application context.
     * @param deviceIDIn
     *            device ID.
     */
    private void getPowerConsumption(final Context contextInput, final String deviceIDIn) {
        MGEControllerDbHelper helper = MGEControllerDbHelper.getInstance(contextInput);
        SQLiteDatabase db = helper.getReadableDatabase();
        double watts = 0;
        String serial = Constants.EMPTY_STRING;
        Calendar cal = Calendar.getInstance();
        DADevices daDevices = new DADevices();
        DAPowerUsage daPowerUsage = new DAPowerUsage();
        Devices device = daDevices.getByID(Long.valueOf(deviceIDIn).longValue(), db);
        long deviceType = device.getDeviceType().getId();
        if (deviceType == DeviceType.DEVICE_TYPE_SWITCH
            || deviceType == DeviceType.DEVICE_TYPE_GANG_SUB_SWITCH
            || deviceType == DeviceType.DEVICE_TYPE_DOUBLE_SUB_SWITCH
            || deviceType == DeviceType.DEVICE_TYPE_WALL_CONTROL_SUB_SWITCH) {
            watts = daPowerUsage.getLastPowerUsageSwitch(context, device.getId());
        } else {
            if (deviceType == DeviceType.DEVICE_TYPE_OUTLET_PLUG
                || deviceType == DeviceType.DEVICE_TYPE_WALL_CONTROL_SUB_JUNCTION_BOX) {
                serial = daDevices.getParent(device, db).getSerial();
            } else {
                serial = device.getSerial();
            }
            watts = daPowerUsage.getLastPowerUsage(serial, device.getChannel(), context);
        }
        sendMessageToAllUserUsingTCP(MGEProtocols.MGE_GET_POWER_CONSUMPTION
            + MGEProtocols.MGE_COMPONENT_DELIMITER + device.getId()
            + MGEProtocols.MGE_COMPONENT_DELIMITER + String.valueOf(watts)
            + MGEProtocols.MGE_COMPONENT_DELIMITER + String.valueOf(cal.getTimeInMillis()));
    }

    /**
     * This method used to get power consumption and brightness of real device and send to update to current user.
     * @param contextInput
     *            Context of current screen.
     * @param deviceID
     *            Id of device.
     */
    private void getPowerConsumptionAndBrightnessFromReal(final Context contextInput, final String deviceID) {
        // Write the code to get power state and brightness from real bellow here.
        Devices device = new DADevices().getDeviceSerial(Long.valueOf(deviceID).longValue(),
            MGEControllerDbHelper.getInstance(contextInput).getReadableDatabase());
        MGEPLibrary.getInstance(contextInput).getBrightness(device.getSerial(),
            eChannel.fromInteger(device.getChannel()));
        MGEPLibrary.getInstance(context).getPowerByChannel(device.getSerial(),
            eChannel.fromInteger(device.getChannel()));
    }

    // /**
    // * This method used to get the power consumption by device ID get from User device then send
    // the
    // * power consumption back.
    // * @param context
    // * : the context.
    // * @param userDeviceID
    // * : device id.
    // */
    // private void getPowerConsumptionAndSendBack(final Context context, final String userDeviceID)
    // {
    // MGEControllerDbHelper helper = MGEControllerDbHelper.getInstance(context);
    // SQLiteDatabase db = helper.getWritableDatabase();
    // DADevices daDevices = new DADevices();
    // Devices devices = daDevices.getByID(Long.parseLong(userDeviceID), db);
    // String deviceId = String.valueOf(devices.getId());
    // int chanel = devices.getChannel();
    // String serial = devices.getSerial();
    // long deviceType = devices.getDeviceType().getId();
    // if (deviceType == DeviceType.DEVICE_TYPE_OUTLET_PLUG) {
    // serial = devices.getSerial(context);
    // }
    // DAPowerUsage powerUsage = new DAPowerUsage();
    // PowerUsage pu = powerUsage.getLatestBySerial2(serial, chanel, db);
    //
    // sendMessage(MGEProtocols.MGE_GET_POWER_CONSUMPTION + MGEProtocols.MGE_COMPONENT_DELIMITER
    // + deviceId + MGEProtocols.MGE_COMPONENT_DELIMITER + String.valueOf(pu.getDraw())
    // + MGEProtocols.MGE_COMPONENT_DELIMITER + String.valueOf(pu.getTime()));
    // }

    /**
     * Method used to send message to others phone device inside WIFI network to update state.
     */
    private void sendUpdateStateToOthersDeviceInsideWifiNet(final String messageBeSendIn) {
        String messageBeSend = messageBeSendIn;
        String messageBeSendForConfirmCommand = messageBeSendIn;
        usersList = NetWorkTCP.getListAllTcpUser();
        if (usersList.size() >= 1) {
            MGELog.uLogI(tag, "Size of current user: " + usersList.size());
            Enumeration enumeration = usersList.keys();
            while (enumeration.hasMoreElements()) {
                int key = (Integer) enumeration.nextElement();
                if (key != oderCameToServer) {
                    // Send to others.
                    usersList.get(key).sendMessageOnNetWork(messageBeSend);
                } else {
                    // Send cofirm to itself.
                    if (messageBeSend.contains(MGEProtocols.MGE_REQUEST_TURN_ON_OFF_LIGHT)) {
                        usersList.get(key).sendMessageOnNetWork(
                            messageBeSendForConfirmCommand.replace(
                                MGEProtocols.MGE_REQUEST_TURN_ON_OFF_LIGHT,
                                MGEProtocols.ACTION_TURN_ON_OFF_DONE));
                    } else if (messageBeSend.contains(MGEProtocols.MGE_REQUEST_DIM_THE_LIGHT)) {
                        usersList.get(key).sendMessageOnNetWork(
                            messageBeSendForConfirmCommand.replace(
                                MGEProtocols.MGE_REQUEST_DIM_THE_LIGHT,
                                MGEProtocols.ACTION_DIM_DONE));
                    } else if (messageBeSend.contains(MGEProtocols.MGE_REQUEST_APPLY_SCENE)) {
                        usersList.get(key).sendMessageOnNetWork(
                            messageBeSendForConfirmCommand.replace(
                                MGEProtocols.MGE_REQUEST_APPLY_SCENE,
                                MGEProtocols.ACTION_APPLY_SCENCE_DONE));
                    }
                }
            }
        }
    }

    /**
     * Sends the message to client
     * @param message
     *            text entered by client
     */
    public void sendMessage(String message) {
        if (out != null && !out.checkError()) {
            out.println(message);
            out.flush();
        }
    }

    /**
     * @param sceneId
     *            the sceneId
     * @param context
     *            : Current context.
     * @author khuyen.nguyen
     */
    private void updateDevice(final Context context, final long sceneId) {
        MGEControllerDbHelper helper = MGEControllerDbHelper.getInstance(context);
        SQLiteDatabase db = helper.getWritableDatabase();
        ArrayList<DeviceScene> lstDeviceScene = new ArrayList<DeviceScene>();
        DADeviceScene dADeviceScene = new DADeviceScene();
        DADevices dADevices = new DADevices();
        DADeviceMap dADeviceMap = new DADeviceMap();
        Devices indexDevice;
        Devices devicesParent = new Devices();
        lstDeviceScene = dADeviceScene.getBySceneId(context, sceneId);
        // Get all value from DB and update to real devices
        for (int i = 0; i < lstDeviceScene.size(); i++) {
            long deviceId = lstDeviceScene.get(i).getDeviceId();
            Devices myDevice = dADevices.getByID(deviceId, db);
            if (!myDevice.isReadOnly()) {
                int brightnessIn = lstDeviceScene.get(i).getBrightness();
                int powerStateIn = lstDeviceScene.get(i).getPowerState();

                myDevice.setBrightness(brightnessIn);
                myDevice.setPowerState(powerStateIn);

                long deviceType = myDevice.getDeviceType().getId();
                eChannel channel = eChannel.fromInteger(myDevice.getChannel());
                if (deviceType == DeviceType.DEVICE_TYPE_LIGHT) {
                    // Update association
                    dADevices.update(context, myDevice);
                    // Update back to real devices
                    String serialNumber = myDevice.getSerial();
                    int level = myDevice.getBrightness();
                    if (myDevice.getPowerState() == Devices.POWERSTATE_OFF) {
                        level = 0;
                    }
                    MGEPLibrary.getInstance(context).setBrightnessAndDonNotCareResponse(serialNumber, channel, level); // CHANNEL_1
                                                                                                  // for
                                                                                                  // the
                                                                                                  // lights
                } else if (deviceType == DeviceType.DEVICE_TYPE_OUTLET
                    || deviceType == DeviceType.DEVICE_TYPE_GANG_SUB_OUTLET) {
                    // Update itself
                    dADevices.update(context, myDevice);
                } else if (deviceType == DeviceType.DEVICE_TYPE_OUTLET_PLUG) {
                    // Update itself
                    dADevices.update(context, myDevice);
                    // Update back to real devices
                    String serialNumber = myDevice.getSerial();
                    int level = myDevice.getBrightness();
                    if (myDevice.getPowerState() == Devices.POWERSTATE_OFF) {
                        level = 0;
                    }
                    ArrayList<Devices> lstOrderColumn = dADeviceMap.getParentByChildID(
                        myDevice.getId(), db);
                    int orderColumn = 0;
                    long idParent = 0;
                    for (int t = 0; t < lstOrderColumn.size(); t++) {
                        indexDevice = lstOrderColumn.get(t);
                        if (indexDevice.getDeviceType().getId() == DeviceType.DEVICE_TYPE_OUTLET
                            || indexDevice.getDeviceType().getId() == DeviceType.DEVICE_TYPE_GANG_SUB_OUTLET) {
                            orderColumn = lstOrderColumn.get(t).getOrder();
                            // Get ID of parent
                            idParent = indexDevice.getId();
                        }
                    }
                    devicesParent = dADevices.getByID(idParent, db);
                    // Get serial number of parent
                    String serialNumberParent = devicesParent.getSerial();
                    serialNumber = serialNumberParent;
                    MGEPLibrary.getInstance(context).setBrightnessAndDonNotCareResponse(serialNumber, channel, level);
                } else if (deviceType == DeviceType.DEVICE_TYPE_DOOR_LOCK) {
                    // Update itself
                    dADevices.update(context, myDevice);
                    if (myDevice.getPowerState() > Devices.POWERSTATE_OFF) {
                        MGEPLibrary.getInstance(context).setBrightnessAndDonNotCareResponse(myDevice.getSerial(),
                            channel, Devices.BRIGHTNESS_HIGHEST);
                    } else {
                        MGEPLibrary.getInstance(context).setBrightnessAndDonNotCareResponse(myDevice.getSerial(),
                            channel, Devices.BRIGHTNESS_LOWEST);
                    }
                }
            }
        }
    }

    /**
     * Method used to put file data on socket.
     * @param pathIndio
     *            :Path guide to the file.
     * @throws IOException
     *             : Failed if push file on socket failed.
     */
    private void pushFileToSocket(final String pathIndio) throws IOException {
        File myFile = new File(pathIndio);
        FileInputStream fis = new FileInputStream(myFile);
        OutputStream os = sslSocket.getOutputStream();
        int filesize = (int) myFile.length();
        byte[] buffer = new byte[filesize];
        int bytesRead = 0;
        while ((bytesRead = fis.read(buffer)) > 0) {

            os.write(buffer, 0, bytesRead);
            // Log display exact the file size
            MGELog.uLogI(tag, "Total file read:" + bytesRead);
        }
        os.flush();
        fis.close();
        MGELog.uLogD(tag, "Server sent file.");
    }

    /**
     * Used to export DB to SDcard to send to user side.
     * @param contextIn
     *            Current context.
     */
    public final void getAllFilesReady(final Context contextIn,
        final String infomationFromUserSide, final String dbversion) {
        boolean isCloneDbSuccess = CloudDatabaseHelper
            .isCloneDatabase(contextIn, Constants.DB_NAME);
        // Compare DB version to decide export DB or not.
        Settings setting = new DASettings().getByName(context, Constants.DB_VERSION);
        int dbVersion = 0;
        if ((!setting.getValue().equals(Constants.EMPTY_STRING) && (setting.getValue() != null))) {
            dbVersion = Integer.parseInt(setting.getValue());
        }
        if (dbVersion != Integer.getInteger(dbversion, 0)) {
            MGELog.uLogI(tag, "Difference DB version with EM, send DB to EM.");
            exportDatabse(Constants.DB_NAME, contextIn, isCloneDbSuccess);
        }
        // Copy all image if it need.
        String allInfoFromUserSide = infomationFromUserSide;
        // Copy brand image to APP_DIR_PATH_USER folder.
        String brandImagePath = Environment.getExternalStorageDirectory() + Constants.SLASH
            + Constants.APP_DIR_PATH + Constants.SLASH + Constants.BRAND_IMAGES_FOLDER
            + Constants.SLASH + GlobalVariables.getBrandImage();
        File brandImage = new File(brandImagePath);
        if (GlobalVariables.getBrandImage() != Constants.EMPTY_STRING && brandImage.exists()) {
            copyFiles(brandImagePath, contextIn, Constants.BRAND_IMAGES_FOLDER);
        }
        // Get ImagePath from db.
//        DAFloorPlans daFloorPlans = new DAFloorPlans();
//        ArrayList<FloorPlans> floorPlans = daFloorPlans.getAll(context);
//        for (FloorPlans imagePath : floorPlans) {
//            MGELog.uLogI(tag, "getImagePath: " + imagePath.getImagePath());
//            copyFiles(imagePath.getImagePath(), contextIn, null);
//        }
    }

    /**
     * Used to export DB to SDcard.
     * @param databaseName
     *            Database name.
     * @param contextInput
     *            : Current context.
     * @param isCloneDbSucessful
     *            True if clone DB successful, false if ohter while.
     */
    private synchronized void exportDatabse(final String databaseName, final Context contextInput,
        final boolean isCloneDbSucessful) {
        try {
            File sd = Environment.getExternalStorageDirectory();
            File data = Environment.getDataDirectory();

            if (sd.canWrite()) {
                String backupDBPath = databaseName;
                String currentDBPath;
                if (isCloneDbSucessful) {
                    currentDBPath = contextInput.getDatabasePath("_" + databaseName)
                        .getAbsolutePath();
                } else {
                    currentDBPath = contextInput.getDatabasePath(databaseName).getAbsolutePath();
                }
                File currentDB = new File(currentDBPath);
                String pathProject = Environment.getExternalStorageDirectory().getAbsolutePath()
                    + Constants.SLASH + APP_DIR_PATH_USER;
                File fileProjectPath = new File(pathProject);
                if (!fileProjectPath.exists()) {
                    fileProjectPath.mkdir();
                }
                File backupDB = new File(fileProjectPath, backupDBPath);
                if (currentDB.exists()) {
                    FileChannel src = new FileInputStream(currentDB).getChannel();
                    FileChannel dst = new FileOutputStream(backupDB).getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
                    MGELog.uLogD(tag, "Copy DB successful!");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            MGELog.uLogD(tag, "Copy DB failed!!");
        }

    }

    /**
     * Used to export DB to SDcard.
     * @param fileNameIn
     *            Database name.
     * @param contextInput
     *            : Current context.
     */
    private synchronized void copyFiles(final String fileNameIn, final Context contextInput, final String destinationInSDCard) {
        int index = fileNameIn.lastIndexOf(Constants.SLASH);
        String fileName = fileNameIn.substring(index + 1);
        // String fileName = fileNameIn
        // .substring(fileNameIn.lastIndexOf(".") + 1, fileNameIn.length());
        MGELog.uLogI(tag, "fileName: " + fileName);
        try {
            File sd = Environment.getExternalStorageDirectory();
            File data = Environment.getDataDirectory();

            if (sd.canWrite()) {
                String pathToCurrentFile = fileNameIn;
                File currentFile = new File(pathToCurrentFile);
                String pathToBackUp = Environment.getExternalStorageDirectory().getAbsolutePath()
                    + Constants.SLASH + APP_DIR_PATH_USER;
                if (destinationInSDCard != null) {
                    pathToBackUp += Constants.SLASH + destinationInSDCard;
                }
                File fileProjectPath = new File(pathToBackUp);
                if (!fileProjectPath.exists()) {
                    fileProjectPath.mkdir();
                }
                File backupFile = new File(fileProjectPath, fileName);
                if (currentFile.exists()) {
                    try {
                        InputStream input = new FileInputStream(currentFile);
                        OutputStream output = new FileOutputStream(backupFile);
                        byte[] buffer = new byte[Files.BUFFER_SIZE];
                        int length;
                        while ((length = input.read(buffer)) > 0) {
                            output.write(buffer, 0, length);
                        }
                        input.close();
                        output.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    MGELog.uLogD(tag, "Copy File Image successful!");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            MGELog.uLogD(tag, "Copy File Image failed!!");
        }

    }

    /**
     * This method used to copy DB files in to MGEController and compress all files.
     */
    private void prepareFilesToSend(final String infomations_usersSide, final String dbVersion) {

        File fileSourceToZip = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
            + Constants.SLASH + APP_DIR_PATH_USER);
        deleteEdgeHomeFolderContent(fileSourceToZip);
        getAllFilesReady(context, infomations_usersSide, dbVersion);

        ZipUtility zipUtility = new ZipUtility();
        File zipToFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
            + Constants.SLASH + APP_DIR_PATH_USER + ".zip");
        try {
            zipUtility.zipDirectory(fileSourceToZip, zipToFile);
        } catch (IOException e) {
            MGELog.uLogD(tag, "Failed zip.");
            e.printStackTrace();
        }
    }

    /**
     * This method is used to delete EDGEHomeMobile folder content after send it to EHM.
     * @param fileInput file input.
     */
    private void deleteEdgeHomeFolderContent(final File fileInput) {
        if (fileInput.isDirectory()) {
            String[] children = fileInput.list();
            for (int i = 0; i < children.length; i++) {
                File file = new File(fileInput, children[i]);
                if (file.isDirectory()) {
                    // Delete BrandImages folder content
                    deleteEdgeHomeFolderContent(file);
                    // Delete BrandImages folder
                    file.delete();
                } else {
                    file.delete();
                }
            }
        }
    }
    /**
     * This method used to send message to all user using TCP mode in Controller.
     * @param contentMessage
     *            : Content on message send to user side.
     */
    private void sendMessageToAllUserUsingTCP(final String contentMessage) {
        if (NetWorkTCP.getListAllTcpUser().size() > 0) {
            Enumeration enumeration = NetWorkTCP.getListAllTcpUser().keys();
            while (enumeration.hasMoreElements()) {
                int key = (Integer) enumeration.nextElement();
                NetWorkTCP.getListAllTcpUser().get(key).sendMessageOnNetWork(contentMessage);
            }
        }
    }
}