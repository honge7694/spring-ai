package kr.hui.springai.rag;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class LengthTextSplitter extends TextSplitter {

    private final int chunkSize;
    private final int chunkOverlap;

    // TextSplitter는 DocumentTransformer를 상속하고 있음.
    @Override
    protected List<String> splitText(String text) {
        List<String> chunks = new ArrayList<>();

        if (!StringUtils.hasText(text)) {
            return chunks;
        }

        int textLength = text.length();
        if (textLength <= chunkOverlap) {
            chunks.add(text);
            return chunks;
        }

        int position = 0;
        while (position < textLength) {
            int end = Math.min(position + chunkOverlap, textLength);
            chunks.add(text.substring(position, end));
            int nextPosition = end - chunkOverlap;
            if (nextPosition <= position) {
                break;
            }
            position = nextPosition;
        }
        return chunks;
    }
}
