// Loop runs closed in the last 90 days.
const arr = v => v == null ? [] : Array.isArray(v) ? v : v.array ? v.array() : [v];
const nm = l => !l ? "" : l.path ? l.path.split("/").pop().replace(/\.md$/, "") : String(l);
const since = dv.date("today").minus(dv.duration("90 days"));
const loops = dv.pages('"blob"').where(p => p.identity__type === "particular" && p.properties__status === "closed" &&
  arr(p.relationships__is_a).some(l => nm(l) === "Process Instance v1.0.0") &&
  arr(p.relationships__is_a).some(l => /Loop/.test(nm(l))) && p.properties__ended_at && p.properties__ended_at >= since);
if (!loops.length) dv.paragraph("*No loops closed in the last 90 days.*");
else dv.table(["Loop", "Started", "Ended"], loops.sort(p => p.properties__ended_at, "desc")
  .map(p => [p.file.link, p.properties__started_at, p.properties__ended_at]));
