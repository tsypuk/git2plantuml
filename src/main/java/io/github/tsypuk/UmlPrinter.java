package io.github.tsypuk;

import io.github.tsypuk.config.GitConfig;
import io.github.tsypuk.core.AnnotatedTag;
import io.github.tsypuk.core.Blob;
import io.github.tsypuk.core.Commit;
import io.github.tsypuk.core.Tree;
import io.github.tsypuk.writer.ConsoleOutput;
import io.github.tsypuk.writer.JekyllFileWriter;
import io.github.tsypuk.writer.PlantUMLFileWriter;
import io.github.tsypuk.writer.ResultsWriter;
import org.eclipse.jgit.revwalk.RevCommit;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UmlPrinter {

    boolean inCommit;
    GitConfig config;
    List<ResultsWriter> resultsWriters;

    String activeCommit;
    Commit activeCm;
    Tree activeTr;
    Blob activeBlob;

    int commitCounter;
    int blobCounter;
    int treeCounter;
    int annotatedTagCounter;

    int blobMarker;

    List<String> colors = Arrays.asList("lightgreen", "lightblue", "orange", "yellow", "pink", "red");
    Set<Commit> commits = new HashSet<>();
    Set<Blob> blobs = new HashSet<>();
    Map<String, Tree> trees = new HashMap<>();
    Map<String, Commit> commitMap = new HashMap<>();

    Map<String, Set<String>> commitRelations = new HashMap<>();

    Map<String, String> refs = new HashMap<>();

    Map<String, AnnotatedTag> annotatedTagMap = new HashMap<>();

    public void registerCommit(RevCommit commit) {
        commitCounter++;
        inCommit = true;
        activeCommit = "Commit" + commitCounter;
        Commit commitToAdd = Commit.builder().sha1(commit.name()).parentCommits(new ArrayList<>()).message(commit.getFullMessage()).timeStamp(commit.getCommitTime()).build();
        commits.add(commitToAdd);
        activeCm = commitToAdd;

        commitMap.putIfAbsent(commitToAdd.getSha1(), commitToAdd);
    }

    public void registerRef(String refName, String oid) {
        refs.putIfAbsent(refName, oid);
    }

    public void registerCommitRelation(String sha1, String sha2) {
        Set<String> strings1 = commitRelations.get(sha1);
        if (strings1 != null) {
            strings1.add(sha2);
        } else {
            Set<String> commits = new HashSet<>();
            commits.add(sha2);
            commitRelations.put(sha1, commits);
        }
    }

    public void dumpTree(String sha1, String content) {
        Tree tree = trees.get(sha1);
        if (tree == null) {
            treeCounter++;
            tree = Tree.builder().sha1(sha1).content(content).blobs(new ArrayList<>()).build();
            trees.put(sha1, tree);
        }
        activeTr = tree;
        activeCm.setTree(tree);
    }

    public void dumpBlob(String sha1, String content) {
        Blob blob = Blob.builder().sha1(sha1).content(content).build();

        if (!blobs.contains(blob)) {
            blobCounter++;
            blobs.add(blob);
        }
        blob = blobs.stream().filter(blob::equals).findAny().orElse(blob);
        activeBlob = blob;
        activeTr.getBlobs().add(blob);
    }

    private String resolveColor(int index) {
        int id = index % (colors.size());
        return colors.get(id);
    }

    public void print(GitConfig config) {
        this.config = config;
        resultsWriters = new ArrayList<>();
        resultsWriters.add(new PlantUMLFileWriter(config));
        if (config.isConsoleDebug()) {
            resultsWriters.add(new ConsoleOutput());
        }
        if (config.isPlantumlJekyll()) {
            resultsWriters.add(new JekyllFileWriter(config));
        }

        blobCounter = 0;
        List<Commit> commitsList = new ArrayList<>(commits);
        commitsList.sort(Comparator.comparingInt(Commit::getTimeStamp).reversed());
        for (int i = commitsList.size() - 1; i >= 0; i--) {
            Commit commit = commitsList.get(i);
            commit.setNodeName("Commit" + (commitsList.size() - i));
            commit.setId(commitsList.size() - i);
            commit.setColor(resolveColor(commitsList.size() - i - 1));
            Tree tree = commit.getTree();
            if (tree.getId() == 0) {
                tree.setId(commit.getId());
                tree.setTreeName("Tree" + tree.getId());
            }
            tree.getBlobs().forEach(blob -> {
                if (blob.getId() == 0) {
                    blob.setBlobName("Blob" + ++blobMarker);
                    blob.setId(blobMarker);
                    blob.setColor(commit.getColor());
                }
            });
        }

        printUmlHeader();
        //Commit
        commitsList.forEach(drawCommit);

        if (config.isShowTreeBlob()) {
            //Tree
            commitsList.stream().map(Commit::getTree).distinct().forEach(this::drawTree);

            drawBlobs();
        }

        drawAnnotatedTags();
        //Annotated Tag-Commit
        annotatedTagMap.forEach((oid, tag) -> {
            drawRelation(tag.getTagName(), commitMap.get(tag.getParrentCommitSha1()).getNodeName(), true);
        });

        //Commit->Commit
        commitsList.stream().forEach(commit -> {
            if (commit.getParentCommits().size() > 0) {
                commit.getParentCommits().forEach(parentCommit -> drawRelation(commit.getNodeName(), parentCommit, false));
            }
        });

        if (config.isShowTreeBlob()) {
            //Commit->Tree
            commitsList.stream().forEach(commit -> drawRelation(commit.getNodeName(), commit.getTree().getTreeName(), false));

            //Tree-Blob
            commitsList.stream().map(commit -> commit.getTree()).distinct().forEach(tree -> checkDistinct(tree.getBlobs().stream(), config.isSingleArrowTree()).forEach(blob -> {
                drawRelation(tree.getTreeName(), blob.getBlobName(), false);
            }));
        }
        commitRelations.forEach((parent, childList) -> childList.stream().forEach(child -> drawCommitRelation(parent, child)));

        if (config.isShowBranches()) {
            //Branches and Tags (Refs)
            refs.forEach((refName, oid) -> {
                if (commitMap.get(oid) == null) {
                    System.err.println(oid + " NOT FOUND in COMMITS");
                    if (annotatedTagMap.get(oid) == null) {
                        System.err.println(oid + "NOT FOUND in TAGS");
                        return;
                    } else {
                        print("note top of " + annotatedTagMap.get(oid).getTagName() + " #red : " + refName);
                    }
                } else {
                    print("note top of " + commitMap.get(oid).getNodeName() + " #" + commitMap.get(oid).getColor() + " : " + refName);
                }
            });
        }

        resultsWriters.forEach(it -> it.endSection());
    }

    private Stream<Blob> checkDistinct(Stream<Blob> stream, boolean flag) {
        return (flag) ? stream.distinct() : stream;
    }

    private void print(String text) {
        resultsWriters.forEach(it -> it.writeOutput(text));
    }

    private Consumer<Commit> drawCommit = commit -> {
        print("class " + commit.getNodeName() + " <<(C," + commit.getColor() + ")>> {");
        print("-sha: " + commit.getSha1().substring(0, config.getHashLimit()));
        print("--");
        print("message: " + commit.getMessage());
        print("--");
        print("timestamp: " + commit.getTimeStamp());
        print("}");
    };

    private void drawTree(Tree tree) {
        print("class Tree" + tree.getId() + " <<(T," + resolveColor(tree.getId() - 1) + ")>> {");
        print("-sha: " + tree.getSha1().substring(0, config.getHashLimit()));
        print("--");
        print(tree.getContent());
        print("}");
    }

    private void drawBlobs() {
        blobs.stream().forEach(blob -> {
            print("class Blob" + blob.getId() + " <<(B," + blob.getColor() + ")>> {");
            print("-sha: " + blob.getSha1().substring(0, config.getHashLimit()));
            print("--");
            print(blob.getContent());
            print("}");
        });
    }

    private void drawAnnotatedTags() {
        annotatedTagMap.forEach((oid, tag) -> {
            print("class Tag" + tag.getId() + " <<(T,red)>> {");
            print("-sha: " + tag.getOid().substring(0, config.getHashLimit()));
            print("--");
            print(tag.getName());
            print("--");
            print(tag.getMessage());
            print("}");
        });
    }

    private void drawCommitRelation(String sha1, String sha2) {
        if ((sha1 == null) || (sha2 == null)) {
            return;
        }
        drawRelation(commitMap.get(sha1).getNodeName(), commitMap.get(sha2).getNodeName(), true);
    }

    private void drawRelation(String object1, String object2, boolean left) {
        if (left) {
            print(object1 + " -l-> " + object2);
        } else {
            print(object1 + " --> " + object2);
        }
    }

    public void addInfoToTree(String oid, String info) {
        trees.get(oid).setContent(info);
    }

    public void registerTag(String oid, String tagName, String shortMessage, String parrentCommitSHA1) {
        annotatedTagCounter++;
        annotatedTagMap.putIfAbsent(oid, AnnotatedTag.builder().oid(oid).id(annotatedTagCounter).tagName("Tag" + annotatedTagCounter).name(tagName).message(shortMessage).parrentCommitSha1(parrentCommitSHA1).build());
    }

    private void printUmlHeader() {
        StringBuilder title = new StringBuilder().append("Git repository snapshot: ").append(commits.size()).append(" commits, ").append(trees.size()).append(" trees, ").append(blobs.size()).append(" blobs, ").append(refs.size()).append(" refs: ");
        title.append(refs.keySet().stream().collect(Collectors.joining(",")));
        resultsWriters.forEach(it -> it.startSection("[plantuml, " + UUID.randomUUID() + ", png, title=\"" + title.toString() + "\", width=1000, height=1000]"));
    }

}
