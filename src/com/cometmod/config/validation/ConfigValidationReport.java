package com.cometmod.config.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregates config validation results with actionable messages.
 */
public class ConfigValidationReport {

    private final List<String> infos = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    public void info(String message) {
        infos.add(message);
    }

    public void warn(String message) {
        warnings.add(message);
    }

    public void error(String message) {
        errors.add(message);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public boolean isClean() {
        return infos.isEmpty() && warnings.isEmpty() && errors.isEmpty();
    }

    public List<String> getInfos() {
        return Collections.unmodifiableList(infos);
    }

    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }
}
