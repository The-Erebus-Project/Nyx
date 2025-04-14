package io.github.vizanarkonin.nyx.Handlers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.vizanarkonin.nyx.Config.CoreCofig;

/**
 * Entry point for all file-related procedures
 */
public class FileFolderHandler {
    private static final Logger log = LogManager.getLogger("FileFolderHandler");

    public static final int DEFAULT_BUFFER_SIZE = 8192;
    
    public static Path createProjectFolder(long projectId) { return createFolder(String.format("%s/%d", CoreCofig.dataFolder, projectId)); }
    public static Path createProjectRunFolder(long projectId, long runId) { return createFolder(String.format("%s/%d/%s", CoreCofig.dataFolder, projectId, runId)); }
    public static Path createTempFolder(String name) { return createFolder(String.format("%s/%s", CoreCofig.tempFolder, name)); }
    public static File createTempFileInFolder(String fileName, Path location) {
        File file = new File(String.format("%s/%s", location.toString(), fileName));
        try {
            file.createNewFile();
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
        
        return file;
    }

    public static void deleteProjectFolder(long projectId) {
        Path folder = Paths.get(String.format("%s/%d", CoreCofig.dataFolder, projectId));
        try {
            FileUtils.deleteDirectory(new File(folder.toUri()));
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static void purgeTempFolder() { 
        try {
            FileUtils.cleanDirectory(new File(CoreCofig.tempFolder));
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static String getDataFolderPath()                    { return CoreCofig.dataFolder; }
    public static long getDataFolderPartitionTotalSizeInMb()    { return new File(CoreCofig.dataFolder).getTotalSpace() / 1024 / 1024;}
    public static long getDataFolderPartitionFreeSpaceInMb()    { return new File(CoreCofig.dataFolder).getUsableSpace() / 1024 / 1024; }
    public static long getDataFolderSizeInMb()                  { return FileUtils.sizeOfDirectory(new File(CoreCofig.dataFolder)) / 1024 / 1024;}
    public static String getTempFolderPath()                    { return CoreCofig.tempFolder; }
    public static long getTempFolderPartitionTotalSizeInMb()    { return new File(CoreCofig.tempFolder).getTotalSpace() / 1024 / 1024; }
    public static long getTempFolderPartitionFreeSpaceInMb()    { return new File(CoreCofig.tempFolder).getUsableSpace() / 1024 / 1024; }
    public static long getTempFolderSizeInMb()                  { return FileUtils.sizeOfDirectory(new File(CoreCofig.tempFolder)) / 1024 / 1024;}

    public static File getProjectRunFolder(long projectId, long runId) { return new File(String.format("%s/%d/%s", CoreCofig.dataFolder, projectId, runId)); }

    public static void unzipFileInto(File file, Path targetLocation) {
        try (ZipFile zipFile = new ZipFile(file)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                // Check if entry is a directory
                Path newFilePath = targetLocation.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(newFilePath);
                } else {
                    try (InputStream inputStream = zipFile.getInputStream(entry)) {
                        try (FileOutputStream outputStream = new FileOutputStream(targetLocation.resolve(entry.getName()).toString())) {
                            int read;
                            byte[] bytes = new byte[DEFAULT_BUFFER_SIZE];
                            while ((read = inputStream.read(bytes)) != -1) {
                                outputStream.write(bytes, 0, read);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static void zipFolder(Path sourceFolderPath, Path zipPath) {
        try {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()));
            Files.walkFileTree(sourceFolderPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    zos.putNextEntry(new ZipEntry(sourceFolderPath.relativize(file).toString()));
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                    }
                });
            zos.close();
        } catch (Exception e) {
            log.error(e);
        }
    }

    public static Path findReportsFolderIn(Path location) {
        File root = new File(location.toUri());
        File[] list = root.listFiles();

        if (list == null) return null;

        for (File f : list) {
            if (f.isDirectory()) {
                Path index = Paths.get(f.getPath() + "/index.html");
                Path app = Paths.get(f.getPath() + "/results.js");
                if (Files.exists(index) && Files.isRegularFile(index) && Files.exists(app) && Files.isRegularFile(app) ) {
                    return f.toPath();
                } else {
                    return findReportsFolderIn(f.toPath());
                }
            }
        }

        return null;
    }

    private static Path createFolder(String address) {
        try {
            return Files.createDirectories(Paths.get(address));
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
