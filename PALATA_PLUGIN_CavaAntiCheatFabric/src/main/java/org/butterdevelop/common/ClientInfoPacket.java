package org.butterdevelop.common;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ClientInfoPacket implements Serializable {

    private final Map<String, String> modsChecksumMap    = new HashMap<>();
    private final Map<String, String> textureChecksumMap = new HashMap<>();
    private final Map<String, String> shadersChecksumMap = new HashMap<>();
    private InspectionResult modsInspectionResult;
    private String modsInspectionMessage;
    private InspectionResult textureInspectionResult;
    private String textureInspectionMessage;
    private InspectionResult shadersInspectionResult;
    private String shadersInspectionMessage;

    public Map<String, String> getModsChecksumMap() {
        return Collections.unmodifiableMap(modsChecksumMap);
    }

    public Map<String, String> getTextureChecksumMap() {
        return Collections.unmodifiableMap(textureChecksumMap);
    }

    public Map<String, String> getShadersChecksumMap() {
        return Collections.unmodifiableMap(shadersChecksumMap);
    }

    public void addModChecksum(String modId, String checksum){
        modsChecksumMap.put(modId, checksum);
    }

    public void addTextureChecksum(String textureName, String checksum){
        textureChecksumMap.put(textureName, checksum);
    }

    public void addShadersChecksum(String shaderName, String checksum){
        shadersChecksumMap.put(shaderName, checksum);
    }

    public void setModsInspectionResult(InspectionResult modsInspectionResult) {
        this.modsInspectionResult = modsInspectionResult;
    }

    public void setModsInspectionMessage(String modsInspectionMessage){
        this.modsInspectionMessage = modsInspectionMessage;
    }

    public InspectionResult getModsInspectionResult() {
        return modsInspectionResult;
    }

    public String getModsInspectionMessage() {
        return modsInspectionMessage;
    }

    public InspectionResult getTextureInspectionResult() {
        return textureInspectionResult;
    }

    public void setTextureInspectionResult(InspectionResult textureInspectionResult) {
        this.textureInspectionResult = textureInspectionResult;
    }

    public String getTextureInspectionMessage() {
        return textureInspectionMessage;
    }

    public void setTextureInspectionMessage(String textureInspectionMessage) {
        this.textureInspectionMessage = textureInspectionMessage;
    }

    public String getShadersInspectionMessage() {
        return shadersInspectionMessage;
    }

    public void setShadersInspectionMessage(String shadersInspectionMessage) {
        this.shadersInspectionMessage = shadersInspectionMessage;
    }

    public InspectionResult getShadersInspectionResult() {
        return shadersInspectionResult;
    }

    public void setShadersInspectionResult(InspectionResult shadersInspectionResult) {
        this.shadersInspectionResult = shadersInspectionResult;
    }
}
