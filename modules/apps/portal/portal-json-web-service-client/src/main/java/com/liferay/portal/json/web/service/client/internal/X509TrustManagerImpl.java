/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.json.web.service.client.internal;

import com.liferay.portal.kernel.util.ArrayUtil;

import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * @author Ivica Cardic
 * @author Igor Beslic
 */
public class X509TrustManagerImpl implements X509TrustManager {

	public X509TrustManagerImpl() {
		try {
			_defaultX509TrustManager = _getX509TrustManager(null);
			_extraX509TrustManager = null;
			_trustSelfSignedCertificates = true;
		}
		catch (Exception exception) {
			throw new RuntimeException(exception);
		}
	}

	public X509TrustManagerImpl(
		KeyStore keyStore, boolean trustSelfSignedCertificates) {

		try {
			_defaultX509TrustManager = _getX509TrustManager(null);

			if (keyStore != null) {
				_extraX509TrustManager = _getX509TrustManager(keyStore);
			}
			else {
				_extraX509TrustManager = null;
			}

			_trustSelfSignedCertificates = trustSelfSignedCertificates;
		}
		catch (Exception exception) {
			throw new RuntimeException(exception);
		}
	}

	@Override
	public void checkClientTrusted(
			X509Certificate[] x509Certificates, String authType)
		throws CertificateException {

		if (!_trustSelfSignedCertificates || (x509Certificates.length != 1)) {
			try {
				_defaultX509TrustManager.checkClientTrusted(
					x509Certificates, authType);
			}
			catch (CertificateException certificateException) {
				if (_extraX509TrustManager != null) {
					_extraX509TrustManager.checkClientTrusted(
						x509Certificates, authType);
				}
				else {
					throw certificateException;
				}
			}
		}
	}

	@Override
	public void checkServerTrusted(
			X509Certificate[] x509Certificates, String authType)
		throws CertificateException {

		X509Certificate[] caCertificates = _getCACertificates(x509Certificates);

		if (!_trustSelfSignedCertificates || (caCertificates.length >= 1)) {
			try {
				_defaultX509TrustManager.checkServerTrusted(
					caCertificates, authType);
			}
			catch (CertificateException certificateException) {
				if (_extraX509TrustManager != null) {
					_extraX509TrustManager.checkServerTrusted(
						x509Certificates, authType);
				}
				else {
					throw certificateException;
				}
			}
		}
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		if (_extraX509TrustManager != null) {
			return ArrayUtil.append(
				_defaultX509TrustManager.getAcceptedIssuers(),
				_extraX509TrustManager.getAcceptedIssuers());
		}

		return _defaultX509TrustManager.getAcceptedIssuers();
	}

	private X509Certificate[] _getCACertificates(
			X509Certificate[] x509Certificates)
		throws CertificateException {

		List<X509Certificate> caCertificates = Arrays.asList();

		for (X509Certificate x509Certificate : x509Certificates) {
			if (!_isSelfSignedCertificate(x509Certificate)) {
				caCertificates.add(x509Certificate);
			}
		}

		return caCertificates.toArray(new X509Certificate[0]);
	}

	private String _getOrganizationFromIssuerDN(String issuerDN) {
		Pattern pattern = Pattern.compile("O=(.*?)(,|$)");

		Matcher matcher = pattern.matcher(issuerDN);

		if (matcher.find()) {
			return matcher.group(1);
		}

		return null;
	}

	private X509TrustManager _getX509TrustManager(KeyStore keyStore)
		throws Exception {

		TrustManagerFactory trustManagerFactory =
			TrustManagerFactory.getInstance(
				TrustManagerFactory.getDefaultAlgorithm());

		trustManagerFactory.init(keyStore);

		for (TrustManager trustManager :
				trustManagerFactory.getTrustManagers()) {

			if (trustManager instanceof X509TrustManager) {
				return (X509TrustManager)trustManager;
			}
		}

		return null;
	}

	private boolean _isSelfSignedCertificate(X509Certificate cert)
		throws CertificateException {

		if (cert == null) {
			throw new CertificateException(
				"Certificate must be an X509Certificate");
		}

		String issuerDN = _getOrganizationFromIssuerDN(
			cert.getIssuerDN(
			).getName());
		String subjectDN = _getOrganizationFromIssuerDN(
			cert.getSubjectDN(
			).getName());

		if ((issuerDN == null) || (subjectDN == null)) {
			throw new CertificateException(
				"The issuer or the subject of certificate is missing");
		}

		return issuerDN.equals(subjectDN);
	}

	private final X509TrustManager _defaultX509TrustManager;
	private final X509TrustManager _extraX509TrustManager;
	private final boolean _trustSelfSignedCertificates;

}