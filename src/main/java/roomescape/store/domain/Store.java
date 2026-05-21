package roomescape.store.domain;

import roomescape.exception.BusinessRuleViolationException;
import roomescape.exception.InvalidDomainStateException;

public class Store {

    private static final int MAX_NAME_LENGTH = 255;
    private static final int MAX_DESCRIPTION_LENGTH = 255;

    private final Long id;
    private final String name;
    private final String description;

    public Store(Long id, String name, String description) {
        validateText(name, "이름", MAX_NAME_LENGTH);
        validateText(description, "설명", MAX_DESCRIPTION_LENGTH);
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public static Store create(String name, String description) {
        return new Store(null, name, description);
    }

    private void validateText(String value, String fieldName, int maxLength) {
        validateNotNull(value, String.format("매장 %s은(는) 반드시 입력해야 합니다.", fieldName));

        if (value.isBlank()) {
            throw new InvalidDomainStateException(String.format("매장 %s은(는) 반드시 입력해야 합니다.", fieldName));
        }

        if (value.length() > maxLength) {
            throw new BusinessRuleViolationException(
                    String.format("매장 %s은(는) %d자 이하로 입력해야 합니다. (현재 길이: %d)", fieldName, maxLength, value.length())
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
}
