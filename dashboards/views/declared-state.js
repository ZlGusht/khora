// The venture's declared state: newest active Decision per kind (thesis, direction, stage, attention).
const arr = v => v == null ? [] : Array.isArray(v) ? v : v.array ? v.array() : [v];
const nm = l => !l ? "" : l.path ? l.path.split("/").pop().replace(/\.md$/, "") : String(l);
const KINDS = ["thesis", "direction", "stage", "attention"];
const ds = dv.pages('"blob"').where(p => p.identity__type === "particular" && p.properties__status === "active" &&
  arr(p.relationships__is_a).some(l => nm(l) === "Decision v1.0.0") && KINDS.includes(p.properties__kind));
const rows = KINDS.flatMap(k => {
  const of = ds.where(p => p.properties__kind === k).sort(p => p.properties__decided_on, "desc").array();
  return of.length ? of.map((p, i) => [i ? "" : `**${k}**`, p.file.link, p.properties__decided_on])
                   : [[`**${k}**`, "*not declared yet — set it in the next weekly Laydown*", ""]];
});
dv.table(["Kind", "Decision", "Since"], rows);
