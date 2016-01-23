package it.polimi.diceH2020.SPACE4CloudWS.fs;

import it.polimi.diceH2020.SPACE4CloudWS.fs.policy.DeletionPolicy;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class FileUtility {

	public static final String LOCAL_DYNAMIC_FOLDER = "TempWorkingDir";
	private static Logger logger = Logger.getLogger(FileUtility.class.getName());

	@Autowired
	private DeletionPolicy policy;

	public boolean delete(File file) {
		return policy.delete(file);
	}

	public static void createWorkingDir() throws IOException {

		Path folder = Paths.get(LOCAL_DYNAMIC_FOLDER);
		Files.createDirectories(folder);
		logger.info(LOCAL_DYNAMIC_FOLDER+" created.");
	}
	
	public static void destroyWorkingDir() throws IOException{
		Path folder = Paths.get(LOCAL_DYNAMIC_FOLDER);
		Files.deleteIfExists(folder);
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

	public static String createLocalSolFile(String nameSolFile) throws IOException {
		String solFilePath = FileUtility.LOCAL_DYNAMIC_FOLDER+"/"+nameSolFile;
		File file = new File(solFilePath);
		if (!file.exists())
			file.createNewFile();
		return solFilePath;
	}

}
