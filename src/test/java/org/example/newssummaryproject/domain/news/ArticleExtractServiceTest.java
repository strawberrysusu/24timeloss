package org.example.newssummaryproject.domain.news;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
class ArticleExtractServiceTest {

    private final ArticleExtractService service = new ArticleExtractService();

    @Test
    void extracts_article_body_from_yonhap_news_tv() {
        var result = service.extract("https://www.yonhapnewstv.co.kr/news/MYH20260327060201Bhm");

        assertFalse(result.content().isEmpty(), "content should not be empty");
        assertTrue(result.content().length() > 100, "content should be longer than 100 chars");
        assertFalse(result.hasVideo(), "regular site article should not expose embedded video");
    }

    @Test
    void extracts_article_body_from_kbs() {
        var result = service.extract("https://news.kbs.co.kr/news/pc/view/view.do?ncd=8519745&ref=A");

        assertFalse(result.content().isEmpty(), "content should not be empty");
        assertTrue(result.content().length() > 100, "content should be longer than 100 chars");
        assertFalse(result.hasVideo(), "KBS extraction should not expose embedded video");
        assertFalse(result.content().contains("제보하기"), "boilerplate text should be removed");
    }

    @Test
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
    void extracts_regular_naver_article_body() {
        var result = service.extract("https://n.news.naver.com/mnews/article/001/0015985475?rc=N&ntype=RANKING");

        assertFalse(result.content().isEmpty(), "content should not be empty");
        assertTrue(result.content().length() > 100, "content should be longer than 100 chars");
    }
}
