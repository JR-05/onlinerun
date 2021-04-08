package com.runcode.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.jaxrs.JerseyDockerCmdExecFactory;
import com.runcode.entities.CodeLang;
import com.runcode.server.callback.ResultCallback;
import com.runcode.utils.DockerConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 控制Docker的Java客户端
 *
 * @author JR
 */
@Slf4j
public class DockerJavaClient {
    private DockerClient dockerClient;
    private ConcurrentHashMap<CodeLang, ArrayList<String>> containerIds = new ConcurrentHashMap<>();
    private static DockerJavaClient singleton;

    private DockerJavaClient() {
        this.dockerClient = getDockerClient();

        // 获取所有相关语言的所有正在运行的容器
        List<Container> containers = this.dockerClient.listContainersCmd()
                .withNameFilter(Arrays.stream(CodeLang.values()).map(it -> it.getContainerNamePrefix()).collect(Collectors.toList()))
                .withStatusFilter(Arrays.asList("running"))
                .exec();

        // 加载已经创建过的容器
        for (Container container : containers) {
            Arrays.stream(CodeLang.values())
                    .filter(it -> it.isImageName(container.getImage()))
                    .findAny()
                    .map(it -> {
                                ArrayList<String> ids = this.containerIds.getOrDefault(it, new ArrayList<>());
                                ids.add(container.getId());
                                return this.containerIds.put(it, ids);
                            }
                    );
        }

        // 如果没有创建过则创建
        for (CodeLang codeLang : CodeLang.values()) {
            if (!this.containerIds.containsKey(codeLang) || Objects.isNull(this.containerIds.get(codeLang)) || this.containerIds.get(codeLang).isEmpty()) {
                this.containerIds.put(codeLang, new ArrayList<>(Arrays.asList(this.createContainer(this.dockerClient, codeLang))));
            }
        }
    }

    /**
     * 单例获取实例
     *
     * @return
     */
    public static DockerJavaClient getSingleton() {
        if (singleton == null) {
            synchronized (DockerJavaClient.class) {
                if (singleton == null) {
                    singleton = new DockerJavaClient();
                }
            }
        }
        return singleton;
    }

    /**
     * 获取一个docker连接
     *
     * @return
     */
    private DockerClient getDockerClient() {
        DockerCmdExecFactory dockerCmdExecFactory = new JerseyDockerCmdExecFactory().withReadTimeout(20000)
                .withConnectTimeout(2000);
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(DockerConfig.DOCKER_HOST)
//                关闭加密通信
//                .withDockerTlsVerify(true)
//                .withDockerCertPath(DockerConfig.DOCKER_CERT_PATH)
//                .withDockerConfig(DockerConfig.DOCKER_CERT_PATH)
                .withRegistryUrl("https://index.docker.io/v1/")
                .withRegistryUsername(DockerConfig.REGISTRY_USER_NAME)
                .withRegistryPassword(DockerConfig.REGISTRY_PASSWORD)
                .withRegistryEmail(DockerConfig.REGISTRY_EMAIL)
                .build();

        return DockerClientBuilder.getInstance(config)
                .withDockerCmdExecFactory(dockerCmdExecFactory).build();
    }

    /**
     * 获取运行代码的容器id
     *
     * @param codeLang
     * @return
     */
    private String getContainerId(CodeLang codeLang) {
        ArrayList<String> containerIds = this.containerIds.get(codeLang);

        if (containerIds.isEmpty()) {
            containerIds.add(this.createContainer(this.dockerClient, codeLang));
            this.containerIds.put(codeLang, containerIds);
        }

        // 随机获取一个容器
        String containerId = containerIds.get(new Random().nextInt(containerIds.size()));

        // 判断该容器是否可运行，不可运行的话获取其他的容器重新判断，否则直接返回该容器id
        if (this.isRunning(containerId)) {
            return containerId;
        } else {
            Iterator<String> iterator = containerIds.iterator();
            String containerIdNext = iterator.next();

            if (containerIdNext.equals(containerId) || this.isRunning(containerId)) {
                containerId = null;
                iterator.remove();
            } else {
                containerId = containerIdNext;
            }
        }

        // 没有一个可以运行的话创建一个新的容器
        if (Objects.isNull(containerId)) {
            containerId = this.createContainer(this.dockerClient, codeLang);
            this.containerIds.get(codeLang).add(containerId);
        }
        return containerId;
    }

