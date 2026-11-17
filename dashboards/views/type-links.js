// Live: how instances of a type actually link to other entities, counted from frontmatter.
// input: {type: "<universal name>"} → lines like "participants → Person (21)".
const { type } = input;
const arr = v => v == null ? [] : Array.isArray(v) ? v : v.array ? v.array() : [v];
const nm = l => !l ? "" : l.path ? l.path.split("/").pop().replace(/\.md$/, "") : String(l).replace(/^\[\[|\]\]$/g, "").split("|")[0];
const all = dv.pages('"blob"');
const kindOf = name => {
  const p = dv.page("blob/" + name + ".md");
  if (!p || p.identity__type !== "particular") return null;
  const ps = arr(p.relationships__is_a).map(nm);
  if (ps.includes("Process Instance v1.0.0")) return ps.some(x => /Loop/.test(x)) ? "loop run" : "process run";
  return (ps[0] || "").replace(/ v\d.*$/, "");
};
const counts = {};
for (const p of all.where(p => p.identity__type === "particular" && arr(p.relationships__is_a).some(l => nm(l) === type))) {
  for (const [k, v] of Object.entries(p)) {
    if (!k.startsWith("relationships__") || k === "relationships__is_a") continue;
    for (const l of arr(v)) {
      const t = kindOf(nm(l)) || "file";
      const key = k.slice(15) + " → " + t;
      counts[key] = (counts[key] || 0) + 1;
    }
  }
}
const rows = Object.entries(counts).sort((a, b) => b[1] - a[1]);
const el = dv.el("div", "");
el.innerHTML = rows.length
  ? `<div style="font-size:var(--font-ui-smaller);color:var(--text-muted)">` + rows.map(([k, c]) => `${k} <b>(${c})</b>`).join("<br>") + `</div>`
  : `<div style="font-size:var(--font-ui-smaller);color:var(--text-faint)">no links used yet</div>`;
