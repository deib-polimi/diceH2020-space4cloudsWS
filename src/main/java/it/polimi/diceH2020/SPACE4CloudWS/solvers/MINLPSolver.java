package it.polimi.diceH2020.SPACE4CloudWS.solvers;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import it.polimi.diceH2020.SPACE4CloudWS.connection.SshConnector;
import it.polimi.diceH2020.SPACE4CloudWS.core.FileUtiliy;

@Component
public class MINLPSolver {

	private static final String RESULTS_RISULTATO_HEURISTIC_SOL = "/results/risultato.heuristic.sol";

	private static final String REMOTE_SCRATCH = "/scratch";
	private static final String REMOTE_RESULTS = "/results";

	private static final String REMOTEPATH_DATA_DAT = REMOTE_SCRATCH + "/data.dat";

	private static final String REMOTEPATH_DATA_RUN = "data.run";

	private static SshConnector connector; // this can be changed with a bean

	private static Logger logger = Logger.getLogger(MINLPSolver.class.getName());

	@Autowired
	private MINLPSettings connSettings;

	@Autowired
	private Environment environment; // this is to check which is the active
										// profile at runtime

	@Autowired
	private ResourceLoader resourceLoader;

	public MINLPSolver() {
	}

	@PostConstruct
	private void init() {
		connector = new SshConnector(connSettings);
	}

	public Float run(String nameDatFile, String nameSolFile) throws Exception {
		float objFunctionValue = 0;
		String fullRemotePath = connSettings.getRemoteWorkDir() + REMOTEPATH_DATA_DAT;
		String fullLocalPath = FileUtiliy.LOCAL_DYNAMIC_FOLDER + File.separator + nameDatFile;
		connector.sendFile(fullLocalPath, fullRemotePath);
		logger.info("file " + nameDatFile + " has been sent");
		String nameRunFile = FileUtiliy.generateRunFile(connSettings.getSolverPath());

		fullRemotePath = connSettings.getRemoteWorkDir() + REMOTE_SCRATCH + "/" + REMOTEPATH_DATA_RUN;
		fullLocalPath = FileUtiliy.LOCAL_DYNAMIC_FOLDER + File.separator + nameRunFile;
		connector.sendFile(fullLocalPath, fullRemotePath);
		logger.info("File " + nameRunFile + " file has been sent");

		logger.info("Processing execution...");
		clearResultDir();
		String command = "cd " + connSettings.getRemoteWorkDir() + REMOTE_SCRATCH + " && "
				+ connSettings.getAmplDirectory() + " " + REMOTEPATH_DATA_RUN;
		logger.info("Remote exit status: " + connector.exec(command));

		fullLocalPath = FileUtiliy.createLocalSolFile(nameSolFile);

		fullRemotePath = connSettings.getRemoteWorkDir() + RESULTS_RISULTATO_HEURISTIC_SOL;

		logger.info("Solution file has been created");
		connector.receiveFile(fullLocalPath, fullRemotePath);

		objFunctionValue = this.analyzeSolution(fullLocalPath);

		logger.info("The value of the objective function is: " + objFunctionValue);

		logger.info("Cleaning result directory");
		clearResultDir();
		return objFunctionValue;
	}

	private float analyzeSolution(String solFilePath) throws IOException {
		File file = new File(solFilePath);
		if (!file.exists())
			file.createNewFile();
		String fileToString = FileUtils.readFileToString(file);
		String centralized = "centralized_obj = ";
		int startPos = fileToString.indexOf(centralized);
		int endPos = fileToString.indexOf('\n', startPos);
		float objFunctionValue = Float.parseFloat(fileToString.substring(startPos + centralized.length(), endPos));

		System.out.println(fileToString);
		System.out.println(objFunctionValue);
		return objFunctionValue;

	}

