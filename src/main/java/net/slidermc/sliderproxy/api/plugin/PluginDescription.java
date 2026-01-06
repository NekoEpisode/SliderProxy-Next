package net.slidermc.sliderproxy.api.plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * 插件描述信息，对应plugin.yml文件
 */
public class PluginDescription {
    private String name;
    private String version;
    private String main;
    private String author;
    private String description;
    private List<String> depends;
    private List<String> softDepends;
    private String apiVersion;

    public PluginDescription() {
        this.depends = new ArrayList<>();
        this.softDepends = new ArrayList<>();
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getMain() {
        return main;
    }

    public void setMain(String main) {
        this.main = main;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getDepends() {
        return depends;
    }

    public void setDepends(List<String> depends) {
        this.depends = depends != null ? depends : new ArrayList<>();
    }

    public List<String> getSoftDepends() {
        return softDepends;
    }

    public void setSoftDepends(List<String> softDepends) {
        this.softDepends = softDepends != null ? softDepends : new ArrayList<>();
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    @Override
    public String toString() {
        return "PluginDescription{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", main='" + main + '\'' +
                ", author='" + author + '\'' +
                ", description='" + description + '\'' +
                ", depends=" + depends +
                ", softDepends=" + softDepends +
                ", apiVersion='" + apiVersion + '\'' +
                '}';
    }
}
