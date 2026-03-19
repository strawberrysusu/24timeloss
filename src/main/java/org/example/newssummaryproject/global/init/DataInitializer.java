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

        // 샘플 기사의 작성자로 사용할 시스템 회원
        Member systemWriter = memberRepository.findByEmail("test@test.com")
                .orElseGet(() -> memberRepository.save(Member.builder()
                        .email("test@test.com")
                        .password(passwordEncoder.encode("1234"))
                        .nickname("테스트유저")
                        .build()));

        LocalDateTime now = LocalDateTime.now();

        // ── POLITICS ────────────────────────────
        Article p1 = save(systemWriter, Category.POLITICS,
                "2026년 지방선거 앞두고 여야 공천 경쟁 본격화",
                "https://example.com/news/p1", "한국일보", now.minusHours(1),
                "2026년 지방선거를 앞두고 여야 각 당의 공천 경쟁이 본격화되고 있다. 여당은 현역 의원들의 지역구 경쟁이 치열해지고 있으며, 야당은 새로운 인물 영입에 주력하고 있다. 정치권 관계자는 \"이번 선거가 향후 정치 지형을 결정짓는 중요한 분수령이 될 것\"이라고 분석했다. 각 당의 공천 심사 기준과 일정에 대한 관심이 높아지고 있으며, 예비 후보들의 활동도 활발해지고 있다.");
        summary(p1,
                "2026 지방선거를 앞두고 여야 모두 공천 경쟁에 돌입했다.",
                "여당은 현역 지역구 경쟁, 야당은 신규 인물 영입에 초점을 맞추고 있다.",
                "이번 선거가 향후 정치 지형의 분수령이 될 것이라는 분석이 나온다.",
                "여야 공천 경쟁 본격화", "여당 현역 vs 야당 인물 영입 구도", "정치 지형 결정의 분수령");

        Article p2 = save(systemWriter, Category.POLITICS,
                "국회, 디지털 기본법 통과… 온라인 플랫폼 규제 강화",
                "https://example.com/news/p2", "연합뉴스", now.minusHours(3),
                "국회 본회의에서 디지털 기본법이 찬성 다수로 통과되었다. 이 법은 대형 온라인 플랫폼의 시장 지배력 남용을 방지하고 이용자 보호를 강화하는 내용을 담고 있다. 플랫폼 기업에 대한 투명성 의무와 데이터 이동권 보장이 핵심이다. 업계에서는 규제 과잉을 우려하는 목소리도 있지만, 소비자 단체는 환영의 뜻을 밝혔다.");
        summary(p2,
                "국회가 대형 온라인 플랫폼 규제를 핵심으로 하는 디지털 기본법을 통과시켰다.",
                "플랫폼의 투명성 의무와 데이터 이동권 보장이 주요 내용이다.",
                "업계는 규제 과잉을 우려하지만, 소비자 단체는 긍정적으로 평가하고 있다.",
                "디지털 기본법 국회 통과", "플랫폼 투명성·데이터 이동권 보장", "업계 우려 vs 소비자 환영");

        Article p3 = save(systemWriter, Category.POLITICS,
                "대통령, 아세안 순방 성과… 경제 협력 MOU 12건 체결",
                "https://example.com/news/p3", "조선일보", now.minusHours(8),
                "대통령이 아세안 3개국 순방을 마치고 귀국했다. 이번 순방에서 총 12건의 경제 협력 양해각서(MOU)를 체결했으며, 반도체·배터리·인프라 분야의 협력 확대에 합의했다. 외교부는 \"아세안과의 전략적 동반자 관계를 한 단계 격상시킨 성과\"라고 평가했다.");
        summary(p3,
                "대통령이 아세안 순방에서 12건의 경제 협력 MOU를 체결했다.",
                "반도체·배터리·인프라 분야의 협력 확대에 합의하며 경제 외교 성과를 냈다.",
                "외교부는 아세안과의 전략적 동반자 관계가 격상되었다고 평가했다.",
                "아세안 경제 협력 MOU 12건 체결", "반도체·배터리·인프라 협력 확대", "전략적 동반자 관계 격상");

        // ── ECONOMY ─────────────────────────────
        Article e1 = save(systemWriter, Category.ECONOMY,
                "한국은행, 기준금리 동결…\"하반기 경기 회복 기대\"",
                "https://example.com/news/e1", "매일경제", now.minusHours(2),
                "한국은행이 기준금리를 현행 3.0%로 동결했다. 금융통화위원회는 \"글로벌 경제 불확실성이 여전하지만, 하반기부터 내수 회복이 가시화될 것으로 전망한다\"고 밝혔다. 시장에서는 하반기 금리 인하 가능성에 주목하고 있으며, 부동산 시장과 가계부채에 미치는 영향에 대한 분석이 이어지고 있다.");
        summary(e1,
                "한국은행이 기준금리 3.0%를 동결하며 하반기 경기 회복에 기대감을 보였다.",
                "글로벌 경제 불확실성 속에서도 내수 회복이 가시화될 것으로 전망했다.",
                "시장은 하반기 금리 인하 가능성과 부동산·가계부채 영향에 주목하고 있다.",
                "기준금리 3.0% 동결", "하반기 내수 회복 전망", "금리 인하 가능성 주목");

        Article e2 = save(systemWriter, Category.ECONOMY,
                "코스피, 3,000선 돌파… 반도체 랠리가 주도",
                "https://example.com/news/e2", "한국경제", now.minusHours(4),
                "코스피 지수가 3년 만에 3,000선을 돌파했다. 삼성전자와 SK하이닉스 등 반도체 대형주가 강세를 보이며 지수 상승을 이끌었다. AI 서버 수요 증가로 메모리 반도체 가격이 회복세를 보이면서 투자 심리가 개선되고 있다. 외국인 순매수도 한 달째 이어지고 있다.");
        summary(e2,
                "코스피가 3년 만에 3,000선을 돌파하며 반도체주가 랠리를 주도했다.",
                "AI 서버 수요 증가로 메모리 반도체 가격 회복이 투자 심리를 개선하고 있다.",
                "외국인 순매수가 한 달째 이어지며 시장에 긍정적 신호를 보내고 있다.",
                "코스피 3,000선 3년 만에 돌파", "AI 수요로 반도체 가격 회복", "외국인 순매수 한 달째 지속");

        Article e3 = save(systemWriter, Category.ECONOMY,
                "정부, 청년 주거 지원 대폭 확대… 전세대출 금리 1%대",
                "https://example.com/news/e3", "서울경제", now.minusHours(10),
                "정부가 청년층 주거 안정을 위한 종합 대책을 발표했다. 핵심은 청년 전세대출 금리를 기존 2.5%에서 1.5%로 인하하고, 청약 가점 우대 범위를 확대하는 것이다. 또한 공공임대주택 5만 호를 추가 공급하기로 했다. 청년 단체에서는 실효성 있는 대책이라며 환영하고 있다.");
        summary(e3,
                "정부가 청년 전세대출 금리를 1.5%로 인하하는 주거 지원 종합 대책을 발표했다.",
                "청약 가점 우대 확대와 공공임대 5만 호 추가 공급도 포함되었다.",
                "청년 단체는 실효성 있는 대책이라며 긍정적으로 평가하고 있다.",
                "청년 전세대출 금리 1.5%로 인하", "공공임대 5만 호 추가 공급", "청약 가점 우대 범위 확대");

        // ── SOCIETY ─────────────────────────────
        Article s1 = save(systemWriter, Category.SOCIETY,
                "수도권 출퇴근 시간 단축 위한 광역급행철도 착공",
                "https://example.com/news/s1", "조선일보", now.minusHours(4),
                "수도권 광역급행철도(GTX) D노선 착공이 확정되었다. 이 노선이 개통되면 경기 남부에서 서울 도심까지 출퇴근 시간이 기존 1시간 30분에서 30분대로 단축될 전망이다. 국토교통부는 \"교통 혁신을 통해 수도권 균형 발전에 기여할 것\"이라고 밝혔다.");
        summary(s1,
                "수도권 GTX D노선 착공이 확정되어 경기 남부~서울 통근 시간이 대폭 단축된다.",
                "출퇴근 시간이 1시간 30분에서 30분대로 줄어들 전망이다.",
                "국토교통부는 수도권 균형 발전에 기여할 교통 혁신이라고 평가했다.",
                "GTX D노선 착공 확정", "출퇴근 1시간30분→30분 단축", "수도권 균형 발전 기여");

        Article s2 = save(systemWriter, Category.SOCIETY,
                "전국 어린이집 의무 CCTV 설치 법안 시행",
                "https://example.com/news/s2", "MBC뉴스", now.minusHours(6),
                "전국 모든 어린이집에 CCTV 설치를 의무화하는 법안이 시행되었다. 이에 따라 보육 시설 내 아동학대 예방과 안전 관리가 강화될 것으로 기대된다. 설치 비용은 정부와 지자체가 분담하며, 영상 보관 기간은 60일로 정해졌다. 학부모들은 안심 보육 환경 조성에 큰 도움이 될 것이라고 평가했다.");
        summary(s2,
                "전국 어린이집에 CCTV 설치를 의무화하는 법안이 시행되었다.",
                "아동학대 예방과 안전 관리 강화가 목적이며, 비용은 정부·지자체가 부담한다.",
                "영상 보관 기간은 60일이며, 학부모들은 안심 보육 환경에 긍정적 반응이다.",
                "어린이집 CCTV 설치 의무화", "설치 비용 정부·지자체 분담", "영상 60일 보관");

        Article s3 = save(systemWriter, Category.SOCIETY,
                "서울시, 2026년 도시재생 뉴딜 사업 본격 추진",
                "https://example.com/news/s3", "KBS뉴스", now.minusHours(12),
                "서울시가 노후 주거지역 10곳을 대상으로 도시재생 뉴딜 사업을 본격 추진한다. 이번 사업은 낡은 주택 정비와 함께 커뮤니티 공간, 주민 편의시설을 확충하는 데 초점을 맞추고 있다. 총 사업비 3,000억 원이 투입되며, 2028년 완공을 목표로 하고 있다.");
        summary(s3,
                "서울시가 노후 주거지역 10곳을 대상으로 도시재생 뉴딜 사업에 착수한다.",
                "낡은 주택 정비와 커뮤니티 공간·편의시설 확충이 핵심이다.",
                "총 사업비 3,000억 원을 투입하여 2028년 완공을 목표로 한다.",
                "노후 주거지역 10곳 도시재생", "커뮤니티 공간·편의시설 확충", "사업비 3,000억 원 투입");

        // ── IT_SCIENCE ──────────────────────────
        Article t1 = save(systemWriter, Category.IT_SCIENCE,
                "국내 AI 스타트업, 글로벌 시장서 주목받는 이유",
                "https://example.com/news/t1", "전자신문", now.minusHours(3),
                "국내 AI 스타트업들이 글로벌 시장에서 빠르게 성장하고 있다. 특히 자연어 처리와 컴퓨터 비전 분야에서 독자적인 기술력을 인정받고 있으며, 실리콘밸리 투자사들의 관심이 집중되고 있다. 업계 관계자는 \"한국의 풍부한 데이터 인프라와 우수한 인재풀이 경쟁력의 핵심\"이라고 분석했다. 정부도 AI 산업 육성을 위한 규제 완화와 지원책을 확대하고 있다.");
        summary(t1,
                "국내 AI 스타트업이 자연어 처리·컴퓨터 비전 분야에서 글로벌 시장의 주목을 받고 있다.",
                "실리콘밸리 투자사들이 한국 AI 기업에 대한 투자를 확대하는 추세다.",
                "정부도 규제 완화와 지원책으로 AI 산업 육성에 나서고 있다.",
                "한국 AI 스타트업의 독자적 기술력 인정", "풍부한 데이터 인프라와 인재풀이 핵심 경쟁력", "정부의 AI 산업 규제 완화 및 지원 확대");

        Article t2 = save(systemWriter, Category.IT_SCIENCE,
                "삼성전자, 차세대 2나노 반도체 양산 돌입",
                "https://example.com/news/t2", "디지털타임스", now.minusHours(5),
                "삼성전자가 세계 최초로 2나노 공정 반도체 양산에 돌입했다. GAA(Gate-All-Around) 기술을 적용한 이번 칩은 기존 3나노 대비 전력 효율이 25% 향상되고 성능은 12% 개선되었다. AI 가속기와 고성능 모바일 프로세서 시장을 겨냥한 것으로, TSMC와의 파운드리 경쟁에서 기술 주도권을 확보하겠다는 전략이다.");
        summary(t2,
                "삼성전자가 세계 최초로 GAA 기술 기반 2나노 반도체 양산을 시작했다.",
                "기존 3나노 대비 전력 효율 25% 향상, 성능 12% 개선을 달성했다.",
                "AI 가속기·모바일 프로세서 시장에서 TSMC와의 기술 경쟁에 나선다.",
                "세계 최초 2나노 GAA 양산", "전력 효율 25%↑ 성능 12%↑", "TSMC와 파운드리 기술 경쟁");

        Article t3 = save(systemWriter, Category.IT_SCIENCE,
                "과기정통부, 6G 핵심기술 로드맵 발표",
                "https://example.com/news/t3", "ZDNet코리아", now.minusHours(14),
                "과학기술정보통신부가 6G 핵심기술 로드맵을 발표했다. 2028년까지 테라헤르츠 통신, AI 기반 네트워크 자동화, 초정밀 위치 측정 기술 등 10대 핵심 기술을 확보하겠다는 계획이다. 총 2조 원의 연구개발 예산이 투입되며, 민관 합동 컨소시엄을 구성하여 글로벌 표준 선점을 목표로 한다.");
        summary(t3,
                "과기정통부가 2028년까지 6G 10대 핵심기술 확보를 위한 로드맵을 발표했다.",
                "테라헤르츠 통신, AI 네트워크 자동화, 초정밀 위치 측정이 핵심 기술이다.",
                "총 2조 원 R&D 투입과 민관 컨소시엄을 통해 글로벌 표준 선점을 목표로 한다.",
                "6G 10대 핵심기술 로드맵", "R&D 예산 2조 원 투입", "글로벌 표준 선점 목표");

        // ── WORLD ───────────────────────────────
        Article w1 = save(systemWriter, Category.WORLD,
                "미·중 정상회담 개최…무역 갈등 해소 논의",
                "https://example.com/news/w1", "연합뉴스", now.minusHours(6),
                "미국과 중국 정상이 회담을 갖고 양국 간 무역 갈등 해소 방안을 논의했다. 양측은 관세 조정과 기술 협력 확대에 대해 의견을 교환했으며, 추가 실무 협상을 이어가기로 합의했다. 국제 사회는 이번 회담이 글로벌 경제 안정에 긍정적 신호가 될 것으로 기대하고 있다.");
        summary(w1,
                "미·중 정상이 무역 갈등 해소를 위한 회담을 개최했다.",
                "관세 조정과 기술 협력 확대에 대해 논의하고 추가 실무 협상에 합의했다.",
                "국제 사회는 글로벌 경제 안정에 긍정적 신호로 평가하고 있다.",
                "미·중 정상회담 개최", "관세 조정·기술 협력 논의", "글로벌 경제 안정 기대감");

        Article w2 = save(systemWriter, Category.WORLD,
                "EU, 탄소국경조정제도 본격 시행… 한국 수출 영향은",
                "https://example.com/news/w2", "경향신문", now.minusHours(9),
                "유럽연합(EU)이 탄소국경조정제도(CBAM)를 본격 시행했다. 철강, 알루미늄, 시멘트 등 탄소 배출량이 많은 제품을 EU에 수출할 때 추가 비용이 부과된다. 한국 수출 기업들도 영향을 받을 것으로 예상되며, 산업통상자원부는 기업 지원 대책을 마련 중이다. 전문가들은 장기적으로 친환경 전환을 가속화하는 계기가 될 것이라고 분석했다.");
        summary(w2,
                "EU가 탄소국경조정제도(CBAM)를 본격 시행하며 수입품에 탄소 비용을 부과한다.",
                "철강·알루미늄 등 한국 수출 기업에도 영향이 예상된다.",
                "전문가들은 친환경 전환을 가속화하는 계기가 될 것으로 분석한다.",
                "EU CBAM 본격 시행", "한국 수출 기업 비용 부담 우려", "친환경 전환 가속화 계기");

        Article w3 = save(systemWriter, Category.WORLD,
                "일본 엔화 약세 지속… 한일 관광 트렌드 변화",
                "https://example.com/news/w3", "뉴시스", now.minusHours(11),
                "일본 엔화가 달러당 160엔을 넘기며 약세를 지속하고 있다. 이로 인해 한국인의 일본 관광이 크게 증가하고 있으며, 반대로 일본인 관광객의 한국 방문은 감소세를 보이고 있다. 관광업계는 엔저가 당분간 지속될 것으로 보며, 양국 관광 시장의 구조적 변화에 주목하고 있다.");
        summary(w3,
                "엔화가 달러당 160엔을 넘기며 약세를 이어가고 있다.",
                "한국인의 일본 관광 급증과 일본인의 한국 방문 감소가 동시에 나타나고 있다.",
                "관광업계는 엔저 지속에 따른 양국 관광 시장 구조 변화에 주목한다.",
                "엔화 달러당 160엔 돌파", "한국인 일본 관광 급증", "양국 관광 시장 구조 변화");

        // ── SPORTS ──────────────────────────────
        Article sp1 = save(systemWriter, Category.SPORTS,
                "손흥민, 시즌 15호골 폭발…팀 승리 이끌어",
                "https://example.com/news/sp1", "스포츠조선", now.minusHours(5),
                "토트넘 홋스퍼의 손흥민이 시즌 15호골을 기록하며 팀의 3-1 승리를 이끌었다. 손흥민은 전반 20분 선제골에 이어 후반 65분 추가골을 넣으며 맹활약했다. 경기 후 감독은 \"손흥민은 세계 최고 수준의 공격수\"라며 극찬했다.");
        summary(sp1,
                "손흥민이 시즌 15호골을 넣으며 토트넘의 3-1 승리를 이끌었다.",
                "전반 선제골에 이어 후반 추가골까지 멀티골을 기록했다.",
                "감독은 손흥민을 세계 최고 수준의 공격수라고 극찬했다.",
                "손흥민 시즌 15호골 멀티골", "토트넘 3-1 승리 이끌어", "감독 '세계 최고 수준' 극찬");

        Article sp2 = save(systemWriter, Category.SPORTS,
                "2026 FIFA 월드컵 조 편성 확정… 한국 C조 배정",
                "https://example.com/news/sp2", "OSEN", now.minusHours(7),
                "2026 FIFA 월드컵 조 편성이 확정되었다. 한국은 C조에 배정되어 멕시코, 세네갈, 뉴질랜드와 같은 조에 속하게 되었다. 축구 전문가들은 한국이 16강 진출 가능성이 높은 비교적 유리한 조에 들어갔다고 분석했다. 대표팀은 월드컵까지 남은 기간 동안 집중 훈련에 돌입할 예정이다.");
        summary(sp2,
                "2026 월드컵 조 편성에서 한국이 C조에 배정되었다.",
                "멕시코, 세네갈, 뉴질랜드와 같은 조로 16강 진출 가능성이 높다는 분석이다.",
                "대표팀은 월드컵까지 집중 훈련에 돌입할 예정이다.",
                "한국 C조 배정 확정", "16강 진출 유리한 조 편성", "대표팀 집중 훈련 돌입");

        Article sp3 = save(systemWriter, Category.SPORTS,
                "KBO 리그 개막… 올 시즌 주목할 신인왕 후보는",
                "https://example.com/news/sp3", "스포츠동아", now.minusHours(13),
                "2026 KBO 리그가 개막했다. 올 시즌은 드래프트 1순위 지명 선수들을 비롯해 역대급 신인들이 대거 프로 무대에 등장하면서 신인왕 경쟁이 뜨거울 전망이다. 특히 고교 통산 최다 홈런 기록을 세운 강타자와 150km 강속구 좌완 투수가 주목받고 있다.");
        summary(sp3,
                "2026 KBO 리그가 개막하며 역대급 신인들이 프로 무대에 등장했다.",
                "드래프트 1순위 선수들을 포함해 신인왕 경쟁이 뜨거울 전망이다.",
                "고교 최다 홈런 강타자와 150km 좌완 투수가 특히 주목받고 있다.",
                "KBO 2026시즌 개막", "역대급 신인 대거 등장", "신인왕 경쟁 뜨거울 전망");

        // ── ENTERTAINMENT ───────────────────────
        Article en1 = save(systemWriter, Category.ENTERTAINMENT,
                "넷플릭스 한국 드라마, 글로벌 1위 재탈환",
                "https://example.com/news/en1", "OSEN", now.minusHours(7),
                "넷플릭스 오리지널 한국 드라마가 공개 첫 주 만에 글로벌 시청 시간 1위를 기록했다. 이 드라마는 90개국 이상에서 TOP 10에 진입하며 K-콘텐츠의 저력을 다시 한번 입증했다. 제작진은 \"한국 스토리텔링의 힘이 세계적으로 통한다는 것을 보여주는 결과\"라고 소감을 밝혔다.");
        summary(en1,
                "넷플릭스 한국 드라마가 공개 첫 주에 글로벌 시청 시간 1위를 기록했다.",
                "90개국 이상에서 TOP 10에 진입하며 K-콘텐츠의 저력을 입증했다.",
                "제작진은 한국 스토리텔링의 세계적 경쟁력을 보여주는 결과라고 평가했다.",
                "글로벌 시청 시간 1위 달성", "90개국 이상 TOP 10 진입", "K-콘텐츠 스토리텔링 경쟁력 입증");

        Article en2 = save(systemWriter, Category.ENTERTAINMENT,
                "BTS 진, 솔로 월드투어 북미 전석 매진",
                "https://example.com/news/en2", "스타뉴스", now.minusHours(9),
                "BTS 멤버 진의 첫 솔로 월드투어 북미 공연이 전석 매진되었다. 뉴욕, LA, 시카고 등 5개 도시에서 총 10회 공연이 진행되며, 티켓 오픈 1분 만에 모두 판매되었다. 소속사는 \"솔로로서의 글로벌 인기를 다시 한번 확인한 결과\"라며 추가 공연을 검토 중이라고 밝혔다.");
        summary(en2,
                "BTS 진의 솔로 월드투어 북미 5개 도시 공연이 전석 매진되었다.",
                "티켓 오픈 1분 만에 모두 판매될 정도로 폭발적인 인기를 보였다.",
                "소속사는 추가 공연을 검토 중이라고 밝혔다.",
                "북미 5개 도시 전석 매진", "티켓 1분 만에 완판", "추가 공연 검토 중");

        Article en3 = save(systemWriter, Category.ENTERTAINMENT,
                "칸 영화제, 한국 영화 3편 경쟁 부문 진출",
                "https://example.com/news/en3", "씨네21", now.minusHours(15),
                "제79회 칸 국제영화제 경쟁 부문에 한국 영화 3편이 진출했다. 이는 역대 최다 기록으로, 한국 영화의 위상이 한 단계 더 올라갔다는 평가다. 봉준호 감독의 신작을 비롯해 신예 감독 2명의 작품이 포함되어 있으며, 황금종려상 수상 가능성에도 관심이 쏠리고 있다.");
        summary(en3,
                "칸 영화제 경쟁 부문에 한국 영화 3편이 역대 최다로 진출했다.",
                "봉준호 감독 신작과 신예 감독 2명의 작품이 포함되었다.",
                "한국 영화의 위상 상승과 황금종려상 수상 가능성에 관심이 쏠린다.",
                "역대 최다 3편 경쟁 부문 진출", "봉준호 신작 포함", "황금종려상 수상 가능성 주목");

    }

    private Article save(Member writer, Category category, String title, String url, String source,
                         LocalDateTime publishedAt, String content) {
        return articleRepository.save(Article.builder()
                .category(category)
                .title(title)
                .originalUrl(url)
                .source(source)
                .thumbnailUrl("https://picsum.photos/seed/" + url.hashCode() + "/400/200")
                .content(content)
                .publishedAt(publishedAt)
                .writer(writer)
                .build());
    }

    private void summary(Article article,
                         String line1, String line2, String line3,
                         String key1, String key2, String key3) {
        articleSummaryRepository.save(ArticleSummary.builder()
                .article(article)
                .summaryLine1(line1).summaryLine2(line2).summaryLine3(line3)
                .keyPoint1(key1).keyPoint2(key2).keyPoint3(key3)
                .build());
    }
}
