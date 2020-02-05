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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.eclipse.jgit.lib.Constants.R_TAGS;

@Slf4j
public class GitUml {
    static UmlPrinter umlPrinter = new UmlPrinter();
    static Set<ObjectId> processedCommits = new HashSet<>();
    static GitConfig config;

    public static void main(String[] args) throws IOException {
        ConfigService configService = new ConfigService();
        config = configService.loadConfig();
        Repository repository = openJGitRepository(config.getRepoPath());

        repository.getRefDatabase().getRefs().stream().forEach(ref -> {
            System.out.println(ref.getName() + ref.getObjectId().name());
            umlPrinter.registerRef(ref.getName(), ref.getObjectId().name());
        });

        resolve(repository, Constants.HEAD);
        config.getResolve().stream().forEach(path -> resolve(repository, path));

        resolveTags(repository);
        umlPrinter.print(config);
    }

    @SneakyThrows
    static void resolveTags(Repository repository) {
        List<Ref> tags = repository.getRefDatabase().getRefsByPrefix(R_TAGS);
        tags.forEach(tag -> {
                    //annotated
                    if (tag.getPeeledObjectId() != null) {
                        try (RevWalk revWalk = new RevWalk(repository)) {
                            RevTag revTag = revWalk.parseTag(tag.getObjectId());
                            umlPrinter.registerTag(tag.getObjectId().name(), revTag.getTagName(), revTag.getShortMessage(), tag.getPeeledObjectId().name());
                        } catch (Exception e) {
                            log.error(e.getMessage());
                        }
                    }
                }
        );
    }

    @SneakyThrows
    static void resolve(Repository repository, String object) {
        AnyObjectId anyObjectId = repository.resolve(object);
        try (RevWalk revWalk = new RevWalk(repository)) {
            RevCommit commit = revWalk.parseCommit(anyObjectId);
            recursive(commit, repository, revWalk, null);
            revWalk.dispose();
        }
    }

    @SneakyThrows
    static void recursive(RevCommit commit, Repository repository, RevWalk revWalk, RevCommit child) {
        dumpCommit(commit, repository);
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


    static public void dumpCommit(RevCommit commit, Repository repository) throws IOException {
        ObjectId commitOid = commit.getId();
        if (!processedCommits.contains(commitOid)) {
            umlPrinter.registerCommit(commit);
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
                            .append(treeWalk.getObjectId(0).name().substring(0, config.getHashLimit()))
                            .append("\n");
                    loader = repository.open(objectId);
                    OutputStream output = getStream();
                    loader.copyTo(output);
                    umlPrinter.dumpBlob(objectId.name(), output.toString());
                }
                // Update tree with filename, mdoe and oid
                umlPrinter.addInfoToTree(tree.name(), treeinfo.toString());
            }
            processedCommits.add(commitOid);
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

    public static Repository openJGitRepository(String repoPath) throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        return builder
                .setGitDir(new File(repoPath + "/.git"))
                .build();
    }
}
