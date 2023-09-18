package ru.hse.mit.git;

import org.junit.jupiter.api.Test;

/*
 * Т.к. в коммитах при каждом новом запуске получаются разные хеши и
 *   разное время отправки, то в expected логах на их местах используются
 *   COMMIT_HASH и COMMIT_DATE заглушки соответственно
 */
public class GitTest extends AbstractGitTest {
    @Override
    protected GitCli createCli(String workingDir) {
        return new GitCliImpl(workingDir);
        // throw new UnsupportedOperationException();
    }

    @Override
    protected TestMode testMode() {
        return TestMode.SYSTEM_OUT;
    }

    @Test
    public void testAdd() throws Exception {
        createFile("file.txt", "aaa");
        status();
        add("file.txt");
        status();
        commit("First commit");
        status();
        log();

        check("add.txt");
    }

    @Test
    public void testMultipleCommits() throws Exception {
        String file1 = "file1.txt";
        String file2 = "file2.txt";
        createFile(file1, "aaa");
        createFile(file2, "bbb");
        status();
        add(file1);
        add(file2);
        status();
        rm(file2);
        status();
        commit("Add file1.txt");
        add(file2);
        commit("Add file2.txt");
        status();
        log();

        check("multipleCommits.txt");
    }

    @Test
    public void testCheckoutFile() throws Exception {
        String file = "file.txt";
        createFile(file, "aaa");
        add(file);
        commit("Add file.txt");

        deleteFile(file);
        status();
        checkoutFiles("--", file);
        fileContent(file);
        status();

        createFile(file, "bbb");
        fileContent(file);
        status();
        checkoutFiles("--", file);
        fileContent(file);
        status();

        check("checkoutFile.txt");
    }

    @Test
    public void testReset() throws Exception {
        String file = "file.txt";
        createFile(file, "aaa");
        add(file);
        commit("First commit");

        createFile(file, "bbb");
        add(file);
        commit("Second commit");
        log();

        reset(1);
        fileContent(file);
        log();

        createFile(file, "ccc");
        add(file);
        commit("Third commit");
        log();

        check("reset.txt");
    }

    @Test
    public void testCheckout() throws Exception {
        String file = "file.txt";
        createFile(file, "aaa");
        add(file);
        commit("First commit");

        createFile(file, "bbb");
        add(file);
        commit("Second commit");
        log();

        checkoutRevision(1);
        status();
        log();

        checkoutMaster();
        status();
        log();

        check("checkout.txt");
    }

    @Test
    public void testBranches() throws Exception {
        createFileAndCommit("file1.txt", "aaa");

        createBranch("develop");
        createFileAndCommit("file2.txt", "bbb");

        status();
        log();
        showBranches();
        checkoutMaster();
        status();
        log();

        createBranch("new-feature");
        createFileAndCommit("file3.txt", "ccc");
        status();
        log();

        checkoutBranch("develop");
        status();
        log();

        check("branches.txt");
    }

    @Test
    public void testBranchRemove() throws Exception {
        createFileAndCommit("file1.txt", "aaa");
        createBranch("develop");
        createFileAndCommit("file2.txt", "bbb");
        status();
        checkoutBranch("master");
        status();
        removeBranch("develop");
        showBranches();

        check("branchRemove.txt");
    }
}
