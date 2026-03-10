package com.aven.avenclipboard.service;

import com.aven.avenclipboard.model.Message;
import com.aven.avenclipboard.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MemoryService {

    private final int FRONTEND_BOARD_NUM = 5;

    @Autowired
    private MessageRepository messageRepository;

    public List<List<String>> getAllMessage() {
        List<List<String>> result = new ArrayList<>();
        for (int id = 0; id < FRONTEND_BOARD_NUM; id++) {
            List<Message> messages = messageRepository.findByBoardIdOrderByCreatedAtDesc(id);
            result.add(messages.stream().map(Message::getContent).collect(Collectors.toList()));
        }
        return result;
    }

    @Transactional
    public List<String> putMessage(int id, String messageContent) {
        if (id < 0 || id >= FRONTEND_BOARD_NUM)
            throw new RuntimeException("invalid id~ please check~");

        Message newMessage = new Message(id, messageContent, LocalDateTime.now());
        messageRepository.save(newMessage);

        List<Message> messages = messageRepository.findByBoardIdOrderByCreatedAtDesc(id);

        if (messages.size() > 5) {
            List<Message> toDelete = messages.subList(5, messages.size());
            messageRepository.deleteAll(toDelete);
            messages = messages.subList(0, 5);
        }

        return messages.stream().map(Message::getContent).collect(Collectors.toList());
    }
}
