package io.github.tsypuk.core;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class Tree {
    private String sha1;
    private int id;
    private String treeName;
    private String content;
    private List<Blob> blobs;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Tree)) {
            return false;
        }

        Tree tree = (Tree) o;

        return sha1.equals(tree.sha1);
    }

    @Override
    public int hashCode() {
        return sha1.hashCode();
    }
}
