#!/usr/bin/env python3
"""
Layout engine for agent-authored canvases. Agents decide WHAT a canvas shows (see the
[[Render Canvas v1.0.0]] process and its per-canvas subtypes in blob/); this script only
does geometry: it turns a nested block spec into a non-overlapping Obsidian .canvas.

    khora-canvas spec <file.canvas>                print the canvas's Spec card (what the canvas must show)
    khora-canvas render <layout.json> <out.canvas> lay out agent-authored blocks into the canvas
    khora-canvas graph                             dump every entity's frontmatter as JSON (context helper)

Every canvas carries its own Spec card (node id "spec"): purpose, what to show, how to gather
context, judgement, layout, triggers. Humans edit it on the canvas; the renderer never changes
it and places generated content to its right. A layout's optional "spec" field seeds the card
only when the canvas has none yet.

Layout (JSON):
{
  "spec": "markdown",                                optional; seeds the Spec card on first render
  "header": "markdown for the title card",          optional
  "rows": [ [block, block, ...], ... ],             rows stacked top->bottom, blocks left->right
  "edges": [ {"from": id, "to": id, "label": str, "color": str} ]
}
block = {
  "id": str, "label": str (draws a group box), "color": "1".."6",
  "layout": "stack" | "grid" | "row" | "hub" | "tree",
  "cards": [card | block, ...]      stack (vertical), row (horizontal), grid (with "columns"), tree (cards carry "parent")
  "center": card | block,           hub: the focal item
  "top"/"left"/"right"/"bottom": [card | block, ...]   hub: what surrounds it
}
card = {"id": str, "text": md} | {"id", "file": "blob/X.md"} | {"id", "live": "<view>", "input": {...}}
       optional "width", "height", "color"
A "live" card renders dashboards/views/<view>.js through dataviewjs (content updates without re-rendering).

Generated ids start with "g-"; nodes and edges a human added by hand are preserved on re-render.
"""
import datetime
import hashlib
import json
import math
import os
import re
import subprocess
import sys
import time
import uuid

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import khora_common  # noqa: E402

ENTITIES = "blob"
VIEWS = "dashboards/views"
CARD_W = 360
GAP = 40
PAD = 40          # inside a labelled group
LABEL_H = 30      # room for the group label
LIVE_H = {"loop-card": 330, "type-count": 150}


# ------------------------------------------------------------------ helpers
def gid(*parts):
    return "g-" + hashlib.sha1("|".join(str(p) for p in parts).encode("utf-8")).hexdigest()[:14]


def is_card(x):
    return any(k in x for k in ("text", "file", "live"))


def text_height(text, width):
    chars = max(20, int((width - 40) / 7.2))
    lines = 0
    in_table = False
    for raw in text.split("\n"):
        s = raw.strip()
        if s.startswith("```"):
            continue
        if s.startswith("#"):
            lines += 1.6
        elif s.startswith("|"):
            lines += 1.3
            in_table = True
        else:
            lines += max(1, math.ceil(len(s) / chars)) if s else 0.6
    return int(min(1400, max(80, 50 + lines * 24)))


def card_size(c):
    w = c.get("width", CARD_W)
    if "height" in c:
        return w, c["height"]
    if "live" in c:
        return w, LIVE_H.get(c["live"], 300)
    if "file" in c:
        return w, 400
    return w, text_height(c.get("text", ""), w)


