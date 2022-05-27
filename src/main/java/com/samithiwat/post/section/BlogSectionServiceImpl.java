package com.samithiwat.post.section;

import com.samithiwat.post.bloguser.BlogUserServiceImpl;
import com.samithiwat.post.common.ContentType;
import com.samithiwat.post.grpc.blogsection.*;
import com.samithiwat.post.grpc.dto.BlogPostSection;
import com.samithiwat.post.grpc.dto.PostContentType;
import com.samithiwat.post.section.entity.BlogSection;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;

public class BlogSectionServiceImpl extends BlogPostSectionServiceGrpc.BlogPostSectionServiceImplBase {
    @Autowired
    BlogSectionRepository repository;

    public BlogSectionServiceImpl() {}

    public BlogSectionServiceImpl(BlogSectionRepository repository) {
        this.repository = repository;
    }

    @Override
    public void findOne(FindOnePostSectionRequest request, StreamObserver<BlogPostSectionResponse> responseObserver) {
        BlogPostSectionResponse.Builder res = BlogPostSectionResponse.newBuilder();

        BlogSection section = this.repository.findById(Long.valueOf(request.getId())).orElse(null);
        if(section == null){
            res.setStatusCode(HttpStatus.NOT_FOUND.value())
                    .addErrors("Not found section");

            responseObserver.onNext(res.build());
            responseObserver.onCompleted();
            return;
        }

        com.samithiwat.post.grpc.dto.BlogPostSection result = BlogPostSection.newBuilder()
                .setId(Math.toIntExact(section.getId()))
                .setPos(section.getPos())
                .setContentTypeValue(RawToDtoContentType(section.getContentType()).getNumber())
                .setContent(section.getContent())
                .build();

        res.setStatusCode(HttpStatus.OK.value())
                .setData(result);

        responseObserver.onNext(res.build());
        responseObserver.onCompleted();
    }

    @Override
    public void create(CreatePostSectionRequest request, StreamObserver<BlogPostSectionResponse> responseObserver) {
        BlogPostSectionResponse.Builder res = BlogPostSectionResponse.newBuilder();

        // TODO: Implement relationship with post

        BlogSection sectionDto = new BlogSection(
                request.getPos(),
                DtoToRawContentType(request.getContentType()),
                request.getContent()
        );

        BlogSection section = this.repository.save(sectionDto);
        com.samithiwat.post.grpc.dto.BlogPostSection result = BlogPostSection.newBuilder()
                .setId(Math.toIntExact(section.getId()))
                .setPos(section.getPos())
                .setContentTypeValue(RawToDtoContentType(section.getContentType()).getNumber())
                .setContent(section.getContent())
                .build();

        res.setStatusCode(HttpStatus.CREATED.value())
                .setData(result);

        responseObserver.onNext(res.build());
        responseObserver.onCompleted();
    }

    @Override
    public void update(UpdatePostSectionRequest request, StreamObserver<BlogPostSectionStatusResponse> responseObserver) {
        BlogPostSectionStatusResponse.Builder res = BlogPostSectionStatusResponse.newBuilder();

        // TODO: Implement relationship with post

        boolean isUpdated = this.repository.update(request.getId(), request.getPos(), request.getContent());

        if(!isUpdated) {

            res.setStatusCode(HttpStatus.NOT_FOUND.value())
                    .addErrors("Not found section")
                    .setData(false);

            responseObserver.onNext(res.build());
            responseObserver.onCompleted();
            return;
        }

        res.setStatusCode(HttpStatus.OK.value())
                .setData(true);

        responseObserver.onNext(res.build());
        responseObserver.onCompleted();
    }

    @Override
    public void delete(DeletePostSectionRequest request, StreamObserver<BlogPostSectionStatusResponse> responseObserver) {
        BlogPostSectionStatusResponse.Builder res = BlogPostSectionStatusResponse.newBuilder();

        try{
            this.repository.deleteById((long) request.getId());
            res.setStatusCode(HttpStatus.OK.value())
                    .setData(true);

            responseObserver.onNext(res.build());
            responseObserver.onCompleted();
        }catch(EmptyResultDataAccessException err){
            res.setStatusCode(HttpStatus.NOT_FOUND.value())
                    .addErrors("Not found section")
                    .setData(false);

            responseObserver.onNext(res.build());
            responseObserver.onCompleted();
        }
    }

    private ContentType DtoToRawContentType(PostContentType contentType){
        return switch(contentType){
            case TEXT -> ContentType.TEXT;
            case IMAGE -> ContentType.IMAGE;
            case CODE -> ContentType.CODE;
            default -> null;
        };
    }

    private PostContentType RawToDtoContentType(ContentType contentType){
       return switch (contentType){
           case TEXT -> PostContentType.TEXT;
           case IMAGE -> PostContentType.IMAGE;
           case CODE -> PostContentType.CODE;
           default -> null;
        };
    }
}
