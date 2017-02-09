/*
Copyright 2016-2017 Eugenio Gianniti
Copyright 2016 Michele Ciavotta

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
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Component
public class FileUtility {

    @Autowired
    private DeletionPolicy policy;

    @Autowired
    private Settings settings;

    public boolean delete(@NotNull Pair<File, File> pFiles) {
        boolean deletedLeft = delete(pFiles.getLeft());
        return delete(pFiles.getRight()) && deletedLeft;
    }

    public boolean delete(@NotNull File file) {
        return policy.delete(file);
    }

    public @NotNull File provideFile(@NotNull String subFolder, @NotNull String fileName) throws IOException {
        File file = new File(new File(settings.getInputDirectory(), subFolder), fileName);
        policy.markForDeletion(file);
        return file;
    }

    public @NotNull	File provideTemporaryFile(@NotNull String prefix, String suffix) throws IOException {
        File file = File.createTempFile(prefix, suffix, settings.getWorkingDirectory());
        policy.markForDeletion(file);
        return file;
    }

    public void writeContentToFile(@NotNull String content, @NotNull File file) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(content);
        }
    }

    public void createWorkingDir() throws IOException {
        Path folder = settings.getWorkingDirectory().toPath();
        Files.createDirectories(folder);
    }

    public boolean destroyDir(@NotNull File path) throws IOException {
        Path directory = path.toPath ();

        @Getter
        @Setter
        class BooleanWrapper {
            boolean deleted;
        }
        BooleanWrapper status = new BooleanWrapper ();

        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                status.setDeleted (policy.delete (file.toFile ()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                status.setDeleted (policy.delete (dir.toFile ()));
                return FileVisitResult.CONTINUE;
            }
        });

        return status.isDeleted ();
    }

    public boolean delete(@NotNull List<File> pFiles) {
        return ! pFiles.isEmpty() && pFiles.stream().map(this::delete).allMatch(r -> r);
    }

    public String createInputSubFolder(@NotNull String folder) throws IOException {
        File file = new File(settings.getInputDirectory(), folder);
        file.mkdirs();
        return file.getCanonicalPath();
    }

    public String generateUniqueString() {
        Date dNow = new Date( );
        SimpleDateFormat ft = new SimpleDateFormat ("Edd-MM-yyyy_HH-mm-ss");
        Random random = new Random();
        return ft.format(dNow)+random.nextInt(99999);
    }

    public String[] listFile(@NotNull File folder, @NotNull String ext) {
        GenericExtFilter filter = new GenericExtFilter(ext);
        return folder.list(filter);
    }

    private static class GenericExtFilter implements FilenameFilter {

        private String ext;

        GenericExtFilter(String ext) {
            this.ext = ext;
        }

        public boolean accept(File dir, String name) {
            return (name.endsWith(ext));
        }
    }
}
