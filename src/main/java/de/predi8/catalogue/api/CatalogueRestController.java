package de.predi8.catalogue.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.predi8.catalogue.error.NotFoundException;
import de.predi8.catalogue.event.Operation;
import de.predi8.catalogue.model.Article;
import de.predi8.catalogue.repository.ArticleRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/articles")
public class CatalogueRestController {

  public static final String PRICE = "price";
  public static final String NAME = "name";
  private ArticleRepository repo;
  private KafkaTemplate<String, Operation> kafka;
  final private ObjectMapper mapper;

  public CatalogueRestController(ArticleRepository repo, KafkaTemplate<String, Operation> kafka, ObjectMapper mapper) {
    this.repo = repo;
    this.kafka = kafka;
    this.mapper = mapper;
  }

  @GetMapping
  public List<Article> index() {
    return repo.findAll();
  }

  @GetMapping("/count")
  public long count() {
    return repo.count();
  }

  @GetMapping("{id}")
  public Article get(@PathVariable String id) {
    return repo.findById(id).orElseThrow(NotFoundException::new);
  }

  @PostMapping
  public ResponseEntity<Article> createArticle(@RequestBody Article article, UriComponentsBuilder builder) {
    System.out.println("article = " + article);

    String uuid = UUID.randomUUID().toString();
    article.setUuid(uuid);

    Article save = repo.save(article);
    return ResponseEntity.created(builder.path("/articles/" + uuid).build().toUri()).body(save);
  }

  @PutMapping("/{id}")
  public void change(@PathVariable String id, @RequestBody Article article) {
    get(id);

    article.setUuid(id);
    repo.save(article);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable String id) {
    repo.delete(get(id));
  }

  @PatchMapping("/{id}")
  public void patch(@PathVariable String id, @RequestBody JsonNode jsonNode) {
    Article old = get(id);

    if (jsonNode.hasNonNull(PRICE)) {
				old.setPrice(new BigDecimal(jsonNode.get(PRICE).asDouble()));
    }

    if(jsonNode.has(NAME)) {
      old.setName(jsonNode.get(NAME).asText());
    }

    repo.save(old);
  }
}