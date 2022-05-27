package com.samithiwat.post.post;

import com.github.javafaker.Faker;
import com.samithiwat.post.TestConfig;
import com.samithiwat.post.bloguser.BlogUserServiceImpl;
import com.samithiwat.post.grpc.blogpost.*;
import com.samithiwat.post.grpc.dto.BlogPost;
import com.samithiwat.post.grpc.dto.BlogUser;
import io.grpc.internal.testing.StreamRecorder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@SpringBootTest(properties = {
        "grpc.server.inProcessName=test-user", // Enable inProcess server
        "grpc.server.port=-1", // Disable external server
        "grpc.client.userService.address=in-process:test" // Configure the client to connect to the inProcess server
})
@SpringJUnitConfig(classes = {TestConfig.class})
@DirtiesContext
@ExtendWith(SpringExtension.class)
public class BlogPostServiceTest {

    @Spy
    private BlogUserServiceImpl blogUserService;

    @Spy
    private BlogPostRepository repository;

    @InjectMocks
    private BlogPostServiceImpl service;

    private List<BlogPost> postDtos;
    private BlogPost postDto;
    private List<com.samithiwat.post.post.entity.BlogPost> posts;
    private Optional<com.samithiwat.post.post.entity.BlogPost> post;
    private BlogUser userDto;
    private com.samithiwat.post.bloguser.entity.BlogUser user;
    private Faker faker;

    @BeforeEach
    void setup(){
        this.faker = new Faker();

        this.user = new com.samithiwat.post.bloguser.entity.BlogUser();
        user.setId(1l);
        user.setUserId(1l);

        this.userDto = BlogUser.newBuilder()
                .setId(1)
                .setFirstname(faker.name().firstName())
                .setLastname(faker.name().lastName())
                .setDisplayName(faker.name().username())
                .build();

        this.posts = new ArrayList<>();
        this.post = Optional.of(new com.samithiwat.post.post.entity.BlogPost(
                user,
                faker.lorem().word(),
                faker.lorem().sentence(),
                true,
                faker.date().past(1, TimeUnit.DAYS).toInstant()
        ));
        this.post.get().setId(1l);

        com.samithiwat.post.post.entity.BlogPost post2 = new com.samithiwat.post.post.entity.BlogPost(
                user,
                faker.lorem().word(),
                faker.lorem().sentence(),
                true,
                faker.date().past(1, TimeUnit.DAYS).toInstant()
        );
        post2.setId(2l);

        com.samithiwat.post.post.entity.BlogPost post3 = new com.samithiwat.post.post.entity.BlogPost(
                user,
                faker.lorem().word(),
                faker.lorem().sentence(),
                true,
                faker.date().past(1, TimeUnit.DAYS).toInstant()
        );
        post3.setId(3l);
        this.posts.add(this.post.get());
        this.posts.add(post2);
        this.posts.add(post3);

        this.postDtos = new ArrayList<BlogPost>();
        this.postDto = BlogPost.newBuilder()
                .setId(1)
                .setAuthor(this.userDto)
                .setSlug(post.get().getSlug())
                .setSummary(post.get().getSummary())
                .setIsPublish(post.get().getPublished())
                .setPublishDate(post.get().getPublishDate().toString())
                .build();

        BlogPost postDto2 = BlogPost.newBuilder()
                .setId(2)
                .setAuthor(this.userDto)
                .setSlug(post2.getSlug())
                .setSummary(post2.getSummary())
                .setIsPublish(post2.getPublished())
                .setPublishDate(post2.getPublishDate().toString())
                .build();

        BlogPost postDto3 = BlogPost.newBuilder()
                .setId(3)
                .setAuthor(this.userDto)
                .setSlug(post3.getSlug())
                .setSummary(post3.getSummary())
                .setIsPublish(post3.getPublished())
                .setPublishDate(post3.getPublishDate().toString())
                .build();

        this.postDtos.add(this.postDto);
        this.postDtos.add(postDto2);
        this.postDtos.add(postDto3);
    }

    @Test
    public void testFindOneSuccess() throws Exception{
        Mockito.doReturn(this.post).when(this.repository).findById(1l);
        Mockito.doReturn(this.userDto).when(this.blogUserService).findOne(1l);

        FindOnePostRequest req = FindOnePostRequest.newBuilder()
                .setId(1)
                .build();

        StreamRecorder<BlogPostResponse> res = StreamRecorder.create();

        service.findOne(req, res);

        if (!res.awaitCompletion(5, TimeUnit.SECONDS)){
            Assertions.fail();
        }

        List<BlogPostResponse> results = res.getValues();

        Assertions.assertEquals(1, results.size());

        BlogPostResponse result = results.get(0);

        Assertions.assertEquals(HttpStatus.OK.value(), result.getStatusCode());
        Assertions.assertEquals(0, result.getErrorsCount());
        Assertions.assertEquals(this.postDto, result.getData());
    }

