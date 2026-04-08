export function SiteFooter() {
  return (
    <footer className="site-footer">
      <div className="container">
        <div className="footer-inner">
          <div className="footer-logo">
            <div className="footer-logo-icon">
              <span>N</span>
            </div>
            <span className="footer-logo-text">NewsPick</span>
          </div>
          <p className="footer-copy">© 2026 NewsPick. AI 뉴스 요약 서비스.</p>
          <nav className="footer-links">
            <a href="#">이용약관</a>
            <a href="#">개인정보처리방침</a>
            <a href="#">문의</a>
          </nav>
        </div>
      </div>
    </footer>
  );
}
