"""
plot_rcm.py  —  Random Cluster Model sampler performance plots
Usage: python plot_rcm.py          (paste text, then Ctrl-D / Ctrl-Z+Enter)
       python plot_rcm.py data.txt (read from file)

Row format (with iterations):
  for p = <p> , time <t> avg size <s> iterations <i>

Row format (without iterations, older output):
  for p = <p> , time <t> avg size <s>

Both formats are accepted. The cross-n plot uses runtime/iteration when
iteration counts are available; it falls back to T / (n²·log n) otherwise.
"""

import re
import sys
import textwrap
import matplotlib.pyplot as plt
import matplotlib.cm as cm
import numpy as np


# ── 1. Parser ────────────────────────────────────────────────────────────────

def parse(text):
    """
    Returns  { n -> { q -> [(p, time_ns, avg_size, iterations_or_None), ...] } }
    NaN rows are silently dropped.
    Rounds q to 1 decimal place to absorb floating-point noise.
    """
    data = {}
    current_n = None
    current_q = None

    re_n   = re.compile(r'For n\s*=\s*(\d+)', re.IGNORECASE)
    re_q   = re.compile(r'For q\s*=\s*([0-9Ee.+\-]+?)[-\s]', re.IGNORECASE)
    # optional trailing  iterations <i>
    re_row = re.compile(
        r'for p\s*=\s*([0-9Ee.+\-]+)\s*,\s*time\s*([0-9Ee.+\-]+)'
        r'\s*avg size\s*([0-9Ee.+\-]+)'
        r'(?:\s*iterations\s*([0-9Ee.+\-]+))?',
        re.IGNORECASE
    )

    for line in text.splitlines():
        mn = re_n.search(line)
        if mn:
            current_n = int(mn.group(1))
            data.setdefault(current_n, {})
            continue
        mq = re_q.search(line)
        if mq:
            current_q = round(float(mq.group(1)), 1)
            if current_n is not None:
                data[current_n].setdefault(current_q, [])
            continue
        mr = re_row.search(line)
        if mr and current_n is not None and current_q is not None:
            p = float(mr.group(1))
            t = float(mr.group(2))
            s = float(mr.group(3))
            i = float(mr.group(4)) if mr.group(4) is not None else None
            if np.isnan(t) or np.isnan(s):
                continue
            if i is not None and np.isnan(i):
                i = None
            data[current_n][current_q].append((p, t, s, i))

    for n in list(data):
        data[n] = {q: pts for q, pts in data[n].items() if pts}

    return data


# ── 2. Colour helpers ─────────────────────────────────────────────────────────

def q_colours(q_vals):
    cmap = cm.get_cmap('plasma', len(q_vals))
    return {q: cmap(i) for i, q in enumerate(sorted(q_vals))}


# ── 3. Plot A — Runtime vs p (one line per q) ────────────────────────────────

def plot_runtime_vs_p(n, qdata, colours, ax=None):
    standalone = ax is None
    if standalone:
        fig, ax = plt.subplots(figsize=(8, 5))

    for q in sorted(qdata):
        pts = sorted(qdata[q])
        ps  = [x[0] for x in pts]
        ts  = [x[1] / 1e6 for x in pts]
        ax.plot(ps, ts, marker='o', markersize=4, linewidth=1.5,
                color=colours[q], label=f'q={q:.1f}')

    ax.set_xlabel('Edge probability  p', fontsize=11)
    ax.set_ylabel('Avg runtime  (M ns)', fontsize=11)
    ax.set_title(f'Runtime vs p  (n = {n})', fontsize=12)
    ax.legend(fontsize=8, ncol=2, loc='upper left')
    ax.grid(True, alpha=0.3)

    if standalone:
        fig.tight_layout()
        plt.show()


# ── 4. Plot B — Cluster size vs p (one line per q) ───────────────────────────

