package com.portshipping.dto;

import com.portshipping.entity.HelpArticle;
import java.time.Instant;

public class HelpArticleDto {
    private Long id;
    private String title;
    private String slug;
    private String category;
    private String content;
    private boolean published;
    private String authorName;
    private Instant createdAt;
    private Instant updatedAt;

    public HelpArticleDto() {}

    public static HelpArticleDto fromEntity(HelpArticle article) {
        HelpArticleDto dto = new HelpArticleDto();
        dto.setId(article.getId());
        dto.setTitle(article.getTitle());
        dto.setSlug(article.getSlug());
        dto.setCategory(article.getCategory());
        dto.setContent(article.getContent());
        dto.setPublished(article.isPublished());
        if (article.getAuthor() != null) {
            dto.setAuthorName(article.getAuthor().getName());
        }
        dto.setCreatedAt(article.getCreatedAt());
        dto.setUpdatedAt(article.getUpdatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
