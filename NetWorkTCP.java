package com.enclaveit.mgecontroller.tcp;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;

import com.enclaveit.mgecontroller.activities.R;
import com.enclaveit.mgecontroller.log.MGELog;
import com.enclaveit.mgecontroller.parse.CloudDatabaseHelper;
import com.parse.ParseUser;

/**
 * This method used to open a socket and initial NSD service which have
 * available port.
 * 
 * @author hau.v.bui
 */
public class NetWorkTCP implements Runnable {

	/**
	 * Server socket used to open port and communication.
	 */
	private static SSLServerSocket serverSocket;
	/**
	 * Socket used for communication.
	 */
	private SSLSocket clientSocket;
	/**
	 * Order of connection come to Socket server.
	 */
	private int numConnections = 0;
	/**
	 * Tag used to print.
	 */
	private String tag = "NetWorkTCP";
	/**
	 * Context of current screen.
	 */
	private Context context;
	/**
	 * Keystore Name of Controller.
	 */
	private final String keystoreMGE = "controller.jks";
	/**
	 * Keypass.
	 */
	private final String keypass = "mgecontrollerprivate";
	/**
	 * Storepass.
	 */
	private final String storepass = "mgecontrollerpublic";
	/**
	 * SSL protocol. "SSLv3""TLSv1.2"
	 */
	private final String sslProtocol = "TLSv1.2";

	/**
	 * Used to save all user list login on CON by TCP.
	 */
	private static Hashtable<Integer, ServerToConnection> userList = new Hashtable<Integer, ServerToConnection>();

	/**
	 * This method used to get all user login in by TCP.
	 * 
	 * @return List of user current login by TCP.
	 */
	public static Hashtable<Integer, ServerToConnection> getListAllTcpUser() {
		return userList;
	}

	/**
	 * Constructor of NetWorkTCP.
	 * 
	 * @param contextIn
	 *            : Current context application.
	 */
	public NetWorkTCP(final Context contextIn) {
		context = contextIn;
	}

	@Override
	public final void run() {
		openSSLSocketServer();

		// Waiting to accept client request connect.
		waittingForAcceptConnection(context);
	}