# ------------------------------------------------------------------ measure
def measure(x):
    """Returns (w, h) of a card or block, caching layout data on the dict."""
    if is_card(x):
        x["_w"], x["_h"] = card_size(x)
        return x["_w"], x["_h"]
    lay = x.get("layout", "stack")
    inner_w = inner_h = 0
    if lay in ("stack", "row"):
        sizes = [measure(c) for c in x.get("cards", [])]
        if lay == "stack":
            inner_w = max([w for w, _ in sizes] or [0])
            inner_h = sum(h for _, h in sizes) + GAP * max(0, len(sizes) - 1)
        else:
            inner_w = sum(w for w, _ in sizes) + GAP * max(0, len(sizes) - 1)
            inner_h = max([h for _, h in sizes] or [0])
    elif lay == "grid":
        items = x.get("cards", [])
        n = max(1, x.get("columns", 2))
        sizes = [measure(c) for c in items]
        colw = [max([sizes[i][0] for i in range(j, len(sizes), n)] or [0]) for j in range(n)]
        rowh = [max(h for _, h in sizes[i:i + n]) for i in range(0, len(sizes), n)]
        x["_colw"], x["_rowh"] = colw, rowh
        inner_w = sum(colw) + GAP * (len([c for c in colw if c]) - 1) if items else 0
        inner_h = sum(rowh) + GAP * max(0, len(rowh) - 1)
    elif lay == "tree":
        items = x.get("cards", [])
        for c in items:
            measure(c)
        x["_tree"] = tree_positions(items)
        inner_w = max([px + c["_w"] for c, (px, py) in zip(items, x["_tree"])] or [0])
        inner_h = max([py + c["_h"] for c, (px, py) in zip(items, x["_tree"])] or [0])
    elif lay == "hub":
        cw, ch = measure(x["center"])
        side = {}
        for s in ("top", "left", "right", "bottom"):
            sizes = [measure(c) for c in x.get(s, [])]
            if s in ("left", "right"):
                side[s] = (max([w for w, _ in sizes] or [0]), sum(h for _, h in sizes) + GAP * max(0, len(sizes) - 1))
            else:
                side[s] = (sum(w for w, _ in sizes) + GAP * max(0, len(sizes) - 1), max([h for _, h in sizes] or [0]))
        x["_side"] = side
        lw, rw = side["left"][0], side["right"][0]
        mid_w = lw + (GAP * 2 if lw else 0) + cw + (GAP * 2 if rw else 0) + rw
        mid_h = max(side["left"][1], ch, side["right"][1])
        x["_mid"] = (mid_w, mid_h)
        inner_w = max(mid_w, side["top"][0], side["bottom"][0])
        th, bh = side["top"][1], side["bottom"][1]
        inner_h = th + (GAP * 2 if th else 0) + mid_h + (GAP * 2 if bh else 0) + bh
    else:
        raise ValueError("unknown layout: " + lay)
    x["_iw"], x["_ih"] = inner_w, inner_h
    pad = PAD if x.get("label") else 0
    x["_w"] = inner_w + 2 * pad
    x["_h"] = inner_h + 2 * pad + (LABEL_H if x.get("label") else 0)
    return x["_w"], x["_h"]


def tree_positions(items):
    """Top-down tidy tree over cards that name a "parent" (an id of another card in the same block)."""
    ids = {c["id"]: c for c in items}
    kids = {}
    roots = []
    for c in items:
        p = c.get("parent")
        if p in ids:
            kids.setdefault(p, []).append(c)
        else:
            roots.append(c)
    width, pos = {}, {}
    row_h = max([c["_h"] for c in items] or [0]) + GAP * 2

    def m(c):
        ks = kids.get(c["id"], [])
        width[c["id"]] = max(c["_w"], sum(m(k) for k in ks) + GAP * (len(ks) - 1)) if ks else c["_w"]
        return width[c["id"]]

    def p(c, x0, d):
        pos[c["id"]] = (x0 + (width[c["id"]] - c["_w"]) / 2, d * row_h)
        cx = x0
        for k in kids.get(c["id"], []):
            p(k, cx, d + 1)
            cx += width[k["id"]] + GAP

    x0 = 0
    for r in roots:
        m(r)
        p(r, x0, 0)
        x0 += width[r["id"]] + GAP
    return [pos[c["id"]] for c in items]


