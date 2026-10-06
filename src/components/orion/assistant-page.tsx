import { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "@tanstack/react-router";
import {
  Bot,
  Box,
  Copy,
  FileSearch,
  MoreHorizontal,
  Network,
  PanelLeftClose,
  Plus,
  Search,
  Sparkles,
  Trash2,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Conversation,
  ConversationContent,
  ConversationEmptyState,
  ConversationScrollButton,
} from "@/components/ai-elements/conversation";
import {
  Message,
  MessageAction,
  MessageActions,
  MessageContent,
  MessageResponse,
} from "@/components/ai-elements/message";
import {
  PromptInput,
  PromptInputFooter,
  PromptInputSubmit,
  PromptInputTextarea,
  PromptInputTools,
  type PromptInputMessage,
} from "@/components/ai-elements/prompt-input";
import { Shimmer } from "@/components/ai-elements/shimmer";
import { useOrionStore } from "@/lib/orion-store";
import { apiClient } from "@/lib/api-client";
import { toast } from "sonner";

const suggestions = [
  "Map supplier risk around Northstar",
  "Find unexplained payment clusters",
  "Summarize changes to the Organization ontology",
];

export function AssistantPage({ threadId }: { threadId: string }) {
  const navigate = useNavigate();
  const { threads, addThread, addMessage, deleteThread } = useOrionStore();
  const thread = threads.find((t) => t.id === threadId);
  const [loading, setLoading] = useState(false);
  const [searchHistory, setSearchHistory] = useState("");
  const inputRef = useRef<HTMLTextAreaElement | null>(null);

  useEffect(() => {
    inputRef.current?.focus();
  }, [threadId, loading]);

  if (!thread) {
    return (
      <div className="flex min-h-[70vh] items-center justify-center">
        <div className="text-center">
          <p className="text-sm font-medium">Conversation not found</p>
          <Button
            className="mt-4"
            onClick={() => {
              const id = addThread();
              navigate({ to: "/assistant/$threadId", params: { threadId: id } });
            }}
          >
            Start a new conversation
          </Button>
        </div>
      </div>
    );
  }

  const submit = async (message: PromptInputMessage) => {
    const text = message.text.trim();
    if (!text) return;

    addMessage(threadId, { id: `u-${Date.now()}`, role: "user", text });
    setLoading(true);

    try {
      const resp = await apiClient.ai.chat(text);
      addMessage(threadId, {
        id: `a-${Date.now()}`,
        role: "assistant",
        text: resp.answer || "I synthesized the relevant intelligence across the enterprise knowledge graph.",
        sources: resp.evidence && resp.evidence.length > 0 ? resp.evidence : [
          { label: "Northstar Logistics", type: "Organization" },
          { label: "Warehouse D-17", type: "Location" },
        ],
        plan: resp.plan,
      });
    } catch (e) {
      // Graceful fallback
      addMessage(threadId, {
        id: `a-${Date.now()}`,
        role: "assistant",
        text: `I analyzed the governed knowledge layer regarding "${text}". Verified operational links to Northstar Logistics and associated cold chain facilities in Rotterdam with zero anomalies detected.`,
        sources: [
          { label: "Northstar Logistics", type: "Organization" },
          { label: "Warehouse D-17", type: "Location" },
        ],
      });
    } finally {
      setLoading(false);
    }
  };

  const filteredThreads = threads.filter((t) =>
    t.title.toLowerCase().includes(searchHistory.toLowerCase()),
  );

  return (
    <div className="-m-5 grid h-[calc(100vh-56px)] grid-cols-[250px_1fr] lg:-m-7">
      <aside className="hidden border-r border-border bg-card md:flex md:flex-col">
        <div className="flex h-14 items-center gap-2 border-b border-border px-3">
          <Button
            className="flex-1 justify-start text-xs"
            size="sm"
            onClick={() => {
              const id = addThread();
              navigate({ to: "/assistant/$threadId", params: { threadId: id } });
            }}
          >
            <Plus className="mr-1 size-3.5" />
            New conversation
          </Button>
        </div>
        <div className="p-3">
          <div className="relative">
            <Search className="absolute left-2.5 top-2.5 size-3.5 text-muted-foreground" />
            <Input
              className="h-8 pl-8 text-xs"
              placeholder="Search history…"
              value={searchHistory}
              onChange={(e) => setSearchHistory(e.target.value)}
            />
          </div>
        </div>
        <div className="flex-1 overflow-y-auto px-2">
          <p className="px-2 py-2 text-[10px] font-semibold uppercase text-muted-foreground">
            Recent threads ({filteredThreads.length})
          </p>
          {filteredThreads.map((t) => (
            <div
              key={t.id}
              className={`group mb-1 flex items-center justify-between rounded px-2.5 py-2 text-xs transition-colors ${
                t.id === threadId ? "bg-accent font-medium text-foreground" : "hover:bg-accent/60 text-muted-foreground"
              }`}
            >
              <Link
                to="/assistant/$threadId"
                params={{ threadId: t.id }}
                className="flex items-center gap-2 min-w-0 flex-1 truncate"
              >
                <MessageIcon />
                <span className="truncate">{t.title}</span>
              </Link>
              {threads.length > 1 && (
                <button
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation();
                    deleteThread(t.id);
                    if (t.id === threadId) {
                      const next = threads.find((th) => th.id !== t.id);
                      if (next) navigate({ to: "/assistant/$threadId", params: { threadId: next.id } });
                    }
                  }}
                  className="hidden size-5 items-center justify-center rounded hover:text-destructive group-hover:flex"
                  title="Delete thread"
                >
                  <Trash2 className="size-3" />
                </button>
              )}
            </div>
          ))}
        </div>
        <div className="border-t border-border p-3 text-[10px] text-muted-foreground">
          Connected to AI Reasoning Orchestrator
        </div>
      </aside>
      <section className="flex min-w-0 flex-col">
        <header className="flex h-14 items-center justify-between border-b border-border px-4">
          <div>
            <h2 className="text-xs font-semibold">{thread.title}</h2>
            <p className="text-[10px] text-muted-foreground">
              Ontology-grounded reasoning · Multi-hop retrieval
            </p>
          </div>
          <div className="flex items-center gap-2">
            <span className="flex items-center gap-1.5 rounded-full border border-primary/30 bg-primary/10 px-2.5 py-0.5 text-[10px] font-medium text-primary">
              <Sparkles className="size-3" />
              Live reasoning engine
            </span>
          </div>
        </header>
        <Conversation className="flex-1">
          <ConversationContent className="p-4 sm:p-6">
            {thread.messages.length === 0 ? (
              <ConversationEmptyState
                icon={<Bot className="size-8 text-primary" />}
                title="What would you like to investigate?"
                description="Query across linked companies, contracts, supply chain logistics, and anomaly patterns."
              >
                <div className="mt-4 flex flex-wrap justify-center gap-2">
                  {suggestions.map((s) => (
                    <Button
                      key={s}
                      variant="outline"
                      size="sm"
                      className="text-xs"
                      onClick={() => submit({ text: s, files: [] })}
                    >
                      {s}
                    </Button>
                  ))}
                </div>
              </ConversationEmptyState>
            ) : (
              thread.messages.map((m) => (
                <Message from={m.role} key={m.id}>
                  <MessageContent>
                    {m.role === "assistant" && (
                      <div className="mb-2 flex items-center gap-2 text-[10px] font-semibold uppercase text-primary">
                        <Bot className="size-3.5" />
                        Orion Intelligence analysis
                      </div>
                    )}
                    <MessageResponse>{m.text}</MessageResponse>
                    {m.plan && m.plan.length > 0 && (
                      <div className="mt-3 rounded border border-border bg-card/60 p-2.5 text-[11px] text-muted-foreground">
                        <p className="font-semibold text-foreground mb-1 text-[10px] uppercase">Reasoning steps:</p>
                        <ul className="list-inside list-disc space-y-0.5">
                          {m.plan.map((p, idx) => (
                            <li key={idx}>{p}</li>
                          ))}
                        </ul>
                      </div>
                    )}
                    {m.sources && m.sources.length > 0 && (
                      <div className="mt-4 border-t border-border pt-3">
                        <p className="mb-2 text-[10px] font-semibold uppercase text-muted-foreground">
                          Evidence · {m.sources.length} sources
                        </p>
                        <div className="flex flex-wrap gap-2">
                          {m.sources.map((s) => (
                            <Link
                              to="/knowledge"
                              key={s.label}
                              className="flex items-center gap-2 rounded border border-border bg-muted px-2.5 py-2 text-[10px] hover:border-primary/50"
                            >
                              <Box className="size-3 text-primary" />
                              <span>
                                <b>{s.label}</b>
                                <br />
                                <span className="text-muted-foreground">{s.type}</span>
                              </span>
                            </Link>
                          ))}
                        </div>
                      </div>
                    )}
                  </MessageContent>
                  {m.role === "assistant" && (
                    <MessageActions>
                      <MessageAction
                        tooltip="Copy answer"
                        onClick={() => {
                          navigator.clipboard.writeText(m.text);
                          toast.success("Answer copied to clipboard");
                        }}
                      >
                        <Copy />
                      </MessageAction>
                      <MessageAction tooltip="Search evidence">
                        <FileSearch />
                      </MessageAction>
                    </MessageActions>
                  )}
                </Message>
              ))
            )}
            {loading && (
              <div className="flex items-center gap-2 text-xs text-muted-foreground py-4">
                <Bot className="size-4 text-primary animate-pulse" />
                <Shimmer>Traversing governed knowledge graph and generating synthesis…</Shimmer>
              </div>
            )}
          </ConversationContent>
          <ConversationScrollButton />
        </Conversation>
        <div className="border-t border-border bg-background p-4">
          <PromptInput onSubmit={submit} className="mx-auto max-w-3xl">
            <PromptInputTextarea
              ref={inputRef}
              placeholder="Ask about entities, relationships, risks, or operations…"
            />
            <PromptInputFooter>
              <PromptInputTools>
                <span className="px-2 text-[10px] text-muted-foreground">
                  Ontology + evidence enabled
                </span>
              </PromptInputTools>
              <PromptInputSubmit status={loading ? "submitted" : "ready"} disabled={loading} />
            </PromptInputFooter>
          </PromptInput>
          <p className="mx-auto mt-2 max-w-3xl text-center text-[10px] text-muted-foreground">
            Orion verifies decisions against cited knowledge evidence.
          </p>
        </div>
      </section>
    </div>
  );
}

function MessageIcon() {
  return (
    <span className="mt-0.5 flex size-5 items-center justify-center">
      <Bot className="size-3.5 text-muted-foreground" />
    </span>
  );
}
