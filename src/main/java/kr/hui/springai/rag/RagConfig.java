package kr.hui.springai.rag;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.document.DocumentWriter;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

@Configuration
public class RagConfig {

    @Bean
    public DocumentReader[] documentReaders(@Value("classpath:spring-ai.pdf") String documentsLocationPattern) throws IOException {
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources(documentsLocationPattern);
        return Arrays.stream(resources).map(TikaDocumentReader::new).toArray(DocumentReader[]::new);
    }

    @Bean
    public DocumentTransformer textSplitter() {
        return new LengthTextSplitter(1000, 200); // TIP: 여러가지 값을 주며 테스트필요.
    }

    @Bean
    public DocumentTransformer keywordMetadataEnricher(ChatModel chatModel) {
        return new KeywordMetadataEnricher(chatModel, 4);
    }

    @Bean
    public DocumentWriter jsonConsoleDocumentWriter(ObjectMapper objectMapper) {
        return documents -> {
            System.out.println("[INFO] Writing Json console document...");
            try {
                System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(documents));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            System.out.println("=======================================");
        };
    }

    @ConditionalOnProperty(prefix = "app.vectorstore.in-memory", name = "enabled", havingValue = "true")
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    @ConditionalOnProperty(prefix = "app.etl.pipeline", name = "init", havingValue = "true")
    @Order(1)
    @Bean
    public ApplicationRunner initEtlPipeLine(DocumentReader[] documentReaders,
                                             DocumentTransformer textSplitter,
                                             DocumentTransformer keywordMetadataEnricher,
                                             DocumentWriter[] documentWriters) {
        return args -> {
            Arrays.stream(documentReaders).map(DocumentReader::read) // 문서를 읽음 (Extract)
                    .map(textSplitter) // chunkData로 자름 (Transform)
                    .map(keywordMetadataEnricher) // chunkData를 keyword를 Metadata를 채운 후
                    .forEach(documents -> Arrays.stream(documentWriters) // vectorStore 저장 (Load)
                            .forEach(documentWriter -> documentWriter.write(documents)));
        };
    }

    @Bean
    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(VectorStore vectorStore,
                                                                     ChatClient.Builder chatClientBuilder,
                                                                     Optional<DocumentPostProcessor> documentPostProcessor) {

        RetrievalAugmentationAdvisor.Builder documentRetrieverBuilder = RetrievalAugmentationAdvisor.builder()
                .queryExpander(MultiQueryExpander.builder().chatClientBuilder(chatClientBuilder).build()) // 쿼리 확장기 다수의 쿼리를 하나의 결과로 묶어줌 (LLM 도움 필요)
                .queryTransformers(TranslationQueryTransformer.builder().chatClientBuilder(chatClientBuilder).targetLanguage("korean").build()) // 한국어로 바꿔주는 역할 (LLM 도움 필요)
                .queryAugmenter(ContextualQueryAugmenter.builder().allowEmptyContext(true).build()) //
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .similarityThreshold(0.3) // 값을 조정하며 튜닝해야 함
                        .topK(3)
                        .build());
        documentPostProcessor.ifPresent(documentRetrieverBuilder::documentPostProcessors);
        return documentRetrieverBuilder.build();
    }

    // RAG 결과를 CLI에 나타내는 코드
    @ConditionalOnProperty(prefix = "app.cli", name = "enabled", havingValue = "true")
    @Bean
    public DocumentPostProcessor documentPostProcessor() {
        return (query, documents) -> {
            System.out.println("\n [ Search Results ]");
            System.out.println("========================================");

            if (documents == null || documents.isEmpty()) {
                System.out.println("No documents found");
                System.out.println("========================================");
                return documents;
            }

            for (int i = 0; i < documents.size(); i++) {
                Document document = documents.get(i);
                System.out.printf("%d Document, Score: %.2f%n", i + 1, document.getScore());
                System.out.println("----------------------------------------");
                Optional.ofNullable(document.getText()).stream()
                        .map(text -> text.split("\n")).flatMap(Arrays::stream)
                        .forEach(line -> System.out.printf("%s%n", line));
                System.out.println("========================================");
            }
            System.out.print("\n[ RAG 사용 응답 ] \n\n");
            return documents;
        };
    }
}
