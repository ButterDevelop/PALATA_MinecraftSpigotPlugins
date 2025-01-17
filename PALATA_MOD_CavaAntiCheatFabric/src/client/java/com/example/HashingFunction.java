package com.example;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashingFunction {

    public static String getFileChecksum(String path) throws NoSuchAlgorithmException, IOException {
        return getFileChecksum(new File(path));
    }

    public static String getFileChecksum(File file) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        // Получаем поток для чтения содержимого файла
        FileInputStream fis = new FileInputStream(file);

        // Читаем файл частями
        byte[] byteArray = new byte[1024];
        int bytesCount;

        while ((bytesCount = fis.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        }
        fis.close();

        // Получаем байты хеша
        byte[] bytes = digest.digest();

        // Преобразуем байты в шестнадцатеричную строку
        StringBuilder sb = new StringBuilder();
        for (byte aByte : bytes) {
            sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }
}
