package ru.hse.mit.git;

import java.io.PrintStream;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class GitCliImpl implements GitCli {
   private PrintStream outputStream = System.out;
   private final MiniGit git;

   public GitCliImpl(String workingDir) {
      git = new MiniGit(workingDir);
   }

   @Override
   public void runCommand(@NotNull String command, @NotNull List<@NotNull String> arguments)
       throws GitException {
      String gitOutput = "";

      switch (command) {
         case GitConstants.INIT -> gitOutput = git.init();
         case GitConstants.ADD -> gitOutput = git.add(arguments);
         case GitConstants.RM -> gitOutput = git.rm(arguments);
         case GitConstants.STATUS -> gitOutput = git.status();
         case GitConstants.COMMIT -> {
            checkExactArguments(command, arguments, 1, List.of("message"));
            gitOutput = git.commit(arguments.get(0));
         }
         case GitConstants.RESET -> {
            checkExactArguments(command, arguments, 1,
                List.of("to_revision: HEAD~N | branch name | commit hash"));
            String toRevision = arguments.get(0);
            if (toRevision.startsWith("HEAD~")) {
               checkHeadShiftArgumentCorrectness(command, toRevision);
               gitOutput = git.reset(getHeadShiftArgumentValue(toRevision));
            } else {
               gitOutput = git.reset(toRevision);
            }
         }
         case GitConstants.LOG -> {
            if (arguments.isEmpty()) {
               gitOutput = git.log();
            } else {
               checkExactArguments(command, arguments, 1,
                   List.of("from_revision: HEAD~N | branch name | commit hash"));

               String fromRevision = arguments.get(0);
               if (fromRevision.startsWith("HEAD~")) {
                  checkHeadShiftArgumentCorrectness(command, fromRevision);
                  gitOutput = git.log(getHeadShiftArgumentValue(fromRevision));
               } else {
                  gitOutput = git.log(fromRevision);
               }
            }
         }
         case GitConstants.CHECKOUT -> {
            if (arguments.size() > 1) {
               String firstArgument = arguments.get(0);
               if (!firstArgument.equals("--")) {
                  throw new GitException("Command '" + command
                      + "' with multiple arguments expects filenames enumeration starting with '--'");
               }

               gitOutput = git.checkout(arguments.subList(1, arguments.size()));
            } else {
               checkExactArguments(command, arguments, 1,
                   List.of("revision: HEAD~N | branch name | commit hash"));
               String fromRevision = arguments.get(0);

               if (fromRevision.startsWith("HEAD~")) {
                  checkHeadShiftArgumentCorrectness(command, fromRevision);
                  gitOutput = git.checkout(getHeadShiftArgumentValue(fromRevision));
               } else {
                  gitOutput = git.checkout(fromRevision);
               }
            }
         }
         case GitConstants.BRANCH_CREATE -> {
            checkExactArguments(command, arguments, 1, List.of("branch"));
            String branchName = arguments.get(0);
            gitOutput = git.createBranch(branchName);
         }
         case GitConstants.SHOW_BRANCHES -> {
            gitOutput = git.showBranches();
         }
         case GitConstants.BRANCH_REMOVE -> {
            checkExactArguments(command, arguments, 1, List.of("branch"));
            String branchName = arguments.get(0);
            gitOutput = git.removeBranch(branchName);
         }
         case GitConstants.MERGE -> {
            checkExactArguments(command, arguments, 1, List.of("branch"));
            String branchName = arguments.get(0);
            gitOutput = git.merge(branchName);
         }
         default -> throw new GitException("Unknown command: '" + command + "'");
      }

      outputStream.print(gitOutput);
   }

   @Override
   public void setOutputStream(@NotNull PrintStream outputStream) {
      this.outputStream = outputStream;
   }

   @Override
   public @NotNull String getRelativeRevisionFromHead(int n) throws GitException {
      return git.getRelativeRevisionFromHead(n);
   }

   private void checkExactArguments(String command, List<String> args, int requiredArgsCount, List<String> argsDescriptions) throws GitException {
      if (args.size() != requiredArgsCount) {
         StringBuilder errorMessage = new StringBuilder();
         errorMessage
             .append("Command '")
             .append(command)
             .append("' must be followed by exactly ")
             .append(requiredArgsCount)
             .append(" argument(s): ");

         for (String description : argsDescriptions) {
            errorMessage.append("[").append(description).append("]");
         }

         throw new GitException(errorMessage.toString());
      }
   }

   private void checkHeadShiftArgumentCorrectness(String command, String revision) throws GitException {
      String shiftNumber = revision.substring(5); // removing "HEAD~" from string
      try {
         int res = Integer.parseInt(shiftNumber);
         if (res < 0) {
            throw new Exception();
         }
      }
      catch (Exception e) {
         throw new GitException(
            "Command '" + command +
            "' accepts argument in HEAD~N format with N being non-negative integer, but got: '" +
            shiftNumber + "'"
         );
      }
   }

   private int getHeadShiftArgumentValue(String revision) {
      String shiftNumber = revision.substring(5); // removing "HEAD~" from string
      return Integer.parseInt(shiftNumber);
   }
}