# ------------------------------------------------------------------ place
def place(x, ox, oy, out, centers):
    if is_card(x):
        n = {"id": gid("card", x["id"]), "x": int(ox), "y": int(oy), "width": int(x["_w"]), "height": int(x["_h"])}
        if "file" in x:
            n.update(type="file", file=x["file"])
        elif "live" in x:
            n.update(type="text", text="```dataviewjs\nawait dv.view(%s, %s)\n```" % (
                json.dumps(VIEWS + "/" + x["live"]), json.dumps(x.get("input", {}), ensure_ascii=False)))
        else:
            n.update(type="text", text=x["text"])
        if x.get("color"):
            n["color"] = x["color"]
        out.append(n)
        centers[x["id"]] = (ox + x["_w"] / 2, oy + x["_h"] / 2, n["id"])
        return
    if x.get("label"):
        g = {"id": gid("group", x.get("id", x["label"])), "type": "group", "label": x["label"],
             "x": int(ox), "y": int(oy), "width": int(x["_w"]), "height": int(x["_h"])}
        if x.get("color"):
            g["color"] = x["color"]
        out.append(g)
        if x.get("id"):
            centers[x["id"]] = (ox + x["_w"] / 2, oy + x["_h"] / 2, g["id"])
        ox, oy = ox + PAD, oy + PAD + LABEL_H
    lay = x.get("layout", "stack")
    if lay == "stack":
        for c in x.get("cards", []):
            place(c, ox, oy, out, centers)
            oy += c["_h"] + GAP
    elif lay == "row":
        for c in x.get("cards", []):
            place(c, ox, oy, out, centers)
            ox += c["_w"] + GAP
    elif lay == "grid":
        n = max(1, x.get("columns", 2))
        for i, c in enumerate(x.get("cards", [])):
            col, row = i % n, i // n
            place(c, ox + sum(x["_colw"][:col]) + GAP * col, oy + sum(x["_rowh"][:row]) + GAP * row, out, centers)
    elif lay == "tree":
        for c, (px, py) in zip(x.get("cards", []), x["_tree"]):
            place(c, ox + px, oy + py, out, centers)
    elif lay == "hub":
        side, (mid_w, mid_h) = x["_side"], x["_mid"]
        iw = x["_iw"]
        th = side["top"][1]
        my = oy + (th + GAP * 2 if th else 0)
        mx = ox + (iw - mid_w) / 2
        # top and bottom rows, centred
        for s, sy in (("top", oy), ("bottom", my + mid_h + GAP * 2)):
            cx = ox + (iw - side[s][0]) / 2
            for c in x.get(s, []):
                place(c, cx, sy, out, centers)
                cx += c["_w"] + GAP
        # left column, centre, right column
        lw = side["left"][0]
        cy = my + (mid_h - side["left"][1]) / 2
        for c in x.get("left", []):
            place(c, mx + (lw - c["_w"]), cy, out, centers)
            cy += c["_h"] + GAP
        cxx = mx + (lw + GAP * 2 if lw else 0)
        place(x["center"], cxx, my + (mid_h - x["center"]["_h"]) / 2, out, centers)
        rx = cxx + x["center"]["_w"] + GAP * 2
        cy = my + (mid_h - side["right"][1]) / 2
        for c in x.get("right", []):
            place(c, rx, cy, out, centers)
            cy += c["_h"] + GAP


def sides(a, b):
    dx, dy = b[0] - a[0], b[1] - a[1]
    if abs(dx) >= abs(dy):
        return ("right", "left") if dx > 0 else ("left", "right")
    return ("bottom", "top") if dy > 0 else ("top", "bottom")


def load_canvas(path):
    if os.path.exists(path):
        try:
            with open(path, encoding="utf-8") as f:
                return json.load(f)
        except Exception:
            pass
    return {"nodes": [], "edges": []}


