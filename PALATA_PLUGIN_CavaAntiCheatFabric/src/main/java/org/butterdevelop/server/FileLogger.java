package org.butterdevelop.server;

import org.bukkit.Bukkit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;

public class FileLogger {

    private final File file;
    private FileWriter fileWriter;

    public FileLogger(String filePath) throws IOException {
        this(new File(filePath));
    }

    public FileLogger(File file) throws IOException {
        this.file = file;
        // Инициализируем FileWriter в режиме дозаписи (append)
        this.fileWriter = new FileWriter(file, true);
    }

    /**
     * Записывает сообщение в файл и сбрасывает буфер.
     *
     * @param message Сообщение для записи.
     */
    public void log(String message) {
        try {
            fileWriter.append(message);
            fileWriter.flush();
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Error while logging to file: " + message, e);
        }
    }

    /**
     * Закрывает FileWriter, освобождая ресурсы.
     */
    public void close() {
        if (fileWriter != null) {
            try {
                fileWriter.close();
            } catch (IOException e) {
                Bukkit.getLogger().log(Level.SEVERE, "Error while closing the logger stream", e);
            }
        }
    }

    /**
     * Открывает FileWriter в режиме дозаписи.
     *
     * @throws IOException если не удалось открыть поток
     */
    public void open() throws IOException {
        // Если writer уже открыт, закрываем перед повторным открытием
        close();
        this.fileWriter = new FileWriter(file, true);
    }
}
