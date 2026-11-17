// Live list of open Questions in an Area, newest first, each with its lean.
// input: {area: "Log", limit: 8}. A Question is open while no active Decision `answers` it.
const { area, limit = 8 } = input || {};
const arr = v => v == null ? [] : Array.isArray(v) ? v : v.array ? v.array() : [v];
const nm = l => !l ? "" : l.path ? l.path.split("/").pop().replace(/\.md$/, "") : String(l).replace(/^\[\[|\]\]$/g, "").split("|")[0];
const isA = (p, t) => arr(p.relationships__is_a).some(l => nm(l) === t);
const all = dv.pages('"blob"');
const answered = new Set(all.where(p => isA(p, "Decision v1.0.0") && p.properties__status === "active")
  .flatMap(p => arr(p.relationships__answers).map(nm)).array());
const rows = all.where(p => p.identity__type === "particular" && isA(p, "Question v1.0.0") && !answered.has(p.file.name) &&
  (!area || arr(p.relationships__in_context_of).some(l => nm(l) === area)))
  .sort(p => p.identity__created_at, "desc");
if (!rows.length) { dv.paragraph("*None.*"); return; }
const el = dv.el("div", "");
el.innerHTML = rows.limit(limit).map(p => `<div style="padding:4px 0;border-bottom:1px solid var(--background-modifier-border)">
  <a class="internal-link" data-href="${p.file.path}" href="${p.file.path}">${p.file.name}</a>
  <div style="font-size:var(--font-ui-smaller);color:var(--text-muted)">${p.properties__lean ? "lean: " + p.properties__lean : "no lean yet"}</div></div>`).join("") +
  (rows.length > limit ? `<div style="font-size:var(--font-ui-smaller);color:var(--text-faint)">+${rows.length - limit} more</div>` : "");
