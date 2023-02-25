package io.github.tsypuk.core;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnnotatedTag {
    private String oid;
    private int id;
    private String name;
    private String tagName;
    private String message;
    private String parrentCommitSha1;
}
