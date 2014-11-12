package com.enclaveit.mgecontroller.tcp;

/**
 * This class is protocol using for transfer data via TCP.
 * @author hau.v.bui
 */
public class MGEProtocols {
    /**
     * Key request Use for send command set brightness to EDGEhome.
     */
    public static final String MGE_REQUEST_SET_BRIGHTNESS = "SET_BRIGHTNESS";
    /**
     * Key request Usefor send command CAN NOT CONNECT TO PI.
     */
    public static final String MGE_MESSAGE_NOT_CONNECT_TO_PI = "CAN_NOT_CONNECT_TO_PI";
    /**
     * Key request Use for send command set brightness to EDGEhome.
     */
    public static final String MGE_MESSAGE_REAL_DEVICE_NOT_RESPONSE = "REAL_DEVICE_NOT_RESPONSE";
    /**
     * Key request turn ON/OFF device.
     */
    public static final String MGE_REQUEST_TURN_ON_OFF_LIGHT = "on_off_light";
    /**
     * Key request change dim level of device.
     */
    public static final String MGE_REQUEST_DIM_THE_LIGHT = "dim_the_light";
    /**
     * Key used for apply the scene.
     */
    public static final String MGE_REQUEST_APPLY_SCENE = "apply_scene";
    /**
     * Key used for request connection with CON.
     */
    public static final String MGE_REQUEST_CONNECT = "connect";
    /**
     * Key used for request disconnect.
     */
    public static final String MGE_REQUEST_DISCONNECT = "disconnect";
    /**
     * Key delimiter of protocol.
     */
    public static final String MGE_COMPONENT_DELIMITER = " ";
    /**
     * Key result request.
     */
    public static final String MGE_RESULT_REQUEST = "result_request";
    /**
     * Key refuse connect.
     */
    public static final String MGE_REFUSE_CONNECT = "refuse_connection";
    /**
     * Key used for get DB.
     */
    public static final String MGE_GET_DB = "get_db";
    /**
     * Key used for get GEP DB.
     */
    public static final String MGE_GET_GEP = "get_gep";
    /**
     * key used for get Images.
     */
    public static final String MGE_GET_IMAGES = "get_images";
    /**
     * Key used for NSD.
     */
    public static final String MGE_NSD_NAME = "MGEController";
    /**
     * Return this key back when control done.
     */
    public static final String ACTION_DIM_DONE = "dim_done";
    /**
     * Return this key back when turn on off device.
     */
    public static final String ACTION_TURN_ON_OFF_DONE = "turn_done";
    /**
     * Return this key back when apply scenes.
     */
    public static final String ACTION_APPLY_SCENCE_DONE = "sce_done";
    /**
     * Key for get power consumption.
     */
    public static final String MGE_GET_POWER_CONSUMPTION = "get_power_consump";
    /**
     * Key for get power state.
     */
    public static final String MGE_GET_POWER_BRIGHTNESS = "get_power_brightness";
    /**
     * Key for get DB version.
     */
    public static final String MGE_GET_DB_VERSION = "get_dbversion";
    /**
     * Key for edit or create sce Command.
     */
    public static final String MGE_REQEST_EDIT_OR_CREATE_SCENCE_SUCCESSFUL = "EDIT_OR_CREATE_SCE";
    /**
     * Key for delete scenceCommand.
     */
    public static final String MGE_REQEST_DELETE_SCENCE_SUCCESSFUL = "DELETE_SCE";
    /**
     * Key for eco-timer via tcp.
     */
    public static final String MGE_REQUEST_ECO_TIMER = "eco_timer_list";
    /**
     * Key for lock outlet via tcp.
     */
    public static final String MGE_REQUEST_LOCK_OUTLETS = "ROO";
    /**
     * Key for confirm that the Controller is currently on.
     */
    public static final String MGE_REQUEST_FOR_CONFIRM_CONTROLLER = "CC";
    /**
     * Key for confirm that the Controller is currently on.
     */
    public static final String MGE_REQUEST_FOR_CONFIRM_MESSAGE_CONTROLLER = "CMC";
    /**
     * Key for send request update state of device.
     */
    public static final String MGE_REQUEST_FOR_UPDATE_FROM_CONTROLLER = "UC";
    /**
     * Key for send request update state of device to EMH.
     */
    public static final String MGE_REQUEST_FOR_UPDATE_STATE_POP_UP = "USP";
    /**
     * Key for send temperature inside from CON.
     */
    public static final String MGE_GET_TEMP_INSIDE = "get_temp_inside";
    /**
     * Key for Use for send get MGE state.
     */
    public static final String MGE_DEACTIVE_ACTION = "DEACTIVE_ACTION";
}
