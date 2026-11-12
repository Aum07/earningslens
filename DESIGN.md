# EarningsLens — Design System (v1)

> **For Claude:** Use this file as the single source of truth when building or restyling the EarningsLens UI. Use only the tokens below — no new colors, fonts, radii or shadows. If something isn't covered, pick the closest existing token and say which one you used.
>
> Figma (screens, assets, styles): https://www.figma.com/design/LP0M3SKisFc09mPsGXJALK

---

## 1. Design principles

1. **Dark and quiet.** Near-black surfaces with low-contrast borders (Grok-inspired). Content carries the page, not decoration.
2. **One accent: gold.** Gold (`--accent`) is used for exactly three things:
   - the thing the user asked about (highlighted quarter, highlighted table row, highlighted chart bar, highlighted passage),
   - citation markers `[1]` `[2]`,
   - the primary action on a screen (only one gold button per view).
3. **Numbers are data.** Every number, ticker, date, quarter label and citation is set in **Geist Mono**.
4. **Trust is visible.** Every answer shows its sources, a "Matches SEC filing" badge, and the official figures next to it.
5. **Status colors are semantic only.** Green = verified/match, red = weaker/mismatch, blue = info/explanatory note. Never decorative.

---

## 2. Color tokens

| Token | Hex | Use |
|---|---|---|
| `--bg-canvas` | `#0B0B0C` | Page background, input wells, passage background |
| `--bg-surface` | `#131315` | Sidebar, cards, panels, drawer, modal |
| `--bg-raised` | `#1B1B1F` | Inputs, list rows, active nav item, chips, progress-bar track |
| `--bg-hover` | `#232328` | Hover state, chips inside inputs |
| `--border` | `#26262B` | Default 1px border (cards, dividers, top bar) |
| `--border-strong` | `#3A3A41` | Inputs, outline buttons, inactive chart bars |
| `--text-primary` | `#F2F2F3` | Headings, body, values |
| `--text-secondary` | `#A1A1AA` | Supporting text, inactive nav |
| `--text-tertiary` | `#6B6B74` | Labels, metadata, placeholders |
| `--accent` | `#F8C630` | Gold accent (see principle 2) |
| `--on-accent` | `#0B0B0C` | Text/icons on gold |
| `--up` | `#2DBE8C` | Match / verified / positive |
| `--down` | `#F0566A` | Weaker / mismatch / negative |
| `--info` | `#6EA8FE` | Info notes (e.g. fiscal calendar) |

**Tints (accent or status color at reduced opacity, placed on a dark surface):**

| Use | Value |
|---|---|
| Selected chip / option background | `rgba(248,198,48,0.08)` + 1px `--accent` border |
| Highlighted table row | `rgba(248,198,48,0.10)` + 1px `--accent` border |
| Citation number box | `rgba(248,198,48,0.14)` |
| "Where it's weaker" callout | `rgba(248,198,48,0.06)` + 1px `--accent` border |
| "Matches SEC filing" badge | `rgba(45,190,140,0.12)`, text `--up` |
| "weaker" pill | `rgba(240,86,106,0.14)`, text `--down` |
| Info note | `rgba(110,168,254,0.08)` |
| Scrim behind drawer | `rgba(11,11,12,0.60)` |
| Scrim behind modal | `rgba(11,11,12,0.72)` |

---

## 3. Typography

**Fonts:** **Geist** (UI and prose) and **Geist Mono** (numbers and data). Both are free under the SIL Open Font License, available on Google Fonts and as the npm package `geist`.

```css
--font-sans: "Geist", ui-sans-serif, system-ui, -apple-system, "Segoe UI", sans-serif;
--font-mono: "Geist Mono", ui-monospace, "SFMono-Regular", Menlo, monospace;
```

| Style | Font | Weight | Size / line-height | Letter-spacing | Use |
|---|---|---|---|---|---|
| Display | Geist | 600 | 50 / 56 | -3% | Home hero |
| H1 | Geist | 600 | 30 / 36 | -2% | Page titles |
| H2 | Geist | 600 | 28 / 34 | -2% | The user's question on the Answer page |
| H3 | Geist | 600 | 16 / 22 | 0 | Card and panel titles |
| Body L | Geist | 400 | 17 / 26 | 0 | Hero subtitle, search placeholder |
| Body | Geist | 400 | 15 / 24 | 0 | Answer paragraphs |
| Body S | Geist | 400–500 | 13 / 20 | 0 | Lists, buttons, nav (nav is 14) |
| Caption | Geist | 400 | 12 / 17 | 0 | Notes, helper text |
| Label | Geist Mono | 400 | 11 / 14 | +6%, UPPERCASE | Section labels ("ANSWER", "SOURCES"), color `--text-tertiary` |
| Data | Geist Mono | 400–500 | 12 / 16 | 0 | Table values, tickers, dates, chips |
| Data L | Geist Mono | 500 | 36 / 40 | -3% | Big numbers (KPIs) |
| Citation | Geist Mono | 500 | 13 / 16 | 0 | Inline `[1]` markers, color `--accent` |

The Display headline puts its second line in gold ("Ask what companies **actually reported.**").

