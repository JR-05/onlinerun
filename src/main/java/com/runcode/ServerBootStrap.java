package com.runcode;

import com.runcode.server.http.RestApiServer;
import com.runcode.server.websocket.WebsocketServer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 服务器启动类
 *
 * @author JR
 */
public class ServerBootStrap {

    public static void main(String[] args) {
//        DockerJavaClient dockerJavaClient = new DockerJavaClient();
//
//        String cpp = "#include <iostream> \n using namespace std;\n int main() { cout<<\"hello codetool!\"<<endl; return 0; }";
//        String java = "class Untitled { public static void main(String[] args) { System.out.println(\"hello codetool!\"); } }";
//        String go = "package main \n import \"fmt\" \n func main() { fmt.Println(\"hello codetool!\") }";
//        String python = "# encoding: utf-8\n" +
//                "if __name__ == \"__main__\":\n" +
//                "    print(\"hello codetool!\")\n";
//        dockerJavaClient.exec(CodeLang.PYTHON3, python, new RunCodeResultCallback(null));

       ExecutorService executorService = new ThreadPoolExecutor(2, 2, 0, TimeUnit.MICROSECONDS,new SynchronousQueue<>());
        // 启动websocket服务器
        executorService.execute(()->{
            try {
                new WebsocketServer().startup();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        // 启动http服务器
        executorService.execute(()->{
            try {
                new RestApiServer().startup();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }
}
