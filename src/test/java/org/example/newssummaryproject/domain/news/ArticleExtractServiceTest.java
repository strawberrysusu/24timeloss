package org.example.newssummaryproject.domain.news;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class ArticleExtractServiceTest {

    private final ArticleExtractService service = new ArticleExtractService();

    @Test
    void 연합뉴스TV_본문_추출() {
        var result = service.extract("https://www.yonhapnewstv.co.kr/news/MYH20260327060201Bhm");

        System.out.println("=== 연합뉴스TV ===");
        System.out.println("제목: " + result.title());
        System.out.println("출처: " + result.source());
        System.out.println("본문 길이: " + result.content().length() + "자");
        System.out.println("본문 앞부분: " + result.content().substring(0, Math.min(200, result.content().length())));
        System.out.println();

        assertFalse(result.content().isEmpty(), "본문이 비어있으면 안됨");
        assertTrue(result.content().length() > 100, "본문이 100자 이상이어야 함");
        // 영상은 네이버만 지원 — 연합뉴스TV는 본문만 추출
        assertFalse(result.hasVideo(), "일반 사이트는 영상 지원 안 함");
    }

    @Test
    void KBS_본문_추출() {
        var result = service.extract("https://news.kbs.co.kr/news/pc/view/view.do?ncd=8519745&ref=A");

        System.out.println("=== KBS ===");
        System.out.println("제목: " + result.title());
        System.out.println("출처: " + result.source());
        System.out.println("본문 길이: " + result.content().length() + "자");
        System.out.println("본문 앞부분: " + result.content().substring(0, Math.min(200, result.content().length())));
        System.out.println();

        assertFalse(result.content().isEmpty(), "본문이 비어있으면 안됨");
        assertTrue(result.content().length() > 100, "본문이 100자 이상이어야 함");
        // KBS는 영상 지원 안 함 (CDN 서명 필요)
        assertFalse(result.hasVideo(), "KBS는 영상 지원 안 함");
        // 본문에 광고/제보 안내가 없어야 함
        assertFalse(result.content().contains("제보하기"), "제보 안내가 포함되면 안됨");
    }

    @Test
    void 네이버뉴스_영상기사_VOD_PLAYER_WRAP에서_clipNo_조회() {
        // 이 SBS 기사는 _VOD_PLAYER_WRAP 타입이다.
        // 네이버 API로 data-video-id + data-inkey → clipNo를 조회해서
        // 공식 tv.naver.com/embed/{clipNo} URL을 만든다.
        var result = service.extract("https://n.news.naver.com/mnews/ranking/article/055/0001343767?ntype=RANKING&sid=001");

        System.out.println("=== 네이버 영상 기사 (SBS, API로 clipNo 조회) ===");
        System.out.println("제목: " + result.title());
        System.out.println("출처: " + result.source());
        System.out.println("영상: " + result.hasVideo() + " / " + result.videoEmbedUrl());
        System.out.println("본문 길이: " + result.content().length() + "자");
        System.out.println();

        assertFalse(result.content().isEmpty(), "본문이 비어있으면 안됨");
        assertTrue(result.hasVideo(), "영상 기사여야 함");
        assertFalse(result.videoEmbedUrl().isEmpty(), "API로 clipNo 조회 후 embed URL이 있어야 함");
        assertTrue(result.videoEmbedUrl().contains("tv.naver.com/embed/"), "공식 embed URL이어야 함");
        // clipNo는 숫자여야 함 (토큰이 아님)
        String clipNo = result.videoEmbedUrl().replace("https://tv.naver.com/embed/", "");
        assertTrue(clipNo.matches("\\d+"), "clipNo는 숫자여야 함, 실제값: " + clipNo);
    }

    @Test
    void 네이버뉴스_기존_기능_정상작동() {
        var result = service.extract("https://n.news.naver.com/mnews/article/001/0015985475?rc=N&ntype=RANKING");

        System.out.println("=== 네이버 뉴스 ===");
        System.out.println("제목: " + result.title());
        System.out.println("출처: " + result.source());
        System.out.println("영상: " + result.hasVideo() + " / " + result.videoEmbedUrl());
        System.out.println("본문 길이: " + result.content().length() + "자");
        System.out.println("본문 앞부분: " + result.content().substring(0, Math.min(200, result.content().length())));
        System.out.println();

        assertFalse(result.content().isEmpty(), "본문이 비어있으면 안됨");
        assertTrue(result.content().length() > 100, "본문이 100자 이상이어야 함");
    }
}