---

## 4. Spacing, radius, borders, effects

**Spacing scale (px):** 2, 4, 6, 8, 10, 12, 14, 16, 20, 24, 28, 32, 40, 48, 56. Use 4/8 multiples by default.

**Radius:**

| Token | px | Use |
|---|---|---|
| `--r-xs` | 4 | Checkbox |
| `--r-sm` | 6 | Citation number box, ticker chip, chart bar top corners |
| `--r-md` | 8 | Buttons, chips, nav items, tabs |
| `--r-lg` | 10 | List rows, segmented-control track, notes |
| `--r-xl` | 12 | Example cards, follow-up input, doc-meta box |
| `--r-2xl` | 14 | KPI and stat cards |
| `--r-3xl` | 16 | Answer card, figures panel, search box |
| `--r-modal` | 18 | Modal |
| `--r-pill` | 999 | Pills, badges, progress bars |

**Borders:** always 1px, drawn on the inside. Use `--border` for cards and dividers, and `--border-strong` for inputs and outline buttons. The quoted source passage has a **3px gold left border**.

**Effects:**

```css
--glow-search:   0 0 40px rgba(248,198,48,0.08);   /* main search box */
--shadow-drawer: -12px 0 40px rgba(0,0,0,0.50);    /* right-side source drawer */
--shadow-modal:  0 24px 60px rgba(0,0,0,0.60);     /* filters modal */
```

There are no other shadows. Cards are separated by borders, not shadows.

---

## 5. Layout (desktop, 1440 wide)

| Element | Size |
|---|---|
| Sidebar | 264px wide, full height, `--bg-surface`, 1px right border |
| Top bar | 60px high, 28px side padding, 1px bottom border |
| Content padding | 32px top and bottom, 40px left and right |
| Home column | 780px, centered |
| Answer page | Answer column (fills the space) + 32px gap + Official-figures panel (420px fixed) |
| Source drawer | 460px, slides in from the right over a scrim |
| Filters modal | 580px, centered over a scrim |

**Sidebar, top to bottom:** logo → "+ New question" (gold, shows ⌘K) → nav (Ask, Accuracy report) → "THIS SESSION" history list → spacer → Coverage card (MSFT · GOOGL · META) → "Not investment advice" footnote.

---

## 6. Components

**Buttons**
- *Primary:* `--accent` background, `--on-accent` text, weight 600, 13–14px, padding 9–10 × 14–18, radius 8–10. Only one per view.
- *Outline:* transparent background, 1px `--border-strong`, `--text-primary`, weight 500.
- *Accent outline:* 1px `--accent` border, `--accent` text (e.g. "Open original document ↗").

**Chips:** `--bg-raised`, 12px text in `--text-secondary`, padding 5 × 10, radius 8. Selected state: gold tint + gold border + gold text.

**Nav item:** padding 8 × 10, radius 8, 16px icon + 14px label. Active: `--bg-raised` background, `--text-primary`, weight 500.

**Search box:** `--bg-raised`, 1px `--border-strong`, radius 16, padding 18, search glow. Row 1 is the placeholder (17px, tertiary). Row 2 has the scope chips on the left and the gold "Ask ↵" button on the right.

**Answer card:** `--bg-surface`, radius 16, padding 22 × 24. Inside, in order:
1. A "ANSWER" label with the "Matches SEC filing" badge on the right.
2. The answer paragraphs, with gold mono `[n]` markers after each claim.
3. A divider.
4. A "SOURCES" label and the source rows.

**Source row:** `--bg-raised`, radius 10, padding 10 × 12. Left: a 26×26 number box (gold tint, gold mono number). Middle: the title (13px, weight 500) and meta (11px mono, tertiary). Right: "View passage". Clicking it opens the source drawer.

**Official-figures panel:**
- Tabs (Revenue / Net income / EPS) as a segmented control: a `--bg-canvas` track with the active tab in `--bg-raised`.
- Bar chart: inactive bars in `--border-strong`, the asked-about quarter in `--accent`. Bar tops have radius 6. Value labels above the bars and quarter labels below, both mono 10–11px.
- Table: mono 12px, with the highlighted row tinted gold.
- A blue info note explaining the fiscal calendar.

**Source drawer:** a doc-meta box (key/value rows in mono), then the "EXACT PASSAGE" block (`--bg-canvas`, 3px gold left border, surrounding context in tertiary, the cited sentence in gold weight 500), then "SUPPORTS THIS SENTENCE", then the action buttons.

**Filters modal:** groups for Companies, Quarters (segmented: Latest / Last 4 / Last 8 / Custom range), Document types and Speaker. Each option is a checkbox chip. The footer has "Reset all" on the left and "Cancel" + "Apply · N filters" (gold) on the right.

**Checkbox:** 16px, radius 4. Off: 1.5px `--border-strong`. On: `--accent` fill with a dark check mark.

**Progress bar:** track `--bg-raised`, 6–8px high, pill radius. Fill is `--accent` when the score is ≥ 85%, and `--down` when it is below 85% (shown with a "weaker" pill).

