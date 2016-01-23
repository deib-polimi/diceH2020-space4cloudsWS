package it.polimi.diceH2020.SPACE4CloudWS.fs;

import it.polimi.diceH2020.SPACE4CloudWS.fs.policy.DeletionPolicy;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class FileUtility {

	private static final File LOCAL_DYNAMIC_FOLDER = new File("TempWorkingDir");
	private static Logger logger = Logger.getLogger(FileUtility.class.getName());

	@Autowired
	private DeletionPolicy policy;

	public boolean delete(File file) {
		return policy.delete(file);
	}

	public File provideTemporaryFile(@Nonnull String prefix, @Nullable String suffix) throws IOException {
		return File.createTempFile(prefix, suffix, LOCAL_DYNAMIC_FOLDER);
	}

	public void writeContentToFile(@Nonnull String content, @Nonnull File file) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		writer.write(content);
		writer.close();
	}

	public static void createWorkingDir() throws IOException {
		Path folder = LOCAL_DYNAMIC_FOLDER.toPath();
		Files.createDirectories(folder);
		logger.info(LOCAL_DYNAMIC_FOLDER + " created.");
	}
	
	public static void destroyWorkingDir() throws IOException{
		Path folder = LOCAL_DYNAMIC_FOLDER.toPath();
		if (Files.deleteIfExists(folder)) {
			logger.info(LOCAL_DYNAMIC_FOLDER+ " deleted.");
		}
	}

	public static void createPNNetFile(int numContainers, double b, double c, double d, int i) {

		String oldFileName = LOCAL_DYNAMIC_FOLDER+File.separator+"SWN_ProfileR.net";
		// this is the reference file. It has a set of placeholders that must be
		// filled

		String tmpFileName = LOCAL_DYNAMIC_FOLDER+File.separator+"SWN_ProfileR" + i + ".net";

		BufferedReader br = null;
		BufferedWriter bw = null;
		try {
			br = new BufferedReader(new FileReader(oldFileName));
			bw = new BufferedWriter(new FileWriter(tmpFileName));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.contains("param1"))
					line = line.replace("param1", String.valueOf(numContainers));
				if (line.contains("param2"))
					line = line.replace("param2", String.valueOf(b));
				if (line.contains("param3"))
					line = line.replace("param3", String.valueOf(c));
				if (line.contains("param4"))
					line = line.replace("param4", String.valueOf(d));
				bw.write(line + "\n");
			}
		} catch (Exception e) {
			return;
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException e) {
				//
			}
			try {
				if (bw != null)
					bw.close();
			} catch (IOException e) {
				//
			}
		}

	}

	public static void createPNDefFile(int param1, int param2, int param3, int i) {
		String oldFileName = LOCAL_DYNAMIC_FOLDER+File.separator+"SWN_ProfileR.def";
		String tmpFileName = LOCAL_DYNAMIC_FOLDER+File.separator+"SWN_ProfileR" + i + ".def";

		BufferedReader bReader = null;
		BufferedWriter bw = null;
		try {
			bReader = new BufferedReader(new FileReader(oldFileName));
			bw = new BufferedWriter(new FileWriter(tmpFileName));
			String line;
			while ((line = bReader.readLine()) != null) {
				if (line.contains("param1"))
					line = line.replace("param1", String.valueOf(param1));
				if (line.contains("param2"))
					line = line.replace("param2", String.valueOf(param2));
				if (line.contains("param3"))
					line = line.replace("param3", String.valueOf(param3));
				bw.write(line + "\n");
			}
		} catch (Exception e) {
			return;
		} finally {
			try {
				if (bReader != null)
					bReader.close();
			} catch (IOException e) {
				//
			}
			try {
				if (bw != null)
					bw.close();
			} catch (IOException e) {
				//
			}
		}

	}

}
