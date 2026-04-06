interface SampleBannerProps {
  onDismiss?: () => void;
}

export function SampleBanner({ onDismiss }: SampleBannerProps) {
  return (
    <div id="sample-banner">
      <div className="container sample-banner-inner">
        <span>
          📌 현재 표시된 뉴스는 <b>시연용 샘플 데이터</b>입니다. 로그인 후 <b>기사 등록</b>으로
          실제 뉴스를 추가할 수 있습니다.
        </span>
        <button className="sample-banner-close" type="button" onClick={onDismiss}>
          ✕
        </button>
      </div>
    </div>
  );
}
