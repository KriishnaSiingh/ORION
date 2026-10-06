import { useEffect, useState } from "react";
import { Link, useNavigate, useRouterState } from "@tanstack/react-router";
import { AlertTriangle, ArrowRight, CheckCircle2, Eye, EyeOff, ShieldCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Spinner } from "@/components/ui/spinner";
import { useAuth, isSupabaseConfigured } from "@/lib/auth-store";
import { apiClient } from "@/lib/api-client";
import { toast } from "sonner";

export function AuthPage({ mode }: { mode: "login" | "register" }) {
  const navigate = useNavigate();
  const location = useRouterState({ select: (s) => s.location.pathname });
  const { signIn, signUp, status, error, clear, initialize, initialized } = useAuth();

  const isLogin = mode === "login";

  const [show, setShow] = useState(false);
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [localError, setLocalError] = useState<string | null>(null);
  const [needConfirm, setNeedConfirm] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    void initialize();
  }, [initialize]);

  useEffect(() => {
    if (status === "authenticated") {
      toast.success(isLogin ? "Signed in" : "Account created");
      void navigate({ to: "/", replace: true });
    }
  }, [status, isLogin, navigate]);

  useEffect(() => {
    setLocalError(null);
    setNeedConfirm(false);
    clear();
  }, [location, clear]);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLocalError(null);
    setNeedConfirm(false);

    if (!isSupabaseConfigured) {
      setLocalError(
        "Supabase is not yet wired up. Add VITE_SUPABASE_URL and VITE_SUPABASE_ANON_KEY to your .env file, then restart the dev server.",
      );
      return;
    }
    if (!email.trim() || !password.trim()) {
      setLocalError("Please enter your email and password.");
      return;
    }
    if (!isLogin && !name.trim()) {
      setLocalError("Please enter your full name.");
      return;
    }
    if (password.length < 6) {
      setLocalError("Password must be at least 6 characters.");
      return;
    }

    setSubmitting(true);
    try {
      if (isLogin) {
        const res = await signIn(email.trim(), password);
        if (!res.ok && res.error) setLocalError(res.error);
        if (res.ok) {
          try {
            const backendRes = await apiClient.post<{ accessToken?: string }>("/auth/login", {
              email: email.trim(),
              password: password,
            });
            const token = (backendRes as { accessToken?: string })?.accessToken;
            if (token) {
              localStorage.setItem("token", token);
            }
          } catch (backendErr) {
            console.warn("Backend login failed, continuing with Supabase auth only:", backendErr);
          }
        }
      } else {
        const res = await signUp(name.trim(), email.trim(), password);
        if (!res.ok && res.error) setLocalError(res.error);
        if (res.needsEmailConfirm) setNeedConfirm(true);
        if (res.ok) {
          try {
            const backendRes = await apiClient.post<{ accessToken?: string }>("/auth/register-tenant", {
              name: name.trim(),
              email: email.trim(),
              password: password,
            });
            const token = (backendRes as { accessToken?: string })?.accessToken;
            if (token) {
              localStorage.setItem("token", token);
            }
          } catch (backendErr) {
            console.warn(
              "Backend register-tenant failed, continuing with Supabase auth only:",
              backendErr,
            );
          }
        }
      }
    } finally {
      setSubmitting(false);
    }
  };

  const disabled = submitting || (initialized && status === "loading");

  return (
    <main className="grid min-h-screen bg-background lg:grid-cols-[1fr_480px]">
      <section className="relative hidden overflow-hidden border-r border-border lg:flex lg:flex-col lg:justify-between lg:p-12">
        <div className="absolute inset-0 bg-auth-grid opacity-50" />
        <div className="relative flex items-center gap-3">
          <div className="flex size-9 items-center justify-center rounded-md bg-primary font-display font-bold text-primary-foreground">
            O
          </div>
          <div>
            <p className="font-display text-sm font-semibold">Orion Intelligence</p>
            <p className="text-[10px] text-muted-foreground">Enterprise Decision OS</p>
          </div>
        </div>
        <div className="relative max-w-2xl">
          <div className="mb-7 flex size-12 items-center justify-center rounded-md border border-primary/30 bg-primary/10">
            <ShieldCheck className="text-primary" />
          </div>
          <h1 className="font-display text-4xl font-semibold leading-tight">
            Turn fragmented data into
            <br />
            operational decisions.
          </h1>
          <p className="mt-5 max-w-lg text-base leading-7 text-muted-foreground">
            A governed knowledge layer for complex investigations, trusted AI, and mission-critical
            applications.
          </p>
        </div>
        <p className="relative text-xs text-muted-foreground">
          SOC 2 Type II · ISO 27001 · Enterprise SSO
        </p>
      </section>

      <section className="flex items-center justify-center p-6">
        <form className="w-full max-w-sm" onSubmit={onSubmit}>
          <div className="mb-10 flex items-center gap-3 lg:hidden">
            <div className="flex size-9 items-center justify-center rounded-md bg-primary font-display font-bold text-primary-foreground">
              O
            </div>
            <span className="font-display font-semibold">Orion Intelligence</span>
          </div>

          <p className="text-[11px] font-semibold uppercase tracking-[.15em] text-primary">
            Secure workspace
          </p>
          <h2 className="mt-3 font-display text-2xl font-semibold">
            {isLogin ? "Sign in to Orion" : "Create your account"}
          </h2>
          <p className="mt-2 text-sm text-muted-foreground">
            {isLogin
              ? "Access your organization’s intelligence workspace."
              : "Join your organization’s intelligence workspace."}
          </p>

          {needConfirm && (
            <Alert variant="default" className="mt-6 border-emerald-500/30 bg-emerald-500/10">
              <CheckCircle2 className="size-4 text-emerald-500" />
              <AlertDescription className="text-xs">
                Check your inbox — we’ve sent a confirmation link to{" "}
                <span className="font-medium">{email}</span>.
              </AlertDescription>
            </Alert>
          )}

          {(localError || error) && !needConfirm && (
            <Alert variant="destructive" className="mt-6">
              <AlertTriangle className="size-4" />
              <AlertDescription className="text-xs">{localError ?? error}</AlertDescription>
            </Alert>
          )}

          <div className="mt-8 space-y-5">
            {!isLogin && (
              <div className="space-y-2">
                <Label htmlFor="name">Full name</Label>
                <Input
                  id="name"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="Maya Chen"
                  autoComplete="name"
                  required
                  disabled={disabled}
                />
              </div>
            )}

            <div className="space-y-2">
              <Label htmlFor="email">Work email</Label>
              <Input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="name@company.com"
                autoComplete={isLogin ? "email" : "username"}
                required
                disabled={disabled}
              />
            </div>

            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <Label htmlFor="password">Password</Label>
                {isLogin && (
                  <button
                    type="button"
                    className="text-xs text-primary hover:underline disabled:opacity-50"
                    disabled={disabled}
                    onClick={() =>
                      toast.info(
                        "Password reset is coming soon. Contact your workspace admin for help.",
                      )
                    }
                  >
                    Forgot password?
                  </button>
                )}
              </div>
              <div className="relative">
                <Input
                  id="password"
                  type={show ? "text" : "password"}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••••••"
                  autoComplete={isLogin ? "current-password" : "new-password"}
                  required
                  disabled={disabled}
                />
                <Button
                  type="button"
                  variant="ghost"
                  size="icon-sm"
                  className="absolute right-1 top-1"
                  onClick={() => setShow(!show)}
                  aria-label={show ? "Hide password" : "Show password"}
                  disabled={disabled}
                >
                  {show ? <EyeOff /> : <Eye />}
                </Button>
              </div>
            </div>

            <Button className="w-full" type="submit" disabled={disabled}>
              {disabled && <Spinner size="sm" />}
              {isLogin ? "Continue to workspace" : "Create account"}
              <ArrowRight />
            </Button>
          </div>

          <div className="my-7 flex items-center gap-3 text-[10px] uppercase tracking-[.12em] text-muted-foreground">
            <span className="h-px flex-1 bg-border" />
            or
            <span className="h-px flex-1 bg-border" />
          </div>

          <Button
            type="button"
            variant="outline"
            className="w-full"
            disabled
            onClick={() => toast.info("SSO / OAuth coming soon.")}
          >
            Continue with SSO
          </Button>

          <p className="mt-7 text-center text-xs text-muted-foreground">
            {isLogin ? "New to Orion? " : "Already have access? "}
            <Link
              to={isLogin ? "/register" : "/login"}
              className="font-medium text-primary hover:underline"
            >
              {isLogin ? "Create account" : "Sign in"}
            </Link>
          </p>

          <p className="mt-8 border-t border-border pt-5 text-center text-[10px] leading-5 text-muted-foreground">
            By continuing, you agree to your organization’s security and acceptable use policies.
          </p>
        </form>
      </section>
    </main>
  );
}
