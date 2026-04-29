package org.example.newssummaryproject.domain.news;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArticleExtractServiceTest {

    private final ArticleExtractService service = new ArticleExtractService();

    // ── SSRF 차단 단위 테스트 (네트워크 호출 없음) ──

    @Test
    void rejects_non_http_scheme() {
        assertThrows(IllegalArgumentException.class,
                () -> service.extract("file:///etc/passwd"));
        assertThrows(IllegalArgumentException.class,
                () -> service.extract("gopher://example.com/"));
    }

    @Test
    void rejects_loopback_address() {
        assertThrows(IllegalArgumentException.class,
                () -> service.extract("http://127.0.0.1/"));
        assertThrows(IllegalArgumentException.class,
                () -> service.extract("http://localhost/"));
    }

    @Test
    void rejects_aws_metadata_address() {
        assertThrows(IllegalArgumentException.class,
                () -> service.extract("http://169.254.169.254/latest/meta-data/"));
    }

    @Test
    void rejects_private_address() {
        assertThrows(IllegalArgumentException.class,
                () -> service.extract("http://10.0.0.1/"));
        assertThrows(IllegalArgumentException.class,
                () -> service.extract("http://192.168.1.1/"));
    }

    // ── 외부 사이트 추출 (네트워크 필요, integration 태그) ──

    @Test
    @Tag("integration")
    void extracts_article_body_from_yonhap_news_tv() {
        var result = service.extract("https://www.yonhapnewstv.co.kr/news/MYH20260327060201Bhm");

        assertFalse(result.content().isEmpty(), "content should not be empty");
        assertTrue(result.content().length() > 100, "content should be longer than 100 chars");
        assertFalse(result.hasVideo(), "regular site article should not expose embedded video");
    }

    @Test
    @Tag("integration")
    void extracts_article_body_from_kbs() {
        var result = service.extract("https://news.kbs.co.kr/news/pc/view/view.do?ncd=8519745&ref=A");

        assertFalse(result.content().isEmpty(), "content should not be empty");
        assertTrue(result.content().length() > 100, "content should be longer than 100 chars");
        assertFalse(result.hasVideo(), "KBS extraction should not expose embedded video");
        assertFalse(result.content().contains("제보하기"), "boilerplate text should be removed");
    }

    @Test
    @Tag("integration")
    void extracts_naver_video_article_and_resolves_clip_number() {
        var result = service.extract("https://n.news.naver.com/mnews/ranking/article/055/0001343767?ntype=RANKING&sid=001");

        assertFalse(result.content().isEmpty(), "content should not be empty");
        assertTrue(result.hasVideo(), "video article should expose embedded video");
        assertFalse(result.videoEmbedUrl().isEmpty(), "embed url should not be empty");
        assertTrue(result.videoEmbedUrl().contains("tv.naver.com/embed/"), "expected official naver embed url");

        String clipNo = result.videoEmbedUrl().replace("https://tv.naver.com/embed/", "");
        assertTrue(clipNo.matches("\\d+"), "clip number should be numeric: " + clipNo);
    }

    @Test
    @Tag("integration")
    void extracts_regular_naver_article_body() {
        var result = service.extract("https://n.news.naver.com/mnews/article/001/0015985475?rc=N&ntype=RANKING");

        assertFalse(result.content().isEmpty(), "content should not be empty");
        assertTrue(result.content().length() > 100, "content should be longer than 100 chars");
    }
}