**Badge (status):** pill, padding 5 × 10, a 6px dot plus a 12px weight-500 label, using the status tint.

---

## 7. Logo

- **Primary mark (concept A, "Lens Chart"):** a magnifier ring containing three ascending bars; the last bar is gold. Ring and handle stroke = 10% of the mark size.
- **Wordmark:** "Earnings" in `--text-primary` + "Lens" in `--accent`, Geist 600, -3% letter-spacing, no space between the two words.
- **Clear space:** at least half the mark's width on every side.
- **Minimum size:** mark 16px, lockup 96px wide.
- **Which version to use:**
  - On dark backgrounds: color mark, or the white mark.
  - On gold: all black.
  - On light backgrounds: the black mark, or the color-on-light mark.
- **Don't:** recolor the bars, add gradients or shadows, stretch the mark, or put the gold mark on a white background.

**Export from Figma → page "Assets (export)":**

| Component | Exports |
|---|---|
| `logo/mark/color-on-dark`, `logo/mark/white`, `logo/mark/gold`, `logo/mark/black`, `logo/mark/color-on-light` | SVG, PNG @1x/2x/3x |
| `logo/lockup/on-dark`, `logo/lockup/on-gold`, `logo/lockup/on-light` | SVG, PNG @1x/2x/3x |
| `app-icon/gold-square`, `app-icon/gold-rounded`, `app-icon/dark-rounded` | PNG 1024/512/192/180, SVG |
| `favicon` | PNG 16/32/48, SVG |
| `icon/ask`, `icon/bars`, `icon/filter`, `icon/check` | SVG (16px) |

The Figma file also includes local text styles (`EarningsLens/…`), effect styles, and a color variable collection (`EarningsLens/Color`) that match this document.

---

## 8. Ready-to-use CSS

```css
:root {
  color-scheme: dark;
  --bg-canvas:#0B0B0C; --bg-surface:#131315; --bg-raised:#1B1B1F; --bg-hover:#232328;
  --border:#26262B; --border-strong:#3A3A41;
  --text-primary:#F2F2F3; --text-secondary:#A1A1AA; --text-tertiary:#6B6B74;
  --accent:#F8C630; --on-accent:#0B0B0C;
  --up:#2DBE8C; --down:#F0566A; --info:#6EA8FE;
  --accent-08:rgba(248,198,48,.08); --accent-10:rgba(248,198,48,.10); --accent-14:rgba(248,198,48,.14);
  --up-12:rgba(45,190,140,.12); --down-14:rgba(240,86,106,.14); --info-08:rgba(110,168,254,.08);
  --font-sans:"Geist",ui-sans-serif,system-ui,sans-serif;
  --font-mono:"Geist Mono",ui-monospace,Menlo,monospace;
  --r-xs:4px; --r-sm:6px; --r-md:8px; --r-lg:10px; --r-xl:12px; --r-2xl:14px; --r-3xl:16px; --r-modal:18px; --r-pill:999px;
  --glow-search:0 0 40px rgba(248,198,48,.08);
  --shadow-drawer:-12px 0 40px rgba(0,0,0,.5);
  --shadow-modal:0 24px 60px rgba(0,0,0,.6);
}
body { background:var(--bg-canvas); color:var(--text-primary); font-family:var(--font-sans); }
.num, .ticker, .cite, .label { font-family:var(--font-mono); font-variant-numeric:tabular-nums; }
.label { font-size:11px; letter-spacing:.06em; text-transform:uppercase; color:var(--text-tertiary); }
.cite  { font-size:13px; font-weight:500; color:var(--accent); }
```

## 9. Tailwind (optional)

```js
// tailwind.config.js → theme.extend
colors: {
  canvas:'#0B0B0C', surface:'#131315', raised:'#1B1B1F', hover:'#232328',
  line:{ DEFAULT:'#26262B', strong:'#3A3A41' },
  ink:{ DEFAULT:'#F2F2F3', 2:'#A1A1AA', 3:'#6B6B74' },
  gold:{ DEFAULT:'#F8C630', on:'#0B0B0C' },
  up:'#2DBE8C', down:'#F0566A', info:'#6EA8FE',
},
fontFamily: { sans:['Geist','ui-sans-serif','system-ui'], mono:['Geist Mono','ui-monospace','Menlo'] },
borderRadius: { xs:'4px', sm:'6px', md:'8px', lg:'10px', xl:'12px', '2xl':'14px', '3xl':'16px', modal:'18px' },
boxShadow: {
  glow:'0 0 40px rgba(248,198,48,.08)',
  drawer:'-12px 0 40px rgba(0,0,0,.5)',
  modal:'0 24px 60px rgba(0,0,0,.6)',
},
```

---

## 10. Content rules

- Every factual sentence in an answer ends with a citation marker `[n]`.
- If the documents don't contain the answer, say "I don't know", say what was searched, and show no figures panel.
- Always show the fiscal quarter together with its calendar months, e.g. "Q1 FY26 = Jul–Sep 2025".
- Never give investment advice, opinions or predictions.
- The figures in the Figma mockups are **mock data**. Use real SEC/XBRL data in the app.
