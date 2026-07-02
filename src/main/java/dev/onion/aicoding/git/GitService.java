package dev.onion.aicoding.git;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class GitService {

    private final File repository;

    public GitService(String repositoryPath) {
        this.repository = new File(repositoryPath);
    }

    public String getDiff() {
        StringBuilder diff = new StringBuilder();

        try {
            Process process = new ProcessBuilder(
                    "git",
                    "--no-pager",
                    "diff"
            )
                    .directory(repository)
                    .redirectErrorStream(true)
                    .start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            )) {
                String line;
                while ((line = reader.readLine()) != null) {
                    diff.append(line).append("\n");
                }
            }

            process.waitFor();

        } catch (Exception e) {
            return "ERROR while running git diff:\n" + e.getMessage();
        }

        return diff.toString();
    }
}