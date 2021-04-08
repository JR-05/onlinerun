package com.runcode.entities;

/**
 * 各种编程语言的枚举
 *
 * @author JR
 */

public enum CodeLang {
    /**
     * Python语言
     */
    PYTHON3 {
        @Override
        public String getImageName() {
            return "python:3";
        }

        @Override
        public String getContainerNamePrefix() {
            return "python-running-script-";
        }

        @Override
        public String[][] getExecCommand(String fileName) {
            return new String[][]{{"python", fileName}};
        }

        @Override
        public String getFileNameSuffix() {
            return ".py";
        }
    },
    /**
     * C++语言
     */
    CPP {
        @Override
        public String getImageName() {
            return "gcc:7.3";
        }

        @Override
        public String getContainerNamePrefix() {
            return "cpp-running-file-";
        }

        @Override
        public String[][] getExecCommand(String fileName) {
            String encodeFileName = fileName.split("\\.")[0];
            return new String[][]{{"g++", fileName, "-o", encodeFileName}, {String.format("./%s", encodeFileName)}};
        }

        @Override
        public String getFileNameSuffix() {
            return ".cpp";
        }
    },
    /**
     * JAVA语言
     */
    JAVA {
        @Override
        public String getImageName() {
            return "openjdk:11";
        }

        @Override
        public String getContainerNamePrefix() {
            return "java-running-file-";
        }

        @Override
        public String[][] getExecCommand(String fileName) {
            // jdk11可以不经过javac
            return new String[][]{{"java", fileName}};
        }

        @Override
        public String getFileNameSuffix() {
            return ".java";
        }
    },
    /**
     * Go语言
     */
    GOLANG {
        @Override
        public String getImageName() {
            return "golang:1.14";
        }

        @Override
        public String getContainerNamePrefix() {
            return "golang-running-file-";
        }

        @Override
        public String[][] getExecCommand(String fileName) {
            // Go可不经过编译
            return new String[][]{{"go", "run", fileName}};
        }

        @Override
        public String getFileNameSuffix() {
            return ".go";
        }
    };

    /**
     * 镜像名称
     */
    public String getImageName() {
        return null;
    }

    /**
     * 容器名称前缀
     */
    public String getContainerNamePrefix() {
        return null;
    }

    /**
     * 执行该文件需要的指令，供Docker EXEC调用
     */
    public String[][] getExecCommand(String fileName) {
        return null;
    }

    /**
     * 该编程语言的文件后缀名
     *
     * @return
     */
    public String getFileNameSuffix() {
        return null;
    }

    /**
     * 判断该镜像名是否为该语言环境
     *
     * @param imageName
     * @return
     */
    public boolean isImageName(String imageName) {
        return imageName.equalsIgnoreCase(getImageName());
    }
}
