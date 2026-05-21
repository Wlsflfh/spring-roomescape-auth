package roomescape.theme.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ThemeRequest(
        @NotBlank(message = "테마 이름은 비어있을 수 없습니다.")
        @Size(max = 255, message = "테마 이름은 255자 이하여야 합니다.")
        String name,

        @NotBlank(message = "테마 설명은 비어있을 수 없습니다.")
        @Size(max = 255, message = "테마 설명은 255자 이하여야 합니다.")
        String description,

        @NotBlank(message = "테마 썸네일 URL은 비어있을 수 없습니다.")
        @Size(max = 1024, message = "테마 썸네일 URL은 1024자 이하여야 합니다.")
        String thumbnailUrl,

        @NotNull(message = "매장 ID는 반드시 입력해야 합니다.")
        Long storeId
) {
}
