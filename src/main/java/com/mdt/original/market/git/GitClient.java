package com.mdt.original.market.git;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class GitClient {
    public void syncRepository(String repositoryUrl, String branch, File targetDir) throws IOException {
        if (isGitRepository(targetDir)) {
            run(targetDir, "git", "-C", targetDir.getAbsolutePath(), "pull", "--ff-only", "origin", branch);
            return;
        }

        File parent = targetDir.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("无法创建缓存目录: " + parent);
        }

        if (targetDir.exists() && targetDir.list() != null && targetDir.list().length > 0) {
            throw new IOException("目标目录已存在且不为空，无法执行 git clone: " + targetDir);
        }

        run(parent, "git", "clone", "--branch", branch, "--single-branch", repositoryUrl, targetDir.getAbsolutePath());
    }

    public boolean isGitInstalled() {
        try {
            run(null, "git", "--version");
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    private boolean isGitRepository(File dir) {
        return new File(dir, ".git").exists();
    }

    private String run(File workDir, String... command) throws IOException {
        List<String> args = new ArrayList<String>();
        for (String item : command) {
            args.add(item);
        }

        ProcessBuilder builder = new ProcessBuilder(args);
        if (workDir != null) {
            builder.directory(workDir);
        }
        builder.redirectErrorStream(true);

        Process process = builder.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
        }

        try {
            int code = process.waitFor();
            if (code != 0) {
                throw new IOException("Git 命令执行失败: " + String.join(" ", command) + "\n" + output);
            }
            return output.toString();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Git 命令被中断: " + String.join(" ", command), exception);
        }
    }
}
