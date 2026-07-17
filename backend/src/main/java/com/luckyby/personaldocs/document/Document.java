package com.luckyby.personaldocs.document;

import com.luckyby.personaldocs.user.AppUser;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "documents")
public class Document {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(nullable = false) private String originalName;
  @Column(nullable = false, unique = true) private String storageKey;
  @Column(nullable = false) private String contentType;
  @Column(nullable = false) private long size;
  @Column(nullable = false) private Instant createdAt = Instant.now();
  @ManyToOne(optional = false, fetch = FetchType.LAZY) private AppUser owner;
  public Long getId() { return id; } public String getOriginalName() { return originalName; } public String getStorageKey() { return storageKey; } public String getContentType() { return contentType; } public long getSize() { return size; } public Instant getCreatedAt() { return createdAt; } public AppUser getOwner() { return owner; }
  public void setOriginalName(String v) { originalName=v; } public void setStorageKey(String v) { storageKey=v; } public void setContentType(String v) { contentType=v; } public void setSize(long v) { size=v; } public void setOwner(AppUser v) { owner=v; }
}
