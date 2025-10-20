package com.helmx.tutorial.docker.utils;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
public class GitUtil {

    /**
     * 克隆 Git 仓库
     *
     * @param gitUrl Git 仓库地址
     * @param branch 分支名称
     * @param targetDir 目标目录
     * @return Git 对象
     * @throws GitAPIException Git 操作异常
     */
    public static Git cloneRepository(String gitUrl, String branch, Path targetDir) throws GitAPIException, IOException {
        log.info("Cloning repository from {} to {}", gitUrl, targetDir);

        Git.cloneRepository()
                .setURI(gitUrl)
                .setDirectory(targetDir.toFile())
                .setBranch(branch != null && !branch.isEmpty() ? branch : "main")
                .call();

        log.info("Repository cloned successfully");
        return Git.open(targetDir.toFile());
    }

    /**
     * 删除临时目录及其内容
     *
     * @param tempDir 临时目录路径
     */
    public static void deleteTempDirectory(Path tempDir) {
        try {
            if (tempDir != null && Files.exists(tempDir)) {
                Files.walk(tempDir)
                        .sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(java.io.File::delete);
                log.info("Temporary directory deleted: {}", tempDir);
            }
        } catch (IOException e) {
            log.warn("Failed to delete temporary directory: {}", tempDir, e);
        }
    }

    /**
     * 克隆需要认证的 Git 仓库
     *
     * @param gitUrl Git 仓库地址
     * @param username 用户名
     * @param password 密码或 token
     * @param branch 分支名称
     * @param targetDir 目标目录
     * @return Git 对象
     * @throws GitAPIException Git 操作异常
     */
    public static Git cloneRepositoryWithAuth(String gitUrl, String username, String password,
                                               String branch, Path targetDir) throws GitAPIException, IOException {
        log.info("Cloning repository from {} to {} with authentication", gitUrl, targetDir);

        Git.cloneRepository()
                .setURI(gitUrl)
                .setDirectory(targetDir.toFile())
                .setBranch(branch != null && !branch.isEmpty() ? branch : "main")
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password))
                .call();

        log.info("Repository cloned successfully with authentication");
        return Git.open(targetDir.toFile());
    }
}
