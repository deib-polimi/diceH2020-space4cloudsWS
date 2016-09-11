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
package it.polimi.diceH2020.SPACE4CloudWS.fileManagement;

import it.polimi.diceH2020.SPACE4CloudWS.fileManagement.policy.DeletionPolicy;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class FileUtility {

	private static final File LOCAL_DYNAMIC_FOLDER = new File("TempWorkingDir");
	private static Logger logger = Logger.getLogger(FileUtility.class.getName());

	@Autowired
	private DeletionPolicy policy;

	public boolean delete(Pair<File, File> pFiles) {
		return pFiles != null && delete(pFiles.getLeft()) & delete(pFiles.getRight());
	}

	public boolean delete(File file) {
		return policy.delete(file);
	}

	public @Nonnull File provideFile(@CheckForNull String fileName) throws IOException {
		File file = new File(LOCAL_DYNAMIC_FOLDER, fileName);
		policy.markForDeletion(file);
		return file;
	}

	public @Nonnull File provideTemporaryFile(@CheckForNull String prefix, String suffix) throws IOException {
		File file = File.createTempFile(prefix, suffix, LOCAL_DYNAMIC_FOLDER);
		policy.markForDeletion(file);
		return file;
	}

	public void writeContentToFile(@Nonnull String content, @Nonnull File file) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		writer.write(content);
		writer.close();
	}

	public void createWorkingDir() throws IOException {
		Path folder = LOCAL_DYNAMIC_FOLDER.toPath();
		Files.createDirectories(folder);
		logger.info(LOCAL_DYNAMIC_FOLDER + " created.");
	}

	public void destroyWorkingDir() throws IOException{
		Path folder = LOCAL_DYNAMIC_FOLDER.toPath();
		if (Files.deleteIfExists(folder)) {
			logger.info(LOCAL_DYNAMIC_FOLDER+ " deleted.");
		}
	}

	public boolean delete(List<File> pFiles) {
		return ! pFiles.isEmpty() && pFiles.stream().map(this::delete).allMatch(r -> r);
	}

	public InputStream getFileAsStream(String fileName) {
		Path filePath = new File(LOCAL_DYNAMIC_FOLDER, fileName).toPath();
		if (Files.exists(filePath)) {
			try {
				return Files.newInputStream(filePath);
			} catch (IOException e) {
				return null;
			}
		}
		else return null;
	}

	public Path getLocalDynamicFolderPath(){
		return LOCAL_DYNAMIC_FOLDER.toPath();
	}
}
