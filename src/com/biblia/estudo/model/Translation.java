package com.biblia.estudo.model;

import java.io.Serializable;

public class Translation implements Serializable {
    private long id;
    private String code;
    private String name;
    private String language;
    private String license;
    private boolean isDefault;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getLicense() { return license; }
    public void setLicense(String license) { this.license = license; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }
}