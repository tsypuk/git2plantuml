package io.github.tsypuk;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import static io.github.tsypuk.UmlPrinter.hashLimit;
import static org.eclipse.jgit.lib.Constants.R_TAGS;

@Slf4j
public class GitUml {
    static UmlPrinter umlPrinter = new UmlPrinter();

    public static void main(String[] args) throws IOException {
        Repository repository = openJGitRepository();

        repository.getRefDatabase().getRefs().stream().forEach(ref -> {
            System.out.println(ref.getName() + ref.getObjectId().name());
            umlPrinter.registerRef(ref.getName(), ref.getObjectId().name());
        });

        resolve(repository, repository.resolve(Constants.HEAD));
//        resolve(repository, repository.resolve("993bb5b58455f0e92dc8311fef8e6b961d68c0c0"));
//        resolve(repository, repository.resolve("c59e17152a12f6e8b080255eb6706d8c8cfb175c"));
//        resolve(repository, repository.resolve("23f73fa3ba852c2168a0ed49d72be74f8cf427b6"));
//        resolve(repository, repository.resolve("c86fa2c2cf08d35a4eea5939fc9b54f5f27f1549"));
//        resolve(repository, repository.resolve("9681605"));
//        System.err.println("TAGS=====");
        resolveTags(repository);
//        System.err.println("TAGS=====");
        umlPrinter.print();
    }

    static void resolveTags(Repository repository) throws IOException {
        List<Ref> tags = repository.getRefDatabase().getRefsByPrefix(R_TAGS);
        tags.forEach(tag -> {
                    //annotated
                    if (tag.getPeeledObjectId() != null) {
                        try (RevWalk revWalk = new RevWalk(repository)) {
                            RevTag revTag = revWalk.parseTag(tag.getObjectId());
                            umlPrinter.registerTag(tag.getObjectId().name(), revTag.getTagName(), revTag.getShortMessage(), tag.getPeeledObjectId().name());
                        } catch (Exception e) {
                            System.err.println(e);
                        }
                    }
                }
        );
    }
    /*
    Map<String, Ref> tags = repository.getTags()
    ref = repository.peel(ref)
    ObjectId taggedObject = ref.getObjectId();
     */

    static void resolve(Repository repository, AnyObjectId anyObjectId) throws IOException {
        try (RevWalk revWalk = new RevWalk(repository)) {
            RevCommit commit = revWalk.parseCommit(anyObjectId);
            recursive(commit, repository, revWalk, null);
            revWalk.dispose();
        }
    }

    @SneakyThrows
    static void recursive(RevCommit commit, Repository repository, RevWalk revWalk, RevCommit child) {
        dumpCommit(commit, repository, child);
        if (child != null) {
            umlPrinter.registerCommitRelation(child.name(), commit.name());
        }
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
                        .append(treeWalk.getObjectId(0).name().substring(0, hashLimit))
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
                .setGitDir(new File("/Users/rtsypuk/projects/personal/git/repo/.git"))
                .build();
    }
}
