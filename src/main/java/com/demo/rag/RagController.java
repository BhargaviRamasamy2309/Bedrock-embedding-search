package com.demo.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeClient;
import software.amazon.awssdk.services.bedrockagentruntime.model.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RagController {

    private final BedrockAgentRuntimeClient bedrockAgentRuntimeClient;
    private final String knowledgeBaseId;
    private final String modelArn;


    public RagController(BedrockAgentRuntimeClient bedrockAgentRuntimeClient, @Value("${rag.knowledge-base-id}") String knowledgeBaseId, @Value("${rag.model-arn}") String modelArn) {
        this.bedrockAgentRuntimeClient = bedrockAgentRuntimeClient;
        this.knowledgeBaseId = knowledgeBaseId;
        this.modelArn = modelArn;
    }

    public record Question(String Question){}
    public record Chunk(String text,Double score,String source){}
    public record Source(String text, String uri){}
    public record Answer(String answer, List<Source> sources){}

    private KnowledgeBaseRetrievalConfiguration retrievalConfiguration(){
        return KnowledgeBaseRetrievalConfiguration.builder().vectorSearchConfiguration(v->v.numberOfResults(3)).build();
    }

    @PostMapping("/search")
    public List<Chunk> search(@RequestBody Question request){
        var response = bedrockAgentRuntimeClient.retrieve(RetrieveRequest.builder().knowledgeBaseId(knowledgeBaseId).retrievalQuery(q->q.text(request.Question.trim())).retrievalConfiguration(retrievalConfiguration()).build());
        return response.retrievalResults().stream().map(result->new Chunk(result.content().text(), result.score(),result.location().s3Location().toString())).toList();
    }

    @PostMapping("/ask")
    public Answer ask(@RequestBody Question request){
        var response = bedrockAgentRuntimeClient.retrieveAndGenerate(RetrieveAndGenerateRequest.builder().input(i->i.text(request.Question)).retrieveAndGenerateConfiguration(config->config.type(RetrieveAndGenerateType.KNOWLEDGE_BASE).knowledgeBaseConfiguration(kb->kb.knowledgeBaseId(knowledgeBaseId).modelArn(modelArn).retrievalConfiguration(retrievalConfiguration()))).build());
        var sources= response.citations().stream().flatMap(citation -> citation.retrievedReferences().stream()).map(retrievedReference -> new Source(retrievedReference.content().text(),retrievedReference.location().toString())).distinct().toList();
        return new Answer(response.output().text(),sources);
    }
}
