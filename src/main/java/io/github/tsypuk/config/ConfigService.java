package io.github.tsypuk.config;

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class ConfigService {

    private GitConfig gitConfig = new GitConfig();

    public GitConfig loadConfig()
            throws FileNotFoundException {
        Yaml yaml = new Yaml();
        InputStream ios = new FileInputStream("config.yml");
        Map<String, Object> obj = yaml.load(ios);
        gitConfig.setRepoPath((String) obj.get("repo-path"));
        gitConfig.setResultFile((String) obj.get("result-file"));
        gitConfig.setShowBranches((Boolean) obj.get("show-branches"));
        gitConfig.setShowTreeBlob((Boolean) obj.get("show-tree-blob"));
        gitConfig.setConsoleDebug((Boolean) obj.get("console-debug"));
        gitConfig.setPlantumlJekyll((Boolean) obj.get("plantuml-jekyll"));
        gitConfig.setSingleArrowTree((Boolean) obj.get("single-arrow-tree"));
        gitConfig.setHashLimit((Integer) obj.get("hash-limit"));
        gitConfig.setResolve((List<String>) obj.get("resolve"));
        return gitConfig;
    }
}

