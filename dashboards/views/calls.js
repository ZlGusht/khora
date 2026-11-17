// Live list of recent calls, newest first. input: {in?: "<Area or loop run>", days: 30, limit: 6}. Without `in`, every call.
const { in: within, days = 30, limit = 6 } = input || {};
const arr = v => v == null ? [] : Array.isArray(v) ? v : v.array ? v.array() : [v];
const nm = l => !l ? "" : l.path ? l.path.split("/").pop().replace(/\.md$/, "") : String(l).replace(/^\[\[|\]\]$/g, "").split("|")[0];
const since = dv.date("today").minus(dv.duration(days + " days"));
const rows = dv.pages('"blob"').where(p => p.identity__type === "particular" &&
  arr(p.relationships__is_a).some(l => nm(l) === "Call v1.0.0") && p.properties__date >= since &&
  (!within || arr(p.relationships__in_context_of).some(l => nm(l) === within))).sort(p => p.properties__date, "desc");
if (!rows.length) { dv.paragraph("*No calls in the last " + days + " days.*"); return; }
const short = n => n.replace(/^Call \d{4}-\d{2}-\d{2} /, "");
const el = dv.el("div", "");
el.innerHTML = rows.limit(limit).map(p => `<div style="padding:4px 0;border-bottom:1px solid var(--background-modifier-border)">
  <a class="internal-link" data-href="${p.file.path}" href="${p.file.path}">${short(p.file.name)}</a>
  <div style="font-size:var(--font-ui-smaller);color:var(--text-muted)">${p.properties__date.toFormat("d LLL")} · ${p.properties__kind || ""} · ${arr(p.relationships__participants).map(nm).map(n => n.split(" ")[0]).join(", ")}</div></div>`).join("") +
  (rows.length > limit ? `<div style="font-size:var(--font-ui-smaller);color:var(--text-faint)">+${rows.length - limit} older in the window</div>` : "");
