package com.luckyby.personaldocs.document;

import com.luckyby.personaldocs.user.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final DocumentRepository docs;
    private final UserRepository users;
    private final OfficePreviewService previews;
    private final Path root;

    public DocumentController(DocumentRepository docs, UserRepository users, OfficePreviewService previews, @Value("${app.storage.path}") String storage)
            throws IOException {
        this.docs = docs;
        this.users = users;
        this.previews = previews;
        root = Paths.get(storage).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    @GetMapping
    public List<DocumentView> list(Authentication a) {
        return docs.findByOwnerIdOrderByCreatedAtDesc(user(a).getId()).stream().map(DocumentView::from).toList();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file, Authentication a) throws IOException {
        if (file.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("message", "文件不能为空"));

        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("untitled");
        String key = UUID.randomUUID() + extension(name);
        Path target = root.resolve(key).normalize();
        file.transferTo(target);
        Document d = new Document();
        d.setOriginalName(name);
        d.setStorageKey(key);
        d.setContentType(Optional.ofNullable(file.getContentType()).orElse("application/octet-stream"));
        d.setSize(file.getSize());
        d.setOwner(user(a));
        docs.save(d);
        return ResponseEntity.status(HttpStatus.CREATED).body(DocumentView.from(d));
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<Resource> content(@PathVariable Long id, Authentication a) throws IOException {
        Document d = owned(id, a);
        Path p = root.resolve(d.getStorageKey());
        Resource r = new org.springframework.core.io.UrlResource(p.toUri());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(d.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + safeName(d.getOriginalName()) + "\"")
                .body(r);
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<Resource> preview(@PathVariable Long id, Authentication a) throws IOException {
        Document d = owned(id, a);
        Path p = previews.preview(d, root.resolve(d.getStorageKey()));
        Resource r = new org.springframework.core.io.UrlResource(p.toUri());
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"preview.pdf\"").body(r);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication a) throws IOException {
        Document d = owned(id, a);
        Files.deleteIfExists(root.resolve(d.getStorageKey()));
        previews.deletePreview(d);
        docs.delete(d);
        return ResponseEntity.noContent().build();
    }

    private AppUser user(Authentication a) {
        return users.findByUsername(a.getName()).orElseThrow();
    }

    private Document owned(Long id, Authentication a) {
        return docs.findByIdAndOwnerId(id, user(a).getId()).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private String extension(String n) {
        int i = n.lastIndexOf('.');
        return i >= 0 ? n.substring(i) : "";
    }

    private String safeName(String n) {
        return n.replaceAll("[\\r\\n\\\"]", "_");
    }

    public record DocumentView(Long id, String originalName, String contentType, long size,
                               java.time.Instant createdAt) {
        static DocumentView from(Document d) {
            return new DocumentView(d.getId(), d.getOriginalName(), d.getContentType(), d.getSize(), d.getCreatedAt());
        }
    }
}
