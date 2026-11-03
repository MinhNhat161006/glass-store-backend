package com.glassstore.lab.config;

import com.glassstore.lab.entity.LabJobStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToLabJobStatusConverter implements Converter<String, LabJobStatus> {

    @Override
    public LabJobStatus convert(String source) {
        if (source == null || source.trim().isEmpty()) {
            return null;
        }
        return LabJobStatus.fromString(source);
    }
}

