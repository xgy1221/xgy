package com.xgy.cloud.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** 活动 highlights / gallery：兼容 JSON 数组与逗号分隔文本 */
public final class TextListUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TextListUtils() {
    }

    public static List<String> parse(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Collections.emptyList();
        }
        String text = raw.trim();
        if (text.startsWith("[")) {
            try {
                List<String> list = MAPPER.readValue(text, new TypeReference<List<String>>() {
                });
                return list == null ? Collections.emptyList() : list.stream()
                        .filter(StringUtils::hasText)
                        .map(String::trim)
                        .collect(Collectors.toList());
            } catch (Exception ignored) {
                // fall through
            }
        }
        return Arrays.stream(text.split("[,，;；\\n]"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public static String join(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(list.stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            return String.join(",", list);
        }
    }
}
