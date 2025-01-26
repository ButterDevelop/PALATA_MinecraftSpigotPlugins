package com.example;

import java.io.Serializable;

public enum InspectionResult implements Serializable {
    IO_EXCEPTION,
    FILE_NOT_FOUND,
    HASH_FUNCTION_NOT_FOUND,
    INVALID_TEXTURE_FORMAT,
    INVALID_SHADER_FORMAT,
    NORMAL;
}
