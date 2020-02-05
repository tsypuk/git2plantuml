package io.github.tsypuk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConfigService {

    private GitConfig gitConfig = new GitConfig();

    GitConfig loadConfig() throws FileNotFoundException {
        Yaml yaml = new Yaml();
        InputStream ios = new FileInputStream(new File("config.yml"));
        Map<String, Object> obj = yaml.load(ios);
        gitConfig.setRepoPath((String) obj.get("repoPath"));
        gitConfig.setDetailed((Boolean) obj.get("detailed"));
        gitConfig.setResolve((List<String>) obj.get("resolve"));
        return gitConfig;
    }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class GitConfig {
    String repoPath;
    boolean detailed;
    List<String> resolve = new ArrayList<>();
}
