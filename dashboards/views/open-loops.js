// Every open loop run with its phase, parent, Reservoir fill and calls.
const arr = v => v == null ? [] : Array.isArray(v) ? v : v.array ? v.array() : [v];
const nm = l => !l ? "" : l.path ? l.path.split("/").pop().replace(/\.md$/, "") : String(l).replace(/^\[\[|\]\]$/g, "");
const isA = (p, t) => arr(p.relationships__is_a).some(l => nm(l) === t);
const refs = (p, key, t) => arr(p[key]).some(l => nm(l) === t);
const all = dv.pages('"blob"');
const loops = all.where(p => p.identity__type === "particular" && p.properties__status === "open" &&
  isA(p, "Process Instance v1.0.0") && arr(p.relationships__is_a).some(l => /Loop|Weekly/.test(nm(l))));
dv.table(["Loop", "Phase", "In", "Started", "Reservoir", "Calls"],
  loops.sort(p => p.properties__started_at, "desc").map(p => {
    const ctx = all.where(x => refs(x, "relationships__in_context_of", p.file.name));
    return [p.file.link, nm(p.properties__current_phase).replace(/ v\d.*$/, "") || "–",
      p.relationships__in_process || p.relationships__in_loop || "–", p.properties__started_at,
      ctx.where(x => isA(x, "Letter v1.0.0") && !x.properties__resolved).length,
      ctx.where(x => isA(x, "Call v1.0.0")).length];
  }));
