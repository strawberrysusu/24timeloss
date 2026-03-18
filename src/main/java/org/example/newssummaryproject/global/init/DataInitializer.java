package org.example.newssummaryproject.global.init;

import lombok.RequiredArgsConstructor;
import org.example.newssummaryproject.domain.member.Member;
import org.example.newssummaryproject.domain.member.MemberRepository;
import org.example.newssummaryproject.domain.news.Article;
import org.example.newssummaryproject.domain.news.ArticleRepository;
import org.example.newssummaryproject.domain.news.ArticleSummary;
import org.example.newssummaryproject.domain.news.ArticleSummaryRepository;
import org.example.newssummaryproject.domain.news.Category;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 개발 초기에 화면 확인용 샘플 데이터를 넣는 초기화 클래스다.
 * 기사가 하나도 없을 때만 동작한다.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final ArticleRepository articleRepository;
    private final ArticleSummaryRepository articleSummaryRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (articleRepository.count() > 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        Article a1 = articleRepository.save(Article.builder()
                .category(Category.POLITICS)
                .title("2026년 지방선거 앞두고 여야 공천 경쟁 본격화")
                .originalUrl("https://example.com/news/1")
                .source("한국일보")
                .thumbnailUrl("https://picsum.photos/seed/news1/400/200")
                .content("2026년 지방선거를 앞두고 여야 각 당의 공천 경쟁이 본격화되고 있다. 여당은 현역 의원들의 지역구 경쟁이 치열해지고 있으며, 야당은 새로운 인물 영입에 주력하고 있다. 정치권 관계자는 \"이번 선거가 향후 정치 지형을 결정짓는 중요한 분수령이 될 것\"이라고 분석했다. 각 당의 공천 심사 기준과 일정에 대한 관심이 높아지고 있으며, 예비 후보들의 활동도 활발해지고 있다.")
                .publishedAt(now.minusHours(1))
                .build());

        Article a2 = articleRepository.save(Article.builder()
                .category(Category.ECONOMY)
                .title("한국은행, 기준금리 동결…\"하반기 경기 회복 기대\"")
                .originalUrl("https://example.com/news/2")
                .source("매일경제")
                .thumbnailUrl("https://picsum.photos/seed/news2/400/200")
                .content("한국은행이 기준금리를 현행 3.0%로 동결했다. 금융통화위원회는 \"글로벌 경제 불확실성이 여전하지만, 하반기부터 내수 회복이 가시화될 것으로 전망한다\"고 밝혔다. 시장에서는 하반기 금리 인하 가능성에 주목하고 있으며, 부동산 시장과 가계부채에 미치는 영향에 대한 분석이 이어지고 있다.")
                .publishedAt(now.minusHours(2))
                .build());

        Article a3 = articleRepository.save(Article.builder()
                .category(Category.IT_SCIENCE)
                .title("국내 AI 스타트업, 글로벌 시장서 주목받는 이유")
                .originalUrl("https://example.com/news/3")
                .source("전자신문")
                .thumbnailUrl("https://picsum.photos/seed/news3/400/200")
                .content("국내 AI 스타트업들이 글로벌 시장에서 빠르게 성장하고 있다. 특히 자연어 처리와 컴퓨터 비전 분야에서 독자적인 기술력을 인정받고 있으며, 실리콘밸리 투자사들의 관심이 집중되고 있다. 업계 관계자는 \"한국의 풍부한 데이터 인프라와 우수한 인재풀이 경쟁력의 핵심\"이라고 분석했다. 정부도 AI 산업 육성을 위한 규제 완화와 지원책을 확대하고 있다.")
                .publishedAt(now.minusHours(3))
                .build());

        Article a4 = articleRepository.save(Article.builder()
                .category(Category.SOCIETY)
                .title("수도권 출퇴근 시간 단축 위한 광역급행철도 착공")
                .originalUrl("https://example.com/news/4")
                .source("조선일보")
                .thumbnailUrl("https://picsum.photos/seed/news4/400/200")
                .content("수도권 광역급행철도(GTX) D노선 착공이 확정되었다. 이 노선이 개통되면 경기 남부에서 서울 도심까지 출퇴근 시간이 기존 1시간 30분에서 30분대로 단축될 전망이다. 국토교통부는 \"교통 혁신을 통해 수도권 균형 발전에 기여할 것\"이라고 밝혔다.")
                .publishedAt(now.minusHours(4))
                .build());

        Article a5 = articleRepository.save(Article.builder()
                .category(Category.SPORTS)
                .title("손흥민, 시즌 15호골 폭발…팀 승리 이끌어")
                .originalUrl("https://example.com/news/5")
                .source("스포츠조선")
                .thumbnailUrl("https://picsum.photos/seed/news5/400/200")
                .content("토트넘 홋스퍼의 손흥민이 시즌 15호골을 기록하며 팀의 3-1 승리를 이끌었다. 손흥민은 전반 20분 선제골에 이어 후반 65분 추가골을 넣으며 맹활약했다. 경기 후 감독은 \"손흥민은 세계 최고 수준의 공격수\"라며 극찬했다.")
                .publishedAt(now.minusHours(5))
                .build());

        Article a6 = articleRepository.save(Article.builder()
                .category(Category.WORLD)
                .title("미·중 정상회담 개최…무역 갈등 해소 논의")
                .originalUrl("https://example.com/news/6")
                .source("연합뉴스")
                .thumbnailUrl("https://picsum.photos/seed/news6/400/200")
                .content("미국과 중국 정상이 회담을 갖고 양국 간 무역 갈등 해소 방안을 논의했다. 양측은 관세 조정과 기술 협력 확대에 대해 의견을 교환했으며, 추가 실무 협상을 이어가기로 합의했다. 국제 사회는 이번 회담이 글로벌 경제 안정에 긍정적 신호가 될 것으로 기대하고 있다.")
                .publishedAt(now.minusHours(6))
                .build());

        Article a7 = articleRepository.save(Article.builder()
                .category(Category.ENTERTAINMENT)
                .title("넷플릭스 한국 드라마, 글로벌 1위 재탈환")
                .originalUrl("https://example.com/news/7")
                .source("OSEN")
                .thumbnailUrl("https://picsum.photos/seed/news7/400/200")
                .content("넷플릭스 오리지널 한국 드라마가 공개 첫 주 만에 글로벌 시청 시간 1위를 기록했다. 이 드라마는 90개국 이상에서 TOP 10에 진입하며 K-콘텐츠의 저력을 다시 한번 입증했다. 제작진은 \"한국 스토리텔링의 힘이 세계적으로 통한다는 것을 보여주는 결과\"라고 소감을 밝혔다.")
                .publishedAt(now.minusHours(7))
                .build());

        // 테스트용 회원 (이메일: test@test.com / 비밀번호: 1234)
        if (!memberRepository.existsByEmail("test@test.com")) {
            memberRepository.save(Member.builder()
                    .email("test@test.com")
                    .password(passwordEncoder.encode("1234"))
                    .nickname("테스트유저")
                    .build());
        }

        // a3(IT_SCIENCE) 기사에 AI 요약 샘플 추가
        articleSummaryRepository.save(ArticleSummary.builder()
                .article(a3)
                .summaryLine1("국내 AI 스타트업이 자연어 처리·컴퓨터 비전 분야에서 글로벌 시장의 주목을 받고 있다.")
                .summaryLine2("실리콘밸리 투자사들이 한국 AI 기업에 대한 투자를 확대하는 추세다.")
                .summaryLine3("정부도 규제 완화와 지원책으로 AI 산업 육성에 나서고 있다.")
                .keyPoint1("한국 AI 스타트업의 독자적 기술력 인정")
                .keyPoint2("풍부한 데이터 인프라와 인재풀이 핵심 경쟁력")
                .keyPoint3("정부의 AI 산업 규제 완화 및 지원 확대")
                .build());
    }
}