def plot_size_vs_p(n, qdata, colours, ax=None):
    standalone = ax is None
    if standalone:
        fig, ax = plt.subplots(figsize=(8, 5))

    for q in sorted(qdata):
        pts = sorted(qdata[q])
        ps  = [x[0] for x in pts]
        ss  = [x[2] for x in pts]
        ax.plot(ps, ss, marker='s', markersize=4, linewidth=1.5,
                color=colours[q], label=f'q={q:.1f}')

    ax.set_xlabel('Edge probability  p', fontsize=11)
    ax.set_ylabel('Avg cluster size', fontsize=11)
    ax.set_title(f'Cluster size vs p  (n = {n})', fontsize=12)
    ax.legend(fontsize=8, ncol=2, loc='upper left')
    ax.grid(True, alpha=0.3)

    if standalone:
        fig.tight_layout()
        plt.show()


# ── 5. Plot C — Runtime vs q (one line per p) ────────────────────────────────

def plot_runtime_vs_q(n, qdata, ax=None):
    standalone = ax is None
    if standalone:
        fig, ax = plt.subplots(figsize=(8, 5))

    all_p  = sorted({round(x[0], 8) for pts in qdata.values() for x in pts})
    p_cmap = cm.get_cmap('viridis', len(all_p))
    p_col  = {p: p_cmap(i) for i, p in enumerate(all_p)}

    for p in all_p:
        qs, ts = [], []
        for q in sorted(qdata):
            match = next((x for x in qdata[q] if abs(x[0] - p) < 1e-9), None)
            if match:
                qs.append(q)
                ts.append(match[1] / 1e6)
        if qs:
            ax.plot(qs, ts, marker='D', markersize=4, linewidth=1.5,
                    color=p_col[p], label=f'p={p:.4g}')

    ax.set_xlabel('Cluster parameter  q', fontsize=11)
    ax.set_ylabel('Avg runtime  (M ns)', fontsize=11)
    ax.set_title(f'Runtime vs q  (n = {n})', fontsize=12)
    ax.legend(fontsize=7, ncol=2, loc='upper left')
    ax.grid(True, alpha=0.3)

    if standalone:
        fig.tight_layout()
        plt.show()


# ── 6. Plot D — Runtime / iteration vs n ─────────────────────────────────────

