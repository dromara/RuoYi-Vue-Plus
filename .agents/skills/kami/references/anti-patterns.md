# Anti-Patterns: AI Document Quality

Common failures when AI generates professional documents. Organized by failure type, each with a bad example and the fix. Use alongside `writing.md` quality bars.

## Content Emptiness

| # | Pattern | Bad | Fix |
|---|---------|-----|-----|
| 1 | Adjective pile-up, no numbers | "Achieved significant growth across key metrics" | State the number: "Revenue grew 34% YoY to $12M" |
| 2 | Opening-paragraph filler | "In today's rapidly evolving landscape..." | Delete the opener. Start with the first real claim. |
| 3 | Restating the heading as a sentence | Heading "Revenue Growth", body "Our revenue growth has been notable" | Body must add information the heading does not carry |
| 4 | Vague time references | "Recently launched", "in the near future" | Pin to a date or quarter: "Launched Q1 2026" |
| 5 | Synonyms masking repetition | Three paragraphs saying "we are growing" in different words | One claim, one proof, move on |

## Metric Fabrication

| # | Pattern | Bad | Fix |
|---|---------|-----|-----|
| 6 | Round numbers implying precision | "Exactly 10,000 users" when the source says "around 10K" | Match the source's precision: "approximately 10,000" |
| 7 | Fake decimal precision | "Market share: 23.7%" with no cited source | Either cite the source or round to "roughly 24%" |
| 8 | Metric-narrative disconnect | Chart shows flat revenue, text says "strong momentum" | Text must match what the chart shows |
| 9 | Invented comparison baselines | "3x faster than alternatives" with no benchmark | Name the alternative and the benchmark method, or remove |
| 10 | Mixing time periods | YoY growth next to QoQ growth as if comparable | Label every comparison window explicitly |

## Structure Mimicry

| # | Pattern | Bad | Fix |
|---|---------|-----|-----|
| 11 | Resume bullet without result | "Managed a cross-functional team" | Action + Scope + Result: "Led 8-person team to ship v2.0, reducing churn 15%" |
| 12 | Template slots filled with filler | Skills section listing "Communication, Teamwork, Problem-solving" | Name the specific skill and where it was applied, or cut the section |
| 13 | Equity report without variant perception | "Company is well-positioned for growth" | State what the market gets wrong and why your thesis differs |
| 14 | One-pager without a clear ask | Three sections of context, no "what we need from you" | The ask belongs above the fold, not implied |
| 15 | Slide title as label, not assertion | "Q3 Results" | Assertion-evidence: "Q3 revenue beat guidance by 12%" |

## Visual Excess

| # | Pattern | Bad | Fix |
|---|---------|-----|-----|
| 16 | More than 3 brand-color accents per page | Four different colored highlights on the same page | One accent color for emphasis; use weight or size for hierarchy |
| 17 | Chart with no insight title | Chart titled "Revenue by Quarter" | Title states the insight: "Revenue accelerated after Q2 price change" |
| 18 | Decorative chart that restates the text | Bar chart showing the same three numbers the paragraph just listed | If the text already communicates it, the chart must add a dimension (comparison, trend, distribution) |
| 19 | Icon or emoji as section marker | Sections led by decorative icons with no semantic value | Use typographic hierarchy (size, weight, spacing) instead |

## Source Gaps

| # | Pattern | Bad | Fix |
|---|---------|-----|-----|
| 20 | Unverified version numbers | "Compatible with v4.2" when latest is v5.1 | Check the official source before citing any version |
| 21 | "Latest" without a date | "Uses the latest framework" | "Uses Next.js 15 (as of 2026-04)" |
| 22 | Competitor comparison without market data | "Leading solution in the market" | Cite the ranking source, or use "one of the established solutions" |
| 23 | Assumed availability | "Available on all major platforms" | List the actual platforms verified |

## Tone Contamination

| # | Pattern | Bad | Fix |
|---|---------|-----|-----|
| 24 | Chinese AI corporate speak | "赋能企业数字化转型", "打造一站式解决方案" | Say what it does: "帮公司把纸质流程搬到线上" |
| 25 | English AI corporate speak | "Leverage our platform to unlock synergies" | "Use the platform to share data between teams" |
| 26 | Caption restates the flow diagram | "六类来源 → 六道过滤 → 配比设计 → 训练分片，四步串联" | Cap 给出图意以外的判断："来源决定知识边界，过滤决定干净程度，配比决定能力侧重" |
| 27 | AI tone cliches (CN dashes and connectors) | "本质上是模型在做预测——这意味着..." / 大量破折号 | 删元评论框架，直接说结论。破折号换冒号或句号。自检: `grep -nE '本质是\|这意味着\|值得注意的是\|不仅.*而且\|[——–]'` |
| 28 | Sans font stack missing CJK fallback | `font-family: Inter` 用在含中文的 th / h3 | CJK 回退到系统 sans (PingFang) 跟 serif 主调冲突。任何可能渲染 CJK 的元素用 `var(--serif)` |
| 29 | Caption restates the slide title | Title: "ORM vs PRM 对比" / Cap: "ORM 跟 PRM 的对比" | Cap 必须给出标题之外的信息：取舍判据、适用场景、或讲到这里要带出的下一步。同义重写浪费了 cap 的注意力位 |
