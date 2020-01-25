package io.github.tsypuk;

import lombok.Builder;
import lombok.Data;
import org.eclipse.jgit.revwalk.RevCommit;

import java.util.*;
import java.util.function.Consumer;

public class UmlPrinter {
    boolean inCommit;
    String activeCommit;
    Commit activeCm;
    boolean inTree;
    String activeTree;
    Tree activeTr;
    Blob activeBlob;

    int commitCounter;
    int blobCounter;
    int treeCounter;

    int blobMarker;

    List<String> colors = Arrays.asList("lightgreen", "lightblue", "orange", "yellow", "red", "blue", "cyen");
    Set<Commit> commits = new HashSet<>();
    Set<Blob> blobs = new HashSet<>();
    Map<String, Tree> trees = new HashMap<>();
    Map<String, Commit> commitMap = new HashMap<>();

    Map<String, Set<String>> commitRelations = new HashMap<>();

    Map<String, String> refs = new HashMap<>();

    public void registerCommit(RevCommit commit, RevCommit childCommit) {
        commitCounter++;
        inCommit = true;
        activeCommit = "Commit" + commitCounter;
        Commit commitToAdd = Commit.builder()
                .sha1(commit.name())
                .parentCommits(new ArrayList<>())
                .message(commit.getFullMessage())
                .timeStamp(commit.getCommitTime())
                .build();
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
        treeCounter++;
        inTree = true;
        activeTree = "Tree" + treeCounter;
        Tree tree = Tree.builder()
                .sha1(sha1)
                .content(content)
                .blobs(new ArrayList<>())
                .build();
        activeTr = tree;
        activeCm.setTree(tree);
        trees.put(sha1, tree);
    }

    public void dumpBlob(String sha1, String content) {
        Blob blob = Blob.builder()
                .sha1(sha1)
                .content(content)
                .build();


        if (!blobs.contains(blob)) {
            blobCounter++;
            blobs.add(blob);
        }
        blob = blobs.stream().filter(blob::equals).findAny().orElse(blob);
        activeBlob = blob;
        activeTr.getBlobs().add(blob);
    }

    public void print() {
        blobCounter = 0;
        List<Commit> commitsList = new ArrayList<>(commits);
        commitsList.sort(Comparator.comparingInt(Commit::getTimeStamp).reversed());
        for (int i = commitsList.size() - 1; i >= 0; i--) {
            Commit commit = commitsList.get(i);
            commit.setNodeName("Commit" + (commitsList.size() - i));
            commit.setId(commitsList.size() - i);
            commit.setColor(colors.get(commitsList.size() - i - 1));
            Tree tree = commit.getTree();
            tree.setTreeName("Tree" + (commitsList.size() - i));
            tree.getBlobs().forEach(blob -> {
                if (blob.getId() == 0) {
                    blob.setBlobName("Blob" + ++blobMarker);
                    blob.setId(blobMarker);
                }
            });
        }

        //Commit
        commitsList.forEach(drawCommit);

        //Tree
        commitsList.stream().forEach(commit -> drawTree(commit.getTree(), commit.getId()));

        drawBlobs();

        //Commit->Commit
        commitsList.stream().forEach(commit -> {
            if (commit.getParentCommits().size() > 0) {
                commit.getParentCommits().forEach(
                        parentCommit -> drawRelation(commit.getNodeName(), parentCommit, false));
            }
        });

        //Commit->Tree
        commitsList.stream().forEach(commit -> drawRelation(commit.getNodeName(), commit.getTree().getTreeName(), false));

        //Tree-Blob
        commitsList.stream().forEach(commit -> {
            Tree tree = commit.getTree();
            tree.getBlobs().stream().forEach(blob -> {
                        drawRelation(tree.getTreeName(), blob.getBlobName(), false);
                    }
            );
        });

        commitRelations.forEach((parent, childList) -> childList.stream().forEach(child -> drawCommitRelation(parent, child)));

        //Branches and Tags (Refs)
        refs.forEach((refName, oid) -> {
            if (commitMap.get(oid) == null) {
                System.err.println(oid + " NOT FOUND");
                return;
            }
            System.out.println("note top of " + commitMap.get(oid).getNodeName() + " #" + commitMap.get(oid).getColor() + " : " + refName);
        });
    }

    private Consumer<Commit> drawCommit = commit -> {
        System.out.println("class " + commit.getNodeName() + " <<(C," + commit.getColor() + ")>> {");
        System.out.println("-" + commit.getSha1());
        System.out.println("--");
        System.out.println(commit.getMessage());
        System.out.println("}");
    };

    private void drawTree(Tree tree, int id) {
        System.out.println("class Tree" + id + " <<(T," + colors.get(id - 1) + ")>> {");
        System.out.println("-" + tree.getSha1());
        System.out.println("--");
        System.out.println(tree.getContent());
        System.out.println("}");
    }

    private void drawBlobs() {
        blobs.stream().forEach(
                blob -> {
                    System.out.println("class Blob" + blob.getId() + " <<(B," + colors.get(blob.getId() - 1) + ")>> {");
                    System.out.println("-" + blob.getSha1());
                    System.out.println("--");
                    System.out.println(blob.getContent());
                    System.out.println("}");
                }
        );
    }

    private void drawCommitRelation(String sha1, String sha2) {
        if ((sha1 == null) || (sha2 == null)) {
            return;
        }
        drawRelation(commitMap.get(sha1).getNodeName(), commitMap.get(sha2).getNodeName(), true);
    }

    private void drawRelation(String object1, String object2, boolean left) {
        if (left) {
            System.out.println(object1 + " -l-> " + object2);
        } else {
            System.out.println(object1 + " --|> " + object2);
        }
    }

    public void drawRefs() {

    }

    public void addInfoToTree(String oid, String info) {
        trees.get(oid).setContent(info);
    }
}

@Data
@Builder
class Commit {
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
        if (this == o) return true;
        if (!(o instanceof Commit)) return false;

        Commit commit = (Commit) o;

        return sha1.equals(commit.sha1);
    }

    @Override
    public int hashCode() {
        return sha1.hashCode();
    }
}

@Data
@Builder
class Tree {
    private String sha1;
    private int id;
    private String treeName;
    private String content;
    private List<Blob> blobs;
}

@Data
@Builder
class Blob {
    private String sha1;
    private int id;
    private String blobName;
    private String content;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Blob)) return false;

        Blob blob = (Blob) o;

        return sha1.equals(blob.sha1);
    }

    @Override
    public int hashCode() {
        return sha1.hashCode();
    }
}
