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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Component
public class FileUtility {

	private static final File LOCAL_DYNAMIC_FOLDER = new File("TempWorkingDir");
	private static final File LOCAL_INPUT_FOLDER = new File(LOCAL_DYNAMIC_FOLDER+File.separator+"Input");
	private static Logger logger = Logger.getLogger(FileUtility.class.getName());

	@Autowired
	private DeletionPolicy policy;

	public boolean delete(Pair<File, File> pFiles) {
		return pFiles != null && delete(pFiles.getLeft()) & delete(pFiles.getRight());
	}

	public boolean delete(File file) {
		return policy.delete(file);
	}

	public @Nonnull File provideFile(@CheckForNull String subFolder,@CheckForNull String fileName) throws IOException {
		File file = new File(LOCAL_INPUT_FOLDER+File.separator+subFolder, fileName);
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
	
	public void destroyDir(String path) throws IOException{
			
		Path directory = Paths.get(path);
		
	   Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
		   @Override
		   public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			   Files.delete(file);
			   return FileVisitResult.CONTINUE;
		   }
	
		   @Override
		   public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			   Files.delete(dir);
			   return FileVisitResult.CONTINUE;
		   }
	
	   });
		   
		logger.info("Deleted Input SubFolder: "+path);
		
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
	
	public String createInputSubFolder(String folder) throws IOException{
		File file = new File(LOCAL_INPUT_FOLDER, folder);
		file.mkdirs();
		System.out.println("ASNARESSS"+file.getCanonicalPath());
		return file.getCanonicalPath();
	}
	
	public String generateUniqueString() {
		//String uuid = UUID.randomUUID().toString().replaceAll("-", "");
		Date dNow = new Date( );
		SimpleDateFormat ft = new SimpleDateFormat ("Edd-MM-yyyy_HH-mm-ss");
		Random random = new Random();
		String id = ft.format(dNow)+random.nextInt(99999);
		return id;
	}
	
	
	public static String[] listFile(String folder, String ext) {

		GenericExtFilter filter = new GenericExtFilter(ext);
		File dirInput = new File(folder);

		String[] list = dirInput.list(filter);

		return list;
	}
	
	public static class GenericExtFilter implements FilenameFilter {

		private String ext;

		public GenericExtFilter(String ext) {
			this.ext = ext;
		}

		public boolean accept(File dir, String name) {
			return (name.endsWith(ext));
		}
	}
}