def plot_runtime_vs_n(data, ax=None):
    """
    Plots (avg runtime) / (avg iterations) in ns-per-iteration vs n.

    If iteration counts are present in the data, each point is the mean of
    (time_ns / iterations) across all (p, q) pairs for that n.

    Falls back to T / (n²·log n) for data without iteration counts, with a
    note on the axis label.

    A flat line across n means the per-iteration cost is constant — i.e. the
    algorithm is O(iterations) with an O(1) per-step cost.  Slow growth
    (log(n), log(n)²) is still polylogarithmic per step.

    Reference lines (flat and log(n)) are anchored to the median-n point.
    """
    standalone = ax is None
    if standalone:
        fig, ax = plt.subplots(figsize=(9, 6))

    ns_sorted = sorted(data.keys())

    # ── decide normalisation mode ─────────────────────────────────────────────
    has_iters = {
        n: any(x[3] is not None for pts in data[n].values() for x in pts)
        for n in ns_sorted
    }
    use_iters = any(has_iters.values())

    def per_iter(pts_list, n):
        """Return per-iteration cost (ns) for a list of (p,t,s,i) tuples."""
        if use_iters and has_iters[n]:
            vals = [x[1] / x[3] for x in pts_list if x[3] is not None and x[3] > 0]
        else:
            # fallback: normalise by n²·log(n)
            vals = [x[1] / (n**2 * np.log(n)) for x in pts_list]
        return vals

    # ── grand mean per-iteration cost ─────────────────────────────────────────
    grand = {}
    for n in ns_sorted:
        all_pts = [x for pts in data[n].values() for x in pts]
        vals = per_iter(all_pts, n)
        if vals:
            grand[n] = np.mean(vals)

    gm_ns   = sorted(grand)
    gm_vals = [grand[n] for n in gm_ns]

    # ── per-q lines ───────────────────────────────────────────────────────────
    all_qs = sorted({q for nd in data.values() for q in nd})
    q_cmap = cm.get_cmap('plasma', len(all_qs))
    q_col  = {q: q_cmap(i) for i, q in enumerate(all_qs)}

    for q in all_qs:
        ns, vals = [], []
        for n in ns_sorted:
            pts = data[n].get(q, [])
            if pts:
                v = per_iter(pts, n)
                if v:
                    ns.append(n)
                    vals.append(np.mean(v))
        if len(ns) >= 2:
            ax.plot(ns, vals, marker='o', markersize=5, linewidth=1.2,
                    color=q_col[q], alpha=0.65, label=f'q={q:.1f}')

    # grand mean on top
    ax.plot(gm_ns, gm_vals, marker='*', markersize=14, linewidth=2.5,
            color='black', zorder=5, label='grand mean')

    # ── reference lines anchored to the middle reliable n ────────────────────
    mid = len(gm_ns) // 2
    anchor_n = gm_ns[mid]
    anchor_v = gm_vals[mid]
    ref_ns   = np.logspace(np.log10(min(gm_ns)), np.log10(max(gm_ns)), 300)

    # flat: per-step cost is O(1) in n
    ax.plot(ref_ns, np.full_like(ref_ns, anchor_v),
            linestyle='--', linewidth=1.6, color='steelblue', zorder=1,
            label='flat  (O(1) per iteration)')

    # log(n): per-step cost is O(log n)
    logn_ref = anchor_v * np.log(ref_ns) / np.log(anchor_n)
    ax.plot(ref_ns, logn_ref,
            linestyle=':', linewidth=1.6, color='darkorange', zorder=1,
            label='∝ log(n)  (O(log n) per iteration)')

    ax.set_xscale('log')
    ax.set_yscale('log')
    ax.set_xlabel('n  (log scale)', fontsize=11)

    if use_iters:
        ax.set_ylabel('Runtime / iteration  (ns per iteration)', fontsize=11)
        ax.set_title(
            'Per-iteration cost  vs  n\n'
            'Flat → O(1) per iteration;  polylog growth → polylog per step',
            fontsize=11)
    else:
        ax.set_ylabel('T / (n²·log n)  [ns / (n²·log n)]', fontsize=11)
        ax.set_title(
            'Normalised runtime  T / (n²·log n)  vs n\n'
            '(no iteration counts in data — using theoretical proxy)',
            fontsize=11)

    ax.set_xticks(ns_sorted)
    ax.set_xticklabels([str(n) for n in ns_sorted])
    ax.legend(fontsize=8, ncol=2, loc='upper left')
    ax.grid(True, alpha=0.3, which='both')

    if standalone:
        fig.tight_layout()
        plt.show()


# ── 7. Per-n dashboard ────────────────────────────────────────────────────────

def plot_per_n(n, qdata):
    colours = q_colours(qdata.keys())
    fig, axes = plt.subplots(1, 3, figsize=(18, 5))
    fig.suptitle(
        f'Random Cluster Model — exact sampler  (n = {n}, q ∈ (1, 2])',
        fontsize=13, y=1.01
    )
    plot_runtime_vs_p(n, qdata, colours, ax=axes[0])
    plot_size_vs_p   (n, qdata, colours, ax=axes[1])
    plot_runtime_vs_q(n, qdata,          ax=axes[2])
    fig.tight_layout()
    plt.show()


# ── 8. Entry point ────────────────────────────────────────────────────────────

def main():
    if len(sys.argv) > 1:
        with open(sys.argv[1]) as f:
            text = f.read()
    else:
        print(textwrap.dedent("""\
            Paste your output text below (all n values at once is fine).
            When done, press  Ctrl-D  (Linux/Mac)  or  Ctrl-Z + Enter  (Windows):
        """))
        text = sys.stdin.read()

    data = parse(text)
    if not data:
        print("No data found. Check that the text matches the expected format.")
        sys.exit(1)

    for n in sorted(data):
        qdata = data[n]
        n_pts = sum(len(v) for v in qdata.values())
        has_i = any(x[3] is not None for pts in qdata.values() for x in pts)
        print(f"n={n:>6}: {len(qdata)} q-values, {n_pts} valid points"
              f"{'  [iterations recorded]' if has_i else ''}")

    # Per-n dashboards
    for n in sorted(data):
        plot_per_n(n, data[n])

    # Cross-n comparison
    if len(data) > 1:
        fig, ax = plt.subplots(figsize=(9, 6))
        plot_runtime_vs_n(data, ax=ax)
        fig.tight_layout()
        plt.show()


main()