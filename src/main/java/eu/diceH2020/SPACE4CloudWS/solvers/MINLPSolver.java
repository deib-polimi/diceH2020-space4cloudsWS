package eu.diceH2020.SPACE4CloudWS.solvers;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import eu.diceH2020.SPACE4CloudWS.connection.SshConnector;

@Component
public class MINLPSolver {

	private static SshConnector connector; // this can be changed with a bean

	private static Logger logger = Logger.getLogger(MINLPSolver.class.getName());

	
	@Autowired
	private MINLPSettings connSettings;

	@Autowired
	private Environment environment; // this is to check which is the active profile at runtime
 	
	public MINLPSolver() {
	}

	@PostConstruct
	private void init() {
		connector = new SshConnector(connSettings.getAddress(), connSettings.getUsername(), connSettings.getPassword(),
				connSettings.getPort());
	}

	public Float run(String nameDatFile, String nameSolFile) throws Exception {
		float objFunctionValue = 0;
		String fullPath = connSettings.getRemoteWorkDir() + "/scratch/data.dat";
		connector.sendFile(nameDatFile, fullPath);
		logger.info("file " + nameDatFile + " has been sent");
		System.out.println("file: " + nameDatFile + " has been sent");
		String nameRunFile = generateRunFile();

		connector.sendFile(nameRunFile, connSettings.getRemoteWorkDir() + nameRunFile);
		logger.info("dat.run file sent");

		String command = "cd " + connSettings.getRemoteWorkDir() + " && " + connSettings.getAmplDirectory()
				+ " dat.run";
		connector.exec(command);
		logger.info("Processing execution...");

		File file = new File(nameSolFile);
		if (!file.exists())
			file.createNewFile();

		logger.info("Solution file created");
		connector.receiveFile(nameSolFile, connSettings.getRemoteWorkDir() + "/results/risultato.heuristic.sol");
		String fileToString = FileUtils.readFileToString(file);

		String centralized = "centralized_obj = ";
		int startPos = fileToString.indexOf(centralized);
		int endPos = fileToString.indexOf('\n', startPos);
		objFunctionValue = Float.parseFloat(fileToString.substring(startPos + centralized.length(), endPos));

		System.out.println(fileToString);
		System.out.println(objFunctionValue);
		logger.info("The value of the objective function is: " + objFunctionValue); 
		return objFunctionValue;
	}

