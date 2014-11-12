package com.enclaveit.mgecontroller.tcp;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import android.content.Context;

/**
 * This class used for certificate key from client.
 * 
 * @author hau.v.bui
 */
public class MyX509TrustManager implements X509TrustManager {
	/*
	 * The default X509TrustManager returned by IbmX509. We'll delegate
	 * decisions to it, and fall back to the logic in this class if the default
	 * X509TrustManager doesn't trust it.
	 */
	/**
	 * Trust Manager chose.
	 */
	private X509TrustManager pkixTrustManager;
	/**
	 * Current context.
	 */
	private Context context;
	/**
	 * Keystore of EM.
	 */
	private final String keystoreEM = "keystore.jks";
	/**
	 * Store pass.
	 */
	private final String storepass = "123123";

	/**
	 * Constructor used to authentication.
	 * 
	 * @param authType
	 *            :Context input.
	 * @throws Exception
	 *             :Throw Exception if authenticate fail.
	 */
	public MyX509TrustManager(final Context authType) throws Exception {
		// create a "default" JSSE X509TrustManager.
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

		this.context = authType;

		// InputStream inputStream = context.getAssets().open("mgeclient.jks");
		InputStream inputStream = context.getAssets().open(keystoreEM);

		// ks.load(inputStream, "mgeclientjks".toCharArray());
		ks.load(inputStream, storepass.toCharArray());

		TrustManagerFactory tmf = TrustManagerFactory
				.getInstance(TrustManagerFactory.getDefaultAlgorithm());

		tmf.init(ks);

		TrustManager[] tms = tmf.getTrustManagers();

		/*
		 * Iterate over the returned trustmanagers, look for an instance of
		 * X509TrustManager. If found, use that as our "default" trust manager.
		 */
		for (int i = 0; i < tms.length; i++) {
			if (tms[i] instanceof X509TrustManager) {
				pkixTrustManager = (X509TrustManager) tms[i];
				return;
			}
		}
		/*
		 * Find some other way to initialize, or else we have to fail the
		 * constructor.
		 */
		throw new Exception("Couldn't initialize");
	}

	/**
	 * Delegate to the default trust manager.
	 * 
	 * @param chain
	 *            : This represents a standard way for accessing the attributes
	 *            of X.509 certificates.
	 * @param authType
	 *            Authenticate type.
	 * @throws CertificateException
	 *             : If check client trust fail.
	 */
	public final void checkClientTrusted(final X509Certificate[] chain,
			final String authType) throws CertificateException {
		try {
			pkixTrustManager.checkClientTrusted(chain, authType);
		} catch (CertificateException excep) {
			// do any special handling here, or re-throw exception.
			excep.printStackTrace();
		}
	}

	/**
	 * Delegate to the default trust manager.
	 * 
	 * @param chain
	 *            : Current certificate.
	 * @param authType
	 *            Authentication type.
	 * @throws CertificateException
	 *             if check trust fail.
	 */
	public final void checkServerTrusted(final X509Certificate[] chain,
			final String authType) throws CertificateException {
		try {
			pkixTrustManager.checkServerTrusted(chain, authType);
		} catch (CertificateException excep) {
			/*
			 * Possibly pop up a dialog box asking whether to trust the cert
			 * chain.
			 */
			excep.printStackTrace();
		}
	}

	/**
	 * Merely pass this through.
	 * 
	 * @return X509Certificate current object.
	 */
	public final X509Certificate[] getAcceptedIssuers() {
		return pkixTrustManager.getAcceptedIssuers();
	}
}
