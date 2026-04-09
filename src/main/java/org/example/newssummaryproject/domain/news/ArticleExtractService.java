package org.example.newssummaryproject.domain.news;

import net.dankito.readability4j.Readability4J;
import net.dankito.readability4j.Article;
import org.jsoup.Jsoup;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 뉴스 URL에서 기사 정보를 자동 추출하는 서비스다.
 *
 * ── 동작 원리 (초보자용 설명) ──
 *
 * 모든 뉴스 사이트의 HTML에는 "메타 태그"라는 게 숨어 있다.
 * 카카오톡에 링크를 공유하면 제목+이미지가 미리보기로 뜨는 것도 이 메타 태그 덕분이다.
 *
 * 다만 네이버 뉴스, KBS 등은 각자 고유한 HTML 구조를 쓴다.
 * 그래서 이 서비스는 사이트별로 다른 전략을 쓴다:
 *   1. 네이버 뉴스 → 네이버 전용 규칙 (본문 + 영상 재생 지원)
 *   2. KBS 뉴스 → KBS 전용 규칙 (본문만, 영상은 외부 재생 불가)
 *   3. 그 외 사이트 → 일반 메타 태그 + Readability4J (본문만)
 *
 * 영상 재생은 네이버만 지원한다.
 *
 * ── 흐름 ──
 * URL 입력 → 사이트 판별 → HTML 다운로드 → 노이즈 제거 → 파싱 → 텍스트 정리 → 결과 반환
 */
@Service
public class ArticleExtractService {

    private static final Logger log = LoggerFactory.getLogger(ArticleExtractService.class);

    /**
     * 본문에서 제거할 boilerplate(찌꺼기) 패턴들.
     *
     * 뉴스 사이트에서 기사 본문과 함께 딸려오는 불필요한 텍스트들:
     * - 저작권 고지 (예: "저작권자(c) 연합뉴스")
     * - 제보 안내 (예: "제보는 카카오톡 okjebo")
     * - 송고 시간 (예: "2026/03/27 06:30 송고")
     * - 해시태그 (예: "#약물운전")
     * - 댓글/반응 UI 텍스트
     * - 공유 버튼 텍스트
     * - 구독/채널 안내
     */
    private static final List<Pattern> BOILERPLATE_PATTERNS = List.of(
            // 저작권 고지
            Pattern.compile("(?i)<저작권자.*?>"),
            Pattern.compile("(?i)\\(c\\).*?(무단|금지|전재|재배포).*"),
            Pattern.compile("(?i)©.*?(무단|금지|전재|재배포).*"),
            Pattern.compile("저작권자.*?(무단|금지|전재|재배포).*"),

            // 제보 안내 — 여러 형태 처리
            Pattern.compile("제보는\\s*카카오톡.*"),
            Pattern.compile("제보하기.*"),
            Pattern.compile("▷.*제보.*"),
            Pattern.compile("■\\s*제보하기.*"),

            // 기자 이메일 (예: "정호윤(ikarus@yna.co.kr)")
            Pattern.compile(".*\\w+@\\w+\\.\\w+\\)\\s*$"),

            // 연합뉴스TV 제보 안내 (예: "연합뉴스TV 기사문의 및 제보 : 카톡/라인 jebo23")
            Pattern.compile(".*기사문의 및 제보.*"),

            // 송고 시간 (예: "2026/03/27 06:30 송고", "2026년03월27일 06시30분 송고")
            Pattern.compile("\\d{4}[/년.-]\\d{1,2}[/월.-]\\d{1,2}.*송고.*"),

            // 해시태그 줄 (예: "#약물운전")
            Pattern.compile("^\\s*#\\S+\\s*$"),

            // 댓글/반응 UI
            Pattern.compile("^\\s*(댓글|좋아요|슬퍼요|화나요|후속요청|댓글 작성하기|개 댓글 전체보기)\\s*$"),

            // 공유 버튼 텍스트
            Pattern.compile("^\\s*(카카오톡|페이스북|네이버 밴드|URL 복사|닫기|북마크|공유하기|공유|프린트|제보|X|페이스북 메신저)\\s*$"),

            // 폰트 크기 UI
            Pattern.compile("^\\s*폰트 \\d단계.*$"),
            Pattern.compile("^\\s*본문 글자 크기.*$"),

            // 채널/구독 안내
            Pattern.compile(".*에서도 뉴스는.*"),
            Pattern.compile(".*에서도 KBS뉴스를.*"),
            Pattern.compile(".*모바일앱을 만나보세요.*"),
            Pattern.compile(".*원스톱 콘텐츠 제공 플랫폼.*"),
            Pattern.compile(".*함께 보면 좋은 콘텐츠.*"),
            Pattern.compile(".*함께 읽기 좋은 콘텐츠.*"),
            Pattern.compile("연합 마이뉴스.*"),

            // URL 복사 알림
            Pattern.compile("^\\s*URL이 복사되었습니다.*$"),

            // Taboola/Dable 광고 레이블
            Pattern.compile("^\\s*(Taboola|후원링크|by 데이블)\\s*$"),

            // 광고 텍스트
            Pattern.compile("^\\s*광고\\s*$"),

            // 템플릿 구문 (mustache 등)
            Pattern.compile("\\{\\{.*?\\}\\}"),

            // 촬영기자/영상편집 크레딧 (예: "촬영기자:박준석/영상편집:이재연")
            Pattern.compile("^\\s*촬영기자.*$"),

            // KBS 제보 관련 — ▷ 뒤에 "제보", "카카오톡", "전화", "이메일", "유튜브" 등
            // 제보 안내 키워드가 있을 때만 제거한다.
            // 주의: "▷ 핵심 요약" 같은 정상 본문을 지우지 않도록 키워드를 특정한다.
            Pattern.compile("^\\s*'KBS제보'.*$"),
            Pattern.compile("^\\s*▷\\s*(카카오톡|전화|이메일|유튜브|네이버|다음).*$")
    );

