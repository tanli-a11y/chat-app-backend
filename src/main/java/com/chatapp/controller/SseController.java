package com.chatapp.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@RestController
@RequestMapping("/api/sse")
public class SseController {

    private static final List<String> DEMO_MESSAGES = List.of(
            "你好！我是 AI 助手。",
            "我可以帮你回答问题、写作、编程等。",
            "这是一个类似于 Gemini 的流式响应演示。",
            "消息通过 Server-Sent Events (SSE) 实时推送。",
            "感谢使用！"
    );

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam(required = false) String prompt) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5 minutes timeout

        scheduler.execute(() -> {
            try {
                if (prompt != null && !prompt.isBlank()) {
                    emitter.send(SseEmitter.event()
                            .name("message")
                            .data("你问的是：「" + prompt + "」"));
                    Thread.sleep(300);
                }
                for (int i = 0; i < DEMO_MESSAGES.size(); i++) {
                    emitter.send(SseEmitter.event()
                            .name("message")
                            .data(DEMO_MESSAGES.get(i)));
                    if (i < DEMO_MESSAGES.size() - 1) {
                        Thread.sleep(500);
                    }
                }
                emitter.send(SseEmitter.event().name("done").data("true"));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                emitter.completeWithError(e);
            }
        });

        emitter.onTimeout(() -> {
            emitter.complete();
        });
        emitter.onError(e -> {
            emitter.completeWithError(e);
        });

        return emitter;
    }
}
