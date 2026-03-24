package com.qituo.mcps.thread;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import com.qituo.mcps.core.MCPSMod;

public class ThreadCommunication {
    private ConcurrentHashMap<String, MessageQueue> messageQueues;
    private AtomicInteger messageIdGenerator;
    
    public void initialize() {
        messageQueues = new ConcurrentHashMap<>();
        messageIdGenerator = new AtomicInteger(0);
        
        // 创建默认的消息队列
        createMessageQueue("main");
        createMessageQueue("world");
        createMessageQueue("entity");
        createMessageQueue("render");
        
        MCPSMod.LOGGER.info("ThreadCommunication initialized with " + messageQueues.size() + " message queues");
    }
    
    public void createMessageQueue(String name) {
        MessageQueue queue = new MessageQueue(name);
        messageQueues.putIfAbsent(name, queue);
        MCPSMod.LOGGER.info("Message queue created: " + name);
    }
    
    public int sendMessage(String queueName, Message message) {
        MessageQueue queue = messageQueues.get(queueName);
        if (queue == null) {
            MCPSMod.LOGGER.warn("Message queue not found: " + queueName);
            return -1;
        }
        
        int messageId = messageIdGenerator.incrementAndGet();
        message.setId(messageId);
        message.setTimestamp(System.currentTimeMillis());
        
        queue.offer(message);
        return messageId;
    }
    
    public int sendMessage(String queueName, String type, Object data) {
        Message message = new Message(type, data);
        return sendMessage(queueName, message);
    }
    
    public Message receiveMessage(String queueName) throws InterruptedException {
        MessageQueue queue = messageQueues.get(queueName);
        if (queue == null) {
            MCPSMod.LOGGER.warn("Message queue not found: " + queueName);
            return null;
        }
        
        return queue.take();
    }
    
    public Message receiveMessageNonBlocking(String queueName) {
        MessageQueue queue = messageQueues.get(queueName);
        if (queue == null) {
            MCPSMod.LOGGER.warn("Message queue not found: " + queueName);
            return null;
        }
        
        return queue.poll();
    }
    
    public int getQueueSize(String queueName) {
        MessageQueue queue = messageQueues.get(queueName);
        return queue != null ? queue.size() : 0;
    }
    
    public void clearQueue(String queueName) {
        MessageQueue queue = messageQueues.get(queueName);
        if (queue != null) {
            queue.clear();
            MCPSMod.LOGGER.info("Message queue cleared: " + queueName);
        }
    }
    
    public static class Message {
        private int id;
        private String type;
        private Object data;
        private long timestamp;
        private String sender;
        
        public Message(String type, Object data) {
            this.type = type;
            this.data = data;
            this.sender = Thread.currentThread().getName();
        }
        
        public int getId() {
            return id;
        }
        
        public void setId(int id) {
            this.id = id;
        }
        
        public String getType() {
            return type;
        }
        
        public Object getData() {
            return data;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
        
        public String getSender() {
            return sender;
        }
        
        @Override
        public String toString() {
            return "Message{id=" + id + ", type='" + type + "', sender='" + sender + "'}";
        }
    }
    
    private static class MessageQueue {
        private final String name;
        private final BlockingQueue<Message> queue;
        
        public MessageQueue(String name) {
            this.name = name;
            this.queue = new LinkedBlockingQueue<>();
        }
        
        public String getName() {
            return name;
        }
        
        public void offer(Message message) {
            queue.offer(message);
        }
        
        public Message take() throws InterruptedException {
            return queue.take();
        }
        
        public Message poll() {
            return queue.poll();
        }
        
        public int size() {
            return queue.size();
        }
        
        public void clear() {
            queue.clear();
        }
    }
}