    /**
     * URL에서 기사 정보를 추출한다.
     *
     * 사이트별로 다른 추출 전략을 사용한다:
     *   네이버 뉴스 → extractNaver()
     *   KBS 뉴스   → extractKbs()
     *   그 외      → extractGeneral()
     */
    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int MAX_RETRIES = 1;

    public ExtractResult extract(String url) {
        Document doc = fetchDocument(url);

        // 사이트별로 추출 방식을 나눈다
        if (isNaverNews(url)) {
            return extractNaver(doc, url);
        }
        if (isKbsNews(url)) {
            return extractKbs(doc, url);
        }
        return extractGeneral(doc, url);
    }

    /**
     * URL에서 HTML을 가져온다. timeout 시 1회 재시도한다.
     */
    private Document fetchDocument(String url) {
        SocketTimeoutException lastTimeout = null;

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                Connection conn = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .timeout(CONNECT_TIMEOUT_MS)
                        .followRedirects(true)
                        .referrer("https://www.google.com");
                return conn.get();
            } catch (SocketTimeoutException e) {
                lastTimeout = e;
                log.warn("기사 페이지 타임아웃 ({}회차): url={}", attempt + 1, url);
            } catch (Exception e) {
                log.error("기사 페이지 요청 실패: url={}, error={}", url, e.getMessage());
                throw new IllegalArgumentException(
                        "기사를 가져올 수 없습니다. URL을 확인해주세요: " + e.getMessage());
            }
        }

        log.error("기사 페이지 타임아웃 (재시도 소진): url={}", url);
        throw new IllegalArgumentException(
                "기사 페이지 응답이 너무 느립니다. 잠시 후 다시 시도해주세요.");
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

        // 영상 감지
        // - iframe이 있으면 바로 사용
        // - _VOD_PLAYER_WRAP이 있으면 네이버 API로 공식 embed URL 조회
        // - 둘 다 없거나 API 실패하면 영상 없음 → 썸네일만
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
        content = cleanBoilerplate(content);

        // 영상 기사에서 본문이 너무 짧으면 og:description으로 폴백
        if (content.length() < 50) {
            String description = getMetaContent(doc, "og:description");
            if (description.length() > content.length()) {
                log.info("네이버 기사 본문 짧음 ({}자), og:description으로 폴백 ({}자)",
                        content.length(), description.length());
                content = description;
            }
        }

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
     * ── 탐색 순서 ──
     * 1. #dic_area 안에 실제 iframe이 있으면 그 URL을 바로 사용
     * 2. ._VOD_PLAYER_WRAP이 있으면 네이버 API로 진짜 clipNo를 조회해서 공식 embed URL 생성
     *
     * ── 2번이 필요한 이유 ──
     * _VOD_PLAYER_WRAP의 data-video-id는 토큰(hex) 형식이라
     * tv.naver.com/embed/{토큰}으로는 재생이 안 된다.
     * 하지만 네이버 API에 data-video-id + data-inkey를 보내면
     * 진짜 숫자 clipNo(contentId)를 돌려주고,
     * tv.naver.com/embed/{clipNo}는 공식 embed URL이라 재생된다.
     */
    private String extractNaverVideoUrl(Document doc) {
        // 패턴 1: 본문 안에 실제 iframe이 있는 경우 (가장 확실)
        Element dicArea = doc.selectFirst("#dic_area");
        if (dicArea != null) {
            Element iframe = dicArea.selectFirst("iframe[data-src*=tv.naver.com/embed]");
            if (iframe != null) {
                return iframe.attr("data-src").trim();
            }
            iframe = dicArea.selectFirst("iframe[src*=tv.naver.com/embed]");
            if (iframe != null) {
                return iframe.attr("src").trim();
            }
        }

        // 패턴 2: _VOD_PLAYER_WRAP → 네이버 API로 clipNo 조회
        Element vodWrap = doc.selectFirst("._VOD_PLAYER_WRAP[data-video-id]");
        if (vodWrap != null) {
            String vid = vodWrap.attr("data-video-id").trim();
            String inkey = vodWrap.attr("data-inkey").trim();
            if (!vid.isEmpty() && !inkey.isEmpty()) {
                String clipNo = resolveNaverClipNo(vid, inkey);
                if (!clipNo.isEmpty()) {
                    return "https://tv.naver.com/embed/" + clipNo;
                }
            }
        }

        return "";
    }

    /**
     * 네이버 API로 진짜 clipNo(contentId)를 조회한다.
     *
     * API: https://apis.naver.com/rmcnmv/rmcnmv/vod/play/v2.0/{vid}?key={inkey}
     * 응답 JSON의 meta.contentId가 숫자 clipNo다.
     * 이 clipNo로 tv.naver.com/embed/{clipNo} 공식 embed URL을 만들 수 있다.
     */
    private String resolveNaverClipNo(String vid, String inkey) {
        try {
            String json = Jsoup.connect(
                            "https://apis.naver.com/rmcnmv/rmcnmv/vod/play/v2.0/" + vid)
                    .data("key", inkey)
                    .ignoreContentType(true)
                    .timeout(5_000)
                    .get()
                    .body()
                    .text();

            // "contentId":"12345678" 패턴에서 숫자를 추출한다
            Matcher matcher = Pattern.compile("\"contentId\"\\s*:\\s*\"(\\d+)\"").matcher(json);
            if (matcher.find()) {
                String clipNo = matcher.group(1);
                log.info("네이버 clipNo 조회 성공: vid={} → clipNo={}", vid, clipNo);
                return clipNo;
            }
        } catch (Exception e) {
            log.warn("네이버 clipNo 조회 실패: vid={}, error={}", vid, e.getMessage());
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

        return extractTextFromHtml(dicArea);
    }

    // ──────────────────────────────────────────────────────
    // KBS 뉴스 전용 추출
    // ──────────────────────────────────────────────────────

    /**
     * URL이 KBS 뉴스인지 판단한다.
     */
    private boolean isKbsNews(String url) {
        return url.contains("news.kbs.co.kr");
    }

    /**
     * KBS 뉴스 전용 추출 규칙.
     *
     * KBS 뉴스의 HTML 구조:
     *   - 본문: #cont_newstext (class="detail-body font-size")
     *     주의! "font-size" 클래스 때문에 일반 cleanDom에서 삭제돼버린다.
     *     그래서 KBS는 별도 핸들러로 처리한다.
     *   - 영상: 외부 재생 불가 (CDN 서명 필요) → 본문만 추출
     *   - 제보 영역: .artical-btm (본문 바깥에 있어서 #cont_newstext만 잡으면 깔끔)
     */
    private ExtractResult extractKbs(Document doc, String url) {
        // 제목
        String title = getMetaContent(doc, "og:title");
        if (title.isEmpty()) title = doc.title();

        // 출처
        String source = getMetaContent(doc, "og:site_name");
        if (source.isEmpty()) source = "KBS 뉴스";

        // 이미지
        String image = getMetaContent(doc, "og:image");
        if (image.startsWith("http://")) {
            image = image.replace("http://", "https://");
        }

        // 본문: #cont_newstext를 직접 타겟팅 (cleanDom 쓰지 않음)
        String content = "";
        Element contentEl = doc.selectFirst("#cont_newstext");
        if (contentEl != null) {
            content = extractTextFromHtml(contentEl);
        }
        if (content.length() < 50) {
            Element detailBody = doc.selectFirst(".detail-body");
            if (detailBody != null) {
                content = extractTextFromHtml(detailBody);
            }
        }
        content = cleanBoilerplate(content);

        if (content.length() < 50) {
            String description = getMetaContent(doc, "og:description");
            if (description.length() > content.length()) {
                content = description;
            }
        }

        // 영상: KBS는 외부 재생 불가 (CDN 서명 필요) → 영상 지원 안 함
        log.info("KBS 기사 추출: content={}자", content.length());
        return new ExtractResult(title, source, image, content, url, false, "");
    }

    // ──────────────────────────────────────────────────────
    // 일반 사이트 추출 (네이버/KBS 외)
    // ──────────────────────────────────────────────────────

    /**
     * 일반 뉴스 사이트에서 기사 정보를 추출한다.
     *
     * ── 순서 ──
     * 1. 메타 태그에서 기본 정보(제목, 출처, 이미지) 추출
     * 2. DOM 정리 (광고, 댓글, 스크립트 등 제거)
     * 3. 본문 텍스트 추출
     * 4. boilerplate 패턴 제거
     * (영상은 네이버만 지원 — 일반 사이트는 본문만 추출)
     */
    private ExtractResult extractGeneral(Document doc, String url) {
        // 1단계: 메타 태그에서 기본 정보
        String title = getMetaContent(doc, "og:title");
        if (title.isEmpty()) title = doc.title();

        String source = extractGeneralSource(doc);
        String image = getMetaContent(doc, "og:image");

        // 2단계: DOM 정리 (본문 추출 전에 노이즈 제거)
        cleanDom(doc);

        // 3단계: 본문 추출
        String content = extractGeneralContent(doc);

        // 4단계: boilerplate 제거
        content = cleanBoilerplate(content);

        // 본문이 너무 짧으면 og:description으로 폴백
        if (content.length() < 50) {
            String description = getMetaContent(doc, "og:description");
            if (description.length() > content.length()) {
                log.info("일반 기사 본문 짧음 ({}자), og:description으로 폴백 ({}자)",
                        content.length(), description.length());
                content = description;
            }
        }

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
     * DOM에서 본문이 아닌 노이즈 요소들을 제거한다.
     *
     * 뉴스 사이트 HTML에는 본문 외에도 광고, 댓글, 추천 기사, JS 위젯 등
     * 수많은 요소가 있다. 이걸 미리 제거해야 깨끗한 본문만 추출할 수 있다.
     *
     * ── 제거 대상 ──
     * 1. script, style, noscript — JS/CSS 코드
     * 2. nav, footer, aside, header — 네비게이션/사이드바
     * 3. 광고 컨테이너 — class/id에 ad, banner, sponsor 등이 포함된 요소
     * 4. 댓글 영역 — comment, reply 등
     * 5. 소셜 공유 버튼 — share, social 등
     * 6. 추천 기사 위젯 — taboola, dable, outbrain 등
     */
    private void cleanDom(Document doc) {
        // 1단계: 무조건 제거할 태그들
        doc.select("script, style, noscript, iframe, svg, form, video, audio").remove();

        // 2단계: 레이아웃용 태그 (본문이 아닌 영역)
        doc.select("nav, footer, aside, header").remove();

        // 3단계: class나 id에 특정 키워드가 포함된 요소 제거
        //   CSS 선택자 문법: [class*=keyword] → class 속성에 keyword가 포함된 요소
        //
        //   주의: 너무 짧거나 일반적인 키워드는 본문까지 날릴 수 있다!
        //   예: "font-size"는 KBS의 본문 영역(.detail-body.font-size)까지 삭제해버렸다.
        //   그래서 여기에는 "본문에는 절대 안 쓰일" 키워드만 넣는다.
        String[] noiseKeywords = {
                // 광고
                "adbox", "adwrap", "adsense", "adfit", "ad-slot",
                "banner", "sponsor", "promotion", "commercial",
                // 댓글
                "comment", "disqus", "livere",
                // 소셜/공유
                "share", "social",
                // 추천 위젯
                "taboola", "dable", "outbrain",
                // 관련 기사
                "related",
                // 사이드바/네비게이션
                "sidebar", "side-bar", "side_bar",
                "gnb", "lnb", "breadcrumb",
                // 저작권/하단 정보
                "copyright",
                // 구독/채널 안내
                "subscribe", "newsletter",
                // 팝업/모달
                "popup", "modal"
        };

        for (String keyword : noiseKeywords) {
            doc.select("[class*=" + keyword + "]").remove();
            doc.select("[id*=" + keyword + "]").remove();
        }

        // 4단계: Google 광고 등 data 속성 기반 제거
        doc.select("[data-ad]").remove();
        doc.select("[data-ad-slot]").remove();
        doc.select("[id^=div-gpt-ad]").remove();
    }

    /**
     * 일반 사이트에서 본문을 추출한다.
     *
     * ── 전략 순서 (3단계 폴백) ──
     * 1단계: 알려진 CSS 셀렉터 — 주요 한국 언론사별 본문 영역을 직접 지정
     * 2단계: Readability4J — Mozilla Firefox 리더 모드 알고리즘으로 자동 추출
     *        (★ 핵심! 모든 사이트의 셀렉터를 등록할 필요 없이 텍스트 밀도로 본문을 찾는다)
     * 3단계: <p> 태그 수집 — 최후 수단
     */
    private String extractGeneralContent(Document doc) {
        // ── 1단계: 알려진 셀렉터로 시도 ──
        //   확실히 맞는 셀렉터가 있으면 가장 깨끗한 결과가 나온다.
        String[] knownSelectors = {
                ".article-body-text",       // 연합뉴스TV
                ".story-news .article",     // 연합뉴스
                "#articleWrap .article",    // 연합뉴스
                "#cont_newstext",           // KBS
                "#article_body",            // 조선/중앙
                ".ab_text",                 // 중앙일보
                "#articleBody",             // 경향신문
                "#iNewsContent",            // MBC
                "#textBody",                // SBS
                "#article_content",         // JTBC
                "#newsViewArea",            // MBN
                "#articletxt",              // 한국경제
                "[itemprop=articleBody]",   // 표준 마크업
        };

        for (String selector : knownSelectors) {
            Element el = doc.selectFirst(selector);
            if (el != null) {
                String text = extractTextFromHtml(el);
                if (text.length() >= 50) return text;
            }
        }

        // ── 2단계: Readability4J로 자동 추출 ──
        //   Mozilla Firefox의 "리더 모드"에 쓰이는 알고리즘이다.
        //   CSS 셀렉터가 아니라 "텍스트 밀도(text density)"를 분석해서
        //   광고/댓글/메뉴 등 노이즈를 자동으로 걸러내고 본문만 추출한다.
        //
        //   덕분에 모든 뉴스 사이트의 HTML 구조를 일일이 등록할 필요가 없다!
        //   위의 알려진 셀렉터에서 못 찾은 사이트도 대부분 여기서 잡힌다.
        try {
            String currentUrl = doc.location(); // 현재 페이지 URL
            Readability4J readability = new Readability4J(currentUrl, doc.html());
            Article article = readability.parse();

            String readableContent = article.getTextContent();
            if (readableContent != null && readableContent.trim().length() >= 50) {
                log.info("Readability4J로 본문 추출 성공: {}자", readableContent.trim().length());
                return readableContent.trim();
            }
        } catch (Exception e) {
            log.warn("Readability4J 추출 실패, <p> 태그 폴백 사용: {}", e.getMessage());
        }

        // ── 3단계: <p> 태그 수집 (최후 수단) ──
        StringBuilder sb = new StringBuilder();
        for (Element p : doc.select("p")) {
            String text = p.text().trim();
            if (text.length() >= 30) sb.append(text).append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * 추출된 텍스트에서 boilerplate(찌꺼기) 패턴을 제거한다.
     *
     * DOM 정리만으로는 잡히지 않는 텍스트 레벨 노이즈가 있다.
     * 예: "저작권자(c) 연합뉴스, 무단 전재 금지" 같은 문구가
     *     본문 요소 안에 직접 들어있는 경우.
     */
    private String cleanBoilerplate(String content) {
        if (content == null || content.isEmpty()) return content;

        StringBuilder sb = new StringBuilder();
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            // 각 boilerplate 패턴과 매칭 확인
            boolean isBoilerplate = false;
            for (Pattern pattern : BOILERPLATE_PATTERNS) {
                if (pattern.matcher(trimmed).matches()) {
                    isBoilerplate = true;
                    break;
                }
            }

            if (!isBoilerplate) {
                sb.append(trimmed).append("\n");
            }
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
     *
     * 대부분의 한국 뉴스 사이트는 <p> 대신 <br>로 줄바꿈한다.
     * JSoup의 .text()만 쓰면 줄바꿈이 사라지므로,
     * <br>을 \n으로 바꾼 뒤 HTML 태그를 제거하는 방식으로 추출한다.
     */
    private String extractTextFromHtml(Element parent) {
        String html = parent.html();
        String text = html
                .replaceAll("<br\\s*/?>", "\n")           // <br> → 줄바꿈
                .replaceAll("<[^>]+>", "")                 // 나머지 HTML 태그 제거
                .replaceAll("&nbsp;", " ")                 // HTML 엔티티 변환
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&amp;", "&")
                .replaceAll("&quot;", "\"");

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
