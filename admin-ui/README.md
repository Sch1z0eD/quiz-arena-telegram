# QuizArena Admin UI

Web admin panel for the QuizArena bot — a single-page React app that talks to the Spring backend over REST. Project overview and backend setup: [`../README.md`](../README.md).

## Stack

- React 19 + TypeScript (strict)
- Vite
- Tailwind CSS v4 + shadcn/ui (Radix primitives), lucide-react icons
- TanStack Query for server state, React Router for routing
- Recharts for dashboard charts

## Develop

```bash
npm install
npm run dev        # http://localhost:5173
```

The dev server proxies `/api` to the backend at `http://localhost:8080` (`vite.config.ts`), so run the backend with the admin panel enabled alongside it — from the repo root:

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

Build and check:

```bash
npm run build      # tsc -b + production build to dist/
npm run preview    # serve the built bundle
npm run typecheck  # type-check only
```

The backend does not bundle this app; serve `dist/` behind the same origin as the API so the session cookie and CSRF stay first-party.

## Talking to the backend

- **API base** — `VITE_API_BASE` (empty in dev, so calls hit `/api/...` and Vite proxies them). Set it for a build that targets a backend on another origin.
- **Auth** — a server-set HttpOnly session cookie; the client never holds a token. Sign in with the Telegram Login widget (`VITE_BOT_USERNAME`; needs a public domain registered via BotFather `/setdomain` — not `localhost`). Dev builds also show a Dev login button, backed by the backend's `dev` profile.
- **CSRF** — mutating requests echo the `XSRF-TOKEN` cookie back in the `X-XSRF-TOKEN` header.

Environment variables (`.env.development`): `VITE_API_BASE`, `VITE_BOT_USERNAME`.
