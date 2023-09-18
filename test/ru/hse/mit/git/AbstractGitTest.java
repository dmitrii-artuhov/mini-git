package ru.hse.mit.git;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public abstract class AbstractGitTest {
    protected enum TestMode {
        TEST_DATA, SYSTEM_OUT
    }

    /*
     * Переключением этой константы можно выбирать режим тестирования
     *   - TEST_DATA -- результат работы гита будет сравниваться
     *     с эталонными логами в папке resources
     *   - SYSTEM_OUT -- все логи выводятся в системную консоль,
     *     никаких assert'ов не вызывается
     */
    protected abstract TestMode testMode();

    protected abstract GitCli createCli(String workingDir);

    private static final String DASHES = "----------------------------";

    // --------------------------------------------------------------------------------------------

    private PrintStream output;
    private ByteArrayOutputStream byteArrayOutputStream;
    private final File projectDir = new File("./playground/");
    private final GitCli cli = createCli(projectDir.getAbsolutePath());

    // ------------------------------------ Различные утильные функции -----------------------------------------

    private void cleanPlayground() {
        projectDir.mkdirs();
        Collection<File> files = FileUtils.listFilesAndDirs(projectDir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        for (File file : files) {
            if (!file.equals(projectDir)) {
                FileUtils.deleteQuietly(file);
            }
        }
    }

    private @Nullable String getFileContent(@NotNull String fileName) {
        try {
            return FileUtils.readFileToString(new File(projectDir, fileName), Charset.defaultCharset());
        } catch (Exception e) {
            return null;
        }
    }

    private @Nullable String getResourcesFileContent(@NotNull String fileName) {
        URL url = ClassLoader.getSystemClassLoader().getResource(fileName);
        if (url == null) {
            return null;
        }
        try {
            return FileUtils.readFileToString(new File(url.toURI()), Charset.defaultCharset());
        } catch (Exception e) {
            return null;
        }
    }

    private void runCommand(@NotNull String command, String... args) throws GitException {
        List<String> arguments = Arrays.asList(args);
        String input = (command + " " + String.join(" ", arguments)).trim();
        output.println(DASHES);
        output.println("Command: " + input);

        cli.runCommand(command, arguments);
    }

    private void runRelativeCommand(@NotNull String command, int to) throws GitException {
        output.println(DASHES);
        output.println("Command: " + command + " HEAD~" + to);

        String revision = cli.getRelativeRevisionFromHead(to);
        cli.runCommand(command, Collections.singletonList(revision));
    }


    // ------------------------------------ Методы для создания тестовых кейсов -----------------------------------------

    // echo content > fileName
    protected void createFile(@NotNull String fileName, @NotNull String content) throws Exception {
        output.println(DASHES);
        output.println("Create file '" + fileName + "' with content '" + content + "'");
        File file = new File(projectDir, fileName);
        FileUtils.writeStringToFile(file, content, Charset.defaultCharset());
    }

    // rm fileName
    protected void deleteFile(@NotNull String fileName) {
        output.println(DASHES);
        output.println("Delete file " + fileName);
        File file = new File(projectDir, fileName);
        FileUtils.deleteQuietly(file);
    }

    // cat fileName
    protected void fileContent(@NotNull String fileName) {
        String content = getFileContent(fileName);
        output.println(DASHES);
        output.println("Command: content of file " + fileName);
        output.println(content);
    }

    // git status
    protected void status() throws GitException {
        runCommand(GitConstants.STATUS);
    }

    // git add files
    protected void add(String... files) throws GitException {
        runCommand(GitConstants.ADD, files);
    }

    // git rm files
    protected void rm(String... files) throws GitException {
        runCommand(GitConstants.RM, files);
    }

    // git commit message
    protected void commit(String message) throws GitException {
        runCommand(GitConstants.COMMIT, message);
    }

    // git reset HEAD~to
    protected void reset(int to) throws GitException {
        runRelativeCommand(GitConstants.RESET, to);
    }

    // git checkout files
    protected void checkoutFiles(String... args) throws GitException {
        runCommand(GitConstants.CHECKOUT, args);
    }

    // git checkout HEAD~to
    protected void checkoutRevision(int to) throws GitException {
        runRelativeCommand(GitConstants.CHECKOUT, to);
    }

    // git checkout master
    protected void checkoutMaster() throws GitException {
        checkoutBranch(GitConstants.MASTER);
    }

    // git log
    protected void log() throws GitException {
        runCommand(GitConstants.LOG);
    }

    // git branch-create branch
    protected void createBranch(@NotNull String branch) throws GitException {
        runCommand(GitConstants.BRANCH_CREATE, branch);
    }

    // git branch-remove
    protected void removeBranch(@NotNull String branch) throws GitException {
        runCommand(GitConstants.BRANCH_REMOVE, branch);
    }

    // git checkout branch
    protected void checkoutBranch(@NotNull String branch) throws GitException {
        runCommand(GitConstants.CHECKOUT, branch);
    }

    // git show-branches
    protected void showBranches() throws GitException {
        runCommand(GitConstants.SHOW_BRANCHES);
    }

    // git merge branch
    protected void merge(@NotNull String branch) throws GitException {
        runCommand(GitConstants.MERGE, branch);
    }

    /*
     * echo content > fileName
     * git add fileName
     * git commit fileName
     */
    protected void createFileAndCommit(@NotNull String fileName, @NotNull String content) throws Exception {
        createFile(fileName, content);
        add(fileName);
        commit(fileName);
    }

    /*
     * Проверяет, что лог команд гита совпадает с логом, находящимся в файле `test/resources/testDataFilePath`
     */
    protected void check(@NotNull String testDataFilePath) {
        if (testMode() == TestMode.SYSTEM_OUT) return;

        String expected = getResourcesFileContent(testDataFilePath);
        if (expected == null) {
            fail(testDataFilePath + " file is missing");
        }
        String actual = byteArrayOutputStream.toString();
        assertEquals(expected, actual);
    }

    // --------------------------------------------------------------------------------------------

    @BeforeEach
    public void setUp() throws GitException {
        cleanPlayground();
        switch (testMode()) {
            case SYSTEM_OUT:
                output = System.out;
                break;
            case TEST_DATA:
                byteArrayOutputStream = new ByteArrayOutputStream();
                output = new PrintStream(byteArrayOutputStream);
                break;
        }
        cli.setOutputStream(output);
        runCommand(GitConstants.INIT);
    }
}
