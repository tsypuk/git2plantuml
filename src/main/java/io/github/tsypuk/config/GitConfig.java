package io.github.tsypuk.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GitConfig {
    String repoPath;
    String resultFile;
    boolean consoleDebug;
    boolean plantumlJekyll;
    boolean showBranches;
    boolean showTreeBlob;
    boolean singleArrowTree;
    int hashLimit;
    List<String> resolve = new ArrayList<>();
}