    @Test
    public void testFindOneNotFoundPost() throws Exception {
        Mockito.doReturn(Optional.empty()).when(this.repository).findById(1l);
        Mockito.doReturn(this.userDto).when(this.blogUserService).findOne(1l);

        FindOnePostRequest req = FindOnePostRequest.newBuilder()
                .setId(1)
                .build();

        StreamRecorder<BlogPostResponse> res = StreamRecorder.create();

        service.findOne(req, res);

        if (!res.awaitCompletion(5, TimeUnit.SECONDS)){
            Assertions.fail();
        }

        List<BlogPostResponse> results = res.getValues();

        Assertions.assertEquals(1, results.size());

        BlogPostResponse result = results.get(0);

        Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), result.getStatusCode());
        Assertions.assertEquals(1, result.getErrorsCount());
        Assertions.assertEquals(BlogPost.newBuilder().build(), result.getData());
    }

    @Test
    public void testFindOneNotFoundUser() throws Exception {
        Mockito.doReturn(this.post).when(this.repository).findById(1l);
        Mockito.doReturn(null).when(this.blogUserService).findOne(1l);

        FindOnePostRequest req = FindOnePostRequest.newBuilder()
                .setId(1)
                .build();

        StreamRecorder<BlogPostResponse> res = StreamRecorder.create();

        service.findOne(req, res);

        if (!res.awaitCompletion(5, TimeUnit.SECONDS)){
            Assertions.fail();
        }

        List<BlogPostResponse> results = res.getValues();

        Assertions.assertEquals(1, results.size());

        BlogPostResponse result = results.get(0);

        Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), result.getStatusCode());
        Assertions.assertEquals(1, result.getErrorsCount());
        Assertions.assertEquals(BlogPost.newBuilder().build(), result.getData());
    }

    @Test
    public void testFindBySlug() throws Exception{
        Mockito.doReturn(this.post).when(this.repository).findBySlug(this.postDto.getSlug());
        Mockito.doReturn(this.userDto).when(this.blogUserService).findOne(1l);

        FindBySlugPostRequest req = FindBySlugPostRequest.newBuilder()
                .setSlug(this.postDto.getSlug())
                .build();

        StreamRecorder<BlogPostResponse> res = StreamRecorder.create();

        service.findBySlug(req, res);

        if (!res.awaitCompletion(5, TimeUnit.SECONDS)){
            Assertions.fail();
        }

        List<BlogPostResponse> results = res.getValues();

        Assertions.assertEquals(1, results.size());

        BlogPostResponse result = results.get(0);

        Assertions.assertEquals(HttpStatus.OK.value(), result.getStatusCode());
        Assertions.assertEquals(0, result.getErrorsCount());
        Assertions.assertEquals(this.postDto, result.getData());
    }

    @Test
    public void testFindBySlugNotFoundPost() throws Exception {
        Mockito.doReturn(Optional.empty()).when(this.repository).findBySlug(this.postDto.getSlug());
        Mockito.doReturn(this.userDto).when(this.blogUserService).findOne(1l);

        FindBySlugPostRequest req = FindBySlugPostRequest.newBuilder()
                .setSlug(this.postDto.getSlug())
                .build();

        StreamRecorder<BlogPostResponse> res = StreamRecorder.create();

        service.findBySlug(req, res);

        if (!res.awaitCompletion(5, TimeUnit.SECONDS)){
            Assertions.fail();
        }

        List<BlogPostResponse> results = res.getValues();

        Assertions.assertEquals(1, results.size());

        BlogPostResponse result = results.get(0);

        Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), result.getStatusCode());
        Assertions.assertEquals(1, result.getErrorsCount());
        Assertions.assertEquals(BlogPost.newBuilder().build(), result.getData());
    }

    @Test
    public void testFindBySlugNotFoundUser() throws Exception {
        Mockito.doReturn(Optional.empty()).when(this.repository).findBySlug(this.postDto.getSlug());
        Mockito.doReturn(null).when(this.blogUserService).findOne(1l);

        FindBySlugPostRequest req = FindBySlugPostRequest.newBuilder()
                .setSlug(this.postDto.getSlug())
                .build();

        StreamRecorder<BlogPostResponse> res = StreamRecorder.create();

        service.findBySlug(req, res);

        if (!res.awaitCompletion(5, TimeUnit.SECONDS)){
            Assertions.fail();
        }

        List<BlogPostResponse> results = res.getValues();

        Assertions.assertEquals(1, results.size());

        BlogPostResponse result = results.get(0);

        Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), result.getStatusCode());
        Assertions.assertEquals(1, result.getErrorsCount());
        Assertions.assertEquals(BlogPost.newBuilder().build(), result.getData());
    }

    @Test
    public void testCreateSuccess() throws Exception{
        Mockito.doReturn(this.userDto).when(this.blogUserService).findOne(1l);
        Mockito.doReturn(this.user).when(this.blogUserService).findOneEntityByUserId(1l);
        Mockito.doReturn(this.post.get()).when(this.repository).save(Mockito.any());

        CreatePostRequest req = CreatePostRequest.newBuilder()
                .setAuthorId(this.postDto.getAuthor().getId())
                .setSlug(this.postDto.getSlug())
                .setSummary(this.postDto.getSummary())
                .setIsPublish(this.postDto.getIsPublish())
                .setPublishDate(this.postDto.getPublishDate())
                .build();

        StreamRecorder<BlogPostResponse> res = StreamRecorder.create();

        service.create(req, res);

        if (!res.awaitCompletion(5, TimeUnit.SECONDS)){
            Assertions.fail();
        }

        List<BlogPostResponse> results = res.getValues();

        Assertions.assertEquals(1, results.size());

        BlogPostResponse result = results.get(0);

        Assertions.assertEquals(HttpStatus.CREATED.value(), result.getStatusCode());
        Assertions.assertEquals(0, result.getErrorsCount());
        Assertions.assertEquals(this.postDto, result.getData());
    }

    @Test
    public void testCreateNotFoundUser() throws Exception{
        Mockito.doReturn(null).when(this.blogUserService).findOne(1l);
        Mockito.doReturn(this.user).when(this.blogUserService).findOneEntityByUserId(1l);
        Mockito.doReturn(this.post).when(this.repository).save(Mockito.any());

        CreatePostRequest req = CreatePostRequest.newBuilder()
                .setAuthorId(this.postDto.getAuthor().getId())
                .setSlug(this.postDto.getSlug())
                .setSummary(this.postDto.getSummary())
                .setIsPublish(this.postDto.getIsPublish())
                .setPublishDate(this.postDto.getPublishDate())
                .build();

        StreamRecorder<BlogPostResponse> res = StreamRecorder.create();

        service.create(req, res);

        if (!res.awaitCompletion(5, TimeUnit.SECONDS)){
            Assertions.fail();
        }

        List<BlogPostResponse> results = res.getValues();

        Assertions.assertEquals(1, results.size());

        BlogPostResponse result = results.get(0);

        Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), result.getStatusCode());
        Assertions.assertEquals(1, result.getErrorsCount());
        Assertions.assertEquals(BlogPost.newBuilder().build(), result.getData());
    }

    @Test
    public void testCreateDuplicatedSlug() throws Exception{
        Mockito.doReturn(this.userDto).when(this.blogUserService).findOne(1l);
        Mockito.doReturn(this.user).when(this.blogUserService).findOneEntityByUserId(1l);
        Mockito.doThrow(new DataIntegrityViolationException("Duplicated slug")).when(this.repository).save(Mockito.any());

        CreatePostRequest req = CreatePostRequest.newBuilder()
                .setAuthorId(this.postDto.getAuthor().getId())
                .setSlug(this.postDto.getSlug())
                .setSummary(this.postDto.getSummary())
                .setIsPublish(this.postDto.getIsPublish())
                .setPublishDate(this.postDto.getPublishDate())
                .build();

        StreamRecorder<BlogPostResponse> res = StreamRecorder.create();

        service.create(req, res);

        if (!res.awaitCompletion(5, TimeUnit.SECONDS)){
            Assertions.fail();
        }

        List<BlogPostResponse> results = res.getValues();

        Assertions.assertEquals(1, results.size());

        BlogPostResponse result = results.get(0);

        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), result.getStatusCode());
        Assertions.assertEquals(1, result.getErrorsCount());
        Assertions.assertEquals(BlogPost.newBuilder().build(), result.getData());
    }

    @Test
    public void testUpdateSuccess() throws Exception {
        Mockito.doReturn(true).when(this.repository).update(1, this.postDto.getSlug(), this.postDto.getSummary(), this.postDto.getIsPublish(), Instant.parse(this.postDto.getPublishDate()));

        UpdatePostRequest req = UpdatePostRequest.newBuilder()
                .setId(1)
                .setSlug(this.postDto.getSlug())
                .setSummary(this.postDto.getSummary())
                .setIsPublish(this.postDto.getIsPublish())
                .setPublishDate(this.postDto.getPublishDate())
                .build();

        StreamRecorder<BlogPostStatusResponse> res = StreamRecorder.create();

        service.update(req, res);

        if (!res.awaitCompletion(5, TimeUnit.SECONDS)){
            Assertions.fail();
        }

        List<BlogPostStatusResponse> results = res.getValues();

        Assertions.assertEquals(1, results.size());

        BlogPostStatusResponse result = results.get(0);

        Assertions.assertEquals(HttpStatus.OK.value(), result.getStatusCode());
        Assertions.assertEquals(0, result.getErrorsCount());
        Assertions.assertTrue(result.getData());
    }

    @Test
    public void testUpdateNotFoundPost() throws Exception {
        Mockito.doReturn(false).when(this.repository).update(1, this.postDto.getSlug(), this.postDto.getSummary(), this.postDto.getIsPublish(), Instant.parse(this.postDto.getPublishDate()));

        UpdatePostRequest req = UpdatePostRequest.newBuilder()
                .setId(1)
                .setSlug(this.postDto.getSlug())
                .setSummary(this.postDto.getSummary())
                .setIsPublish(this.postDto.getIsPublish())
                .setPublishDate(this.postDto.getPublishDate())
                .build();

        StreamRecorder<BlogPostStatusResponse> res = StreamRecorder.create();

        service.update(req, res);

        if (!res.awaitCompletion(5, TimeUnit.SECONDS)){
            Assertions.fail();
        }

        List<BlogPostStatusResponse> results = res.getValues();

        Assertions.assertEquals(1, results.size());

        BlogPostStatusResponse result = results.get(0);

        Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), result.getStatusCode());
        Assertions.assertEquals(1, result.getErrorsCount());
        Assertions.assertFalse(result.getData());
    }

    @Test
    public void testUpdateDuplicatedSlug() throws Exception{
        Mockito.doThrow(new DataIntegrityViolationException("Duplicated slug")).when(this.repository).update(1, this.postDto.getSlug(), this.postDto.getSummary(), this.postDto.getIsPublish(), Instant.parse(this.postDto.getPublishDate()));

        UpdatePostRequest req = UpdatePostRequest.newBuilder()
                .setId(1)
                .setSlug(this.postDto.getSlug())
                .setSummary(this.postDto.getSummary())
                .setIsPublish(this.postDto.getIsPublish())
                .setPublishDate(this.postDto.getPublishDate())
                .build();

        StreamRecorder<BlogPostStatusResponse> res = StreamRecorder.create();

        service.update(req, res);

        if (!res.awaitCompletion(5, TimeUnit.SECONDS)){
            Assertions.fail();
        }

        List<BlogPostStatusResponse> results = res.getValues();

        Assertions.assertEquals(1, results.size());

        BlogPostStatusResponse result = results.get(0);

        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), result.getStatusCode());
        Assertions.assertEquals(1, result.getErrorsCount());
        Assertions.assertFalse(result.getData());
    }

    @Test
    public void testDeleteSuccess() throws Exception {
        Mockito.doNothing().when(this.repository).deleteById(1l);

        DeletePostRequest req = DeletePostRequest.newBuilder()
                .setId(1)
                .build();

        StreamRecorder<BlogPostStatusResponse> res = StreamRecorder.create();

        service.delete(req, res);

        if (!res.awaitCompletion(5, TimeUnit.SECONDS)){
            Assertions.fail();
        }

        List<BlogPostStatusResponse> results = res.getValues();

        Assertions.assertEquals(1, results.size());

        BlogPostStatusResponse result = results.get(0);

        Assertions.assertEquals(HttpStatus.OK.value(), result.getStatusCode());
        Assertions.assertEquals(0, result.getErrorsCount());
        Assertions.assertTrue(result.getData());
    }

    @Test
    public void testDeleteNotFoundPost() throws Exception {
        Mockito.doThrow(new EmptyResultDataAccessException("Not found post", 1)).when(this.repository).deleteById(1l);

        DeletePostRequest req = DeletePostRequest.newBuilder()
                .setId(1)
                .build();

        StreamRecorder<BlogPostStatusResponse> res = StreamRecorder.create();

        service.delete(req, res);

        if (!res.awaitCompletion(5, TimeUnit.SECONDS)){
            Assertions.fail();
        }

        List<BlogPostStatusResponse> results = res.getValues();

        Assertions.assertEquals(1, results.size());

        BlogPostStatusResponse result = results.get(0);

        Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), result.getStatusCode());
        Assertions.assertEquals(1, result.getErrorsCount());
        Assertions.assertFalse(result.getData());
    }
}
