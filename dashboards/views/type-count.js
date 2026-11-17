// Live instance count for a type, plus its newest instances. Used by dashboards/Ontology.canvas.
// dv.view("dashboards/views/type-count", {type}) — type is a universal name, or "Loop runs" / "Process runs".
const { type } = input;
const arr = v => v == null ? [] : Array.isArray(v) ? v : v.array ? v.array() : [v];
const nm = l => !l ? "" : l.path ? l.path.split("/").pop().replace(/\.md$/, "") : String(l).replace(/^\[\[|\]\]$/g, "").split("|")[0];
const parents = p => arr(p.relationships__is_a).map(nm);

const memo = {};
const hasAncestor = (name, target, seen = new Set()) => {
  if (name === target) return true;
  if (seen.has(name)) return false;
  seen.add(name);
  const u = dv.page("blob/" + name + ".md");
  return !!u && parents(u).some(q => q !== name && hasAncestor(q, target, seen));
};
const isLoopRun = p => parents(p).includes("Process Instance v1.0.0") &&
  parents(p).some(q => q !== "Process Instance v1.0.0" && (memo[q] ??= hasAncestor(q, "Loop v2.0.0")));

const all = dv.pages('"blob"').where(p => p.identity__type === "particular");
let rows;
if (type === "Loop runs") rows = all.where(isLoopRun);
else if (type === "Process runs") rows = all.where(p => parents(p).includes("Process Instance v1.0.0") && !isLoopRun(p));
else rows = all.where(p => parents(p).includes(type));

const newest = rows.sort(p => p.file.ctime, "desc").limit(4);
const el = dv.el("div", "");
el.innerHTML = `<div style="margin-top:6px;padding-top:6px;border-top:1px solid var(--background-modifier-border)">
<b style="font-size:1.2em">${rows.length}</b> <span style="color:var(--text-muted)">instance${rows.length === 1 ? "" : "s"}</span>
${newest.length ? `<div style="font-size:var(--font-ui-smaller);color:var(--text-muted);margin-top:2px">newest: ` +
  newest.map(p => `<a class="internal-link" data-href="${p.file.path}" href="${p.file.path}">${p.file.name}</a>`).join(" · ") + `</div>` : ""}
</div>`;
