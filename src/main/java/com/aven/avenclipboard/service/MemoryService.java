package com.aven.avenclipboard.service;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class MemoryService implements InitializingBean {

    private final int FRONTEND_BOARD_NUM = 5;

    // board id - clipboard message
    private final ConcurrentHashMap<Integer, Deque<String>> messageMap = new ConcurrentHashMap<>();

    // lock for each queue
    private final ReentrantLock[] locks = new ReentrantLock[FRONTEND_BOARD_NUM];

    public List<List<String>> getAllMessage() {
        List<List<String>> result = new ArrayList<>();

        for (int id = 0; id < FRONTEND_BOARD_NUM; id++) {
            ReentrantLock nowUsing = locks[id];
            try {
                nowUsing.lock();
                result.add(new ArrayList<>(messageMap.get(id)));
            } finally {
                nowUsing.unlock();
            }
        }
        return result;
    }

    public List<String> putMessage(int id, String message) {
        if (id < 0 || id >= FRONTEND_BOARD_NUM) throw new RuntimeException("invalid id~ please check~");
        // add to queue head
        Deque<String> queue = messageMap.get(id);
        ReentrantLock nowUsing = locks[id];

        try {
            nowUsing.lock();
            queue.addFirst(message);
            if (queue.size() > 5) queue.removeLast();
            return new ArrayList<>(messageMap.get(id));
        } finally {
            nowUsing.unlock();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // init history queue
        for (int i = 0; i < FRONTEND_BOARD_NUM; i++) {
            messageMap.put(i, new ConcurrentLinkedDeque<>());
        }

        // init locks
        for (int i = 0; i < FRONTEND_BOARD_NUM; i++) {
            locks[i] = new ReentrantLock();
        }
    }
}
