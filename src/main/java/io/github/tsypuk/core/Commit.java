package io.github.tsypuk.core;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class Commit {
    private String sha1;
    private int timeStamp;
    private int id;
    private String nodeName;
    private String content;
    private String message;
    private String color;
    private Tree tree;
    private List<String> parentCommits;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Commit)) {
            return false;
        }

        Commit commit = (Commit) o;

        return sha1.equals(commit.sha1);
    }

    @Override
    public int hashCode() {
        return sha1.hashCode();
    }
}
