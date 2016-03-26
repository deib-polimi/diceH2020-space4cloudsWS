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

	// copying remoteFile on AMPL server in localFile on local machine
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