	public String generateRunFile() {

		String data =  "../scratch/data.dat";
		String result = "../results/risultato";
		String solverPath = connSettings.getSolverPath();

		StringBuilder builder = new StringBuilder();
		builder.append("reset;\n");

		builder.append("option solver \"" + solverPath + "\";\n");

		builder.append("model ../problems/models1.mod;\n");
		builder.append("data " + data + ";\n");

		builder.append("option randseed 1;\n");
		builder.append("include ../utils/compute_psi.run;\n");
		builder.append("include ../utils/compute_job_profile.run;\n");
		builder.append("include ../utils/compute_penalties.run;\n");

		builder.append("include ../utils/save_aux.run;\n");
		builder.append("let outfile := \"" + result + "\";\n");

		builder.append("include ../problems/centralized.run;\n");

		builder.append("solve centralized_prob;\n");
		builder.append("include ../utils/params_for_heuristic.run;\n");
		builder.append("include ../utils/save_centralized.run;\n");
		builder.append("include ../utils/calculedsd.run;\n");

		builder.append("include ../solve/AM_closed_form.run;\n");

		builder.append("include ../utils/simulated_time.run;\n");
		builder.append("include ../utils/save_centralized.run;\n");

		builder.append("if (solve_result_num < 200) then\n");
		builder.append("{\n");

		builder.append("include ../utils/make_integer.run;\n");

		builder.append("let outfile := (outfile & \".heuristic\");\n");
		builder.append("include ../utils/save_centralized.run;\n");
		builder.append("}\n");

		String name = "dat.run";
		File file = new File(name);

		//generating the file
		try (PrintWriter out = new PrintWriter(file)) {
			out.println(builder.toString());
			out.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return name;
	}

	public void initRemoteEnvironment() throws Exception{
		 List<String> lstProfiles = Arrays.asList(this.environment.getActiveProfiles());
		 String localPath = "src/main/resources/static/initFiles/MINLPSolver";
		 System.out.println("--------------------------------------------");
		 System.out.println("Starting math solver service initializatio phase");
		 System.out.println("--------------------------------------------");
		 if (lstProfiles.contains("test")){
			 System.out.println("Test phase: the remote work directory tree is assumed to be ok.");
			 
		 }
		 else{
			System.out.println("- Clearing remote work directory tree");
			connector.clear();
			System.out.println("- Creating new remote work directory tree");
			connector.exec("mkdir AMPL && cd AMPL && mkdir problems utils solve scratch results");
			
			System.out.println("- Sending work files");
			System.out.print("[#                    ]- Sending work files\r");
			connector.sendFile(localPath+"/problems/models1.mod",  connSettings.getRemoteWorkDir()+"/problems/models1.mod");
			System.out.print("[##                   ]- Sending work files\r");
			connector.sendFile(localPath+"/utils/compute_psi.run",  connSettings.getRemoteWorkDir()+"/utils/compute_psi.run");
			System.out.print("[###                  ]- Sending work files\r");
			connector.sendFile(localPath+"/utils/compute_job_profile.run",  connSettings.getRemoteWorkDir()+"/utils/compute_job_profile.run");
			System.out.print("[####                 ]- Sending work files\r");
			connector.sendFile(localPath+"/utils/compute_penalties.run",  connSettings.getRemoteWorkDir()+"/utils/compute_penalties.run");
			System.out.print("[#####                ]- Sending work files\r");
			connector.sendFile(localPath+"/utils/save_aux.run",  connSettings.getRemoteWorkDir()+"/utils/save_aux.run");
			System.out.print("[######               ]- Sending work files\r");
			connector.sendFile(localPath+"/problems/centralized.run",  connSettings.getRemoteWorkDir()+"/problems/centralized.run");
			System.out.print("[#######              ]- Sending work files\r");
			connector.sendFile(localPath+"/utils/params_for_heuristic.run",  connSettings.getRemoteWorkDir()+"/utils/params_for_heuristic.run");
			System.out.print("[########             ]- Sending work files\r");
			connector.sendFile(localPath+"/utils/save_centralized.run",  connSettings.getRemoteWorkDir()+"/utils/save_centralized.run");
			System.out.print("[#########            ]- Sending work files\r");
			connector.sendFile(localPath+"/utils/calculedsd.run",  connSettings.getRemoteWorkDir()+"/utils/calculedsd.run");
			System.out.print("[##########           ]- Sending work files\r");
			connector.sendFile(localPath+"/solve/AM_closed_form.run",  connSettings.getRemoteWorkDir()+"/solve/AM_closed_form.run");
			System.out.print("[###########          ]- Sending work files\r");
			connector.sendFile(localPath+"/utils/simulated_time.run",  connSettings.getRemoteWorkDir()+"/utils/simulated_time.run");
			System.out.print("[############         ]- Sending work files\r");
			connector.sendFile(localPath+"/utils/save_centralized.run",  connSettings.getRemoteWorkDir()+"/utils/save_centralized.run");
			System.out.print("[#############        ]- Sending work files\r");
			connector.sendFile(localPath+"/utils/make_integer.run",  connSettings.getRemoteWorkDir()+"/utils/make_integer.run");
			System.out.print("[##############       ]- Sending work files\r");
			connector.sendFile(localPath+"/utils/save_centralized.run",  connSettings.getRemoteWorkDir()+"/utils/save_centralized.run");
			System.out.println("Done");
		 }
	}
	public List<String> pwd() throws Exception {
		return connector.pwd();
	}
	public List<String> clear() throws Exception {
		return connector.clear();
	}
	
	@Profile("test")
	public SshConnector getConnector(){
		return connector;
	}
	
	
	
}