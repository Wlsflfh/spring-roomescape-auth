package roomescape.theme.domain;

import roomescape.exception.BusinessRuleViolationException;
import roomescape.exception.InvalidDomainStateException;
import roomescape.store.domain.Store;

public class Theme {

    private static final int MAX_NAME_LENGTH = 255;
    private static final int MAX_DESCRIPTION_LENGTH = 255;
    private static final int MAX_THUMBNAIL_URL_LENGTH = 1024;

    private final Long id;
    private final String name;
    private final String description;
    private final String thumbnailUrl;
    private final Store store;

    public Theme(Long id, String name, String description, String thumbnailUrl, Store store) {
        validateText(name, "이름", MAX_NAME_LENGTH);
        validateText(description, "설명", MAX_DESCRIPTION_LENGTH);
        validateText(thumbnailUrl, "썸네일 URL", MAX_THUMBNAIL_URL_LENGTH);
        validateNotNull(store, "테마 매장은 반드시 지정해야 합니다.");
        this.id = id;
        this.name = name;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.store = store;
    }

    public static Theme create(String name, String description, String thumbnailUrl, Store store) {
        return new Theme(null, name, description, thumbnailUrl, store);
    }

    public Theme update(String name, String description, String thumbnailUrl, Store store) {
        return new Theme(this.id, name, description, thumbnailUrl, store);
    }

    private void validateText(String value, String fieldName, int maxLength) {
        validateNotNull(value, String.format("테마 %s은(는) 반드시 입력해야 합니다.", fieldName));

        if (value.isBlank()) {
            throw new InvalidDomainStateException(String.format("테마 %s은(는) 반드시 입력해야 합니다.", fieldName));
        }

        if (value.length() > maxLength) {
            throw new BusinessRuleViolationException(
                    String.format("테마 %s은(는) %d자 이하로 입력해야 합니다. (현재 길이: %d)", fieldName, maxLength, value.length())
            );
        }
    }

    private void validateNotNull(Object obj, String message) {
        if (obj == null) {
            throw new InvalidDomainStateException(message);
        }
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public Store getStore() {
        return store;
    }
}