def render(spec, out_path):
    old = load_canvas(out_path)
    seed = []
    spec_node = next((n for n in old.get("nodes", []) if n.get("id") == "spec"), None)
    if spec_node is None and spec.get("spec"):
        spec_node = {"id": "spec", "type": "text", "x": 0, "y": 0, "width": 560,
                     "height": text_height(spec["spec"], 560), "color": "6", "text": spec["spec"]}
        seed = [spec_node]
    x0 = spec_node["x"] + spec_node["width"] + GAP * 4 if spec_node else 0
    y0 = spec_node["y"] if spec_node else 0
    nodes, centers = [], {}
    y = y0
    if spec.get("header"):
        hdr = {"id": "_header", "text": spec["header"], "width": spec.get("header_width", 1200), "color": "2"}
        measure(hdr)
        place(hdr, x0, y, nodes, centers)
        y += hdr["_h"] + GAP * 3
    for row in spec.get("rows", []):
        x, rh = x0, 0
        for b in row:
            w, h = measure(b)
            place(b, x, y, nodes, centers)
            x += w + GAP * 3
            rh = max(rh, h)
        y += rh + GAP * 3
    edges = []
    for e in spec.get("edges", []):
        if e["from"] not in centers or e["to"] not in centers:
            print("  skipped edge %s -> %s (unknown id)" % (e["from"], e["to"]), file=sys.stderr)
            continue
        a, b = centers[e["from"]], centers[e["to"]]
        fs, ts = sides(a, b)
        ed = {"id": gid("edge", e["from"], e["to"], e.get("label", "")), "fromNode": a[2], "fromSide": fs,
              "toNode": b[2], "toSide": ts, "toEnd": "arrow"}
        for k in ("label", "color"):
            if e.get(k):
                ed[k] = e[k]
        edges.append(ed)
    # keep hand-made nodes/edges
    keep_n = seed + [n for n in old.get("nodes", []) if not str(n.get("id", "")).startswith("g-")]
    keep_e = [e for e in old.get("edges", []) if not str(e.get("id", "")).startswith("g-")]
    ids = {n["id"] for n in nodes + keep_n}
    all_edges = [e for e in edges + keep_e if e["fromNode"] in ids and e["toNode"] in ids]
    os.makedirs(os.path.dirname(os.path.abspath(out_path)), exist_ok=True)
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump({"nodes": nodes + keep_n, "edges": all_edges}, f, indent="\t", ensure_ascii=False)
    print("rendered %s: %d nodes, %d edges (%d hand-made nodes kept)" % (out_path, len(nodes), len(edges), len(keep_n)))


# ------------------------------------------------------------------ context helper
def graph_dump():
    def khora_tmp():
        if sys.platform == "darwin":
            return subprocess.run(["getconf", "DARWIN_USER_TEMP_DIR"], capture_output=True, text=True).stdout.strip()
        if os.name == "nt":
            return os.path.join(os.environ["LOCALAPPDATA"], "Temp")
        return os.environ.get("TMPDIR", "/tmp")
    base = os.path.join(khora_tmp(), "khora", "dataview")
    inp, out = os.path.join(base, "in"), os.path.join(base, "out")
    os.makedirs(inp, exist_ok=True)
    os.makedirs(out, exist_ok=True)
    rid = str(uuid.uuid4())
    fin, fout = os.path.join(inp, rid + ".dql"), os.path.join(out, rid + ".json")
    with open(fin, "w", encoding="utf-8") as f:
        f.write('TABLE WITHOUT ID file.name AS name, file.frontmatter AS fm FROM "' + ENTITIES + '"')
    try:
        khora_common.obsidian("command", "id=khora:dataview-query")
        for _ in range(60):
            if os.path.exists(fout):
                time.sleep(0.2)
                with open(fout, encoding="utf-8") as f:
                    env = json.load(f)
                rows = (env.get("value") or {}).get("values", [])
                sys.stdout.reconfigure(encoding="utf-8")
                print(json.dumps({r[0]: r[1] for r in rows}, ensure_ascii=False, indent=1))
                return
            time.sleep(0.25)
        sys.exit("graph: no answer from the khora plugin (is Obsidian running?)")
    finally:
        for f in (fin, fout):
            if os.path.exists(f):
                os.remove(f)


def main():
    if len(sys.argv) >= 4 and sys.argv[1] == "render":
        with open(sys.argv[2], encoding="utf-8") as f:
            render(json.load(f), sys.argv[3])
    elif len(sys.argv) >= 3 and sys.argv[1] == "spec":
        n = next((n for n in load_canvas(sys.argv[2]).get("nodes", []) if n.get("id") == "spec"), None)
        sys.stdout.reconfigure(encoding="utf-8")
        print(n["text"] if n else "No Spec card (node id 'spec') on this canvas.")
    elif len(sys.argv) >= 2 and sys.argv[1] == "graph":
        graph_dump()
    else:
        print(__doc__)
        sys.exit(2)


if __name__ == "__main__":
    main()
