package it.polimi.diceH2020.SPACE4CloudWS.connection;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.*;

class ScpTo {

	private ConnectionCreator connector;

	ScpTo(ConnectionCreator connector) {
		this.connector = connector;
	}

	// copying localFile on local machine in remoteFile on AMPL server
	void sendFile(String localFile, String remoteFile, boolean ptimestamp) throws JSchException, IOException {
		Session session = connector.createSession();
		ChannelExec channel = null;

		try {
			// creating connection
			session.connect();

			// exec 'scp -t remoteFile' remotely
			String command = "scp " + (ptimestamp ? "-p" : "") + " -t " + remoteFile;
			channel = (ChannelExec) session.openChannel("exec");
			channel.setCommand(command);
			// get I/O streams for remote scp
			OutputStream out = channel.getOutputStream();
			InputStream in = channel.getInputStream();
			// connecting channel
			channel.connect();

			if (AckChecker.checkAck(in) != 0) {
				throw new JSchException(String.format("error sending: %s", localFile));
			}

			File file = new File(localFile);

			if (ptimestamp) {
				command = "T" + (file.lastModified() / 1000) + " 0";
				command += (" " + (file.lastModified() / 1000) + " 0\n");
				out.write(command.getBytes());
				out.flush();
				if (AckChecker.checkAck(in) != 0) {
					throw new JSchException(String.format("error sending: %s", localFile));
				}
			}

			// send "C0644 fileSize filename", where filename should not include '/'
			long fileSize = file.length();
			command = "C0644 " + fileSize + " ";
			if (localFile.lastIndexOf('/') > 0) {
				command += localFile.substring(localFile.lastIndexOf('/') + 1);
			} else {
				command += localFile;
			}
			command += "\n";
			out.write(command.getBytes());
			out.flush();
			if (AckChecker.checkAck(in) != 0) {
				throw new JSchException(String.format("error sending: %s", localFile));
			}

			byte[] buffer = new byte[1024];
			// send content of localFile
			try (FileInputStream stream = new FileInputStream(localFile)) {
				int length;
				while ((length = stream.read(buffer, 0, buffer.length)) > 0) {
					out.write(buffer, 0, length);
				}
			}
			buffer[0] = 0;
			out.write(buffer, 0, 1);
			out.flush();
			if (AckChecker.checkAck(in) != 0) {
				throw new JSchException(String.format("error sending: %s", localFile));
			}
			out.close();

		} finally {
			if (channel != null) {
				channel.disconnect();
			}
			session.disconnect();
		}
	}

	void sendFile(String localFile, String remoteFile) throws JSchException, IOException {
		sendFile(localFile, remoteFile, true);
	}

}