	public void initRemoteEnvironment() {
		List<String> lstProfiles = Arrays.asList(this.environment.getActiveProfiles());
		// TODO solve this.

		// System.out.println(ClassLoader.getSystemResource("/static/initFiles/MINLPSolver").toURI().toString());

//		ClassPathResource classPathResource = new ClassPathResource("/static/initFiles/MINLPSolver");
//		System.out.println(classPathResource.exists());
//		String localPath = classPathResource.getPath();
//		System.out.println(localPath);
		String localPath = "/static/initFiles/MINLPSolver";
		System.out.println("------------------------------------------------");
		System.out.println("Starting math solver service initialization phase");
		System.out.println("------------------------------------------------");
		if (lstProfiles.contains("test") && !connSettings.isForceClean()) {
			System.out.println("Test phase: the remote work directory tree is assumed to be ok.");

		} else {
			System.out.println("- Clearing remote work directory tree");
			try {
				connector.clear();

				System.out.println("- Creating new remote work directory tree");
				connector.exec("mkdir AMPL && cd AMPL && mkdir problems utils solve scratch results");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			System.out.println("- Sending work files");
			System.out.print("[#                    ]- Sending work files\r");
//			sendFile(localPath + "/problems/models1.mod", connSettings.getRemoteWorkDir() + "/problems/models1.mod");
//			System.out.print("[##                   ]- Sending work files\r");
			sendFile(localPath + "/utils/compute_psi.run", connSettings.getRemoteWorkDir() + "/utils/compute_psi.run");
			System.out.print("[###                  ]- Sending work files\r");
			sendFile(localPath + "/utils/compute_job_profile.run",
					connSettings.getRemoteWorkDir() + "/utils/compute_job_profile.run");
			System.out.print("[####                 ]- Sending work files\r");
			sendFile(localPath + "/utils/compute_penalties.run",
					connSettings.getRemoteWorkDir() + "/utils/compute_penalties.run");
			System.out.print("[#####                ]- Sending work files\r");
			sendFile(localPath + "/utils/save_aux.run", connSettings.getRemoteWorkDir() + "/utils/save_aux.run");
			System.out.print("[######               ]- Sending work files\r");
			sendFile(localPath + "/problems/centralized.run",
					connSettings.getRemoteWorkDir() + "/problems/centralized.run");
			System.out.print("[#######              ]- Sending work files\r");
			sendFile(localPath + "/utils/params_for_heuristic.run",
					connSettings.getRemoteWorkDir() + "/utils/params_for_heuristic.run");
			System.out.print("[########             ]- Sending work files\r");
			sendFile(localPath + "/utils/save_centralized.run",
					connSettings.getRemoteWorkDir() + "/utils/save_centralized.run");
			System.out.print("[#########            ]- Sending work files\r");
			sendFile(localPath + "/utils/calculedsd.run", connSettings.getRemoteWorkDir() + "/utils/calculedsd.run");
			System.out.print("[##########           ]- Sending work files\r");
			sendFile(localPath + "/solve/AM_closed_form.run",
					connSettings.getRemoteWorkDir() + "/solve/AM_closed_form.run");
			System.out.print("[###########          ]- Sending work files\r");
			sendFile(localPath + "/utils/simulated_time.run",
					connSettings.getRemoteWorkDir() + "/utils/simulated_time.run");
			System.out.print("[############         ]- Sending work files\r");
			sendFile(localPath + "/utils/save_centralized.run",
					connSettings.getRemoteWorkDir() + "/utils/save_centralized.run");
			System.out.print("[#############        ]- Sending work files\r");
			sendFile(localPath + "/utils/make_integer.run",
					connSettings.getRemoteWorkDir() + "/utils/make_integer.run");
			System.out.print("[##############       ]- Sending work files\r");
			sendFile(localPath + "/utils/save_centralized.run",
					connSettings.getRemoteWorkDir() + "/utils/save_centralized.run");
			System.out.print("[###############      ]- Sending work files\r");
			sendFile(localPath + "/utils/order_AM.run",
					connSettings.getRemoteWorkDir() + "/utils/save_centralized.run");
			System.out.println("Done");
		}
	}

	private void sendFile(String localPath, String remotePath)   {
		InputStream in = this.getClass().getResourceAsStream(localPath);
		System.out.println("ehyiii");
//		InputStream in = resourceLoader.getResource("classpath:" +localPath).getURL().openStream();
//		System.out.println(localPath+" "+resource.exists());
		FileOutputStream out = null;
		File tempFile = null;
		try {
			tempFile = File.createTempFile("S4C-temp", "tmp");
			tempFile.deleteOnExit();
			out = new FileOutputStream(tempFile);
		} catch (IOException e1) {
			System.out.println("error creating temp file");
		}
		
		
		try {
			System.out.println("before copying");
			IOUtils.copy(in, out);
			System.out.println("after copying");
		} catch (IOException ex) {
			ex.printStackTrace();
			System.out.println("error copying");
		}
		System.out.println("aftercopying");
		try {
			connector.sendFile(tempFile.getAbsolutePath(), remotePath);
		} catch (Exception e) {
			System.out.println("error sending file");
		}

	}

	public List<String> pwd() throws Exception {
		return connector.pwd();
	}

	public List<String> clear() throws Exception {
		return connector.clear();
	}

	private void clearResultDir() throws Exception {
		String command = "rm -rf " + connSettings.getRemoteWorkDir() + REMOTE_RESULTS + "/*";
		connector.exec(command);
	}

	@Profile("test")
	public SshConnector getConnector() {
		return connector;
	}

}