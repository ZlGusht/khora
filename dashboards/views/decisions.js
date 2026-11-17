// Live list of active decisions of the given kinds, newest first, with a short link to where each was made.
// input: {kinds?: ["design", "rule"], area?: "Log", loop?: "<loop run>", limit: 8}. No kinds = every kind.
const { kinds, area, loop, limit = 8 } = input || {};
const arr = v => v == null ? [] : Array.isArray(v) ? v : v.array ? v.array() : [v];
const nm = l => !l ? "" : l.path ? l.path.split("/").pop().replace(/\.md$/, "") : String(l).replace(/^\[\[|\]\]$/g, "").split("|")[0];
const rows = dv.pages('"blob"').where(p => p.identity__type === "particular" && p.properties__status === "active" &&
  arr(p.relationships__is_a).some(l => nm(l) === "Decision v1.0.0") && (!kinds || kinds.includes(p.properties__kind)) &&
  (!area || arr(p.relationships__in_context_of).some(l => nm(l) === area)) &&
  (!loop || arr(p.relationships__in_context_of).some(l => nm(l) === loop) || nm(p.relationships__originates_in) === loop))
  .sort(p => p.properties__decided_on, "desc");
if (!rows.length) { dv.paragraph("*None.*"); return; }
const src = p => {
  const c = p.relationships__originates_in;
  if (!c || !c.path) return "";
  const d = p.properties__decided_on ? p.properties__decided_on.toFormat("d LLL") : "";
  return ` · <a class="internal-link" data-href="${c.path}" href="${c.path}">${d}</a>`;
};
const el = dv.el("div", "");
el.innerHTML = rows.limit(limit).map(p => `<div style="padding:4px 0;border-bottom:1px solid var(--background-modifier-border)">
  <a class="internal-link" data-href="${p.file.path}" href="${p.file.path}">${p.file.name}</a>
  <div style="font-size:var(--font-ui-smaller);color:var(--text-muted)">${p.properties__kind}${src(p)}</div></div>`).join("") +
  (rows.length > limit ? `<div style="font-size:var(--font-ui-smaller);color:var(--text-faint)">+${rows.length - limit} more</div>` : "");
