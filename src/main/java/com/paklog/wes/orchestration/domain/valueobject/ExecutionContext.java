package com.paklog.wes.orchestration.domain.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Execution context for workflow runtime state
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionContext implements Serializable {

    @Builder.Default
    private Map<String, Object> variables = new HashMap<>();

    @Builder.Default
    private Map<String, String> headers = new HashMap<>();

    @Builder.Default
    private Map<String, Object> systemProperties = new HashMap<>();

    public void set(String key, Object value) {
        this.variables.put(key, value);
    }

    public Object get(String key) {
        return this.variables.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = this.variables.get(key);
        if (value != null && type.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        return null;
    }

    public void setHeader(String key, String value) {
        this.headers.put(key, value);
    }

    public String getHeader(String key) {
        return this.headers.get(key);
    }

    public void setSystemProperty(String key, Object value) {
        this.systemProperties.put(key, value);
    }

    public Object getSystemProperty(String key) {
        return this.systemProperties.get(key);
    }

    public boolean hasVariable(String key) {
        return this.variables.containsKey(key);
    }

    public void remove(String key) {
        this.variables.remove(key);
    }

    public void clear() {
        this.variables.clear();
        this.headers.clear();
        this.systemProperties.clear();
    }

    public void merge(ExecutionContext other) {
        if (other != null) {
            this.variables.putAll(other.variables);
            this.headers.putAll(other.headers);
            this.systemProperties.putAll(other.systemProperties);
        }
    }
}