import { useEffect, useRef } from "react";

const BOT_USERNAME = import.meta.env.VITE_BOT_USERNAME;
const API_BASE = import.meta.env.VITE_API_BASE ?? "";

/**
 * Official Telegram Login Widget in redirect mode: the browser is sent straight to the backend callback
 * with the signed payload, so the raw hash is verified server-side and never handled by this client.
 * Needs a public domain registered with BotFather (/setdomain); localhost is rejected by Telegram.
 */
export function TelegramLoginButton() {
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const container = containerRef.current;
    if (!container || !BOT_USERNAME) {
      return;
    }
    const script = document.createElement("script");
    script.src = "https://telegram.org/js/telegram-widget.js?22";
    script.async = true;
    script.setAttribute("data-telegram-login", BOT_USERNAME);
    script.setAttribute("data-size", "large");
    script.setAttribute("data-auth-url", `${API_BASE}/api/admin/auth/telegram`);
    script.setAttribute("data-request-access", "write");
    container.appendChild(script);
    return () => container.replaceChildren();
  }, []);

  if (!BOT_USERNAME) {
    return (
      <p className="text-sm text-muted-foreground">
        Set VITE_BOT_USERNAME and a public domain (BotFather /setdomain) to enable the Telegram widget.
      </p>
    );
  }

  return <div ref={containerRef} />;
}
