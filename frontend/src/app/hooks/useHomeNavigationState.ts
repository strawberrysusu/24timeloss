import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";

import type { PageName } from "../AppShell";

type HomeSection = "cat-tabs-wrap" | "trending-list" | null;

function derivePageName(pathname: string): PageName {
  if (pathname.startsWith("/detail/")) {
    return "detail";
  }
  if (pathname.startsWith("/mypage")) {
    return "mypage";
  }
  return "home";
}

export function useHomeNavigationState() {
  const location = useLocation();
  const navigate = useNavigate();

  const [showSampleBanner, setShowSampleBanner] = useState(true);
  const [homeResetKey, setHomeResetKey] = useState(0);
  const [pendingHomeSection, setPendingHomeSection] = useState<HomeSection>(null);

  useEffect(() => {
    if (location.pathname !== "/" || !pendingHomeSection) {
      return;
    }

    const timer = window.setTimeout(() => {
      document.getElementById(pendingHomeSection)?.scrollIntoView({
        behavior: "smooth",
        block: "start",
      });
      setPendingHomeSection(null);
    }, 100);

    return () => window.clearTimeout(timer);
  }, [location.pathname, pendingHomeSection, homeResetKey]);

  function navigateHome(section: HomeSection = null) {
    setHomeResetKey((previous) => previous + 1);
    setPendingHomeSection(section);
    navigate("/");
  }

  return {
    currentPage: derivePageName(location.pathname),
    showSampleBanner,
    homeResetKey,
    dismissSampleBanner: () => setShowSampleBanner(false),
    navigateHome,
  };
}
