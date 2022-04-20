package ai.datasqrl.config.util;


import ai.datasqrl.config.error.ErrorCollector;

import java.io.IOException;
import java.nio.file.Path;
import org.apache.commons.io.FilenameUtils;

public class FileUtil {

    public static String getExtension(Path p) {
        return FilenameUtils.getExtension(p.getFileName().toString());
    }

    public static String removeExtension(Path p) {
        return FilenameUtils.removeExtension(p.getFileName().toString());
    }

    public static<T> T executeFileRead(Path p, ExecuteFileRead<T> exec, ErrorCollector errors) {
        try {
            return exec.execute(p);
        } catch (IOException e) {
            errors.fatal("Could not read file or directory [%s]: [%s]",p,e);
            return null;
        }
    }

    @FunctionalInterface
    public interface ExecuteFileRead<T> {

        T execute(Path p) throws IOException;

    }

}