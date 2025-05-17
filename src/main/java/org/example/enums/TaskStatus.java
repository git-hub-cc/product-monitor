package org.example.enums;

/**
 * 任务状态枚举类
 * 用于表示任务的不同运行状态
 */
public enum TaskStatus {
    /**
     * 运行中
     * 表示任务正在执行
     */
    RUNNING,

    /**
     * 已停止
     * 表示任务已经停止运行
     */
    STOPPED,

    /**
     * 已暂停
     * 表示任务暂时停止但可以继续运行
     */
    PAUSED,

    /**
     * 错误
     * 表示任务执行出现错误
     */
    ERROR
}