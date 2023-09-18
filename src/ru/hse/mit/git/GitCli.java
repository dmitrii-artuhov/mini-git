package ru.hse.mit.git;

import java.io.PrintStream;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public interface GitCli {
    /*
     * Запустить команду [command] с аргументами [arguments].
     */
    void runCommand(@NotNull String command, @NotNull List<@NotNull String> arguments) throws GitException;

    /*
     * Установить outputStream, в который будет выводиться лог
     */
    void setOutputStream(@NotNull PrintStream outputStream);

    /*
     * Вернуть хеш n-го перед HEAD коммита
     */
    @NotNull String getRelativeRevisionFromHead(int n) throws GitException;
}
