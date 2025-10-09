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

/**
 * RAG(Retrieval-Augmented Generation) 파이프라인과 관련된 Spring Bean 설정을 담당합니다.
 * 이 설정 클래스는 문서 로딩, 변환, 임베딩, 저장을 포함한 ETL(Extract, Transform, Load) 프로세스와
 * 검색 증강을 위한 ChatClient 어드바이저를 구성합니다.
 */
@Configuration
public class RagConfig {

    /**
     * 지정된 위치(classpath:spring-ai.pdf)의 문서를 로드하는 DocumentReader Bean을 생성합니다.
     * Apache Tika를 사용하여 PDF와 같은 다양한 형식의 파일에서 텍스트를 추출합니다.
     *
     * @param documentsLocationPattern 문서 파일의 위치 패턴
     * @return TikaDocumentReader 배열
     * @throws IOException 파일 로딩 중 오류 발생 시
     */
    @Bean
    public DocumentReader[] documentReaders(@Value("${app.rag.documents-location-pattern}") String documentsLocationPattern) throws IOException {
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources(documentsLocationPattern);
        return Arrays.stream(resources).map(TikaDocumentReader::new).toArray(DocumentReader[]::new);
    }

    /**
     * 문서를 지정된 길이의 청크(chunk)로 분할하는 DocumentTransformer Bean을 생성합니다.
     * 이는 긴 문서를 LLM이 처리하기 쉬운 작은 단위로 나누기 위해 필수적입니다.
     *
     * @return LengthTextSplitter 인스턴스
     */
    @Bean
    public DocumentTransformer textSplitter() {
        return new LengthTextSplitter(1000, 200); // TIP: 여러가지 값을 주며 테스트필요.
    }

    /**
     * 문서 청크에서 LLM을 사용하여 키워드를 추출하고 메타데이터에 추가하는 DocumentTransformer Bean을 생성합니다.
     * 이렇게 생성된 키워드는 나중에 문서 검색 시 필터링에 사용될 수 있습니다.
     *
     * @param chatModel 키워드 추출에 사용될 ChatModel
     * @return KeywordMetadataEnricher 인스턴스
     */
    @Bean
    public DocumentTransformer keywordMetadataEnricher(ChatModel chatModel) {
        return new KeywordMetadataEnricher(chatModel, 4);
    }

    /**
     * 처리된 문서를 JSON 형식으로 콘솔에 예쁘게 출력하는 DocumentWriter Bean을 생성합니다.
     * ETL 파이프라인의 결과를 디버깅하는 데 유용합니다.
     *
     * @param objectMapper JSON 직렬화를 위한 ObjectMapper
     * @return DocumentWriter 인스턴스
     */
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

    /**
     * 인메모리(In-memory) 벡터 저장소인 SimpleVectorStore를 Bean으로 생성합니다.
     * 'app.vectorstore.in-memory.enabled=true'일 때만 활성화됩니다.
     *
     * @param embeddingModel 문서를 벡터로 변환하기 위한 EmbeddingModel
     * @return VectorStore 인스턴스
     */
    @ConditionalOnProperty(prefix = "app.vectorstore.in-memory", name = "enabled", havingValue = "true")
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    /**
     * 애플리케이션 시작 시 ETL(Extract, Transform, Load) 파이프라인을 실행하는 ApplicationRunner Bean을 생성합니다.
     * 'app.etl.pipeline.init=true'일 때만 실행됩니다.
     * 문서를 읽고(Extract), 청크로 나누고 키워드를 추가한(Transform) 후, 설정된 DocumentWriter(예: VectorStore)에 저장합니다(Load).
     *
     * @param documentReaders 문서를 읽는 Reader
     * @param textSplitter 텍스트를 분할하는 Transformer
     * @param keywordMetadataEnricher 키워드를 추가하는 Transformer
     * @param documentWriters 문서를 저장하는 Writer(VectorStore 등)
     * @return ApplicationRunner 인스턴스
     */
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

    /**
     * RAG(검색 증강 생성)를 ChatClient에 통합하기 위한 Advisor Bean을 생성합니다.
     * 이 어드바이저는 사용자 쿼리를 받아 확장/변환하고, VectorStore에서 관련 문서를 검색한 후,
     * 검색된 문서를 컨텍스트로 사용하여 LLM에 최종 답변을 요청하는 전체 RAG 프로세스를 조정합니다.
     *
     * @param vectorStore 관련 문서 검색을 위한 VectorStore
     * @param chatClientBuilder 쿼리 확장/변환에 LLM을 사용하기 위한 ChatClient.Builder
     * @param documentPostProcessor 검색된 문서의 후처리기 (선택 사항)
     * @return RetrievalAugmentationAdvisor 인스턴스
     */
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

    /**
     * RAG 파이프라인에서 검색된 문서들을 CLI에 출력하는 DocumentPostProcessor Bean을 생성합니다.
     * 'app.cli.enabled=true'일 때만 활성화됩니다.
     * 검색 결과의 투명성을 높이고 디버깅에 도움을 줍니다.
     *
     * @return DocumentPostProcessor 인스턴스
     */
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