	/**
	 * This method used to open port on socket and broadcast NSD service.
	 */
	private void openSSLSocketServer() {
		// char[] ksPass = "mgeserverjks".toCharArray();
		// char[] ctPass = "mgeserver".toCharArray();
		char[] ksPass = storepass.toCharArray();
		char[] ctPass = keypass.toCharArray();
		try {
			// Get default KeyStore instance with default type.
			KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
			// Create InputStream to get KeyStore from assets folder.
			// InputStream stream = context.getAssets().open("mgeserver.jks");
			InputStream stream = context.getAssets().open(keystoreMGE);
			// Load KeyStore into KeyStore instance with password to open that
			// KeyStore.
			ks.load(stream, ksPass);
			// Get algorithm from KeyManagerFactory by using default.
			KeyManagerFactory kmf = KeyManagerFactory
					.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			// Initializes this instance with the specified key store and
			// password.

			kmf.init(ks, ctPass);

			// kmf.init(ks, null);

			// Creates a new SSLContext instance for the specified protocol,
			// we are using SSL in both side include controller and client
			// devices.

			SSLContext sc = SSLContext.getInstance(sslProtocol);
			// SSLContext sc =
			// SSLContext.getInstance(SSLContext.getDefault().getProtocol());
			MGELog.uLogI(tag, "Protocol = " + sc.getProtocol());
			MGELog.uLogI(tag, "Protocol Version = "
					+ sc.getProvider().getVersion());
			// SSLParameters parameters = sc.getDefaultSSLParameters();
			// MGELog.uLogI(tag, "SSL" + parameters.getProtocols().toString());
			// MGELog.uLogI(tag, "SSL" + parameters.toString());
			// Load our TrustManager store from our certificated file of client
			// devices.

			TrustManager[] myTMs = new TrustManager[] { new MyX509TrustManager(
					context) };
			// TrustManager[] myTMs = new TrustManager[] {new
			// MyX509TrustManager(context), new
			// MyX509TrustManagerIOS(context) };

			// Load KeyManagerFactory and TrustManager in to SSLContext to
			// create
			// SSLServerSocketFactory

			// TrustManagerFactory tmf =
			// TrustManagerFactory.getInstance(TrustManagerFactory
			// .getDefaultAlgorithm());
			// tmf.init((KeyStore) null);
			// X509TrustManager xtm = (X509TrustManager)
			// tmf.getTrustManagers()[0];
			// for (X509Certificate cert : xtm.getAcceptedIssuers()) {
			// String certStr = "S:" + cert.getSubjectDN().getName() + "\nI:"
			// + cert.getIssuerDN().getName();
			// MGELog.uLogI(tag, certStr);
			// }
			// TrustManager[] myTMs = new TrustManager[] {xtm};

			sc.init(kmf.getKeyManagers(), myTMs, null);

			// Create SSLServerSocketFactory to open one port and let client
			// connect with.
			SSLServerSocketFactory ssf = sc.getServerSocketFactory();
			if (ssf != null) {
				// Create SSLServerSocket and open port 6789 for client devices
				// can connect to it.
				serverSocket = (SSLServerSocket) ssf.createServerSocket(60789);
				MGELog.uLogI(tag, "create 60789");

				// Use to reuse the address of Controller for the next run.
				serverSocket.setReuseAddress(true);

				// serverSocket.bind(new InetSocketAddress(60789));

				if (serverSocket == null) {
					serverSocket = (SSLServerSocket) ssf.createServerSocket(0);
				}
				// serverSocket = (SSLServerSocket) ssf.createServerSocket(0);
				// InetAddress inetAddress = InetAddress.getByName("127.0.0.1");
				// serverSocket = (SSLServerSocket) ssf.createServerSocket(6789,
				// 0,
				// inetAddress);
				// serverSocket.setNeedClientAuth(true);
				printServerSocketInfo(serverSocket);
				// int port = NSDBroadcastService.
				// Call to broadcast ip and port to client.
				// innitialNsdBroadcastService(serverSocket.getLocalPort());
				saveToReferenceAndUpToCloud();
			} else {
				MGELog.uLogI(tag, "Factory Server Null");
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (CertificateException e) {
			e.printStackTrace();
		} catch (UnrecoverableKeyException e) {
			e.printStackTrace();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method used to add one user to list of user login by TCP.
	 * 
	 * @param oderCameToServer
	 *            Oder of user come to Controller.
	 * @param serverToConnection
	 *            Object used to represent for connection between Controller and
	 *            user.
	 */
	public final void addToUserList(final int oderCameToServer,
			final ServerToConnection serverToConnection) {
		userList.put(oderCameToServer, serverToConnection);
		MGELog.uLogI(tag,
				"Added one user on userList; size = " + userList.size());
	}

	/**
	 * This method used to remove one user from user list current is login by
	 * TCP.
	 * 
	 * @param orderCame
	 *            Order of user come to Controller.
	 */
	public static void removeFromUserList(final int orderCame) {
		userList.remove(orderCame);
		MGELog.uLogI("TCP",
				"Removed one user on userList; size = " + userList.size());
	}

	/**
	 * Variable show state of thread auto accept connection with user side.
	 */
	public boolean isRunning = true;

	/**
	 * This method is waiting for EM connect to CON via opened port.
	 * 
	 * @param contextIn
	 *            :Server current context.
	 */
	private void waittingForAcceptConnection(final Context contextIn) {

		while (isRunning) {
			try {
				if (serverSocket != null) {
					MGELog.uLogI(tag, "CON waiting for new EM...");
					clientSocket = (SSLSocket) serverSocket.accept();
					// Set time out for each connection with device is 20
					// minutes.
					// serverSocket.setSoTimeout(12000000);
					printOutSocketInfo();
					numConnections++;
					MGELog.uLogI(tag, "Order = " + numConnections);

					ServerToConnection oneconnection = new ServerToConnection(
							clientSocket, numConnections, contextIn);
					addToUserList(numConnections, oneconnection);
					new Thread(oneconnection).start();

				} else {
					break;
				}
			} catch (IOException e) {
				e.printStackTrace();
				MGELog.uLogI(tag, "Exit loop waiting for user.");
				break;
			}
		}

		MGELog.uLogI(tag, "Stoped waiting for new EM...");
		// Close all client socket.
		if (userList.size() > 0) {
			Enumeration<?> enumeration = userList.keys();
			while (enumeration.hasMoreElements()) {
				int key = (Integer) enumeration.nextElement();
				try {
					userList.get(key).getSocketComunication().close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			MGELog.uLogI(tag, "Closed all socket from EM.");
		} else {
			MGELog.uLogI(tag, "No socket user.");
		}

		// close server socket.
		try {
			if (clientSocket != null) {
				clientSocket.close();
				MGELog.uLogI(tag, "Closed client socket .");
			}
			if (serverSocket != null) {
				serverSocket.close();
				MGELog.uLogI(tag, "Closed Server socket .");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method used to update state of thread auto accept connection with
	 * user.
	 * 
	 * @param isRunningIn
	 *            New state of thread auto accept connection with EM.
	 */
	public final void setIsStillRunning(final boolean isRunningIn) {
		isRunning = isRunningIn;
		// close server socket.
		MGELog.uLogI(tag, "isRunning = " + isRunning);
		try {
			if (serverSocket != null) {
				serverSocket.close();
				MGELog.uLogI(tag, "Force closed Server socket .");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Print out socket Information.
	 */
	private void printOutSocketInfo() {
		MGELog.uLogI(tag, "New client connected");
		MGELog.uLogI(tag, "Client IP = "
				+ clientSocket.getInetAddress().toString());
		MGELog.uLogI(tag, "Server Local Port = " + clientSocket.getPort());
		MGELog.uLogI(tag, "Client Port = " + clientSocket.getLocalPort());
	}

	/**
	 * This method used to save host and port in to ShareReferences and upload
	 * it to cloud.
	 */
	public final void saveToReferenceAndUpToCloud() {
		WifiManager wim = (WifiManager) context
				.getSystemService(android.content.Context.WIFI_SERVICE);
		// String currentIpAddress =
		// Formatter.formatIpAddress(wim.getConnectionInfo().getIpAddress());
		int ipAddress = wim.getConnectionInfo().getIpAddress();
		MGELog.uLogI(tag, "Server IP in int = " + ipAddress);
		String currentIpAddress = String.format("%d.%d.%d.%d",
				(ipAddress & 0xff), (ipAddress >> 8 & 0xff),
				(ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));

		MGELog.uLogI(tag, "Port chose = " + serverSocket.getLocalPort());
		MGELog.uLogI(tag, "Server IP = " + currentIpAddress);
		if (ParseUser.getCurrentUser() != null
				&& ParseUser.getCurrentUser().getUsername() != null) {
			CloudDatabaseHelper.createOrUpdatePOTCP(currentIpAddress,
					String.valueOf(serverSocket.getLocalPort()));
			saveHostPortToShareReference(currentIpAddress
					+ MGEProtocols.MGE_COMPONENT_DELIMITER
					+ serverSocket.getLocalPort());
		} else {
			MGELog.uLogI(tag, "User = " + ParseUser.getCurrentUser());
		}
	}

	/**
	 * This method used to save host and port to ShareReferences file.
	 * 
	 * @param hostandport
	 *            : host and port open from controller.
	 */
	private void saveHostPortToShareReference(final String hostandport) {
		String keyHostPortInfo = "hostAndPort";
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(context);
		Editor editor = sharedPreferences.edit();
		editor.putString(keyHostPortInfo, hostandport);
		editor.commit();
	}

	/**
	 * This method used to initial NSD service and broadcast it on local
	 * network.
	 * 
	 * @param port
	 *            Available port could open.
	 */
	private void innitialNsdBroadcastService(final int port) {
		NSDBroadcastService broadcastService = new NSDBroadcastService(context);
		broadcastService.initializeNsd();
		broadcastService.registerService(port);
		startService();

	}

	/**
	 * Used to start NSD broadcast service.
	 */
	public final void startService() {
		Intent intentNsd = new Intent(context, NSDBroadcastService.class);
		context.startService(intentNsd);
	}

	/**
	 * This function use for print out socket server information, after setup
	 * keystore and cipher suite success.
	 * 
	 * @param s
	 *            :Server socket opened.
	 */
	private void printServerSocketInfo(final SSLServerSocket s) {
		MGELog.uLogI(tag, "Server socket class: " + s.getClass());
		MGELog.uLogI(tag, "   Socker address = "
				+ s.getInetAddress().toString());
		MGELog.uLogI(tag, "   Socker port = " + s.getLocalPort());
		MGELog.uLogI(tag,
				"   Need client authentication = " + s.getNeedClientAuth());
		MGELog.uLogI(tag,
				"   Want client authentication = " + s.getWantClientAuth());
		MGELog.uLogI(tag, "   Use client mode = " + s.getUseClientMode());
	}

	/**
	 * Get current server socket running.
	 * 
	 * @return SSLServerSocket
	 */
	public static SSLServerSocket getServerSocket() {
		return serverSocket;
	}

	/**
	 * This method used to send message to all user using TCP mode in
	 * Controller.
	 * 
	 * @param contentMessage
	 *            : Content on message send to user side.
	 */
	public static void sendMessageToAllUserUsingTCP(final Context context,
			final String contentMessage) {
		int key;
		if (NetWorkTCP.getListAllTcpUser() != null
				&& NetWorkTCP.getListAllTcpUser().size() > 0) {
			Enumeration enumeration = NetWorkTCP.getListAllTcpUser().keys();
			while (enumeration.hasMoreElements()) {
				key = (Integer) enumeration.nextElement();
				NetWorkTCP.getListAllTcpUser().get(key)
						.sendMessageOnNetWork(contentMessage);
			}
		}
	}
}
