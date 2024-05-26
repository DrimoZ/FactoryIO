package com.drimoz.factoryio.shared;

import com.drimoz.factoryio.FactoryIO;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FactoryIOPaths {
    public static final Path LOCK_FILE = createCustomPath("");
    public static final Path INSERTERS = createCustomPath("inserters/");
    public static final Path CONVOYERS = createCustomPath("convoyers/");


    private static Path createCustomPath(String pathName) {
        Path customPath = Paths.get(FactoryIOUtils.getConfigDirectory().toAbsolutePath().toString(), FactoryIO.MOD_ID, pathName);
        createDirectory(customPath, pathName);
        return customPath;
    }

    private static void createDirectory(Path path, String dirName) {
        try {
            Files.createDirectories(path);
        } catch (FileAlreadyExistsException ignored) {
        } catch (IOException e) {
            System.out.println("Failed to create \"" + dirName + "\" directory");
        }
    }
}
