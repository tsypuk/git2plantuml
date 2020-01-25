package io.github.tsypuk;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

@Slf4j
public class GitUml {
    static UmlPrinter umlPrinter = new UmlPrinter();

    public static void main(String[] args) throws IOException {
        Repository repository = openJGitRepository();

        repository.getRefDatabase().getRefs().stream().forEach(System.out::println);

        ObjectId lastCommitId = repository.resolve(Constants.HEAD);
        try (RevWalk revWalk = new RevWalk(repository)) {
            RevCommit commit = revWalk.parseCommit(lastCommitId);
            recursive(commit, repository, revWalk, null);
            revWalk.dispose();
        }
        umlPrinter.print();
    }

    @SneakyThrows
    static void recursive(RevCommit commit, Repository repository, RevWalk revWalk, RevCommit child) {
        dumpCommit(commit, repository, child);
        System.out.println(commit.name() + " ->" + ((child != null) ? child.name() : "top"));
        Arrays.asList(commit.getParents()).stream()
                .forEach(cm -> {
                    recursive(getCommit(revWalk, repository, cm), repository, revWalk, commit);
                });
    }

    @SneakyThrows
    static RevCommit getCommit(RevWalk revWal, Repository repository, RevCommit revCommit) {
        return revWal.parseCommit(repository.resolve(revCommit.name()));
    }


    static public void dumpCommit(RevCommit commit, Repository repository, RevCommit child) throws IOException {
        umlPrinter.registerCommit(commit, child);
        RevTree tree = commit.getTree();
        ObjectLoader loader = repository.open(tree);
        OutputStream outputStream = getStream();
        loader.copyTo(outputStream);
        umlPrinter.dumpTree(tree.name(), outputStream.toString());

        StringBuilder treeinfo = new StringBuilder();
        try (TreeWalk treeWalk = new TreeWalk(repository)) {
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);

            while (treeWalk.next()) {
                ObjectId objectId = treeWalk.getObjectId(0);
                if (objectId.name().equals("0000000000000000000000000000000000000000")) {
                    break;
                }
                treeinfo.append("# ")
                        .append(treeWalk.getPathString())
                        .append(" ")
                        .append(treeWalk.getFileMode(0))
                        .append(" ")
                        .append(treeWalk.getObjectId(0).name())
                        .append("\n");
                loader = repository.open(objectId);
                OutputStream output = getStream();
                loader.copyTo(output);
                umlPrinter.dumpBlob(objectId.name(), output.toString());
            }
            // Update tree with filename, mdoe and oid
            umlPrinter.addInfoToTree(tree.name(), treeinfo.toString());
        }
    }

    public static OutputStream getStream() {
        return new OutputStream() {
            private StringBuilder string = new StringBuilder();

            @Override
            public void write(int b) {
                this.string.append((char) b);
            }

            public String toString() {
                return this.string.toString();
            }
        };
    }

    public static Repository openJGitRepository() throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        return builder
                .setGitDir(new File("/Users/rtsypuk/projects/personal/git/branches/.git"))
                .build();
    }
}
