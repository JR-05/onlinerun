package com.runcode;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ListImagesCmd;
import com.github.dockerjava.api.model.Image;
import com.runcode.docker.DockerJavaClient;
import com.runcode.entities.CodeLang;
import com.runcode.server.http.RestApiServer;
import com.runcode.server.http.handler.ApiServerHandler;
import com.runcode.server.websocket.WebsocketServer;

import java.util.List;
import java.util.concurrent.*;

/**
 * 服务器启动类
 * @author RhettPeng
 */
public class ServerBootStrap {

    public static void main(String[] args) {
        DockerJavaClient dockerJavaClient = new DockerJavaClient();
//        String container = dockerJavaClient.createContainer(dockerJavaClient.getDockerClient(), CodeLang.JAVA);
//        System.out.println(container);
        dockerJavaClient.exec(CodeLang.JAVA,"class Untitled {\n" +
                "    public static void main(String[] args) {\n" +
                "        Runtime.getRuntime().availableProcessors();\n" +
                "    }\n" +
                "}",null);

//        ExecutorService executorService = new ThreadPoolExecutor(2, 2, 0, TimeUnit.MICROSECONDS,new SynchronousQueue<>());
//        // 启动websocket服务器
//        executorService.execute(()->{
//            try {
//                new WebsocketServer().startup();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        });
//        // 启动http服务器
//        executorService.execute(()->{
//            try {
//                new RestApiServer().startup();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        });
    }
}
