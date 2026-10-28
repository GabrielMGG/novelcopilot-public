package com.novelcopilot.backend.service;

public class ChapterVersionConflictException extends RuntimeException {

    private final Integer chapterId;
    private final Long requestedVersion;
    private final Long currentVersion;

    public ChapterVersionConflictException(
            Integer chapterId,
            Long requestedVersion,
            Long currentVersion
    ) {
        this(chapterId, requestedVersion, currentVersion, null);
    }

    public ChapterVersionConflictException(
            Integer chapterId,
            Long requestedVersion,
            Long currentVersion,
            Throwable cause
    ) {
        super("O capítulo foi alterado em outra aba ou sessão.", cause);
        this.chapterId = chapterId;
        this.requestedVersion = requestedVersion;
        this.currentVersion = currentVersion;
    }

    public Integer getChapterId() {
        return chapterId;
    }

    public Long getRequestedVersion() {
        return requestedVersion;
    }

    public Long getCurrentVersion() {
        return currentVersion;
    }
}
