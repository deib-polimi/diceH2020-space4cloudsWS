/*
Copyright 2016 Michele Ciavotta
Copyright 2016 Eugenio Gianniti

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package it.polimi.diceH2020.SPACE4CloudWS.connection;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.*;

class ScpFrom {

	private ConnectionCreator connector;

	ScpFrom(ConnectionCreator connector) {
		this.connector = connector;
	}

	void receiveFile(String localFile, String remoteFile) throws JSchException, IOException {
		String prefix = null;
		if (new File(localFile).isDirectory()) {
			prefix = localFile + File.separator;
		}

		Session session = connector.createSession();
		ChannelExec channel = null;

		try {
			session.connect();
			// exec 'scp -f remoteFile' remotely
			String command = "scp -f " + remoteFile;
			channel = (ChannelExec) session.openChannel("exec");
			channel.setCommand(command);
			// get I/O streams for remote scp
			OutputStream out = channel.getOutputStream();
			InputStream in = channel.getInputStream();

			channel.connect();

			byte[] buffer = new byte[1024];

			buffer[0] = 0;
			out.write(buffer, 0, 1);
			out.flush();

			// reading channel
			while (true) {
				int c = AckChecker.checkAck(in);
				if (c != 'C') {
					break;
				}

				in.read(buffer, 0, 5);

				long fileSize = 0L;
				while (in.read(buffer, 0, 1) >= 0 && buffer[0] != ' ') {
					fileSize = fileSize * 10L + (long) (buffer[0] - '0');
				}

				String file;
				for (int i = 0;; ++i) {
					in.read(buffer, i, 1);
					if (buffer[i] == (byte) 0x0a) {
						file = new String(buffer, 0, i);
						break;
					}
				}

				buffer[0] = 0;
				out.write(buffer, 0, 1);
				out.flush();

				try (FileOutputStream stream =
							 new FileOutputStream(prefix == null ? localFile : prefix + file)) {
					while (fileSize > 0L) {
						int i = buffer.length < fileSize ? buffer.length : (int) fileSize;
						i = in.read(buffer, 0, i);
						if (i < 0) {
							break;
						}
						stream.write(buffer, 0, i);
						fileSize -= i;
					}
				}

				if (AckChecker.checkAck(in) != 0) {
					throw new JSchException(String.format("error receiving: %s", remoteFile));
				}

				buffer[0] = 0;
				out.write(buffer, 0, 1);
				out.flush();
			}
		} finally {
			if (channel != null) {
				channel.disconnect();
			}
			session.disconnect();
		}
	}

}
