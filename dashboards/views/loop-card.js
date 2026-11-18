// Live card for one loop run. Used by dashboards/Loops.canvas: dv.view("dashboards/views/loop-card", {loop})
const { loop } = input;
const arr = v => v == null ? [] : Array.isArray(v) ? v : v.array ? v.array() : [v];
const nm = l => !l ? "" : l.path ? l.path.split("/").pop().replace(/\.md$/, "") : String(l).replace(/^\[\[|\]\]$/g, "").split("|")[0];
const isA = (p, t) => arr(p.relationships__is_a).some(l => nm(l) === t);
const refs = (p, key, t) => arr(p[key]).some(l => nm(l) === t);
const fmt = d => d ? (d.toFormat ? d.toFormat("yyyy-MM-dd") : String(d)) : "–";
const link = (name, label) => `<a class="internal-link" data-href="blob/${name}.md" href="blob/${name}.md">${label || name}</a>`;

const p = dv.page("blob/" + loop + ".md");
if (!p) { dv.paragraph("⚠ Loop not found: " + loop); return; }

const all = dv.pages('"blob"');
const PHASES = ["Inception", "Laydown", "Operate", "Observe", "Process"];
const cur = nm(p.properties__current_phase);
const ci = PHASES.findIndex(x => cur.startsWith(x));
const status = p.properties__status || "?";
const parent = nm(p.relationships__in_process) || nm(p.relationships__in_loop);

const ctx = all.where(x => x.file.path !== p.file.path && refs(x, "relationships__in_context_of", loop));
const deposits = ctx.where(x => isA(x, "Letter v1.0.0"));
const unresolved = deposits.where(x => !x.properties__resolved);
const answered = new Set(all.where(x => isA(x, "Decision v1.0.0") && x.properties__status === "active")
  .flatMap(x => arr(x.relationships__answers).map(nm)).array());
const open = all.where(x => isA(x, "Question v1.0.0") && x.identity__type === "particular" &&
  refs(x, "relationships__originates_in", loop) && !answered.has(x.file.name));
const conversations = ctx.where(x => isA(x, "Conversation v1.0.0"));
const decisions = all.where(x => isA(x, "Decision v1.0.0") && x.properties__status === "active" &&
  (refs(x, "relationships__in_context_of", loop) || refs(x, "relationships__originates_in", loop)));
const subs = all.where(x => x.identity__type === "particular" && refs(x, "relationships__in_process", loop));
const subsOpen = subs.where(x => x.properties__status === "open");
const touched = ctx.length ? ctx.sort(x => x.file.mtime, "desc")[0].file.mtime : p.file.mtime;

const chip = (label, on) => `<span style="padding:2px 8px;border-radius:10px;font-size:var(--font-ui-smaller);` +
  (on ? `background:var(--interactive-accent);color:var(--text-on-accent);font-weight:600;`
      : `border:1px solid var(--background-modifier-border);color:var(--text-muted);`) + `">${label}</span>`;
const stat = (n, label, warn) => `<div style="text-align:center"><div style="font-size:1.4em;font-weight:700;` +
  (warn ? `color:var(--text-error)` : ``) + `">${n}</div><div style="font-size:var(--font-ui-smaller);color:var(--text-muted)">${label}</div></div>`;
const badge = status === "open"
  ? `<span style="color:var(--color-green);font-weight:600">● open</span>`
  : `<span style="color:var(--text-muted)">○ ${status}</span>`;

const el = dv.el("div", "");
el.innerHTML = `
<div style="display:flex;justify-content:space-between;align-items:baseline;gap:8px">
  <div style="font-size:1.25em;font-weight:700">${link(loop)}</div>${badge}
</div>
<div style="display:flex;flex-wrap:wrap;gap:4px;margin:8px 0">${PHASES.map((x, i) => chip(x, i === ci)).join("")}</div>
<div style="font-size:var(--font-ui-small);color:var(--text-muted)">
  started ${fmt(p.properties__started_at)}${p.properties__ended_at ? " · ended " + fmt(p.properties__ended_at) : ""}${parent ? " · in " + link(parent) : ""}
</div>
<div style="display:grid;grid-template-columns:repeat(5,1fr);gap:4px;margin-top:10px;padding-top:8px;border-top:1px solid var(--background-modifier-border)">
  ${stat(unresolved.length + (deposits.length > unresolved.length ? `<span style="font-size:.6em;color:var(--text-muted)">/${deposits.length}</span>` : ""), "reservoir")}
  ${stat(open.length, "open questions")}
  ${stat(decisions.length, "decisions")}
  ${stat(conversations.length, "conversations")}
  ${stat(subsOpen.length + (subs.length > subsOpen.length ? `<span style="font-size:.6em;color:var(--text-muted)">/${subs.length}</span>` : ""), "sub-loops")}
</div>
<div style="font-size:var(--font-ui-smaller);color:var(--text-faint);margin-top:6px">last activity ${fmt(touched)}</div>`;