    /**
     * 创建运行代码的容器
     *
     * @param dockerClient
     * @param langType
     * @return
     */
    private String createContainer(DockerClient dockerClient, CodeLang langType) {
        // 创建容器请求
        CreateContainerResponse containerResponse = dockerClient.createContainerCmd(langType.getImageName())
                .withName(langType.getContainerNamePrefix() + System.currentTimeMillis() / 1000)
                .withWorkingDir(DockerConfig.DOCKER_CONTAINER_WORK_DIR)
                .withStdinOpen(true)
                .exec();

        return containerResponse.getId();
    }

    /**
     * 判断该容器是否能够正常访问
     *
     * @param containerId
     * @return
     */
    private boolean isRunning(String containerId) {
        return !this.dockerClient.listContainersCmd()
                .withIdFilter(Arrays.asList(containerId))
                .withStatusFilter(Arrays.asList("running"))
                .exec()
                .isEmpty();
    }

    /**
     * 将程序代码写入容器中的一个文件
     *
     * @param containerId
     * @param langType
     * @param sourcecode
     * @return
     * @throws InterruptedException
     */
    private String writeFileToContainer(String containerId, CodeLang langType, String sourcecode) throws InterruptedException {
        String workDir = DockerConfig.DOCKER_CONTAINER_WORK_DIR;
        String fileName = UUID.randomUUID() + langType.getFileNameSuffix();
        String path = workDir + "/" + fileName;

        // 将\替换为\\\\，转义反斜杠
        sourcecode = sourcecode.replaceAll("\\\\", "\\\\\\\\\\\\\\\\");

        // 将"替换为\"，转义引号
        sourcecode = sourcecode.replaceAll("\\\"", "\\\\\"");

        // 通过重定向符写入文件，注意必须要带前面两个参数，否则重定向符会失效，和Docker CMD的机制有关
        ExecCreateCmdResponse createCmdResponse = this.dockerClient.execCreateCmd(containerId)
                .withCmd("/bin/sh", "-c", "echo \"" + sourcecode + "\" > " + path)
                .exec();
        dockerClient.execStartCmd(createCmdResponse.getId())
                .exec(new ExecStartResultCallback(System.out, System.err))
                .awaitCompletion();
        return fileName;
    }

    /**
     * 删除代码文件
     *
     * @param containerId
     * @param fileName
     */
    private void deleteFile(String containerId, String fileName) {
        String workDir = DockerConfig.DOCKER_CONTAINER_WORK_DIR;
        String path = workDir + "/" + fileName;

        try {
            ExecCreateCmdResponse createCmdResponse = this.dockerClient.execCreateCmd(containerId)
                    .withCmd("/bin/sh", "-c", "rm " + path)
                    .exec();
            dockerClient.execStartCmd(createCmdResponse.getId())
                    .exec(new ExecStartResultCallback(System.out, System.err))
                    .awaitCompletion();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 在容器上EXEC一条CMD命令
     *
     * @param command     命令，EXEC数组
     * @param containerId 容器ID
     * @param timeout     超时时间（单位为秒）
     * @throws InterruptedException
     */
    private void runCommandOnContainer(String[] command, String containerId,
                                       int timeout, ResultCallback callback) throws InterruptedException {
        ExecCreateCmdResponse createCmdResponse = this.dockerClient.execCreateCmd(containerId)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withCmd(command)
                .exec();
        this.dockerClient.execStartCmd(createCmdResponse.getId())
                .exec(callback)
                .awaitCompletion(timeout, TimeUnit.SECONDS);
    }

    /**
     * 执行一个程序
     *
     * @param langType   编程语言类型
     * @param sourcecode 源代码
     * @throws InterruptedException
     * @throws IOException
     */
    public void exec(CodeLang langType, String sourcecode, ResultCallback callback) {
        // 创建容器
        String containerId = this.getContainerId(langType);
        String fileName = null;

        try {
            log.info("开始写文件");
            fileName = writeFileToContainer(containerId, langType, sourcecode);
            log.info("写文件结束");

            String[][] commands = langType.getExecCommand(fileName);
            for (int i = 0; i < commands.length; i++) {
                log.info("开始执行第" + (i + 1) + "条指令");

                // 指定执行完最后一条命令才刷新响应
                boolean isFinal = i == commands.length - 1;
                callback.setFinal(isFinal);

                this.runCommandOnContainer(commands[i], containerId, 10, callback);
                log.info("执行第" + (i + 1) + "条指令结束");
            }
        } catch (Exception e) {
            log.info("Exec ");
        } finally {
            // 删除代码文件
            this.deleteFile(containerId, fileName);
        }
    }
}