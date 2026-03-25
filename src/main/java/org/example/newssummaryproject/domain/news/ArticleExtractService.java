package org.example.newssummaryproject.domain.news;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 뉴스 URL에서 기사 정보를 자동 추출하는 서비스다.
 *
 * ── 동작 원리 (초보자용 설명) ──
 *
 * 모든 뉴스 사이트의 HTML에는 "메타 태그"라는 게 숨어 있다.
 * 카카오톡에 링크를 공유하면 제목+이미지가 미리보기로 뜨는 것도 이 메타 태그 덕분이다.
 *
 * 다만 네이버 뉴스처럼 자체 구조를 쓰는 사이트는 메타 태그만으로는 부족하다.
 * 그래서 이 서비스는 두 가지 전략을 쓴다:
 *   1. 네이버 뉴스 URL이면 → 네이버 전용 규칙으로 추출
 *   2. 그 외 사이트면 → 일반 메타 태그(og:title 등)로 추출
 *
 * ── 흐름 ──
 * URL 입력 → 네이버인지 판단 → HTML 다운로드 → 파싱 → 결과 반환
 */
@Service
public class ArticleExtractService {

    private static final Logger log = LoggerFactory.getLogger(ArticleExtractService.class);

    /**
     * URL에서 기사 정보를 추출한다.
     */
    public ExtractResult extract(String url) {
        try {
            // HTML 페이지를 가져온다
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(10_000)
                    .get();

            // 네이버 뉴스인지 판단해서 추출 방식을 나눈다
            if (isNaverNews(url)) {
                return extractNaver(doc, url);
            }
            return extractGeneral(doc, url);

        } catch (Exception e) {
            log.error("기사 추출 실패: url={}, error={}", url, e.getMessage());
            throw new IllegalArgumentException(
                    "기사를 가져올 수 없습니다. URL을 확인해주세요: " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────
    // 네이버 뉴스 전용 추출
    // ──────────────────────────────────────────────────────

    /**
     * URL이 네이버 뉴스인지 판단한다.
     */
    private boolean isNaverNews(String url) {
        return url.contains("n.news.naver.com") || url.contains("news.naver.com");
    }

    /**
     * 네이버 뉴스 전용 추출 규칙.
     *
     * 네이버 뉴스의 HTML 구조:
     *   - 제목: og:title 메타 태그
     *   - 언론사: twitter:creator 메타 태그 (og:site_name이 없다!)
     *   - 이미지: og:image (영상 기사면 영상 썸네일을 우선 사용)
     *   - 본문: #dic_area (주의: <p> 대신 <br>로 줄바꿈하는 구조)
     *   - 영상: ._VOD_PLAYER_WRAP 요소가 있으면 영상 기사
     */
    private ExtractResult extractNaver(Document doc, String url) {
        // 제목
        String title = getMetaContent(doc, "og:title");
        if (title.isEmpty()) {
            Element headline = doc.selectFirst(".media_end_head_headline span");
            if (headline != null) title = headline.text().trim();
        }

        // 언론사명 — 네이버 뉴스 전용 순서
        String source = extractNaverSource(doc);

        // 영상 감지 — 두 가지 패턴을 확인한다
        // 패턴1: ._VOD_PLAYER_WRAP[data-video-id] (구형 네이버 영상)
        // 패턴2: #dic_area iframe[data-src] 또는 iframe[src] (iframe형 네이버 영상)
        String videoEmbedUrl = extractNaverVideoUrl(doc);
        boolean hasVideo = !videoEmbedUrl.isEmpty();

        // 이미지 — 영상 기사면 영상 썸네일을 우선 사용
        String image;
        Element vodWrap = doc.selectFirst("._VOD_PLAYER_WRAP[data-video-id]");
        if (vodWrap != null && vodWrap.hasAttr("data-cover-image-thumbnail-url")) {
            image = vodWrap.attr("data-cover-image-thumbnail-url");
        } else {
            image = getMetaContent(doc, "og:image");
        }

        // 본문 — #dic_area에서 <br>을 줄바꿈으로 변환해서 추출
        String content = extractNaverContent(doc);

        log.info("네이버 기사 추출: source={}, hasVideo={}, videoUrl={}, content={}자",
                source, hasVideo, videoEmbedUrl.isEmpty() ? "없음" : videoEmbedUrl, content.length());
        return new ExtractResult(title, source, image, content, url, hasVideo, videoEmbedUrl);
    }

    /**
     * 네이버 뉴스에서 언론사 이름을 추출한다.
     *
     * 네이버 뉴스에는 og:site_name이 없다.
     * 대신 아래 위치들에 언론사 이름이 있다.
     */
    private String extractNaverSource(Document doc) {
        // 1순위: twitter:creator (예: "SBS")
        String source = getMetaContent(doc, "twitter:creator");
        if (!source.isEmpty()) return source;

        // 2순위: 언론사 로고 이미지의 alt 텍스트
        Element pressImg = doc.selectFirst(".media_end_head_top_logo img");
        if (pressImg != null && !pressImg.attr("alt").trim().isEmpty()) {
            return pressImg.attr("alt").trim();
        }

        // 3순위: og:article:author에서 | 앞부분 (예: "SBS | 네이버" → "SBS")
        source = getMetaContent(doc, "og:article:author");
        if (!source.isEmpty()) {
            return source.contains("|") ? source.split("\\|")[0].trim() : source;
        }

        return "";
    }

    /**
     * 네이버 뉴스에서 영상 임베드 URL을 추출한다.
     *
     * 네이버 영상 기사에는 두 가지 패턴이 있다:
     *   1. #dic_area 안에 iframe이 있는 경우 (data-src 또는 src에 tv.naver.com/embed/... 형태)
     *   2. ._VOD_PLAYER_WRAP에 data-video-id가 있는 경우
     *
     * iframe 방식이 더 일반적이고 임베드도 쉬워서 1번을 우선 시도한다.
     */
    private String extractNaverVideoUrl(Document doc) {
        // 패턴 1: #dic_area 안의 iframe (가장 흔한 네이버 영상 패턴)
        Element dicArea = doc.selectFirst("#dic_area");
        if (dicArea != null) {
            // data-src에 있는 경우 (lazy loading)
            Element iframe = dicArea.selectFirst("iframe[data-src*=tv.naver.com/embed]");
            if (iframe != null) {
                return iframe.attr("data-src").trim();
            }
            // src에 있는 경우
            iframe = dicArea.selectFirst("iframe[src*=tv.naver.com/embed]");
            if (iframe != null) {
                return iframe.attr("src").trim();
            }
        }

        // 패턴 2: ._VOD_PLAYER_WRAP (구형 패턴) — video-id로 embed URL을 만든다
        Element vodWrap = doc.selectFirst("._VOD_PLAYER_WRAP[data-video-id]");
        if (vodWrap != null) {
            String videoId = vodWrap.attr("data-video-id").trim();
            if (!videoId.isEmpty()) {
                return "https://tv.naver.com/embed/" + videoId;
            }
        }

        return "";
    }

    /**
     * 네이버 뉴스 본문을 추출한다.
     *
     * 네이버 뉴스는 본문이 #dic_area 안에 있는데,
     * <p> 태그가 아니라 <br> 태그로 줄바꿈하는 구조다.
     * 그래서 .text()만 쓰면 줄바꿈이 다 사라져서 읽기 어렵다.
     *
     * 해결: <br>을 줄바꿈 문자(\n)로 바꾸고, HTML 태그를 제거한다.
     */
    private String extractNaverContent(Document doc) {
        Element dicArea = doc.selectFirst("#dic_area");
        if (dicArea == null) {
            dicArea = doc.selectFirst("#newsct_article");
        }
        if (dicArea == null) {
            return extractGeneralContent(doc);
        }

        // <br> 태그를 줄바꿈 마커로 바꾼다
        String html = dicArea.html();
        String text = html
                .replaceAll("<br\\s*/?>", "\n")           // <br> → 줄바꿈
                .replaceAll("<[^>]+>", "")                 // 나머지 HTML 태그 제거
                .replaceAll("&nbsp;", " ")                 // &nbsp; → 공백
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&amp;", "&");

        // 빈 줄 정리: 연속 빈 줄을 하나로
        StringBuilder sb = new StringBuilder();
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                sb.append(trimmed).append("\n");
            }
        }
        return sb.toString().trim();
    }

    // ──────────────────────────────────────────────────────
    // 일반 사이트 추출 (네이버 외)
    // ──────────────────────────────────────────────────────

    /**
     * 일반 뉴스 사이트에서 기사 정보를 추출한다.
     * og:title, og:site_name, og:image 메타 태그를 기본으로 사용한다.
     */
    private ExtractResult extractGeneral(Document doc, String url) {
        String title = getMetaContent(doc, "og:title");
        if (title.isEmpty()) title = doc.title();

        String source = extractGeneralSource(doc);
        String image = getMetaContent(doc, "og:image");
        String content = extractGeneralContent(doc);

        log.info("일반 기사 추출: source={}, content={}자", source, content.length());
        return new ExtractResult(title, source, image, content, url, false, "");
    }

    /**
     * 일반 사이트에서 언론사 이름을 추출한다.
     */
    private String extractGeneralSource(Document doc) {
        String source = getMetaContent(doc, "og:site_name");
        if (!source.isEmpty()) return source;

        source = getMetaContent(doc, "twitter:creator");
        if (!source.isEmpty()) return source;

        source = getMetaContent(doc, "og:article:author");
        if (!source.isEmpty()) return source;

        return "";
    }

    /**
     * 일반 사이트에서 본문을 추출한다.
     */
    private String extractGeneralContent(Document doc) {
        // 전략 1: <article> 태그
        Element article = doc.selectFirst("article");
        if (article != null) {
            String text = extractText(article);
            if (text.length() >= 50) return text;
        }

        // 전략 2: 흔한 본문 영역
        String[] selectors = {
                ".article_body", ".news_end",
                "#article-view-content-div", "[itemprop=articleBody]"
        };
        for (String selector : selectors) {
            Element el = doc.selectFirst(selector);
            if (el != null) {
                String text = extractText(el);
                if (text.length() >= 50) return text;
            }
        }

        // 전략 3: 긴 <p> 태그 모으기
        StringBuilder sb = new StringBuilder();
        for (Element p : doc.select("p")) {
            String text = p.text().trim();
            if (text.length() >= 30) sb.append(text).append("\n");
        }
        return sb.toString().trim();
    }

    // ──────────────────────────────────────────────────────
    // 공통 유틸
    // ──────────────────────────────────────────────────────

    /**
     * HTML 메타 태그에서 값을 읽는다.
     */
    private String getMetaContent(Document doc, String property) {
        Element el = doc.selectFirst("meta[property=" + property + "]");
        if (el != null && el.hasAttr("content")) return el.attr("content").trim();

        el = doc.selectFirst("meta[name=" + property + "]");
        if (el != null && el.hasAttr("content")) return el.attr("content").trim();

        return "";
    }

    /**
     * HTML 요소 안에서 텍스트를 추출한다. <br>도 줄바꿈으로 처리한다.
     */
    private String extractText(Element parent) {
        // <br>을 줄바꿈으로 변환
        String html = parent.html();
        String text = html
                .replaceAll("<br\\s*/?>", "\n")
                .replaceAll("<[^>]+>", "")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&");

        StringBuilder sb = new StringBuilder();
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) sb.append(trimmed).append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * 추출 결과를 담는 레코드.
     */
    public record ExtractResult(
            String title,         // 기사 제목
            String source,        // 언론사 이름 (예: "SBS")
            String thumbnailUrl,  // 대표 이미지 URL
            String content,       // 기사 본문
            String originalUrl,   // 원문 URL
            boolean hasVideo,     // 영상 기사 여부
            String videoEmbedUrl  // 영상 iframe URL (예: "https://tv.naver.com/embed/12345")
    ) {}
}
