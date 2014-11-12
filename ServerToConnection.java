package com.enclaveit.mgecontroller.tcp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import javax.net.ssl.SSLSocket;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Looper;
import android.preference.PreferenceManager;

import com.enclaveit.mgecontroller.log.MGELog;
import com.enclaveit.mgecontroller.utils.AESSecure;
import com.enclaveit.mgecontroller.utils.GlobalVariables;

/**
 * This class used for accept connection and handle all events come to CON.
 * @author hau.v.bui
 */
public class ServerToConnection implements Runnable {

    /**
     * Tool used for read data on socket pipe.
     */
    private BufferedReader is;
    /**
     * Tool used for write data on socket pipe.
     */
    private PrintWriter os;
    /**
     * Socket used to send and get data.
     */
    private SSLSocket clientSocket;
    /**
     * Current server context of CON.
     */
    private Context serverContext;
    private String tag = "ServerToConnection TCP ";

    private int orderCame = 0;

    /**
     * Constructor of class used to save socket connected and current context. Also initial all
     * tools necessary.
     * @param clientSocketInput
     *            :The socket will be saved.
     * @param numConnectionsInput
     *            :Order of connection.
     * @param context
     *            :Current context.
     */
    public ServerToConnection(final SSLSocket clientSocketInput, final int numConnectionsInput,
            final Context context) {
        this.clientSocket = clientSocketInput;
        serverContext = context;
        orderCame = numConnectionsInput;

        getToolsToCommunication();

    }

    /**
     * Method used to initial all necessary tool used for transfer data.
     */
    private void getToolsToCommunication() {
        try {
            os = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                    clientSocket.getOutputStream())), true);
            is = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public SSLSocket getSocketComunication() {
        return clientSocket;
    }

    public void sendMessageOnNetWork(String message) {
        new SendMessageTask().execute(message);
    }

//    private static Handler handler = new Handler();

    class SendMessageTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            sendMessage(params[0]);

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }
    }

    /**
     * Sends the message to client.
     * @param message
     *            text from server.
     */
    public void sendMessage(String message) {
        if (os != null && ! os.checkError()) {
            os.println(message);
//            os.write(message.toCharArray(), 0, message.length());
            os.flush();
            MGELog.uLogI(tag, "Message sent = " + message);
        } else if (os != null && os.checkError()) {
            // Remove user have error socket out of user list.
            NetWorkTCP.removeFromUserList(orderCame);
        }
    }
    

    @Override
    public void run() {
        String receivedMessage;
        boolean isLogedInToSystem = false;
        
            Looper.prepare();
            boolean serverStop = false;
            while (true) {
                try {
                receivedMessage = readLineOnSocket();
                if (receivedMessage != null) {
                    if (GlobalVariables.mgeMode == GlobalVariables.INSTALLATION_MODE) {
                        // Send command back to EHM.
                        sendMessage(MGEProtocols.MGE_DEACTIVE_ACTION);
                    } else {
//                    MGELog.uLogI(tag, "Client IP = " + clientSocket.getInetAddress().toString());
                    if (receivedMessage.contains(MGEProtocols.MGE_REQUEST_CONNECT)) {
                        boolean canAcceptLogin = false;
                        receivedMessage = receivedMessage.replace(MGEProtocols.MGE_REQUEST_CONNECT
                                + MGEProtocols.MGE_COMPONENT_DELIMITER, "");
                        canAcceptLogin = getAndCheckInfomationRequireLogIn(receivedMessage);
                        MGELog.uLogI(tag, "Did Login ? = " + canAcceptLogin);
                        String[] strings = receivedMessage
                                .split(MGEProtocols.MGE_COMPONENT_DELIMITER);
                        if (canAcceptLogin) {
                            isLogedInToSystem = true;
                            sendMessage(MGEProtocols.MGE_RESULT_REQUEST
                                    + MGEProtocols.MGE_COMPONENT_DELIMITER + strings[0]
                                    + MGEProtocols.MGE_COMPONENT_DELIMITER + strings[1]);
                        } else {
                            sendMessage(MGEProtocols.MGE_REFUSE_CONNECT
                                    + MGEProtocols.MGE_COMPONENT_DELIMITER + strings[0]
                                    + MGEProtocols.MGE_COMPONENT_DELIMITER + strings[1]);
                            MGELog.uLogI(tag, "Login info wrong!");
                            closeStreamAndSocket();
                            break;
                        }
                    } else {
                        if (isLogedInToSystem) {
                            HandleTCPClientJobs clientJobs = new HandleTCPClientJobs(
                                    clientSocket, serverContext, receivedMessage, os, is, orderCame);
                            new Thread(clientJobs).start();
                        } else {
                            MGELog.uLogI(tag, "User did not login, maybe attacker to your system.");
                            MGELog.uLogI(tag, "Attacker IP = "
                                    + clientSocket.getInetAddress().toString());
                            MGELog.uLogI(tag, "Attacker unknow message = " + receivedMessage);
                        }
                    }
                }
                }
                } catch (IOException e) {
                    MGELog.uLogE(tag, e.toString());
                    MGELog.uLogI(tag, "One Client socket was out of system.");
                    break;
                }
            }
            MGELog.uLogI(tag, "Exist loop receiving data.");
            closeStreamAndSocket();

    }
    /**
     * Clean up environment.
     */
    private void closeStreamAndSocket() {
        //Close and clean
        try {
            if (os != null) {
                os.close();
            }
            if (is != null) {
                is.close();
            }
            if (clientSocket != null) {
                clientSocket.close();
            }
            MGELog.uLogI(tag, "Close stream and client socket.");
        } catch (IOException e) {
            MGELog.uLogI(tag, "Error when close socket.");
            e.printStackTrace();
        }
    }

    /**
     * This method used to read one line on socket.
     * @return A line
     * @throws IOException
     *             happen when nothing to read, socket closed.
     */
    private String readLineOnSocket() throws IOException {
        if (is != null) {
            return is.readLine();
        } else {
            return null;
        }
    }

    /**
     * ShareReference user name.
     */
    private final String userNameKey = "userName";
    /**
     * ShareReference pass word.
     */
    private final String passWordKey = "passWord";

    /**
     * Method used to check information log in from EM.
     * @param line
     *            : The information got from EM.
     * @return True if log in info is right. False for info log in wrong.
     */
    private boolean getAndCheckInfomationRequireLogIn(final String line) {
        new AESSecure(serverContext);
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(serverContext);
        String userName = sharedPreferences.getString(userNameKey, "");
        String passWord = sharedPreferences.getString(passWordKey, "");
        passWord = AESSecure.decrypt(passWord);
        if (!userName.isEmpty() && !passWord.isEmpty()) {
            String[] infoLogin = new String[2];
            infoLogin = line.split(MGEProtocols.MGE_COMPONENT_DELIMITER);
            if (infoLogin[0].equals(userName) && infoLogin[1].equals(passWord)) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

}
