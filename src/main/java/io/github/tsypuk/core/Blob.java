package io.github.tsypuk.core;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Blob {
    private String sha1;
    private int id;
    private String blobName;
    private String content;
    private String color;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Blob)) {
            return false;
        }

        Blob blob = (Blob) o;

        return sha1.equals(blob.sha1);
    }

    @Override
    public int hashCode() {
        return sha1.hashCode();
    }
}
