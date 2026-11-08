package io.groo.core.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Proxy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** 업로드 파일명 경로 조작 방어 — 저장 디렉터리 밖에 쓰지 못하고, 정상 이름은 그대로 저장된다. */
class FileUtilsSaveTest {

    @TempDir
    Path root;

    /** spring-test 없이 쓰는 최소 MultipartFile (이름 · 내용 · transferTo 만). */
    private static MultipartFile multipart(String originalName) {
        return (MultipartFile) Proxy.newProxyInstance(
                MultipartFile.class.getClassLoader(), new Class<?>[]{MultipartFile.class},
                (proxy, m, args) -> switch (m.getName()) {
                    case "isEmpty" -> false;
                    case "getOriginalFilename" -> originalName;
                    case "getSize" -> 3L;
                    case "transferTo" -> {
                        if (args[0] instanceof java.io.File f) {
                            java.nio.file.Files.write(f.toPath(), new byte[]{1, 2, 3});
                        } else {
                            java.nio.file.Files.write((Path) args[0], new byte[]{1, 2, 3});
                        }
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException(m.getName());
                });
    }

    private boolean save(Path dir, String name) {
        MultipartFile f = multipart(name);
        return FileUtils.save(f, dir.toString(), name, null);
    }

    private long countFiles() throws Exception {
        try (Stream<Path> s = Files.walk(root)) {
            return s.filter(Files::isRegularFile).count();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"../../x.png", "..\\x.png", "/etc/x.png", "a/../../b.png", "a\0b.png", "..", "../x", "sub/x.png"})
    void 악성_이름은_거절하고_밖에_쓰지_않는다(String name) throws Exception {
        Path dir = root.resolve("upload");
        assertThat(save(dir, name)).isFalse();
        assertThat(countFiles()).isZero();
    }

    @Test
    void 퍼센트_인코딩은_해석하지_않고_글자_그대로_안쪽에_저장된다() throws Exception {
        Path dir = root.resolve("upload");
        assertThat(save(dir, "%2e%2e%2fx.png")).isTrue();
        assertThat(Files.exists(dir.resolve("%2e%2e%2fx.png"))).isTrue();
        assertThat(countFiles()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"사진 1.png", "a.b.c.tar.gz", "내 문서.final.v2.jpg", "photo_550e8400-e29b-41d4-a716-446655440000.png", "...hidden.png"})
    void 정상_이름은_그대로_저장된다(String name) throws Exception {
        Path dir = root.resolve("upload");
        assertThat(save(dir, name)).isTrue();
        assertThat(Files.exists(dir.resolve(name))).isTrue();
    }

    @Test
    void 이름을_안_주면_원본_파일명을_쓰되_악성이면_거절한다() throws Exception {
        Path dir = root.resolve("upload");
        MultipartFile bad = multipart("../../x.png");
        assertThat(FileUtils.save(bad, dir.toString(), null, null)).isFalse();
        MultipartFile ok = multipart("ok.png");
        assertThat(FileUtils.save(ok, dir.toString(), null, null)).isTrue();
        assertThat(countFiles()).isEqualTo(1);
    }
}
