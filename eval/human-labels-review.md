# Answers to label by hand (E5.2)

Fill in `eval/human-labels.jsonl`, one line per answer below, matching `label_id`.

- `faithful`: `true` if every factual claim in the answer is stated by, or follows by simple arithmetic from, the cited sources shown (any of them). `false` if even one claim is not. Ignore whether a marker sits on the right sentence.
- `correctness`: 1 to 5 against the gold answer, using task 3 of `eval/judge-rubric.md` (5 all key facts right; 4 minor detail missing; 3 partly right; 2 mostly wrong; 1 wrong or a false "not available"). If the gold answer says it is not in the documents: 5 if the answer declines, 1 if it gives a figure as the answer.
- `note`: optional, for example which claim is unsupported.

Write `true`/`false` and numbers without quotes. Do not look at judge output before you finish.

---

## 1. q001-C (single_fact)

**Question:** What was Microsoft's total revenue in Q2 FY2026?

**Gold answer:** $81.3 billion, up 17% year over year (15% in constant currency).

**Answer:**

> Microsoft's total revenue in Q2 FY2026 was $81.3 billion, which represented an increase of 17% compared to the same quarter in the previous fiscal year [13].

**Cited sources (1):**

[13] MSFT | FY2026 Q2 | PRESS_RELEASE | section: Microsoft Cloud and AI Strength Drives Second Quarter Results

```text
REDMOND, Wash. — January 28, 2026 — Microsoft Corp. today announced the following results for the quarter ended December 31, 2025, as compared to the corresponding period of last fiscal year:

• Revenue was $81.3 billion and increased 17% (up 15% in constant currency)

• Operating income was $38.3 billion and increased 21% (up 19% in constant currency)

• Net income on a GAAP basis was $38.5 billion and increased 60%, and on a non-GAAP basis was $30.9 billion and increased 23% (up 21% in constant currency)

• Diluted earnings per share on a GAAP basis was $5.16 and increased 60%, and on a non-GAAP basis was $4.14 and increased 24% (up 21% in constant currency)

• Non-GAAP results exclude the impact from investments in OpenAI, explained in the Non-GAAP Definition section below

“We are only at the beginning phases of AI diffusion and already Microsoft has built an AI business that is larger than some of our biggest franchises," said Satya Nadella, chairman and chief executive officer of Microsoft. “We are pushing the frontier across our entire AI stack to drive new value for our customers and partners.”

“Microsoft Cloud revenue crossed $50 billion this quarter, reflecting the strong demand for our portfolio of services,” said Amy Hood, executive vice president and chief financial officer of Microsoft. “We exceeded expectations across revenue, operating income, and earnings per share.”

The following table reconciles our financial results reported in accordance with generally accepted accounting principles (GAAP) to non-GAAP financial results. Additional information regarding our non-GAAP definition is provided below. All growth comparisons relate to the corresponding period in the last fiscal year.
```

---

## 2. q002-C (single_fact)

**Question:** What was Microsoft Cloud revenue in the first quarter of fiscal 2027?

**Gold answer:** $63.9 billion, up 30% (31% in constant currency).

**Answer:**

> Microsoft Cloud revenue in the first quarter of fiscal year 2027 was $63.9 billion, which represented an increase of 30% compared to the same period in the previous fiscal year [1][4].

**Cited sources (2):**

[1] MSFT | FY2027 Q1 | FORM_10Q | section: OVERVIEW

```text
Microsoft is a technology company committed to making digital technology and artificial intelligence (“AI”) available broadly and doing so responsibly, with a mission to empower every person and every organization on the planet to achieve more. We create platforms and applications, powered by AI, that deliver innovative solutions that meet the evolving needs of our customers.

We generate revenue by offering a wide range of cloud-based solutions, content, and other services to people and businesses; licensing and supporting an array of software products; delivering relevant online advertising to a global audience; and designing and selling devices. Our most significant expenses are related to compensating employees; supporting and investing in our cloud-based services, including datacenter operations; designing, manufacturing, marketing, and selling our other products and services; and income taxes.

Highlights from the first quarter of fiscal year 2027 compared with the first quarter of fiscal year 2026 included:

• Commercial remaining performance obligation increased 73% to $677 billion.

• Microsoft Cloud revenue increased 30% to $63.9 billion.

• Azure revenue increased 48%.

• Microsoft 365 commercial cloud revenue increased 17%.

• Microsoft 365 consumer cloud revenue increased 17%.

• Industry solutions cloud revenue increased 10%.

• Search and advertising revenue excluding traffic acquisition costs increased 12%.

• XBOX content and services revenue decreased 2%.

• Windows OEM and devices revenue decreased 22%.
```

[4] MSFT | FY2027 Q1 | PRESS_RELEASE | section: Microsoft Announces First Quarter Results

```text
REDMOND, Wash. — October 28, 2026 — Microsoft Corp. today announced the following results for the quarter ended September 30, 2026, as compared to the corresponding period of last fiscal year:

• Revenue was $92.3 billion and increased 19%

• Operating income was $45.9 billion and increased 21% (up 22% in constant currency)

• Net income was $36.6 billion and increased 32% on a GAAP basis, and was $36.4 billion and increased 18% (up 19% in constant currency) on a non-GAAP basis

• Diluted earnings per share was $4.92 and increased 32% on a GAAP basis, and was $4.89 and increased 18% (up 19% in constant currency) on a non-GAAP basis

• Non-GAAP results exclude the impact from investments in OpenAI, explained in the Non-GAAP Definition section below

“We are delivering greater capability, choice, control, and cost efficiency for customers across our entire stack,” said Satya Nadella, chairman and chief executive officer of Microsoft. “By helping organizations build and own their own intelligence, and earning trust through agent safety and security, we are translating our innovation into growing adoption and real-world impact across every industry.”

“We delivered a strong start to the fiscal year with Microsoft Cloud revenue of $63.9 billion, with growth accelerating to 30% (up 31% in constant currency) year-over-year,” said Amy Hood, executive vice president and chief financial officer of Microsoft. “Results exceeded expectations across revenue, operating income, and earnings per share.”

The following table adjusts for the impact from investments in OpenAI and reconciles our financial results reported in accordance with generally accepted accounting principles (GAAP) to non-GAAP financial results. Additional information regarding our non-GAAP definition is provided below. All growth comparisons relate to the corresponding period in the last fiscal year.
```

---

## 3. q003-C (single_fact)

**Question:** How large was the gain from Microsoft's investment in Anthropic in Q4 FY2026?

**Gold answer:** A $3.2 billion gain.

**Answer:**

> The gain from Microsoft's investment in Anthropic in Q4 FY2026 was $3.2 billion [1].

**Cited sources (1):**

[1] MSFT | FY2026 Q4 | TRANSCRIPT

```text
Speaker: AMY HOOD
Thank you, Satya, and good afternoon everyone. This fiscal year, we delivered over $331 billion in revenue, with growth accelerating to 18%, driven by strong demand across both the Azure platform and our first-party AI applications and services. Operating income growth outpaced revenue growth, increasing 21% to more than $155 billion as we invested in long-term growth while continuing to expand operating leverage.

This quarter, revenue was $90 billion, up 18% and 17% in constant currency. Gross margin dollars increased 15% and operating income increased 18%. Earnings per share was $4.74, an increase of 23%, when adjusted for the impact from our investment in OpenAI. And FX was roughly in line with guidance.

Several discrete items impacted our financial results in the quarter when compared to our forward-looking guidance provided on our April earnings call, resulting in a benefit of 27 cents on diluted earnings per share.

These include a $3.2 billion gain from our investment in Anthropic and lower-than-expected expenses related to the Voluntary Retirement Program, which were partially offset by severance expense and impairment charges in XBOX.

When adjusting for these items, we exceeded expectations across revenue, operating income and earnings per share due to strong demand and execution in the quarter.

Company gross margin percentage was 67%, down year-over-year, driven by sales mix shift to Azure as well as continued investments in AI infrastructure and growing product usage, partially offset by ongoing efficiency gains, particularly in Azure and M365 Commercial cloud.

Operating expenses increased 10% driven by continued investment in R&D compute capacity, talent, and data to support product development across the portfolio. G and A growth was impacted by a low prior-year comparable as well as some of the discrete items mentioned earlier. Operating margins increased slightly year-over-year to 45%.

Total company headcount declined 2% year-over-year.

When adjusted for the impact of our investments in OpenAI, other income and expense was $2.8 billion driven by the gain on investment in Anthropic noted earlier.

Capital expenditures were $41 billion including the impact from higher component pricing as noted in our guide. Roughly two thirds of our capex was for short-lived assets, primarily CPUs and GPUs as customers increasingly build solutions that leverage both AI and non-AI infrastructure.

The remaining spend was for long-lived assets. This quarter, total finance leases were $5.6 billion and were primarily for large datacenter sites. And cash paid for P, P, and E was $35.8 billion.

Cash flow from operations was $55.4 billion, up 30% driven by strong cloud billings and collections, partially offset by an increase in operating lease payments. And free cash flow was $19.6 billion reflecting higher capital expenditures.
```

---

## 4. q004-C (single_fact)

**Question:** How many fully autonomous rides per week was Waymo doing according to Alphabet's Q1 2026 results?

**Gold answer:** Over 500,000 fully autonomous rides a week.

**Answer:**

> Waymo surpassed 500,000 fully autonomous rides per week in Q1 2026, doubling in less than a year [1][3].

**Cited sources (2):**

[1] GOOGL | FY2026 Q1 | PRESS_RELEASE

```text
Exhibit 99.1

Alphabet Announces First Quarter 2026 Results

MOUNTAIN VIEW, Calif. – April 29, 2026 – Alphabet Inc. (NASDAQ: GOOG, GOOGL) today announced financial results for the quarter ended March 31, 2026.

•Consolidated Alphabet revenues increased 22%, or 19% in constant currency, to $109.9 billion, reflecting strong performance across the business and our 11th consecutive quarter of double-digit growth.

•Google Services revenues increased 16% to $89.6 billion, led by 19% growth in Google Search & other, 19% in Google subscriptions, platforms, and devices, and 11% in YouTube ads.

•Google Cloud saw a meaningful acceleration in growth as revenues increased 63% to $20.0 billion, led by an increase in Google Cloud Platform (GCP) across enterprise AI Solutions and enterprise AI Infrastructure, as well as core GCP services.

•Consolidated Alphabet operating income increased 30% and operating margin expanded by 2 percentage points to 36.1%.

•Other income reflected a net gain of $37.7 billion, primarily the result of net unrealized gains on our non-marketable equity securities.

•Net income increased 81% and EPS increased 82% to $5.11.

•The company announced a 5% increase to the dividend, resulting in a quarterly cash dividend of $0.22.

Sundar Pichai, CEO of Alphabet and Google, said: “2026 is off to a terrific start. Our AI investments and full stack approach are lighting up every part of the business. Search had a strong quarter with AI experiences driving usage, queries at an all time high, and 19% revenue growth. Google Cloud revenues grew 63% with backlog nearly doubling quarter on quarter to over $460 billion. This was our strongest quarter ever for our consumer AI plans, driven by the Gemini App. Overall the number of paid subscriptions has now reached 350 million, with YouTube and Google One being the key drivers. Gemini Enterprise has great momentum with 40% quarter on quarter growth in paid monthly active users. And, finally, I’m pleased to see Waymo surpass 500,000 fully autonomous rides a week.

These outstanding results are built on our differentiated, full stack approach. Our first-party models, like Gemini, are now processing more than 16 billion tokens per minute via direct API use by our customers, up 60% from last quarter.

It’s really exciting to see how our AI investments are delivering value for our users, customers and business.”

Q1 2026 Financial Highlights (unaudited)
```

[3] GOOGL | FY2026 Q1 | TRANSCRIPT | section: 2026 Q1 Earnings Call

```text
Speaker: Sundar Pichai, CEO, Alphabet and Google
The performance of Wiz so far has exceeded our expectations.

Together with Google's Threat Intelligence, Security Operations and AI models, Wiz is helping organizations detect, prevent and respond to threats.

We introduced new Gemini powered agents for threat detection, continuous red teaming, and automated remediation to protect software code and Cloud systems

Customers like Deloitte, Priceline and Shell are using our agentic defense to strengthen their security posture. All of this is powered by the AI Infrastructure I mentioned earlier.

Our TPUs continue our leadership in performance, cost and power efficiency for customers like Thinking Machines Lab, Hudson River Trading and Boston Dynamics. As TPU demand grows from AI labs, capital markets firms, and high performance computing applications, we will begin to deliver TPUs to a select group of customers in their own data centers in a hardware configuration to expand our addressable market opportunity.

Turning to YouTube, where our momentum continues.

In the Living Room, U.S. viewers are watching over 200 million hours of YouTube content daily.

And as of March, we've reached a new milestone with over ten million channels now publishing Shorts each day. This level of daily activity is a testament to how people enjoy this content and how we've made it easier for creators.

And in Q1, our YouTube Music and Premium offering saw its largest quarterly increase in the total number of non-trial subscribers, both globally and in the US, since YouTube Premium launched in June 2018. I hope you'll tune in to Brandcast on May 13.

Moving to Other Bets.

Waymo is on a great trajectory. It launched in Nashville a few weeks ago. That makes six new cities so far in 2026, and operations in eleven major U.S. cities in total. Waymo also surpassed 500,000 fully autonomous rides per week, doubling in less than a year.

Wing continues to expand across the U.S in partnership with Walmart and Doordash, and announced plans to operate in the Bay Area.

In summary, a terrific start to the year. With so many great opportunities ahead, we're not slowing down. Huge thanks to all our employees and our partners.

See you at I/O on May 19th.

Philipp, over to you.
```

---

## 5. q005-C (single_fact)

**Question:** How many monthly active users did the Gemini App have in Q2 2026?

**Gold answer:** 950 million monthly active users.

**Answer:**

> In FY2026 Q2, the Gemini App had 950 million monthly active users [1][4].

**Cited sources (2):**

[1] GOOGL | FY2026 Q2 | TRANSCRIPT | section: 2026 Q2 Earnings Call

```text
Speaker: Sundar Pichai, CEO, Alphabet and Google
It allows users to create anything from any input, starting with video. Since launching at I/O in May, there's been a 40% increase in daily active users creating videos on the Gemini App.

Our Gemma family of open models, small enough to run on local devices, are hugely popular. These models have been downloaded over 900 million times.

And our latest Gemma 4 models have been downloaded over 300 million times since launching in April.

Our agentic development platform Antigravity allows anyone to build in the agent-first era. It has more than 2.4 million weekly active users. Antigravity is a powerful tool for users and enterprises, and it's completely accelerated how we build internally. As just one example, a team in Chrome is now on track to accelerate delivery by eight times, compressing a two-year timeline into three months through model-driven refactoring.

Moving on to our global product footprint, which brings AI to more people than any other company.

Starting with Search. AI continues to drive an expansionary moment, with new experiences resonating with users and driving growth in queries. As a big football fan, I was particularly excited to see Search usage hit an all-time high during the World Cup this year. This really highlights how much people turn to Google in moments that matter.

We continue to make Search more helpful and intuitive. We recently brought together AI Overviews and AI Mode into one seamless Search experience that combines our frontier capabilities with the best of the web. And we're continuing to incorporate more frontier capabilities into Search with agents, personal intelligence and notebooks.

Our AI-powered features are driving increased Search usage. Since expanding AI Mode globally last October, we've surpassed one billion monthly active users. And just like AI Overviews, AI Mode is driving an incremental increase in Search queries overall, and we're now sending billions of clicks to websites every week through AI features in Search.

As we serve more of these queries, we have continued to drive efficiencies. Thanks to our engineering and hardware optimizations, this quarter we reduced the cost of AI Mode responses to its lowest level since launch, even as we have brought more advanced AI capabilities.

Philipp will also talk more about how we are innovating the ads experience.

Next, the Gemini App, which now has 950 million monthly active users, with daily active users tripling in the last year. Users love new agentic features like Daily Brief and our personalized agent, Gemini Spark, which is now available in the US and internationally. We've been shipping helpful new features like this at an incredible clip.
```

[4] GOOGL | FY2026 Q2 | TRANSCRIPT | section: 2026 Q2 Earnings Call

```text
Speaker: Sundar Pichai, CEO, Alphabet and Google
Philipp will also talk more about how we are innovating the ads experience.

Next, the Gemini App, which now has 950 million monthly active users, with daily active users tripling in the last year. Users love new agentic features like Daily Brief and our personalized agent, Gemini Spark, which is now available in the US and internationally. We've been shipping helpful new features like this at an incredible clip.

On to YouTube. Month after month, when a big event happens in the world, the world turns to YouTube. It's been incredible to see that over 1.7 billion unique viewers globally watched World Cup-related videos on YouTube during the FIFA World Cup 2026.

We are also bringing the power of conversational AI directly into the YouTube experience. Ask YouTube uses our Gemini models to let people ask complex questions about individual videos, get quick takeaways, and jump straight to moments in those videos. The early engagement is encouraging. More than 140 million users engaged with Ask YouTube on the watch page in June 2026. And we're bringing that Ask experience to the wider search experience on YouTube.

Next, Cloud. Our continued momentum is driven by our integrated AI portfolio consisting of chips, models, data, security, and agent platforms, all designed to work together. Gemini continues to be a key driver of growth, and is deeply integrated across all of our Cloud products, including Gemini Enterprise, Data Analytics, Cybersecurity, and Google Workspace.

We are seeing strong, diversified demand across products, customers, geographies, and industries. Our product differentiation is driving expansion in three ways.

We're winning new customers, more than doubling our acquisition velocity year-over-year.

We're deepening our relationships with existing customers, who are expanding their usage and exceeding their commitments by more than 50%, also an acceleration over last quarter.

We are driving growth with partners, with transactions on Google Cloud Marketplace growing over seven times year-over-year.

One of the strongest parts of our growth comes from the rapid adoption of our Gemini Enterprise platform. It is differentiated with easy-to-use tools to build agents and automate processes, connectivity to enterprise systems, cost management and governance tools. In Q2, Agent Development Kit, our framework for building and deploying enterprise AI agents, reached nearly 70 million total downloads.

As I said, nearly 90% of the Fortune 100 are using Gemini Enterprise. We have customers like PepsiCo for AI and analytics solutions, Intel to streamline core processes, HSBC for wealth management, Bell Canada for customer engagement, Macy's for commerce experiences, and SIGNAL IDUNA for knowledge management.
```

---

## 6. q006-C (single_fact)

**Question:** What quarterly dividend did Alphabet announce with its Q1 2026 results?

**Gold answer:** A 5% increase to $0.22 per share per quarter.

**Answer:**

> Alphabet announced a quarterly cash dividend of $0.22 per share for Q1 2026, representing a 5% increase from the previous quarterly dividend of $0.21 per share [3][6].

**Cited sources (2):**

[3] GOOGL | FY2026 Q1 | FORM_10Q | section: Dividend Program

```text
In the three months ended March 31, 2026, total cash dividends were $1.2 billion for Class A, $176 million for Class B, and $1.1 billion for Class C shares, respectively.

In April 2026, the company's Board of Directors declared a quarterly cash dividend of $0.22 per share, representing a 5% increase from the previous quarterly dividend of $0.21 per share. The dividend is payable on June 15, 2026 to stockholders of record for each of the company's Class A, Class B, and Class C shares as of June 8, 2026.

The company has declared a quarterly cash dividend in the current quarter, and intends to pay quarterly cash dividends in the future, subject to review and approval by the company’s Board of Directors in its sole discretion.
```

[6] GOOGL | FY2026 Q1 | PRESS_RELEASE | section: Dividend Program

```text
On April 27, 2026, Alphabet’s Board of Directors declared a quarterly cash dividend of $0.22 per share, representing a 5% increase from the previous quarterly dividend of $0.21 per share. The dividend is payable on June 15, 2026 to stockholders of record for each of the Company’s Class A, Class B, and Class C shares as of June 8, 2026.
```

---

## 7. q007-C (single_fact)

**Question:** How many daily active people across Meta's Family of Apps did Meta report on average in December 2025?

**Gold answer:** 3.58 billion on average, up 7% year over year.

**Answer:**

> Meta reported an average of 3.58 billion daily active people (DAP) across its Family of Apps in December 2025, which represents a 7% increase year-over-year from 3.35 billion in December 2024 [1][4].

**Cited sources (2):**

[1] META | FY2025 Q4 | FORM_10K | section: Trends in Our Family Metrics

```text
The numbers for our key Family metrics, our DAP and average revenue per person (ARPP), do not include users on our other products unless they would otherwise qualify as DAP based on their other activities on our Family products.

Trends in the number of people in our community affect our revenue and financial results by influencing the number of ads we are able to show, the value of our ads to marketers, as well as our expenses and capital expenditures. Substantially all of our daily active people (as defined below) access our Family products on mobile devices.

•Daily Active People (DAP). We define a daily active person as a registered and logged-in user of Facebook, Instagram, Messenger, and/or WhatsApp (collectively, our "Family" of products) who visited at least one of these Family products through a mobile device application or using a web or mobile browser on a given day. We do not require people to use a common identifier or link their accounts to use multiple products in our Family, and therefore must seek to attribute multiple user accounts within and across products to individual people. Our calculations of DAP rely upon complex techniques, algorithms, and machine learning models that seek to estimate the underlying number of unique people using one or more of these products, including by matching user accounts within an individual product and across multiple products when we believe they are attributable to a single person, and counting such group of accounts as one person. As these techniques and models require significant judgment, are developed based on internal reviews of limited samples of user accounts, and are calibrated against user survey data, there is necessarily some margin of error in our estimates. We view DAP as a measure of engagement across our products. For additional information, see the section entitled "Limitations of Key Metrics and Other Data" in this Annual Report on Form 10-K.

------

Note: We report the numbers of DAP as specific amounts, but these numbers are estimates of the numbers of unique people using our products and are subject to statistical variances and errors. While we expect the error margin for these estimates to vary from period to period, we estimate that such margin generally will be approximately 3% of our worldwide DAP. At our scale, it is very difficult to attribute multiple user accounts within and across products to individual people, and it is possible that the actual numbers of unique people using our products may vary significantly from our estimates, potentially beyond our estimated error margins. For additional information, see the section entitled "Limitations of Key Metrics and Other Data" in this Annual Report on Form 10-K.

Worldwide DAP increased 7% to 3.58 billion on average during December 2025 from 3.35 billion during December 2024.
```

[4] META | FY2025 Q4 | FORM_10K | section: Family of Apps Metrics

```text
•Family daily active people (DAP) was 3.58 billion on average for December 2025, an increase of 7% year-over-year.

•Ad impressions delivered across our Family of Apps increased by 12% year-over-year in 2025.

•Average price per ad increased by 9% year-over-year in 2025.
```

---

## 8. q008-C (single_fact)

**Question:** How big was the legal charge in Meta's Q3 2026 costs and expenses?

**Gold answer:** $9.89 billion, related to the agreement with state attorneys general.

**Answer:**

> In Meta's Q3 2026 costs and expenses, there was a legal charge of $9.89 billion related to the August 2026 agreement with U.S. State Attorneys General to resolve litigation related to teen mental health and social media [1][5][14].

**Cited sources (3):**

[1] META | FY2026 Q3 | TRANSCRIPT | p.7-9

```text
Speaker: Susan Li, CFO
Meta One is rolling out gradually with more than 50

features across Instagram, Facebook, and WhatsApp, including Meta Business Agent and Meta AI, and has surpassed 15 million subscriptions and trials to date. The core experience across our apps and Meta AI will remain free, while subscriptions offer more specialized capabilities and expanded AI usage.

Beyond monetizing engagement across our existing services, we are building new experiences and revenue streams around consumer agents.

Mark highlighted the impressive early momentum with Muse. Users have found many reasons to use Muse with the top 3 categories being shopping, personal productivity and creativity. We are encouraged by the breadth of use cases, with shopping in particular being used by over 25% of Muse weekly actives.

While our near-term priority is to grow adoption and expand Muse's reach, we see multiple paths to monetization, beginning with subscriptions.

We also see a significant opportunity to extend Muse to small businesses. Based on a recent user survey, we estimate that at least 500 million self-identified small businesses use Meta's family of apps to reach customers and generate revenue. Muse could help these businesses acquire customers and operate more efficiently, while helping people complete transactions. Over time, consumer and business agents could connect and transact directly, creating opportunities around paid usage, commerce, and business services.

Alongside growth, we are focused on building sustainable unit economics. Muse's compute intensity means that expanding its user base will require significant investment, and we expect total serving costs to rise. At the same time, we expect to improve compute cost efficiency by more than tenfold over the next year, driven by better utilization of off-peak capacity, more efficient models and inference optimization.

As revenue scales, these efficiency gains give us confidence in our ability to build a highly profitable business over time.

Moving now to our financial outlook.

We expect fourth quarter 2026 total revenue to be in the range of $73-76 billion. Our guidance assumes foreign currency is an approximately 1% headwind to year-over-year total revenue growth, based on current exchange rates.

Turning to the expense and capex outlooks.

We are raising our expense outlook to incorporate the $9.9 billion charge related to legal proceedings recognized in Q3. We now expect full year 2026 total expenses to be in the range of $175-179 billion.

Excluding the Q3 legal charge, we continue to expect 2026 operating income to be above 2025 operating income.

At a segment level, we now expect Reality Labs losses to decline compared to 2025 to a range of $18-19 billion.

We anticipate 2026 capital expenditures, including principal payments on finance leases, to be in the range of $135-145 billion, narrowed from our prior outlook of $130-145 billion.
```

[5] META | FY2026 Q3 | PRESS_RELEASE | section: Non-GAAP Financial Measures

```text
____________________________________

(1) The third quarter 2026 general and administrative expenses include a $9.89 billion charge related to the August 2026 agreement with U.S. State Attorneys General to resolve litigation related to teen mental health and social media.

META PLATFORMS, INC.
CONDENSED CONSOLIDATED BALANCE SHEETS
(In millions)
(Unaudited)
September 30, 2026 | December 31, 2025
Assets
Current assets:
Cash and cash equivalents | $11,222 | $35,873
Marketable securities | 71,227 | 45,719
Accounts receivable, net | 22,716 | 19,769
Prepaid expenses and other current assets | 14,735 | 7,361
Total current assets | 119,900 | 108,722
Non-marketable equity investments | 32,624 | 27,524
Property and equipment, net | 253,222 | 176,400
Operating lease right-of-use assets | 26,576 | 20,404
Goodwill | 23,421 | 24,534
Other assets | 23,093 | 8,437
Total assets | $478,836 | $366,021
Liabilities and stockholders' equity
Current liabilities:
Accounts payable | $19,097 | $8,894
Operating lease liabilities, current | 2,522 | 2,213
Accrued expenses and other current liabilities | 41,392 | 30,729
Total current liabilities | 63,011 | 41,836
Operating lease liabilities, non-current | 28,651 | 22,940
Long-term debt | 80,922 | 58,744
Long-term income taxes | 20,521 | 21,005
Other liabilities | 13,345 | 4,253
Total liabilities | 206,450 | 148,778
Commitments and contingencies
Stockholders' equity:
Common stock and additional paid-in capital | 108,244 | 95,793
Accumulated other comprehensive income (loss) | (1,200) | 271
Retained earnings | 165,342 | 121,179
Total stockholders' equity | 272,386 | 217,243
Total liabilities and stockholders' equity | $478,836 | $366,021

META PLATFORMS, INC.
CONDENSED CONSOLIDATED STATEMENTS OF CASH FLOWS
(In millions)
(Unaudited)
Three Months Ended September 30, | Nine Months Ended September 30,
2026 | 2025 | 2026 | 2025
Cash flows from operating activities
```

[14] META | FY2026 Q3 | PRESS_RELEASE | section: Agreement with U.S. State Attorneys General

```text
In August 2026, we announced an agreement with 52 attorneys general to resolve litigation related to teen mental health and social media, which provides for payments of up to approximately $18.59 billion. Third quarter 2026 costs and expenses includes a $9.89 billion charge related to this agreement. Excluding this charge, the third quarter 2026:

•Income from operations of $12.99 billion would have increased by $9.89 billion to $22.88 billion.

•Diluted EPS of $3.83 would have increased by $3.57 to $7.40.

•Effective tax rate of 24% would have decreased by 7 percentage points to 17%.
```

---

## 9. q009-C (single_fact)

**Question:** How many employees were affected by Meta's May 2026 headcount reduction?

**Gold answer:** Approximately 8,000 employees.

**Answer:**

> Approximately 8,000 employees were impacted by Meta's May 2026 headcount reduction [3][4].

**Cited sources (2):**

[3] META | FY2026 Q2 | PRESS_RELEASE | section: Second Quarter 2026 Operational and Other Financial Highlights

```text
•Family daily active people (DAP) – DAP was 3.60 billion on average for June 2026, an increase of 3% year-over-year.

•Ad impressions – Ad impressions delivered across our Family of Apps increased by 14% year-over-year.

•Average price per ad – Average price per ad increased by 12% year-over-year.

•Revenue – Revenue was $60.80 billion, an increase of 28% year-over-year. Revenue on a constant currency basis would have increased by 27% year-over-year.

•Costs and expenses – Total costs and expenses were $42.03 billion, an increase of 55% year-over-year. This includes $2.40 billion of charges related to legal proceedings and $1.18 billion of severance expenses in connection with the May 2026 headcount reduction.

•Capital expenditures – Capital expenditures, including principal payments on finance leases, were $31.08 billion.

•Capital return program – Dividend and dividend equivalent payments were $1.35 billion.

•Cash, cash equivalents, and marketable securities – Cash, cash equivalents, and marketable securities were $90.26 billion as of June 30, 2026.

•Long-term debt – Long-term debt was $83.66 billion as of June 30, 2026.

•Cash flow – Cash flow from operating activities was $31.86 billion, and free cash flow was $784 million.(1)

•Headcount – Headcount was 75,472 as of June 30, 2026, a decrease of 1% year-over-year. Our reported headcount includes approximately 8,000 employees impacted by the May 2026 headcount reduction, the majority of whom will no longer be reflected in our headcount by the end of the third quarter of 2026.

____________________________________

(1) For more information on our free cash flow non-GAAP financial measure, see the sections entitled "Non-GAAP Financial Measures" and "Reconciliation of GAAP to Non-GAAP Results" in this press release.
```

[4] META | FY2026 Q2 | FORM_10Q | section: Consolidated and Segment Results

```text
We report our financial results for our two reportable segments: Family of Apps (FoA) and Reality Labs (RL). FoA includes Facebook, Instagram, Messenger, WhatsApp, and other services. RL includes our virtual and augmented reality related consumer hardware, software, and content.

Family of Apps | Reality Labs | Total
Three Months Ended June 30, | % change | Three Months Ended June 30, | % change | Three Months Ended June 30, | % change
2026 | 2025 | 2026 | 2025 | 2026 | 2025
(in millions, except percentages)
Revenue | $60,370 | $47,146 | 28% | $431 | $370 | 16% | $60,801 | $47,516 | 28%
Costs and expenses | 36,976 | 22,175 | 67% | 5,050 | 4,900 | 3% | 42,026 | 27,075 | 55%
Income (loss) from operations | $23,394 | $24,971 | (6)% | $(4,619) | $(4,530) | (2)% | $18,775 | $20,441 | (8)%
Operating margin | 39% | 53% | (1,072)% | (1,224)% | 31% | 43%

•Net income was $15.85 billion, with diluted earnings per share (EPS) of $6.18 for the three months ended June 30, 2026.

•Capital expenditures, including principal payments on finance leases, were $31.08 billion for the three months ended June 30, 2026.

•Dividend and dividend equivalent payments were $1.35 billion for the three months ended June 30, 2026.

•Cash, cash equivalents, and marketable securities were $90.26 billion as of June 30, 2026.

•Long-term debt was $83.66 billion as of June 30, 2026.

•Effective tax rate was 16% for the three months ended June 30, 2026.

•Headcount was 75,472 as of June 30, 2026, a decrease of 1% year-over-year. Our reported headcount includes approximately 8,000 employees impacted by the May 2026 headcount reduction, the majority of whom will no longer be reflected in our headcount by the end of the third quarter of 2026.
```

---

## 10. q010-C (single_fact)

**Question:** How many people had set up Muse personal agents by Meta's Q3 2026 call?

**Gold answer:** More than 10 million people in the US and Canada, in about a month and a half.

**Answer:**

> As of the Q3 2026 call, more than 10 million people in the U.S. and Canada had set up Muse personal agents [1][11].

**Cited sources (2):**

[1] META | FY2026 Q3 | TRANSCRIPT | p.1-2

```text
Speaker: Mark Zuckerberg, CEO
Hey everyone. Thanks for joining today.

This has been a busy and exciting few months. Our family of apps is now used by 3.64 billion people each day, and we're seeing all-time highs in engagement on both Facebook and Instagram driven by strong growth in video. Threads has now overtaken X in monthly actives and continues to grow quickly. WhatsApp is hitting all-time highs in messaging and is becoming an increasingly important platform for agents as well.

In the last few months we have had several major milestones towards our goal of delivering personal superintelligence to everyone. We had one of our most successful launches ever with the Muse personal agent; we shipped Muse Wonder as our first frontier model; and we shipped several new glasses and gadgets with Muse integration, including coming soon to our new audio glasses.

We have been discussing our vision for personal superintelligence for more than a year now. We want to help every person have an extremely capable personal agent that understands your goals and can help you achieve them and improve your life in whatever ways you want -- whether that's improving your relationships, helping you save time, save money, build a business, create new things, or anything else that you can dream of. Muse is the product form of this vision. So now we have the foundational technology in place to deliver this. There is still a lot of work ahead, but our initial position is very strong.

In just a month and a half, more than 10 million people in the US and Canada have set up Muse personal agents. Growth continues to be strong and we have been #1 in the App Store almost every day. Engagement and retention are also off to a strong start. About 25% of people who have set up Muse use it on any given day. For our earliest adopters, the longer that they use Muse,

the better that retention seems to get. To put this in perspective, this is more than twice the level that Threads started at or where we believe that other AI apps started at as well.

The most common use case for Muse is shopping, with more than a quarter of people who are using Muse engaging in this with an average purchase size over $200. Plaid told us they hadn't seen activity like they're seeing with their connector on any other service before. And in the first week we had to work with Stripe to raise credit limits since the volumes were above what any of us expected.

Beyond shopping, the most common connectors are for personal productivity and connecting Muse to your other services like email and calendar. This shows the importance of the Muse Secure VM that we've built to provide state-of-the-art privacy and security, and we're looking forward to following up soon with the Muse Confidential VM, which will make it so that even Meta cannot see your content.
```

[11] META | FY2026 Q3 | TRANSCRIPT | p.10-11

```text
Speaker: Mark Zuckerberg, CEO
Yes. I think I can talk about both of those, and then Susan can jump in with anything else. All right.

So right now for Muse, we are -- and one of the things I pointed out is we have more than 10 million people in the U.S. and Canada who have set it up, but I think one of the biggest constraints so far is that because this is a compute- intensive service to run, we have not launched it globally yet. That's partially because of wanting to optimize it for each place, and there's some work around connectors for that. But it's partially that we want to make sure that we do this efficiently.

So I mean there's pros and cons there. The pro, of course is that, that just makes it even more impressive that you have 10 million people using it and signing up to use it in the first six weeks because those are all kind of Americans and Canadians in a small number of markets.

So if you actually did roll it out more broadly, then you would expect that number to be significantly larger than what we're seeing today.

But then on the flip side, we actually have to go do the work to bring the efficiency in line to make that make sense.

And so that's a big part of it that we need to do. We -- the compute efficiency work is going to be important on this also because we want to make sure that we've built enough compute to be able to serve what we think is going to be 1 billion plus people if we can achieve certain compute efficiency targets, but we obviously have a lot of work to do there as well. And that will be one of the limiting factors over the next year.

We want to build out the business around it. So I talked about that. And I think doing that in tandem with bringing down the compute -- or increasing the compute efficiency is going to be important for getting this to work from a business perspective which I'm optimistic about, but I think that that's one of the things that we need to do.

And then there's just continuing to increase the value in the product which I think we will largely do by continuing to develop new models. That's why I mentioned that we launched Muse first on Muse Spark, and that's what is currently the default across the free tier. And we rolled out Muse Wonder, and people can access that in the paid tier today. And we know that it performs a

lot better, so we're working on getting a more efficient version of Muse Wonder that we'll be able to roll out more broadly.
```

---

## 11. q011-C (single_fact)

**Question:** Did Alphabet buy back any shares in Q3 2026?

**Gold answer:** No. There were no repurchases of Class A or Class C shares in the three and nine months ended September 30, 2026.

**Answer:**

> In the three and nine months ended September 30, 2026, there were no repurchases of the company's Class A or Class C shares [6][15].

**Cited sources (2):**

[6] GOOGL | FY2026 Q3 | FORM_10Q | section: Share Repurchase Program

```text
In the three and nine months ended September 30, 2026, there were no repurchases of the company's Class A or Class C shares.

In April 2025, the company's Board of Directors authorized a $70.0 billion share repurchase program for its Class A and Class C shares. As of September 30, 2026, $69.5 billion remained available for Class A and Class C share repurchases.

For additional information, see Note 11 of the Notes to Consolidated Financial Statements included in Item 1 of this Quarterly Report on Form 10-Q.
```

[15] GOOGL | FY2026 Q3 | FORM_10Q | section: Share Repurchases

```text
In the three and nine months ended September 30, 2026, there were no repurchases of the company's Class A or Class C shares.

In April 2025, the company's Board of Directors authorized a $70.0 billion share repurchase program for its Class A and Class C shares. As of September 30, 2026, $69.5 billion remained available for Class A and Class C share repurchases.

Repurchases may be executed from time to time, subject to general business and market conditions and other investment opportunities, through open market purchases or privately negotiated transactions, including through Rule 10b5-1 plans. The repurchase programs do not have an expiration date.
```

---

## 12. q012-C (multi_quarter)

**Question:** How did Meta's 2026 capital expenditure outlook change over the last four quarters?

**Gold answer:** $115-135B (Q4 2025), raised to $125-145B (Q1 2026), narrowed to $130-145B (Q2), narrowed to $135-145B (Q3).

**Answer:**

> Here is the capital expenditure outlook for Meta over the last four quarters of 2026:
> 
> **FY2026 Q1**
> - Anticipated capital expenditures: $115-135 billion [1][2].
> 
> **FY2026 Q2**
> - Revised anticipated capital expenditures: $125-145 billion, increased from the previous range due to higher component pricing and additional data center costs [2][3].
> 
> **FY2026 Q3**
> - Further revised anticipated capital expenditures: $130-145 billion, narrowed from the previous outlook [3][4].
> 
> **FY2026 Q4**
> - Latest anticipated capital expenditures: $135-145 billion, narrowed from the previous outlook [4][8].
> 
> In summary, the capital expenditure outlook increased from a range of $115-135 billion in Q1 to a range of $135-145 billion by Q4, reflecting ongoing investments in infrastructure and technology.

**Cited sources (5):**

[1] META | FY2025 Q4 | TRANSCRIPT | p.8

```text
Speaker: Susan Li, CFO
ranking and recommendation training workloads, in addition to the inference workloads it currently runs.

More broadly, as we invest in infrastructure to meet our business needs, we continue to prioritize maintaining long-term flexibility so we can adapt to how the market develops. We’re doing so in several ways, including changing how we develop data center sites, establishing strategic partnerships, contracting cloud capacity, and establishing new ownership structures for some of our large data center sites.

We have a strong net cash balance and expect our business will continue to generate sufficient cash to fund our infrastructure investments in 2026, which is reflected in our expectations. Nonetheless, we will continue to look for opportunities to periodically supplement our strong operating cash flow with prudent amounts of cost-efficient external financing, which may lead us to eventually maintain a positive net debt balance.

Moving to our financial outlook.

We expect first quarter 2026 total revenue to be in the range of $53.5-56.5 billion. Our guidance assumes foreign currency is an approximately 4% tailwind to year-over-year total revenue growth, based on current exchange rates.

Turning to the expense and capex outlooks.

We expect full year 2026 total expenses to be in the range of $162-169 billion.

The majority of expense growth will be driven by infrastructure costs, which includes third party cloud spend, higher depreciation, and higher infrastructure operating expenses.

The second-largest contributor to total expense growth is employee compensation, driven by investments in technical talent. This includes 2026 hires to support our priority areas, particularly AI, as well as a full year of expenses from 2025 hires.

At a segment level, we expect expense growth to be driven by the Family of Apps, with Reality Labs operating losses remaining similar to 2025 levels.

We anticipate 2026 capital expenditures, including principal payments on finance leases, to be in the range of $115-135 billion, with year-over-year growth driven by increased investment to support our Meta Superintelligence Labs efforts and core business.

Despite the meaningful step up in infrastructure investment, in 2026 we expect to deliver operating income that is above 2025 operating income.

Absent any changes to our tax landscape, we expect our full year 2026 tax rate to be 13-16%.

Finally, we recently aligned with the European Commission on further changes to our Less Personalized Ads offering, which we will begin rolling out this quarter. However, we continue to monitor legal and regulatory headwinds in the EU and the US that could significantly impact our business and financial results. For example, we continue to see scrutiny on youth-related issues and have a number of trials scheduled for this year in the US, which may ultimately result in a material loss.
```

[2] META | FY2026 Q1 | TRANSCRIPT | p.7-8

```text
Speaker: Susan Li, CFO
Last month, we rolled out our affiliate partnerships offering on Facebook to more test partners, so creators can tag products from participating retailers on their posts and earn a commission when someone makes a purchase. We have also started testing similar experiences on Instagram.

We see a real opportunity to help people more easily discover and buy products within our services, particularly as we incorporate AI deeply across our platforms.

Next, I would like to discuss our approach to capital allocation.

Compute is becoming increasingly important as it determines the quality of the services we can provide, including powering more capable models and delivering innovative new products. It is also becoming more critical to how we work at Meta, as we are entering a world where employees are managing agents to help them generate new ideas, run experiments, execute tasks, and build products.

We are investing aggressively to meet our infrastructure needs and ensure we maximize our strategic flexibility over the coming years. This includes substantially expanding our own data center footprint and striking deals throughout the supply chain to secure necessary components for future capacity. We are also signing cloud deals that will come online over the course of this year and 2027, allowing us to scale more quickly. These multi-year cloud deals and our

infrastructure purchase agreements drove a $107 billion step up in our contractual commitments this quarter.

Our investments will support our training needs for future models, and most importantly, provide us the inference capacity necessary to deliver personal and business agents to billions of people around the world, along with several other AI product experiences we’re developing.

As we grow our infrastructure spend, we remain committed to operating efficiently, and we recently shared internally that we plan to reduce the size of our employee base in May. We believe a leaner operating model will allow us to move more quickly while also helping to offset the substantial investments we’re making.

Moving to our financial outlook.

We expect second quarter 2026 total revenue to be in the range of $58-61 billion. Our guidance assumes foreign currency is an approximately 2% tailwind to year-over-year total revenue growth, based on current exchange rates.

Turning to the expense and capex outlooks.

We expect full year 2026 total expenses to be in the range of $162-169 billion, unchanged from our prior outlook.

We continue to expect to deliver operating income this year that is above 2025 operating income.

We anticipate 2026 capital expenditures, including principal payments on finance leases, to be in the range of $125-145 billion, increased from our prior range of $115-135 billion. This reflects our expectations for higher component pricing this year and, to a lesser extent, additional data center costs to support future year capacity.

Absent any changes to our tax landscape, we expect our tax rate for the remaining quarters of 2026 to be between 13-16%.
```

[3] META | FY2026 Q2 | PRESS_RELEASE | section: CFO Outlook Commentary

```text
We expect third quarter 2026 total revenue to be in the range of $61-64 billion. Our guidance assumes foreign currency is an approximately 1% headwind to year-over-year total revenue growth, based on current exchange rates.

We are raising the lower-end of our expense outlook to incorporate the $2.4 billion charges related to legal proceedings recognized in the second quarter. We now expect full year 2026 total expenses to be in the range of $165-169 billion.

We continue to expect to deliver operating income this year that is above 2025 operating income.

We anticipate 2026 capital expenditures, including principal payments on finance leases, to be in the range of $130-145 billion, narrowed from our prior outlook of $125-145 billion.

Absent any changes to our tax landscape, we expect our tax rate for the remaining quarters of 2026 to be between 15-17%, an increase from our prior outlook of 13-16%.

Finally, we continue to monitor active legal and regulatory matters that could significantly impact our business and financial results. For example, we continue to see scrutiny on youth-related issues in several markets and have a number of youth-related trials scheduled for this year in the U.S., which may ultimately result in a material loss.
```

[4] META | FY2026 Q3 | TRANSCRIPT | p.7-9

```text
Speaker: Susan Li, CFO
Meta One is rolling out gradually with more than 50

features across Instagram, Facebook, and WhatsApp, including Meta Business Agent and Meta AI, and has surpassed 15 million subscriptions and trials to date. The core experience across our apps and Meta AI will remain free, while subscriptions offer more specialized capabilities and expanded AI usage.

Beyond monetizing engagement across our existing services, we are building new experiences and revenue streams around consumer agents.

Mark highlighted the impressive early momentum with Muse. Users have found many reasons to use Muse with the top 3 categories being shopping, personal productivity and creativity. We are encouraged by the breadth of use cases, with shopping in particular being used by over 25% of Muse weekly actives.

While our near-term priority is to grow adoption and expand Muse's reach, we see multiple paths to monetization, beginning with subscriptions.

We also see a significant opportunity to extend Muse to small businesses. Based on a recent user survey, we estimate that at least 500 million self-identified small businesses use Meta's family of apps to reach customers and generate revenue. Muse could help these businesses acquire customers and operate more efficiently, while helping people complete transactions. Over time, consumer and business agents could connect and transact directly, creating opportunities around paid usage, commerce, and business services.

Alongside growth, we are focused on building sustainable unit economics. Muse's compute intensity means that expanding its user base will require significant investment, and we expect total serving costs to rise. At the same time, we expect to improve compute cost efficiency by more than tenfold over the next year, driven by better utilization of off-peak capacity, more efficient models and inference optimization.

As revenue scales, these efficiency gains give us confidence in our ability to build a highly profitable business over time.

Moving now to our financial outlook.

We expect fourth quarter 2026 total revenue to be in the range of $73-76 billion. Our guidance assumes foreign currency is an approximately 1% headwind to year-over-year total revenue growth, based on current exchange rates.

Turning to the expense and capex outlooks.

We are raising our expense outlook to incorporate the $9.9 billion charge related to legal proceedings recognized in Q3. We now expect full year 2026 total expenses to be in the range of $175-179 billion.

Excluding the Q3 legal charge, we continue to expect 2026 operating income to be above 2025 operating income.

At a segment level, we now expect Reality Labs losses to decline compared to 2025 to a range of $18-19 billion.

We anticipate 2026 capital expenditures, including principal payments on finance leases, to be in the range of $135-145 billion, narrowed from our prior outlook of $130-145 billion.
```

[8] META | FY2026 Q3 | PRESS_RELEASE | section: CFO Outlook Commentary

```text
We expect fourth quarter 2026 total revenue to be in the range of $73-76 billion. Our guidance assumes foreign currency is an approximately 1% headwind to year-over-year total revenue growth, based on current exchange rates.

We are raising our expense outlook to incorporate the $9.9 billion charge related to legal proceedings recognized in the third quarter. We now expect full year 2026 total expenses to be in the range of $175-179 billion.

Excluding the third quarter legal charge, we continue to expect 2026 operating income to be above 2025 operating income.

At a segment level, we now expect Reality Labs losses to decline compared to 2025 to a range of $18-19 billion.

We anticipate 2026 capital expenditures, including principal payments on finance leases, to be in the range of $135-145 billion, narrowed from our prior outlook of $130-145 billion.

Absent any changes to our tax landscape, we expect our fourth quarter 2026 tax rate to be between 15-17%.

Additionally, we continue to monitor active legal and regulatory matters that could significantly impact our business and financial results. For example, we continue to see scrutiny on youth-related issues in several markets and have a number of youth-related trials scheduled in the coming months in the U.S.

Looking beyond 2026, we are still developing our budget and capacity plans for next year, but we expect to continue investing aggressively to support both our core business and emerging AI initiatives. Our current capacity and what is scheduled to come online gives us the flexibility to allocate resources across model training, core business growth, and the development and support of new products.

Our AI investments are already delivering substantial incremental advertising revenue through stronger engagement, better recommendations, and more efficient monetization. Building on this progress, our MSL investments are creating the technology and infrastructure needed to unlock significant new revenue opportunities and strengthen capabilities across the business. While these investments will take longer to generate returns than those in our core business, the breadth of monetization opportunities and internal efficiencies gives us confidence in their long-term potential.

Looking ahead, although we are not providing a full-year 2027 revenue outlook, the benefits our AI investments are delivering to the core business, combined with emerging opportunities from model advances and product launches, position us to sustain strong overall revenue growth throughout 2027.

Separately, we remain focused on improving Reality Labs profitability and expect losses to narrow next year to around $15 billion as we reallocate investment toward other priority areas, primarily AI.
```

---

## 13. q013-C (multi_quarter)

**Question:** How did Alphabet's full-year 2026 CapEx guidance evolve from the Q4 2025 call to the Q3 2026 call?

**Gold answer:** $175-185B (Q4 2025), raised to $180-190B (Q1 2026, includes Intersect), raised to $195-205B (Q2), unchanged at $195-205B (Q3).

**Answer:**

> Alphabet's full-year 2026 CapEx guidance evolved as follows:
> 
> - In the FY2025 Q4 call, the CapEx guidance for 2026 was initially set in the range of **$175 billion to $185 billion** [1].
> - In the FY2026 Q1 call, this guidance was updated to **$180 billion to $190 billion** [2].
> - By the FY2026 Q2 call, the CapEx guidance was further increased to **$195 billion to $205 billion** [3].
> - The FY2026 Q3 call maintained the CapEx guidance at **$195 billion to $205 billion** [4].
> 
> Thus, the CapEx guidance increased from $175-$185 billion to $195-$205 billion over the course of the quarters.

**Cited sources (4):**

[1] GOOGL | FY2025 Q4 | TRANSCRIPT | section: 2025 Q4 Earnings Call

```text
Speaker: Anat Ashkenazi, SVP and CFO, Alphabet and Google
First, in terms of revenues, we're pleased with the overall momentum of the business. At current spot rates, we would expect to see an FX tailwind toward consolidated revenues in Q1. However, the volatility in exchange rates could affect the impact of FX on Q1 revenues.

In Google Services, we expect growth to be driven by ongoing innovation in the user experience, as well as improved ROI for advertisers, keeping in mind the normal seasonal pattern for advertising revenue.

In Google Cloud, we're seeing significant demand for our products and services, which we expect to continue to drive strong growth, despite the tight supply environment we're operating in.

Moving to investments, the investments we have been making in AI are already translating to strong performance across the business, as you've seen in our financial results. Our successful execution, coupled with strong performance, reinforces our conviction to make the investments required to further capitalize on the AI opportunity.

For the full year 2026, we expect CapEx to be in the range of $175 billion to $185 billion, with investments ramping over the course of the year.

We're investing in AI compute capacity to support frontier model development by Google DeepMind, ongoing efforts to improve the user experience, and drive higher advertiser ROI in Google Services, significant Cloud customer demand, as well as strategic investments in Other Bets.

Keep in mind that the availability of supply, pricing of components, and timing of cash payments can cause some variability in the reported CapEx number.

In terms of expenses, as we've discussed on previous calls, the significant increase in our investments in technical infrastructure will continue to put pressure on the P&L in the form of higher depreciation expense and related data centers' operations costs such as energy.

In 2025, depreciation increased by nearly $6 billion, or 38%, from $15.3 billion in 2024, to $21.1 billion in 2025. Given the increase in our CapEx investments in recent years, we expect the growth rate in 2026 depreciation to accelerate in Q1, and meaningfully increase for the full year.

We're also planning to continue hiring in key investment areas, such as AI and Cloud.

In 2025, our teams delivered amazing innovation, executing with a high level of discipline and velocity. These efforts provided great experiences for consumers and outstanding performance for creators, partners, and enterprise customers driving strong revenue growth.

I'm going to take this opportunity to thank our employees for their contribution to this impressive performance.

Now, Sundar, Philipp, and I will take your questions.
```

[2] GOOGL | FY2026 Q1 | TRANSCRIPT | section: 2026 Q1 Earnings Call

```text
Speaker: Anat Ashkenazi, SVP and CFO, Alphabet and Google
The increase was driven by strong demand for our enterprise AI offerings and the inclusion of TPU hardware sales that Sundar referenced earlier. The majority of the backlog is related to typical GCP contracts, and we expect to recognize just over 50% of the backlog as revenue over the next 24 months.

In Other Bets, revenues were $411 million, and operating loss was $2.1 billion.

For the past few years, we have been working to prioritize our efforts and investments in the Other Bets. In Q1 of this year, Verily completed an external capital raise that resulted in its deconsolidation from Alphabet. GFiber announced plans to combine with Astound Broadband, which will result in its deconsolidation from Alphabet when the deal closes, which we expect to take place in Q4.

And we continue to allocate significant resources to businesses where we see meaningful opportunities to create value, such as Waymo.

Turning to our outlook, I would like to provide some commentary on factors that will impact our business performance in the second quarter and full year 2026.

First, in terms of revenues, we're pleased with the overall momentum of the business. At current spot rates, we would expect to see an FX tailwind of approximately one percentage point toward consolidated revenue in Q2, compared to three percentage points FX tailwinds in the first quarter.

In Google Cloud, as Sundar mentioned, we will begin to deliver TPU hardware to a select group of customers in their own data centers. We expect to begin recognizing a small percent of the revenues from these agreements later this year, with the vast majority of revenues to be realized in 2027.

It is important to keep in mind that revenues from TPU hardware sales will fluctuate from quarter to quarter, depending on when TPUs are shipped to customers.

And finally, we're excited to welcome the Wiz team to Google Cloud with the closing of the acquisition in March, and are very pleased with the performance to date.

A couple of items to highlight related to the acquisition. First, Wiz will be reporting in the Google Cloud segment. And second, we expect a low single digit percentage point headwind to Cloud's operating margin for the remainder of 2026 related to the acquisition.

Moving to investment, we are updating our full year 2026 CapEx guidance range to $180 to $190 billion, up from our previous estimate of $175 to $185 billion, to now include investment related to the acquisition of Intersect, which closed in March.

We are seeing unprecedented internal and external demand for AI compute resources. The investment we're making in AI is delivering strong growth, as evidenced by the record revenue and backlog growth in Google Cloud and strong performance in Google Services.
```

[3] GOOGL | FY2026 Q2 | TRANSCRIPT | section: 2026 Q2 Earnings Call

```text
Speaker: Anat Ashkenazi, SVP and CFO, Alphabet and Google
The increase was driven by strong demand for our Enterprise AI offerings. The majority of the backlog is related to typical GCP contacts for our broad mix of customers, and we expect to recognize just over 50% of the total backlog as revenue over the next 24 months.

In Other Bets, revenues were $382 million, and operating loss was $1.8 billion as we continue to expand Waymo's business and invest in key Other Bets.

In Alphabet Level Activities, the operating loss was $5.8 billion, driven by shared AI R&D expenses.

Turning to our outlook in business performance expectations in the second half of 2026.

First, in terms of revenues, at the current spot rates, we would expect a slight FX headwind to our consolidated revenue in Q3, compared to a one percentage point FX tailwind in Q2. This impact will be seen primarily in Search and YouTube Ads.

In Google Services, as Philipp mentioned, we continue to benefit from innovation in Search by improving the user and advertiser experience with AI. At the same time, in Q3, we will begin lapping an acceleration in Search performance that began in the third quarter last year.

In Google Cloud, we're seeing significant demand for products and services, which we expect to drive strong growth. As I mentioned earlier, we started delivering TPU systems to customer data centers in the second quarter. We continue to expect to recognize a relatively small portion of the revenues from our existing TPU system sales agreements this year, ramping as we exit 2026. We anticipate the vast majority of the revenues from these agreements will be realized in 2027.

And given the supply constraint environment, we plan to expand the use of third-party capacity in Q3 as a bridging strategy while we build out more internal capacity. This strategy allows us to keep growing our customer base and capture greater overall value. However, it will create modest margin pressure in the near-term as we utilize this capacity.

Moving to investments, we are updating our full year 2026 CapEx guidance range to $195-205 billion, up from our previous estimate of $180-190 billion. The increase in the range is primarily due to an acceleration in the delivery of capacity to meet growing demand.

As we previously shared, we continue to expect our CapEx to increase significantly in 2027, and we'll provide more details at a later date.

In terms of expenses, the significant increase in our investments and technical infrastructure will continue to put pressure on the P&L in the form of higher depreciation expense and related data center operations costs, such as energy. We also expect to continue hiring in key investment areas such as AI and cloud, and we are investing in marketing to support our AI products.
```

[4] GOOGL | FY2026 Q3 | TRANSCRIPT | section: 2026 Q3 Earnings Call

```text
Speaker: Anat Ashkenazi, SVP and CFO, Alphabet and Google
The Google Cloud segment delivered record growth in the third quarter. Cloud revenues were up 103% to $30.7 billion, driven by strong growth in GCP, with strength in AI, including both AI infrastructure and AI solutions and Core GCP.

TPU systems sales were also a meaningful contributor. GCP accounted for the vast majority of the revenue generated in Q3. The Cloud revenue growth rate, excluding the impact of TPU systems, was again incredibly strong.

Cloud operating income was $11.1 billion, more than tripling year-over-year. Operating margin increased from 23.7% in the third quarter last year to 36.2%.

The third-party bridging capacity, which we discussed on our prior call, came online only at the very end of Q3 and, therefore, had limited negative impact to our margin in the quarter.

Google Cloud's backlog was $561 billion in the third quarter, with the majority of the backlog related to typical GCP contracts. We expect to recognize just over 55% of the total backlog as revenues over the next 24 months.

In Other Bets, revenues were $372 million, and the operating loss was $2 billion dollars as we continue to expand Waymo's business and invest in key Other Bets.

In Alphabet-level activities, the operating loss was $7.7 billion, driven by shared AI R&D expenses.

I'll now turn to our outlook for the remainder of 2026. First, in terms of revenues, at current spot rates, we would expect a slightly larger FX headwind to our consolidated revenues in Q4, relative to the prior quarter. This impact will be seen primarily in Search and YouTube Ads.

In Google Services, we will continue to lap the Search performance acceleration that began in the third quarter last year, which will pressure revenue growth in Q4.

In Google Cloud, we expect TPU system sales to continue to scale in Q4, followed by a meaningful revenue ramp in 2027. With respect to Cloud margins, given the supply-constraint environment, we started utilizing third-party compute capacity at the end of Q3 and will ramp usage in Q4. We expect margin headwinds in the near-term as we utilize this capacity.

Moving to investments, our full-year 2026 CapEx guidance range of $195-$205 billion remain unchanged. We continue to expect our CapEx to increase significantly in 2027, and we'll provide further details on our Q4 call.

In terms of expenses, the significant increase in our investment in technical infrastructure will continue to weigh on the P&L in the form of higher depreciation expenses and related data centers' operation costs such as energy.
```

---

## 14. q014-C (multi_quarter)

**Question:** How did Azure growth change from Q2 to Q4 of Microsoft's fiscal 2026?

**Gold answer:** Azure and other cloud services grew 39% (Q2), 40% (Q3) and 43% (Q4).

**Answer:**

> Here is the change in Azure growth from Q2 to Q4 of Microsoft's fiscal 2026:
> 
> **FY2026 Q2**
> - Azure and other cloud services revenue grew 39% [1].
> 
> **FY2026 Q4**
> - Azure and other cloud services revenue grew 41% [6].
> 
> The growth rate increased from 39% in Q2 to 41% in Q4.

**Cited sources (2):**

[1] MSFT | FY2026 Q2 | FORM_10Q | section: Reportable Segments

```text
Productivity and Business Processes

Revenue increased $4.7 billion or 16%.

• Microsoft 365 Commercial products and cloud services revenue increased $3.4 billion or 16%. Microsoft 365 Commercial cloud revenue grew 17% with growth in revenue per user driven by Microsoft 365 E5 and Microsoft 365 Copilot. Microsoft 365 Commercial seats grew 6% driven by small and medium businesses and frontline worker offerings. Microsoft 365 Commercial products revenue grew 13% driven by Windows Commercial on-premises components of Microsoft 365 suite sales, as well as an increase in Office 2024 transactional purchasing.

• Microsoft 365 Consumer products and cloud services revenue increased $484 million or 27%. Microsoft 365 Consumer cloud revenue grew 29% driven by growth in revenue per user and Microsoft 365 Consumer subscriber growth of 6%.

• LinkedIn revenue increased $495 million or 11% driven by growth in Marketing Solutions.

• Dynamics products and cloud services revenue increased $291 million or 15% driven by growth in Dynamics 365. Dynamics 365 revenue grew 19% with growth across all workloads.

Operating income increased $3.7 billion or 22%.

• Cost of revenue increased $541 million or 10% driven by growth in Microsoft 365 Commercial cloud.

• Gross margin increased $4.1 billion or 17% driven by growth in Microsoft 365 Commercial cloud. Gross margin percentage increased primarily driven by efficiency gains in Microsoft 365 Commercial cloud even with continued investments in AI infrastructure and growing AI product usage.

• Operating expenses increased $424 million or 6% driven by research and development investments in compute capacity and AI talent, as well as higher advertising expenses.

Revenue, gross margin, and operating income included a favorable foreign currency impact of 2%, 2%, and 3%, respectively.

Intelligent Cloud

Revenue increased $7.4 billion or 29%.

• Server products and cloud services revenue increased $7.2 billion or 31% driven by Azure and other cloud services. Azure and other cloud services revenue grew 39% driven by demand for our portfolio of services with continued growth across all workloads. Server products revenue increased 2% driven by demand for our hybrid solutions, with benefit from the launch of SQL Server 2025, and higher transactional purchasing.

• Enterprise and partner services revenue increased $146 million or 8% driven by growth in Enterprise Support Services.

Operating income increased $3.0 billion or 28%.

• Cost of revenue increased $4.2 billion or 44% driven by growth in Azure.

• Gross margin increased $3.2 billion or 20% driven by growth in Azure. Gross margin percentage decreased driven by the continued investments in AI infrastructure and sales mix shift to Azure, offset in part by efficiency gains in Azure.
```

[6] MSFT | FY2026 Q4 | PRESS_RELEASE | section: Microsoft Cloud and AI Strength Fuels Fourth Quarter Results

```text
REDMOND, Wash. — July 29, 2026 — Microsoft Corp. today announced the following results for the quarter ended June 30, 2026, as compared to the corresponding period of last fiscal year:

• Revenue was $90.0 billion and increased 18% (up 17% in constant currency)

• Operating income was $40.6 billion and increased 18%

• Net income was $35.8 billion and increased 31% on a GAAP basis, and was $35.3 billion and increased 22% on a non-GAAP basis

• Diluted earnings per share was $4.81 and increased 32% on a GAAP basis, and was $4.74 and increased 23% on a non-GAAP basis

• Non-GAAP results exclude the impact from investments in OpenAI, explained in the Non-GAAP Definition section below

Several discrete items impacted our financial results in the quarter when compared to our forward-looking guidance provided on April 29, 2026, resulting in a benefit of $0.27 on diluted earnings per share. These include a $3.2 billion gain from our investment in Anthropic and lower-than-expected expenses related to the Voluntary Retirement Program which were partially offset by severance expense and impairment charges in XBOX. When adjusting for these items, we exceeded expectations across revenue, operating income, and diluted earnings per share.

“We are advancing the frontier on the cost-to-outcome curve, ensuring every customer can turn tokens into business results,” said Satya Nadella, chairman and chief executive officer of Microsoft. “This year, Azure revenue surpassed $100 billion for the first time, and Microsoft 365 Copilot reached over 30 million paid seats, reflecting the confidence customers are placing in us to power their AI transformation.”

“We delivered a strong quarter to close out the fiscal year, highlighted by Microsoft Cloud revenue of $59.3 billion, up 27% year-over-year,” said Amy Hood, executive vice president and chief financial officer of Microsoft.

The following table adjusts for the impact from investments in OpenAI and reconciles our financial results reported in accordance with generally accepted accounting principles (GAAP) to non-GAAP financial results. Additional information regarding our non-GAAP definition is provided below. All growth comparisons relate to the corresponding period in the last fiscal year.
```

---

## 15. q015-C (multi_quarter)

**Question:** How did Google Cloud revenue and growth develop from Q4 2025 to Q3 2026?

**Gold answer:** $17.7B (+48%), $20.0B (+63%), $24.8B (+82%), $30.7B (+103%): growth accelerated every quarter.

**Answer:**

> Here is the development of Google Cloud revenue and growth from Q4 2025 to Q3 2026:
> 
> **FY2025 Q4**
> - Google Cloud revenue: $17.7 billion
> - Year-over-year growth: 48% [1].
> 
> **FY2026 Q1**
> - Google Cloud revenue: $20 billion
> - Year-over-year growth: 63% [2].
> 
> **FY2026 Q2**
> - Google Cloud revenue: $24.8 billion
> - Year-over-year growth: 82% [11].
> 
> **FY2026 Q3**
> - Google Cloud revenue: $30.7 billion
> - Year-over-year growth: 103% [20].
> 
> In summary, Google Cloud revenue increased from $17.7 billion in Q4 2025 to $30.7 billion in Q3 2026, showing significant growth each quarter.

**Cited sources (4):**

[1] GOOGL | FY2025 Q4 | TRANSCRIPT | section: 2025 Q4 Earnings Call

```text
Speaker: Anat Ashkenazi, SVP and CFO, Alphabet and Google
Operating income increased 16% to $35.9 billion, and operating margin was 31.6%. Both operating income and operating margin were negatively impacted by the $2.1 billion Waymo charge in the quarter.

Other income and expenses was $3.2 billion, primarily due to unrealized gains in our non-marketable equity securities portfolio.

Net income increased 30% to $34.5 billion, and earnings per share increased 31% to $2.82.

We generated record operating cash flow of $52.4 billion in the fourth quarter, and $164.7 billion for the full year. This translated into $24.6 billion of free cashflow in the fourth quarter and $73.3 billion for the full year. We ended the quarter with $126.8 billion in cash and marketable securities, and $46.5 billion in long-term debts.

Turning to segment results, Google Services revenues increased 14% to $95.5 billion, reflecting strong growth in Search and Subscriptions.

Google Search and Other advertising revenues increased by 17% to $63.1 billion, representing another strong quarter with continued growth across all major verticals, with the largest contribution from Retail.

YouTube advertising revenues increased 9% to $11.4 billion, driven by direct response advertising. Results were negatively affected from the lapping of the strong spend on U.S. election in the fourth quarter of 2024 that we've mentioned on previous earnings calls.

Network advertising revenues of $7.8 billion were down 2%.

Subscriptions, Platforms and Devices revenues increased 17% this quarter to $13.6 billion, due to strong growth in YouTube Subscriptions, particularly YouTube Music and Premium, and growth in Google One, which benefited from increased demand for AI plans.

Google Services operating income increased 22% to $40.1 billion, and operating margins was 41.9%.

The Google Cloud segment delivered outstanding results in the fourth quarter as the business continued to benefit from strong demand for our enterprise AI products.

Cloud revenue accelerated meaningfully and was up 48% to $17.7 billion. Revenues were driven by strong performance in GCP, which continued to grow at a rate that was much higher than Cloud's overall revenue growth rate. As Sundar noted, we're driving performance through strong growth and a win rate of new customers, signing larger customer commitments, and increasing spend with existing customers.

GCP's performance was driven by accelerating growth in enterprise AI products, which were generating billions in quarterly revenues. We had strong growth in both enterprise AI infrastructure driven by deployment of TPUs and GPUs, and enterprise AI solutions, which benefited from demand for our industry-leading models, including Gemini 3.
```

[2] GOOGL | FY2026 Q1 | TRANSCRIPT | section: 2026 Q1 Earnings Call

```text
Speaker: Anat Ashkenazi, SVP and CFO, Alphabet and Google
We ended the quarter with $126.8 billion in cash and marketable securities and $77.5 billion in long-term debt.

And as we announced today, our Board of Directors declared a 5% increase in the quarterly dividend.

Turning to segment results, Google Services revenues increased 16% to $89.6 billion, reflecting strong growth in Search and Subscriptions. Google Services revenues also benefited from a strong FX tailwind.

Google Search and other advertising revenues increased by 19% to $60.4 billion, driven by growth in the retail and financial services verticals.

YouTube advertising revenues increased 11% to $9.9 billion, driven by Direct Response advertising as well as Brand.

Network advertising revenues of $7 billion were down 4%.

Subscriptions, Platforms & Devices revenues increased 19% this quarter to $12.4 billion due to strong growth in both YouTube subscriptions, particularly in YouTube Music and Premium, and Google One subscriptions, which benefited from increased demand for AI plans.

Google Services operating income increased 24% to $40.6 billion, and operating margin was 45.3%.

The Google Cloud segment delivered outstanding results in the first quarter. Cloud revenues accelerated across all key areas and were up 63% to $20 billion. Revenue growth was driven by strong performance in GCP, which continued to grow at a rate that was much higher than Cloud's overall revenue growth rate.

The largest contributor to Cloud's growth this quarter was AI solutions, driven by strong demand for industry leading models, including Gemini 3. In addition, we had strong growth in AI infrastructure due to continued deployment of TPUs and GPUs; and core GCP continues to be a sizable contributor, driven by demand for infrastructure and other services such as cybersecurity and data analytics.

Workspace again delivered strong double digit revenue growth, driven by an increase in the number of seats and the average revenue per seat.

Cloud operating income was $6.6 billion, tripling year over year, and operating margin increased from 17.8% in the first quarter of last year to 32.9%.

Google Cloud's backlog nearly doubled sequentially, reaching $462 billion at the end of the first quarter. The increase was driven by strong demand for our enterprise AI offerings and the inclusion of TPU hardware sales that Sundar referenced earlier. The majority of the backlog is related to typical GCP contracts, and we expect to recognize just over 50% of the backlog as revenue over the next 24 months.

In Other Bets, revenues were $411 million, and operating loss was $2.1 billion.
```

[11] GOOGL | FY2026 Q2 | TRANSCRIPT | section: 2026 Q2 Earnings Call

```text
Speaker: Anat Ashkenazi, SVP and CFO, Alphabet and Google
We ended the quarter with $242.5 billion in cash and marketable securities, which includes $87.1 million of equitable marketable securities. Long-term debt was $98.2 billion.

And, as mentioned in our press release, our Board of Directors declared a quarterly cash dividend on our common stock of 22-cents per share, which is payable in September.

Turning to segment results. Google Services revenues increased 15% to $94.5 billion, reflecting strong growth in Search and Subscriptions.

Total advertising revenues were up 14% with growth across all major verticals. We experienced strong Ads growth related to the World Cup, particularly in YouTube Ads.

Google Search and other advertising revenues increased by 17% to $63.3 billion, with retail and finance driving the largest contributions.

YouTube advertising revenues increased 13% to $11.1 billion, driven by Direct Response advertising as well as Brand, with strength in the Living Room, as Philipp mentioned earlier.

Network advertising revenues of $7.3 billion were down 1%.

Subscriptions, Platforms & Devices revenues increased 15% this quarter to $12.9 billion due to strong growth in both YouTube subscriptions, particularly YouTube Music and Premium, and Google One, which was driven by demand for AI plans.

Google Services operating income increased 20% to $39.5 billion, and operating margin was 41.8%.

The Google Cloud segment delivered outstanding results in the second quarter, driven by our enterprise AI products and services. Cloud revenues were up 82% to $24.8 billion, driven primarily by GCP, which grew faster than Cloud overall. Core GCP, AI solutions, and AI infrastructure were all important drivers of growth.

We also began to recognize revenues from TPU system sales, which we delivered to customer data centers for the first time in Q2. Cloud revenue growth accelerated meaningfully, even after excluding the impact of TPU system sales.

Cloud operating income was $8.8 billion, more than tripling year-over-year, and operating margin increased from 20.7% in the second quarter last year to 35.6%.

Google Cloud's backlog increased by more than $50 billion sequentially, reaching $514 billion in the second quarter. The increase was driven by strong demand for our Enterprise AI offerings. The majority of the backlog is related to typical GCP contacts for our broad mix of customers, and we expect to recognize just over 50% of the total backlog as revenue over the next 24 months.

In Other Bets, revenues were $382 million, and operating loss was $1.8 billion as we continue to expand Waymo's business and invest in key Other Bets.
```

[20] GOOGL | FY2026 Q3 | PRESS_RELEASE

```text
Exhibit 99.1

Alphabet Announces Third Quarter 2026 Results

MOUNTAIN VIEW, Calif. – October 28, 2026 – Alphabet Inc. (NASDAQ: GOOG, GOOGL) today announced financial results for the quarter ended September 30, 2026.

•Consolidated Alphabet revenues increased 25% to $127.6 billion, reflecting strong performance across the business and our 13th consecutive quarter of double-digit revenue growth.

•Google Services revenues increased 11% to $96.3 billion, led by 12% growth in Google Search & other with the year-over-year Search query growth rate the highest it has been in the last five years.

•Google Cloud delivered record growth as revenues increased 103% to $30.7 billion, led by strong growth in Google Cloud Platform (GCP) across enterprise AI infrastructure and enterprise AI solutions, as well as core GCP services. TPU systems sales were also a meaningful contributor to revenue growth.

•Consolidated Alphabet operating income increased 30% and operating margin was 31.8%.

•Net income available to common stockholders was $26.2 billion and EPS was $2.12.

Sundar Pichai, CEO of Google and Alphabet, said:

“We had a strong quarter and are now on a half-trillion dollar revenue run rate. Our deep full stack approach is driving momentum across the business, with Alphabet revenues growing 25% year-over year.

Our AI investments are driving overall Search query growth, including commercial queries. In fact, our year-over-year query growth rate is the highest it’s been in the last five years.

Google Cloud revenues grew 103%, powered by huge demand for our AI infrastructure and AI solutions. We’re also seeing strong demand for our TPU systems across a wide range of customers.

Gemini 4 Argon and Carbon are now leading at the frontier. Overall, our first-party model APIs are now processing nearly 32 billion tokens per minute, more than four times what we processed a year ago.

On top of that, the Gemini app has reached one billion users—making it the fastest-growing product in Google’s history—and we surpassed 150 million YouTube Music and Premium subscribers globally, including trials.

We’re excited by the range of big opportunities ahead of us.”

Q3 2026 Financial Highlights (unaudited)

The following table summarizes our consolidated financial results for the quarters ended September 30, 2025 and 2026 (in millions, except for per share information and percentages).
```

---

## 16. q016-C (multi_quarter)

**Question:** What was Meta's revenue in each of the first three quarters of 2026?

**Gold answer:** Q1 $56.31B (+33%), Q2 $60.80B (+28%), Q3 $64.60B (+26%).

**Answer:**

> Here is Meta's revenue for each of the first three quarters of 2026:
> 
> **FY2026 Q1**: Total revenue was $56.31 billion, an increase of 33% compared to the first quarter of 2025 [10].
> 
> **FY2026 Q2**: Total revenue was $60.80 billion, an increase of 28% compared to the second quarter of 2025 [5].
> 
> **FY2026 Q3**: Total revenue was $64.60 billion, an increase of 26% compared to the third quarter of 2025 [6].

**Cited sources (3):**

[5] META | FY2026 Q2 | FORM_10Q | section: Executive Overview of Second Quarter Results

```text
Our mission is to build the future of human connection and the technology that makes it possible.

Our financial results and key Family metrics for the second quarter of 2026 are set forth below. Total revenue for the second quarter of 2026 was $60.80 billion, an increase of 28% compared to the second quarter of 2025, due to an increase in advertising revenue. Revenue on a constant currency basis would have increased 27% compared to the second quarter of 2025. Ad impressions delivered across our Family of Apps in the second quarter of 2026 increased 14% year-over-year, and our average price per ad in the second quarter of 2026 increased 12% year-over-year.

Income from operations for the second quarter of 2026 was $18.78 billion, a decrease of $1.67 billion, or 8%, compared to the second quarter of 2025, driven by higher costs and expenses. The increase in costs and expenses was primarily due to increases in employee compensation, including severance expenses; infrastructure expenses related to our data centers, technical infrastructure, and third-party cloud services; legal-related costs; and third-party AI token costs.
```

[6] META | FY2026 Q3 | FORM_10Q | section: Executive Overview of Third Quarter Results

```text
Our mission is to build the future of human connection and the technology that makes it possible.

Our financial results and key Family metrics for the third quarter of 2026 are set forth below. Total revenue for the third quarter of 2026 was $64.60 billion, an increase of 26% compared to the third quarter of 2025, due to an increase in advertising revenue. Ad impressions delivered across our Family of Apps in the third quarter of 2026 increased 10% year-over-year, and our average price per ad in the third quarter of 2026 increased 14% year-over-year.

Income from operations for the third quarter of 2026 was $12.99 billion, a decrease of $7.55 billion, or 37%, compared to the third quarter of 2025, driven by higher costs and expenses. The increase in costs and expenses was primarily due to increases in legal-related costs, including the $9.89 billion charge related to the August 2026 multistate attorneys general agreement; infrastructure expenses related to our data centers, technical infrastructure, and third-party cloud services; and third-party AI token costs.
```

[10] META | FY2026 Q1 | FORM_10Q | section: Executive Overview of First Quarter Results

```text
Our mission is to build the future of human connection and the technology that makes it possible.

Our financial results and key Family metrics for the first quarter of 2026 are set forth below. Total revenue for the first quarter of 2026 was $56.31 billion, an increase of 33% compared to the first quarter of 2025, due to an increase in advertising revenue. Revenue on a constant currency basis would have increased 29% compared to the first quarter of 2025. Ad impressions delivered across our Family of Apps in the first quarter of 2026 increased 19% year-over-year, and our average price per ad in the first quarter of 2026 increased 12% year-over-year.

Income from operations for the first quarter of 2026 was $22.87 billion, an increase of $5.32 billion, or 30%, compared to the first quarter of 2025, driven by an increase in advertising revenue, partially offset by an increase in costs and expenses. The increase in costs and expenses was mainly due to increases in infrastructure costs and employee compensation.
```

---

## 17. q017-C (multi_quarter)

**Question:** What was Microsoft's total revenue over the last three quarters?

**Gold answer:** Q3 FY2026 $82.9B (+18%), Q4 FY2026 $90.0B (+18%), Q1 FY2027 $92.3B (+19%).

**Answer:**

> Here is Microsoft's total revenue over the last three quarters organized by quarter:
> 
> **FY2026 Q3**: Total revenue was $82.9 billion [1][4].
> 
> **FY2026 Q4**: Total revenue was $90.0 billion [2][5].
> 
> **FY2027 Q1**: Total revenue was $92.3 billion [3][12].
> 
> To calculate the total revenue over these three quarters:
> 
> - FY2026 Q3: $82.9 billion
> - FY2026 Q4: $90.0 billion
> - FY2027 Q1: $92.3 billion
> 
> Total revenue over the last three quarters = $82.9 billion + $90.0 billion + $92.3 billion = **$265.2 billion**.

**Cited sources (6):**

[1] MSFT | FY2026 Q3 | PRESS_RELEASE | section: Business Highlights

```text
Microsoft Cloud revenue was $54.5 billion and increased 29% (up 25% in constant currency), and commercial remaining performance obligation increased 99% to $627 billion.

Revenue in Productivity and Business Processes was $35.0 billion and increased 17% (up 13% in constant currency), with the following business highlights:

• Microsoft 365 Commercial cloud revenue increased 19% (up 15% in constant currency)

• Microsoft 365 Consumer cloud revenue increased 33% (up 29% in constant currency)

• LinkedIn revenue increased 12% (up 9% in constant currency)

• Dynamics 365 revenue increased 22% (up 17% in constant currency)

Revenue in Intelligent Cloud was $34.7 billion and increased 30% (up 28% in constant currency), with the following business highlights:

• Azure and other cloud services revenue increased 40% (up 39% in constant currency)

Revenue in More Personal Computing was $13.2 billion and decreased 1% (down 3% in constant currency), with the following business highlights:

• Windows OEM and Devices revenue decreased 2% (down 3% in constant currency)

• Xbox content and services revenue decreased 5% (down 7% in constant currency)

• Search advertising revenue excluding traffic acquisition costs increased 12% (up 9% in constant currency)

Microsoft returned $10.2 billion to shareholders in the form of dividends and share repurchases in the third quarter of fiscal year 2026.
```

[2] MSFT | FY2026 Q4 | PRESS_RELEASE | section: Business Highlights

```text
Microsoft Cloud revenue was $59.3 billion and increased 27%, and commercial remaining performance obligation increased 84% to $678 billion.

Revenue in Productivity and Business Processes was $37.8 billion and increased 14%, with the following business highlights:

• Microsoft 365 Commercial cloud revenue increased 16% when adjusted for the prior year comparable that benefited from 2 points of in-period revenue recognition. On a reported basis, Microsoft 365 Commercial cloud revenue increased 14%.

• Microsoft 365 Consumer cloud revenue increased 24% (up 22% in constant currency)

• LinkedIn revenue increased 12% (up 10% in constant currency)

• Dynamics 365 revenue increased 13% (up 12% in constant currency)

Revenue in Intelligent Cloud was $39.3 billion and increased 32% (up 31% in constant currency), with the following business highlights:

• Azure and other cloud services revenue increased 43%

Revenue in More Personal Computing was $12.9 billion and decreased 4% (down 5% in constant currency), with the following business highlights:

• Windows OEM and Devices revenue decreased 7%

• XBOX content and services revenue decreased 10%

• Search advertising revenue excluding traffic acquisition costs increased 10% (up 9% in constant currency)

Microsoft returned $10.2 billion to shareholders in the form of dividends and share repurchases in the fourth quarter of fiscal year 2026.
```

[3] MSFT | FY2027 Q1 | FORM_10Q | section: Reportable Segments

```text
Agents and Infra

Revenue increased $15.1 billion or 25%.

• Azure revenue increased $10.8 billion or 48% driven by demand for services across the platform with continued growth across all workloads.

• Microsoft 365 cloud revenue increased $4.1 billion or 17% driven by Microsoft 365 commercial cloud. Microsoft 365 commercial cloud revenue increased 17% driven by higher revenue per user, reflecting continued upsell to premium seat offerings, including Microsoft 365 Copilot, E5, and E7, as well as increased consumption of usage-based services, primarily in GitHub Copilot. Microsoft 365 commercial seats grew 5% driven by installed base expansion across all customer segments, primarily small and medium business and frontline worker offerings. Microsoft 365 consumer cloud revenue increased 17% driven by growth in revenue per user and Microsoft 365 consumer subscriber growth of 7%.

• Productivity and server licensing revenue decreased $271 million or 3% driven by a decrease in transactional purchasing as we lap the benefit from product launches in the prior year, offset in part by a greater mix of contracts that carry higher in-period revenue recognition.

• Industry solutions revenue increased $443 million or 9% driven by growth in Industry solutions cloud. Industry solutions cloud revenue increased 10% with growth in Dynamics 365 and LinkedIn Talent Solutions.

• Frontier and support services revenue increased $86 million or 4% driven by growth in Customer Success Support (formerly Enterprise Support Services).

Operating income increased $7.8 billion or 24%. Operating income percentage decreased slightly as strong revenue growth, platform efficiency gains, and focused investments across our portfolio were more than offset by sales mix shift to Azure.

• Cost of revenue increased $6.2 billion or 35% driven by investments in datacenter infrastructure to support growing customer demand across Azure and Microsoft 365 cloud.

• Gross margin increased $8.9 billion or 20%.

• Operating expenses increased $1.1 billion or 10% driven by continued investments in research and development compute capacity and talent.

Devices and Consumer

Revenue decreased $517 million or 3%.

• Search and advertising revenue increased $612 million or 10%. Search and advertising revenue excluding traffic acquisition costs increased 12% driven by higher rate and volume across LinkedIn, Edge, and Bing.

• XBOX revenue decreased $129 million or 2% driven by declines in XBOX content and services and XBOX hardware. XBOX content and services revenue decreased 2% driven by declines in third-party content and XBOX Game Pass, offset in part by growth in first-party content. XBOX hardware revenue declined 14% driven by lower volume of consoles sold.
```

[4] MSFT | FY2026 Q3 | FORM_10Q | section: OVERVIEW

```text
Microsoft is a technology company committed to making digital technology and artificial intelligence (“AI”) available broadly and doing so responsibly, with a mission to empower every person and every organization on the planet to achieve more. We create platforms and tools, powered by AI, that deliver innovative solutions that meet the evolving needs of our customers.

We generate revenue by offering a wide range of cloud-based solutions, content, and other services to people and businesses; licensing and supporting an array of software products; delivering relevant online advertising to a global audience; and designing and selling devices. Our most significant expenses are related to compensating employees; supporting and investing in our cloud-based services, including datacenter operations; designing, manufacturing, marketing, and selling our other products and services; and income taxes.

Highlights from the third quarter of fiscal year 2026 compared with the third quarter of fiscal year 2025 included:

• Microsoft Cloud revenue increased 29% to $54.5 billion.

• Commercial remaining performance obligation increased 99% to $627 billion.

• Microsoft 365 Commercial cloud revenue increased 19%.

• Microsoft 365 Consumer cloud revenue increased 33%.

• LinkedIn revenue increased 12%.

• Dynamics 365 revenue increased 22%.

• Azure and other cloud services revenue increased 40%.

• Windows OEM and Devices revenue decreased 2%.

• Xbox content and services revenue decreased 5%.

• Search advertising (formerly Search and news advertising) revenue excluding traffic acquisition costs increased 12%.
```

[5] MSFT | FY2026 Q4 | PRESS_RELEASE | section: Microsoft Cloud and AI Strength Fuels Fourth Quarter Results

```text
REDMOND, Wash. — July 29, 2026 — Microsoft Corp. today announced the following results for the quarter ended June 30, 2026, as compared to the corresponding period of last fiscal year:

• Revenue was $90.0 billion and increased 18% (up 17% in constant currency)

• Operating income was $40.6 billion and increased 18%

• Net income was $35.8 billion and increased 31% on a GAAP basis, and was $35.3 billion and increased 22% on a non-GAAP basis

• Diluted earnings per share was $4.81 and increased 32% on a GAAP basis, and was $4.74 and increased 23% on a non-GAAP basis

• Non-GAAP results exclude the impact from investments in OpenAI, explained in the Non-GAAP Definition section below

Several discrete items impacted our financial results in the quarter when compared to our forward-looking guidance provided on April 29, 2026, resulting in a benefit of $0.27 on diluted earnings per share. These include a $3.2 billion gain from our investment in Anthropic and lower-than-expected expenses related to the Voluntary Retirement Program which were partially offset by severance expense and impairment charges in XBOX. When adjusting for these items, we exceeded expectations across revenue, operating income, and diluted earnings per share.

“We are advancing the frontier on the cost-to-outcome curve, ensuring every customer can turn tokens into business results,” said Satya Nadella, chairman and chief executive officer of Microsoft. “This year, Azure revenue surpassed $100 billion for the first time, and Microsoft 365 Copilot reached over 30 million paid seats, reflecting the confidence customers are placing in us to power their AI transformation.”

“We delivered a strong quarter to close out the fiscal year, highlighted by Microsoft Cloud revenue of $59.3 billion, up 27% year-over-year,” said Amy Hood, executive vice president and chief financial officer of Microsoft.

The following table adjusts for the impact from investments in OpenAI and reconciles our financial results reported in accordance with generally accepted accounting principles (GAAP) to non-GAAP financial results. Additional information regarding our non-GAAP definition is provided below. All growth comparisons relate to the corresponding period in the last fiscal year.
```

[12] MSFT | FY2027 Q1 | PRESS_RELEASE | section: Microsoft Announces First Quarter Results

```text
REDMOND, Wash. — October 28, 2026 — Microsoft Corp. today announced the following results for the quarter ended September 30, 2026, as compared to the corresponding period of last fiscal year:

• Revenue was $92.3 billion and increased 19%

• Operating income was $45.9 billion and increased 21% (up 22% in constant currency)

• Net income was $36.6 billion and increased 32% on a GAAP basis, and was $36.4 billion and increased 18% (up 19% in constant currency) on a non-GAAP basis

• Diluted earnings per share was $4.92 and increased 32% on a GAAP basis, and was $4.89 and increased 18% (up 19% in constant currency) on a non-GAAP basis

• Non-GAAP results exclude the impact from investments in OpenAI, explained in the Non-GAAP Definition section below

“We are delivering greater capability, choice, control, and cost efficiency for customers across our entire stack,” said Satya Nadella, chairman and chief executive officer of Microsoft. “By helping organizations build and own their own intelligence, and earning trust through agent safety and security, we are translating our innovation into growing adoption and real-world impact across every industry.”

“We delivered a strong start to the fiscal year with Microsoft Cloud revenue of $63.9 billion, with growth accelerating to 30% (up 31% in constant currency) year-over-year,” said Amy Hood, executive vice president and chief financial officer of Microsoft. “Results exceeded expectations across revenue, operating income, and earnings per share.”

The following table adjusts for the impact from investments in OpenAI and reconciles our financial results reported in accordance with generally accepted accounting principles (GAAP) to non-GAAP financial results. Additional information regarding our non-GAAP definition is provided below. All growth comparisons relate to the corresponding period in the last fiscal year.
```

---

## 18. q018-C (multi_quarter)

**Question:** How much did Meta spend on capital expenditures in each quarter of 2026 so far?

**Gold answer:** Q1 $19.84B, Q2 $31.08B, Q3 $34.91B (including principal payments on finance leases).

**Answer:**

> Here are the capital expenditures for Meta in each quarter of 2026:
> 
> - **FY2026 Q1**: Capital expenditures were $19.84 billion [7].
> - **FY2026 Q2**: Capital expenditures were not explicitly stated, but the full year 2026 capital expenditures were expected to be in the range of $125-145 billion [1]. 
> - **FY2026 Q3**: Capital expenditures were anticipated to be in the range of $130-145 billion, narrowed from the prior outlook of $125-145 billion [14].
> 
> Thus, the specific capital expenditures for Q2 are not provided, leading to an incomplete answer for that quarter. 
> 
> Overall, the figures for Q1 and Q3 are:
> - Q1: $19.84 billion
> - Q3: Range of $130-145 billion (specific figure not provided) 
> 
> For Q2, the information is insufficient to provide a specific figure. 
> 
> Therefore, the answer is:
> - **FY2026 Q1**: $19.84 billion
> - **FY2026 Q2**: INSUFFICIENT_EVIDENCE
> - **FY2026 Q3**: Range of $130-145 billion

**Cited sources (3):**

[1] META | FY2026 Q1 | TRANSCRIPT | p.7-8

```text
Speaker: Susan Li, CFO
Last month, we rolled out our affiliate partnerships offering on Facebook to more test partners, so creators can tag products from participating retailers on their posts and earn a commission when someone makes a purchase. We have also started testing similar experiences on Instagram.

We see a real opportunity to help people more easily discover and buy products within our services, particularly as we incorporate AI deeply across our platforms.

Next, I would like to discuss our approach to capital allocation.

Compute is becoming increasingly important as it determines the quality of the services we can provide, including powering more capable models and delivering innovative new products. It is also becoming more critical to how we work at Meta, as we are entering a world where employees are managing agents to help them generate new ideas, run experiments, execute tasks, and build products.

We are investing aggressively to meet our infrastructure needs and ensure we maximize our strategic flexibility over the coming years. This includes substantially expanding our own data center footprint and striking deals throughout the supply chain to secure necessary components for future capacity. We are also signing cloud deals that will come online over the course of this year and 2027, allowing us to scale more quickly. These multi-year cloud deals and our

infrastructure purchase agreements drove a $107 billion step up in our contractual commitments this quarter.

Our investments will support our training needs for future models, and most importantly, provide us the inference capacity necessary to deliver personal and business agents to billions of people around the world, along with several other AI product experiences we’re developing.

As we grow our infrastructure spend, we remain committed to operating efficiently, and we recently shared internally that we plan to reduce the size of our employee base in May. We believe a leaner operating model will allow us to move more quickly while also helping to offset the substantial investments we’re making.

Moving to our financial outlook.

We expect second quarter 2026 total revenue to be in the range of $58-61 billion. Our guidance assumes foreign currency is an approximately 2% tailwind to year-over-year total revenue growth, based on current exchange rates.

Turning to the expense and capex outlooks.

We expect full year 2026 total expenses to be in the range of $162-169 billion, unchanged from our prior outlook.

We continue to expect to deliver operating income this year that is above 2025 operating income.

We anticipate 2026 capital expenditures, including principal payments on finance leases, to be in the range of $125-145 billion, increased from our prior range of $115-135 billion. This reflects our expectations for higher component pricing this year and, to a lesser extent, additional data center costs to support future year capacity.

Absent any changes to our tax landscape, we expect our tax rate for the remaining quarters of 2026 to be between 13-16%.
```

[7] META | FY2026 Q1 | FORM_10Q | section: Liquidity and Capital Resources

```text
Our principal sources of liquidity are our cash, cash equivalents, marketable securities, and cash generated from operations. Cash, cash equivalents, and marketable securities are comprised of cash on deposit with banks, time deposits, money market funds, U.S. government and agency securities, investment grade corporate debt securities, and marketable equity securities. As part of our cash management strategy, we concentrate cash deposits with large financial institutions and our investment holdings are in diversified highly rated securities. Cash, cash equivalents, and marketable securities were $81.18 billion as of March 31, 2026, a decrease of $412 million from December 31, 2025. The decrease was primarily due to $19.84 billion of capital expenditures, which includes purchases of property and equipment and principal payments on finance leases; $4.42 billion of taxes paid related to net share settlement of employee restricted stock unit (RSU) awards; $1.57 billion of net unrealized losses on marketable equity securities; $1.35 billion of payments of dividends and dividend equivalents; and $372 million of acquired intangible assets. These decreases were offset by $32.23 billion of cash generated from operations.

In addition, during the quarter ended March 31, 2026, we reclassified $5.00 billion of unrestricted money market funds to restricted cash equivalents related to the terms of a multi-year purchase agreement. These funds are restricted from general corporate use and are expected to be released upon satisfying the underlying purchase obligations. For additional information regarding restricted cash equivalents, see Note 4 — Financial Instruments.

The following table presents our cash flows (in millions):

Three Months Ended March 31,
2026 | 2025
Net cash provided by operating activities | $32,226 | $24,026
Net cash used in investing activities | $(33,678) | $(20,010)
Net cash used in financing activities | $(6,553) | $(19,495)
```

[14] META | FY2026 Q2 | PRESS_RELEASE | section: CFO Outlook Commentary

```text
We expect third quarter 2026 total revenue to be in the range of $61-64 billion. Our guidance assumes foreign currency is an approximately 1% headwind to year-over-year total revenue growth, based on current exchange rates.

We are raising the lower-end of our expense outlook to incorporate the $2.4 billion charges related to legal proceedings recognized in the second quarter. We now expect full year 2026 total expenses to be in the range of $165-169 billion.

We continue to expect to deliver operating income this year that is above 2025 operating income.

We anticipate 2026 capital expenditures, including principal payments on finance leases, to be in the range of $130-145 billion, narrowed from our prior outlook of $125-145 billion.

Absent any changes to our tax landscape, we expect our tax rate for the remaining quarters of 2026 to be between 15-17%, an increase from our prior outlook of 13-16%.

Finally, we continue to monitor active legal and regulatory matters that could significantly impact our business and financial results. For example, we continue to see scrutiny on youth-related issues in several markets and have a number of youth-related trials scheduled for this year in the U.S., which may ultimately result in a material loss.
```

---

## 19. q019-C (multi_quarter)

**Question:** What did Microsoft's CFO say about upcoming capital expenditures on the last three earnings calls?

**Gold answer:** Q3 FY2026: next-quarter CapEx to rise to over $40B. Q4 FY2026: FY27 CapEx to grow year over year. Q1 FY2027: Q2 CapEx over $50B.

**Answer:**

> **FY2026 Q3**: Amy Hood mentioned that capital expenditures were expected to continue supporting growth in cloud offerings and investments in AI infrastructure and training. She noted that operating expenses increased due to continued investments in research and development compute capacity, AI talent, and data to support product development across the portfolio [1][4][7].
> 
> **FY2026 Q4**: Amy Hood stated that capital expenditures for the quarter were $41 billion, with a significant portion allocated to short-lived assets, primarily CPUs and GPUs. She also indicated that the company expected to continue investing in capital expenditures to support growth in cloud offerings and AI infrastructure [2][5][8].
> 
> **FY2027 Q1**: Amy Hood reported that capital expenditures were $45.2 billion for the quarter, with a focus on short-lived assets, including CPUs and GPUs. She reiterated the company's commitment to investing in infrastructure required for customers to build and scale AI applications [3][9].

**Cited sources (8):**

[1] MSFT | FY2026 Q3 | FORM_10Q | section: Other Planned Uses of Capital

```text
We will continue to invest in sales, marketing, product support infrastructure, and existing and advanced areas of technology, as well as acquisitions that align with our business strategy. Additions to property and equipment will continue, including new facilities, datacenters, and computer systems for research and development, sales and marketing, support, and administrative staff. We will continue to invest in capital expenditures to support growth in our cloud offerings and our investments in AI infrastructure and training. We have operating and finance leases for datacenters, corporate offices, research and development facilities, and certain equipment. We have not engaged in any related party transactions or arrangements with unconsolidated entities or other persons that are reasonably likely to materially affect liquidity or the availability of capital resources.
```

[2] MSFT | FY2026 Q4 | PRESS_RELEASE | section: Microsoft Cloud and AI Strength Fuels Fourth Quarter Results

```text
REDMOND, Wash. — July 29, 2026 — Microsoft Corp. today announced the following results for the quarter ended June 30, 2026, as compared to the corresponding period of last fiscal year:

• Revenue was $90.0 billion and increased 18% (up 17% in constant currency)

• Operating income was $40.6 billion and increased 18%

• Net income was $35.8 billion and increased 31% on a GAAP basis, and was $35.3 billion and increased 22% on a non-GAAP basis

• Diluted earnings per share was $4.81 and increased 32% on a GAAP basis, and was $4.74 and increased 23% on a non-GAAP basis

• Non-GAAP results exclude the impact from investments in OpenAI, explained in the Non-GAAP Definition section below

Several discrete items impacted our financial results in the quarter when compared to our forward-looking guidance provided on April 29, 2026, resulting in a benefit of $0.27 on diluted earnings per share. These include a $3.2 billion gain from our investment in Anthropic and lower-than-expected expenses related to the Voluntary Retirement Program which were partially offset by severance expense and impairment charges in XBOX. When adjusting for these items, we exceeded expectations across revenue, operating income, and diluted earnings per share.

“We are advancing the frontier on the cost-to-outcome curve, ensuring every customer can turn tokens into business results,” said Satya Nadella, chairman and chief executive officer of Microsoft. “This year, Azure revenue surpassed $100 billion for the first time, and Microsoft 365 Copilot reached over 30 million paid seats, reflecting the confidence customers are placing in us to power their AI transformation.”

“We delivered a strong quarter to close out the fiscal year, highlighted by Microsoft Cloud revenue of $59.3 billion, up 27% year-over-year,” said Amy Hood, executive vice president and chief financial officer of Microsoft.

The following table adjusts for the impact from investments in OpenAI and reconciles our financial results reported in accordance with generally accepted accounting principles (GAAP) to non-GAAP financial results. Additional information regarding our non-GAAP definition is provided below. All growth comparisons relate to the corresponding period in the last fiscal year.
```

[3] MSFT | FY2027 Q1 | PRESS_RELEASE | section: Microsoft Announces First Quarter Results

```text
REDMOND, Wash. — October 28, 2026 — Microsoft Corp. today announced the following results for the quarter ended September 30, 2026, as compared to the corresponding period of last fiscal year:

• Revenue was $92.3 billion and increased 19%

• Operating income was $45.9 billion and increased 21% (up 22% in constant currency)

• Net income was $36.6 billion and increased 32% on a GAAP basis, and was $36.4 billion and increased 18% (up 19% in constant currency) on a non-GAAP basis

• Diluted earnings per share was $4.92 and increased 32% on a GAAP basis, and was $4.89 and increased 18% (up 19% in constant currency) on a non-GAAP basis

• Non-GAAP results exclude the impact from investments in OpenAI, explained in the Non-GAAP Definition section below

“We are delivering greater capability, choice, control, and cost efficiency for customers across our entire stack,” said Satya Nadella, chairman and chief executive officer of Microsoft. “By helping organizations build and own their own intelligence, and earning trust through agent safety and security, we are translating our innovation into growing adoption and real-world impact across every industry.”

“We delivered a strong start to the fiscal year with Microsoft Cloud revenue of $63.9 billion, with growth accelerating to 30% (up 31% in constant currency) year-over-year,” said Amy Hood, executive vice president and chief financial officer of Microsoft. “Results exceeded expectations across revenue, operating income, and earnings per share.”

The following table adjusts for the impact from investments in OpenAI and reconciles our financial results reported in accordance with generally accepted accounting principles (GAAP) to non-GAAP financial results. Additional information regarding our non-GAAP definition is provided below. All growth comparisons relate to the corresponding period in the last fiscal year.
```

[4] MSFT | FY2026 Q3 | FORM_10Q | section: Reportable Segments

```text
• Operating expenses increased $259 million or 7% driven by impairment and other related expenses in our Gaming business and continued investments in research and development compute capacity, AI talent, and data to support product development that benefits the entire portfolio.

Revenue, gross margin, and operating income included a favorable foreign currency impact of 2%, 2%, and 3%, respectively.

Productivity and Business Processes

Revenue increased $14.5 billion or 16%.

• Microsoft 365 Commercial products and cloud services revenue increased $10.6 billion or 17%. Microsoft 365 Commercial cloud revenue grew 18% with growth in revenue per user driven by Microsoft 365 E5 and Microsoft 365 Copilot, and continued Microsoft 365 Commercial seat growth. Microsoft 365 Commercial products revenue grew 10% driven by an increase in Office 2024 transactional purchasing, as well as growth in the Windows Commercial on-premises components of Microsoft 365 suite sales.

• Microsoft 365 Consumer products and cloud services revenue increased $1.4 billion or 27%. Microsoft 365 Consumer cloud revenue grew 29% driven by growth in revenue per user and continued growth in Microsoft 365 Consumer subscribers.

• LinkedIn revenue increased $1.4 billion or 11% with growth across all lines of business.

• Dynamics products and cloud services revenue increased $941 million or 17% driven by growth in Dynamics 365. Dynamics 365 revenue grew 20% with growth across all workloads.

Operating income increased $11.2 billion or 22%.

• Cost of revenue increased $1.6 billion or 10% driven by investments in AI infrastructure to support Microsoft 365 Copilot seat and usage growth.

• Gross margin increased $12.8 billion or 18% driven by growth in Microsoft 365 Commercial cloud. Gross margin percentage increased driven by efficiency gains in Microsoft 365 Commercial cloud even with continued investments in AI infrastructure and growing AI product usage.

• Operating expenses increased $1.6 billion or 8% driven by continued investments in research and development compute capacity, AI talent, and data to support product development that benefits the entire portfolio, as well as higher Copilot advertising expenses.

Revenue, gross margin, and operating income included a favorable foreign currency impact of 2%, 3%, and 4%, respectively.

Intelligent Cloud

Revenue increased $22.1 billion or 29%.

• Server products and cloud services revenue increased $21.8 billion or 31% driven by Azure and other cloud services. Azure and other cloud services revenue grew 40% driven by demand for services across the platform with continued growth across all workloads. Server products revenue increased 1% primarily driven by higher purchases of licenses running in multi-cloud environments.

• Enterprise and partner services revenue increased $381 million or 7% driven by growth in Enterprise Support Services.
```

[5] MSFT | FY2026 Q4 | TRANSCRIPT

```text
Speaker: AMY HOOD
Thank you, Satya, and good afternoon everyone. This fiscal year, we delivered over $331 billion in revenue, with growth accelerating to 18%, driven by strong demand across both the Azure platform and our first-party AI applications and services. Operating income growth outpaced revenue growth, increasing 21% to more than $155 billion as we invested in long-term growth while continuing to expand operating leverage.

This quarter, revenue was $90 billion, up 18% and 17% in constant currency. Gross margin dollars increased 15% and operating income increased 18%. Earnings per share was $4.74, an increase of 23%, when adjusted for the impact from our investment in OpenAI. And FX was roughly in line with guidance.

Several discrete items impacted our financial results in the quarter when compared to our forward-looking guidance provided on our April earnings call, resulting in a benefit of 27 cents on diluted earnings per share.

These include a $3.2 billion gain from our investment in Anthropic and lower-than-expected expenses related to the Voluntary Retirement Program, which were partially offset by severance expense and impairment charges in XBOX.

When adjusting for these items, we exceeded expectations across revenue, operating income and earnings per share due to strong demand and execution in the quarter.

Company gross margin percentage was 67%, down year-over-year, driven by sales mix shift to Azure as well as continued investments in AI infrastructure and growing product usage, partially offset by ongoing efficiency gains, particularly in Azure and M365 Commercial cloud.

Operating expenses increased 10% driven by continued investment in R&D compute capacity, talent, and data to support product development across the portfolio. G and A growth was impacted by a low prior-year comparable as well as some of the discrete items mentioned earlier. Operating margins increased slightly year-over-year to 45%.

Total company headcount declined 2% year-over-year.

When adjusted for the impact of our investments in OpenAI, other income and expense was $2.8 billion driven by the gain on investment in Anthropic noted earlier.

Capital expenditures were $41 billion including the impact from higher component pricing as noted in our guide. Roughly two thirds of our capex was for short-lived assets, primarily CPUs and GPUs as customers increasingly build solutions that leverage both AI and non-AI infrastructure.

The remaining spend was for long-lived assets. This quarter, total finance leases were $5.6 billion and were primarily for large datacenter sites. And cash paid for P, P, and E was $35.8 billion.

Cash flow from operations was $55.4 billion, up 30% driven by strong cloud billings and collections, partially offset by an increase in operating lease payments. And free cash flow was $19.6 billion reflecting higher capital expenditures.
```

[7] MSFT | FY2026 Q3 | PRESS_RELEASE | section: Microsoft Cloud and AI Strength Fuels Third Quarter Results

```text
REDMOND, Wash. — April 29, 2026 — Microsoft Corp. today announced the following results for the quarter ended March 31, 2026, as compared to the corresponding period of last fiscal year:

• Revenue was $82.9 billion and increased 18% (up 15% in constant currency)

• Operating income was $38.4 billion and increased 20% (up 16% in constant currency)

• Net income was $31.8 billion and increased 23% on a GAAP basis, and increased 20% (up 18% in constant currency) on a non-GAAP basis

• Diluted earnings per share was $4.27 and increased 23% on a GAAP basis, and increased 21% (up 18% in constant currency) on a non-GAAP basis

• Non-GAAP results exclude the impact from investments in OpenAI, explained in the Non-GAAP Definition section below

“We are focused on delivering cloud and AI infrastructure and solutions that empower every business to eval-max their outcomes in the agentic computing era,” said Satya Nadella, chairman and chief executive officer of Microsoft. “Our AI business surpassed an annual revenue run rate of $37 billion, up 123% year-over-year.”

“We delivered results that exceeded expectations across revenue, operating income, and earnings per share, reflecting strong execution and growing demand for the Microsoft Cloud,” said Amy Hood, executive vice president and chief financial officer of Microsoft.

The following table reconciles our financial results reported in accordance with generally accepted accounting principles (GAAP) to non-GAAP financial results. Additional information regarding our non-GAAP definition is provided below. All growth comparisons relate to the corresponding period in the last fiscal year.

Three Months Ended March 31,
2026 | 2025 | Percentage Change Y/Y
($ in millions, except per share amounts) | As Reported (GAAP) | Adjustment* | As Adjusted (non-GAAP) | As Reported (GAAP) | Adjustment* | As Adjusted (non-GAAP) | GAAP | Constant Currency | Non-GAAP | Non-GAAP Constant Currency
Net Income | $31,778 | $14 | $31,792 | $25,824 | $583 | $26,407 | 23% | 20% | 20% | 18%
Diluted Earnings per Share | $4.27 | $0.00 | $4.27 | $3.46 | $0.08 | $3.54 | 23% | 21% | 21% | 18%

*Adjustment is the impact from investments in OpenAI
```

[8] MSFT | FY2026 Q4 | TRANSCRIPT

```text
Speaker: AMY HOOD
Operating income decreased 14% and 15% in constant currency and operating margins decreased year-over-year to 21%.

Now, before I move to outlook, effective at the start of FY27, we are extending the estimated useful lives of our datacenters and office buildings, from 15 to 25 years, reflecting our operating history and expected use of these assets. The impact of this update is reflected in today's guidance.

This change affects only the timing of future depreciation and is expected to have a minimal benefit to FY27 operating income. The greater impact is on capital expenditures as more of our future datacenter leases will shift from finance leases to operating leases as a result of this update. Finance leases are included in capital expenditures while operating leases are not. Outside of this useful life impact, our calendar year 2026 CapEx investment expectations remain unchanged. However, the shift from finance to operating leases adjusts our expectation to approximately $175 billion.

Now, moving to our outlook.

Let me start with some full year commentary for FY27.

First some reminders. In both the M365 Commercial products and Server products KPIs, we are lapping higher transactional purchasing from the timing of product launches and expect revenue from both to decline in the mid-single digits for the full fiscal year.

Growth in Windows OEM and Devices will be impacted by lower PC market demand as higher component costs increase device pricing, a prior-year comparable that benefited from Windows 10 end-of-support, and elevated inventory levels. As a result, we expect revenue to decline in the high-teens for the fiscal year.

Moving to FX. Assuming current rates remain stable, we now expect FX to decrease full-year fiscal revenue growth by less than 1 point with no meaningful impact to COGS and operating expense growth.

At the company level, with strong commercial momentum, we continue to expect another fiscal year of double-digit revenue and operating income growth. Operating expenses should grow in the mid to high-single digits, reflecting continued investment in R&D compute capacity, talent, and data. And we expect FY27 capital expenditures will grow year-over-year given demand signals across our portfolio.

Even as we invest to meet growing demand, full fiscal year operating margins should be down less than a point. In addition, we expect to remain free cash flow positive in FY27.

And finally, we expect our FY27 effective tax rate to be approximately 20%.

Now, to the outlook for our first quarter, which unless specifically noted otherwise, is on a US dollar basis.

Based on current rates, we expect FX to decrease total revenue growth by less than 1 point with no meaningful impact to COGS or operating expense growth. Within the segments, we expect FX to decrease revenue growth in Productivity and Business Processes by roughly 1 point and Intelligent Cloud by less than 1 point. There is no meaningful impact in More Personal Computing.
```

[9] MSFT | FY2027 Q1 | TRANSCRIPT

```text
Speaker: AMY HOOD
Thank you, Satya, and good afternoon everyone. Our results exceeded expectations across revenue, operating income, and earnings per share driven by strong demand across the Microsoft Cloud.

This quarter, revenue was $92.3 billion, up 19%. Gross margin dollars increased 17% and gross margin percentage was 68%. Operating income increased 21% and 22% in constant currency. Earnings per share was $4.89, an increase of 18% and 19% in constant currency, when adjusted for the impact from our investment in OpenAI. And FX was roughly in line with guidance.

Operating expenses increased 6%, driven by continued investments in R&D compute capacity and talent. Operating margins increased slightly year-over-year to 50%, driven by strong revenue growth, platform efficiency gains, and focused investments across our portfolio, partially offset by sales mix shift to Azure.

When adjusted for the impact from our investments in OpenAI, other income and expense was negative $234 million, slightly lower than expected.

Capital expenditures were $45.2 billion, as some third-party capacity deliveries we expected in Q1 were delayed. Roughly three quarters of our capex was for short-lived assets, primarily CPUs and GPUs, including higher memory and storage costs, as we continue to invest in the infrastructure required for our customers to build and scale AI applications.

The remaining spend was for long-lived assets. This quarter, finance leases were $7.4 billion. As a reminder, we recognize the full principal value of finance leases in capex at the time of lease commencement. And cash paid for P, P, and E was $39.4 billion.

Cash flow from operations was $58 billion, up 29% driven by strong cloud billings and collections. And free cash flow was $18.6 billion reflecting higher capital expenditures.

And finally, we returned $10.2 billion to shareholders through dividends and share repurchases.

Now, to our segment results, reported under the updated segment structure announced in September.

Revenue from Agents and Infra was $76.8 billion and grew 25%, reflecting strength across the connected layers of our platform.

Commercial bookings decreased 12% and 11% in constant currency driven by the impact from large Azure commitments in the prior year, partially offset by healthy growth in our commercial sales motions including a large Azure commitment this quarter.

Commercial remaining performance obligation grew 73% to $677 billion, with a smaller sequential decline relative to historical seasonality due to the large Azure commitment this quarter.

Our RPO has a weighted average duration of 2.3 years. And roughly 30% will be recognized in revenue in the next 12 months, up 29% year-over-year. The remaining portion recognized beyond the next 12 months increased 98%.
```

---

## 20. q021-C (qualitative)

**Question:** What is Muse and how did Meta describe its launch in Q3 2026?

**Gold answer:** Muse is Meta's personal agent, the product form of its personal superintelligence vision; one of its most successful launches ever.

**Answer:**

> **FY2026 Q3**
> 
> In Q3 2026, Meta launched Muse, a personal agent designed to deliver personal superintelligence to users. The launch was described as one of their most successful, with over 10 million people in the U.S. and Canada setting up Muse personal agents within the first six weeks. Engagement and retention rates were reported to be strong, with about 25% of users engaging with Muse daily, which is more than double the initial engagement levels seen with other AI apps like Threads [4][9].
> 
> The most common use cases for Muse included shopping, personal productivity, and creativity, with over 25% of weekly active users engaging in shopping activities. The average purchase size for shopping through Muse was reported to be over $200 [2][4][10]. 
> 
> Meta's CEO, Mark Zuckerberg, emphasized the importance of building a large-scale Muse network, which they believe will enhance the value of the service as more users and businesses participate [16].

**Cited sources (5):**

[2] META | FY2026 Q3 | TRANSCRIPT | p.7-9

```text
Speaker: Susan Li, CFO
Meta One is rolling out gradually with more than 50

features across Instagram, Facebook, and WhatsApp, including Meta Business Agent and Meta AI, and has surpassed 15 million subscriptions and trials to date. The core experience across our apps and Meta AI will remain free, while subscriptions offer more specialized capabilities and expanded AI usage.

Beyond monetizing engagement across our existing services, we are building new experiences and revenue streams around consumer agents.

Mark highlighted the impressive early momentum with Muse. Users have found many reasons to use Muse with the top 3 categories being shopping, personal productivity and creativity. We are encouraged by the breadth of use cases, with shopping in particular being used by over 25% of Muse weekly actives.

While our near-term priority is to grow adoption and expand Muse's reach, we see multiple paths to monetization, beginning with subscriptions.

We also see a significant opportunity to extend Muse to small businesses. Based on a recent user survey, we estimate that at least 500 million self-identified small businesses use Meta's family of apps to reach customers and generate revenue. Muse could help these businesses acquire customers and operate more efficiently, while helping people complete transactions. Over time, consumer and business agents could connect and transact directly, creating opportunities around paid usage, commerce, and business services.

Alongside growth, we are focused on building sustainable unit economics. Muse's compute intensity means that expanding its user base will require significant investment, and we expect total serving costs to rise. At the same time, we expect to improve compute cost efficiency by more than tenfold over the next year, driven by better utilization of off-peak capacity, more efficient models and inference optimization.

As revenue scales, these efficiency gains give us confidence in our ability to build a highly profitable business over time.

Moving now to our financial outlook.

We expect fourth quarter 2026 total revenue to be in the range of $73-76 billion. Our guidance assumes foreign currency is an approximately 1% headwind to year-over-year total revenue growth, based on current exchange rates.

Turning to the expense and capex outlooks.

We are raising our expense outlook to incorporate the $9.9 billion charge related to legal proceedings recognized in Q3. We now expect full year 2026 total expenses to be in the range of $175-179 billion.

Excluding the Q3 legal charge, we continue to expect 2026 operating income to be above 2025 operating income.

At a segment level, we now expect Reality Labs losses to decline compared to 2025 to a range of $18-19 billion.

We anticipate 2026 capital expenditures, including principal payments on finance leases, to be in the range of $135-145 billion, narrowed from our prior outlook of $130-145 billion.
```

[4] META | FY2026 Q3 | TRANSCRIPT | p.1-2

```text
Speaker: Mark Zuckerberg, CEO
Hey everyone. Thanks for joining today.

This has been a busy and exciting few months. Our family of apps is now used by 3.64 billion people each day, and we're seeing all-time highs in engagement on both Facebook and Instagram driven by strong growth in video. Threads has now overtaken X in monthly actives and continues to grow quickly. WhatsApp is hitting all-time highs in messaging and is becoming an increasingly important platform for agents as well.

In the last few months we have had several major milestones towards our goal of delivering personal superintelligence to everyone. We had one of our most successful launches ever with the Muse personal agent; we shipped Muse Wonder as our first frontier model; and we shipped several new glasses and gadgets with Muse integration, including coming soon to our new audio glasses.

We have been discussing our vision for personal superintelligence for more than a year now. We want to help every person have an extremely capable personal agent that understands your goals and can help you achieve them and improve your life in whatever ways you want -- whether that's improving your relationships, helping you save time, save money, build a business, create new things, or anything else that you can dream of. Muse is the product form of this vision. So now we have the foundational technology in place to deliver this. There is still a lot of work ahead, but our initial position is very strong.

In just a month and a half, more than 10 million people in the US and Canada have set up Muse personal agents. Growth continues to be strong and we have been #1 in the App Store almost every day. Engagement and retention are also off to a strong start. About 25% of people who have set up Muse use it on any given day. For our earliest adopters, the longer that they use Muse,

the better that retention seems to get. To put this in perspective, this is more than twice the level that Threads started at or where we believe that other AI apps started at as well.

The most common use case for Muse is shopping, with more than a quarter of people who are using Muse engaging in this with an average purchase size over $200. Plaid told us they hadn't seen activity like they're seeing with their connector on any other service before. And in the first week we had to work with Stripe to raise credit limits since the volumes were above what any of us expected.

Beyond shopping, the most common connectors are for personal productivity and connecting Muse to your other services like email and calendar. This shows the importance of the Muse Secure VM that we've built to provide state-of-the-art privacy and security, and we're looking forward to following up soon with the Muse Confidential VM, which will make it so that even Meta cannot see your content.
```

[9] META | FY2026 Q3 | TRANSCRIPT | p.10-11

```text
Speaker: Mark Zuckerberg, CEO
Yes. I think I can talk about both of those, and then Susan can jump in with anything else. All right.

So right now for Muse, we are -- and one of the things I pointed out is we have more than 10 million people in the U.S. and Canada who have set it up, but I think one of the biggest constraints so far is that because this is a compute- intensive service to run, we have not launched it globally yet. That's partially because of wanting to optimize it for each place, and there's some work around connectors for that. But it's partially that we want to make sure that we do this efficiently.

So I mean there's pros and cons there. The pro, of course is that, that just makes it even more impressive that you have 10 million people using it and signing up to use it in the first six weeks because those are all kind of Americans and Canadians in a small number of markets.

So if you actually did roll it out more broadly, then you would expect that number to be significantly larger than what we're seeing today.

But then on the flip side, we actually have to go do the work to bring the efficiency in line to make that make sense.

And so that's a big part of it that we need to do. We -- the compute efficiency work is going to be important on this also because we want to make sure that we've built enough compute to be able to serve what we think is going to be 1 billion plus people if we can achieve certain compute efficiency targets, but we obviously have a lot of work to do there as well. And that will be one of the limiting factors over the next year.

We want to build out the business around it. So I talked about that. And I think doing that in tandem with bringing down the compute -- or increasing the compute efficiency is going to be important for getting this to work from a business perspective which I'm optimistic about, but I think that that's one of the things that we need to do.

And then there's just continuing to increase the value in the product which I think we will largely do by continuing to develop new models. That's why I mentioned that we launched Muse first on Muse Spark, and that's what is currently the default across the free tier. And we rolled out Muse Wonder, and people can access that in the paid tier today. And we know that it performs a

lot better, so we're working on getting a more efficient version of Muse Wonder that we'll be able to roll out more broadly.
```

[10] META | FY2026 Q3 | TRANSCRIPT | p.7-8

```text
Speaker: Susan Li, CFO
We estimate that advertisers who adopted its recommendations drove almost 5% more conversions at the same cost per conversion and increased their advertising investment by 9% compared with similar non-adopters. We are rolling out additional capabilities, including creative insights, stronger business context and more agentic actions. These features will allow marketers to plan, create and optimize campaigns, as well as schedule tasks and alerts, through a conversational experience with approvals and previews that keep them in control.

I’ll share one example of how Meta AI business assistant is empowering our advertisers. Simply Organic Maple, a family-run maple syrup producer in Vermont, used Meta AI business assistant to improve its campaign messaging. After the business assistant recommended testing longer, more descriptive advertising copy, the business saw a 57% increase in purchases and a 36% reduction in cost per purchase. Its strongest revised ad delivered a 3x return on ad spend, a 52% improvement from the original.

Adoption of our generative AI ad creative tools also continues to scale. In Q3, nearly 10 million advertisers used at least one of these tools. Advertiser adoption of our video generation features grew 25% quarter-over-quarter, driven in part by ready-made videos that reduce generation time to just a few seconds from a single image upload.

We're also expanding the opportunity at the intersection of creators and advertising.

Partnership Ads, which allow advertisers to run ads with creators, brands and other businesses, continued to scale rapidly in Q3. Our partnership ads product grew over 97% year-over-year and surpassed $15 billion in annual revenue run-rate.

In Q3, we also completed the global rollout of Creator Marketing Hub to all advertisers, bringing creator discovery, content organization and partnership ad activation into one place, with more than 10 million creators now discoverable. We are integrating Meta AI Business Assistant into the Hub so advertisers can conversationally search for creators, identify content to use in partnership ads and get guidance across their creator marketing workflows.

Outside of ads, we are also building out additional ways to monetize engagement across our ecosystem.

In September, we announced the global expansion of Meta One, our unified subscription service for consumers, creators and businesses. Meta One is rolling out gradually with more than 50

features across Instagram, Facebook, and WhatsApp, including Meta Business Agent and Meta AI, and has surpassed 15 million subscriptions and trials to date. The core experience across our apps and Meta AI will remain free, while subscriptions offer more specialized capabilities and expanded AI usage.

Beyond monetizing engagement across our existing services, we are building new experiences and revenue streams around consumer agents.
```

[16] META | FY2026 Q3 | TRANSCRIPT | p.20

```text
Speaker: Mark Zuckerberg, CEO
Part of the reason why we're not just charging everyone upfront to use this is we want to establish this Muse network, and we think that it's going to be more valuable for people at scale when there are a lot of people and businesses using Muse so that way they can collaborate and work together.

I think that that's really a very important state to get to and one that we're quite excited about. And again I don't think that there are many companies in the world that are well situated to be able to do that in the way that we are. I think we are very well situated to do that, and we plan to push on that quite aggressively.
```

---

## 21. q022-C (qualitative)

**Question:** What did Alphabet say about demand for its TPU systems in Q3 2026?

**Gold answer:** Strong demand across a wide range of customers, expected to continue as it scales; TPU system sales contributed meaningfully to Cloud revenue growth.

**Answer:**

> In FY2026 Q3, Alphabet reported extraordinary demand for its TPU systems, with strong growth coming from various sectors including AI labs, capital markets, financial service institutions, and traditional enterprises. The company noted that they have over a hundred significant TPU customers and expect this number to grow as these customers scale their use of TPU systems. The demand is driven by the performance improvements and price performance benefits of TPU systems compared to competitors, as well as the growing need for accelerated compute in the market [1][4][15].

**Cited sources (3):**

[1] GOOGL | FY2026 Q3 | TRANSCRIPT | section: 2026 Q3 Earnings Call

```text
Speaker: Thomas Kurian, SVP and CEO, Google Cloud
Eric, on the demand in Cloud, we are always managing our portfolio to ensure we have a breadth of customers; and we have different types of customers from digital natives, AI labs, traditional enterprises, financial and capital markets customers, so it's a broad set of customers.

Many of them have either started with using TPU systems, for example, in our Cloud. We have over a hundred significant TPU customers who start in our Cloud, they learn to use TPUs; and remember, a TPU system is a combination of hardware, system software, compilers, the whole thing, assembled as a system, including optical networking and other things.

We have taken a very deliberate approach on how we're bringing TPUs to customers. The first one is to build the right silicon, so we have 8t for training, 8i for inference. They both offer substantial price performance and performance improvements relative to prior generations.

Second, we've optimized the software. The optimization of the software is not just to make the systems run better and to give you great performance, but also to make portability seamless.

Recently, we introduced, for example, Pytorch, which opens up a much larger market for us by optimizing Pytorch on the TPU.

Third, we are broadening distribution. So, in the past, you could use TPUs in our Cloud. Now we make, for customers who want TPU systems in their data centers, we're making TPU systems available in their data centers.

We're also broadening availability to other providers. For example, we announced a partnership with Oracle, and we've launched, along with Blackstone, an agreement for Crux AI, a joint venture, to offer customers TPU systems.

We are seeing extraordinary demand, and strong growth from both AI labs, capital markets, financial service institutions, and Enterprises. And if you imagine these hundred-plus customers growing to many more, and also scaling their use into TPU systems, we see strong demand over the future.
```

[4] GOOGL | FY2026 Q3 | TRANSCRIPT | section: 2026 Q3 Earnings Call

```text
Speaker: Anat Ashkenazi, SVP and CFO, Alphabet and Google
The Google Cloud segment delivered record growth in the third quarter. Cloud revenues were up 103% to $30.7 billion, driven by strong growth in GCP, with strength in AI, including both AI infrastructure and AI solutions and Core GCP.

TPU systems sales were also a meaningful contributor. GCP accounted for the vast majority of the revenue generated in Q3. The Cloud revenue growth rate, excluding the impact of TPU systems, was again incredibly strong.

Cloud operating income was $11.1 billion, more than tripling year-over-year. Operating margin increased from 23.7% in the third quarter last year to 36.2%.

The third-party bridging capacity, which we discussed on our prior call, came online only at the very end of Q3 and, therefore, had limited negative impact to our margin in the quarter.

Google Cloud's backlog was $561 billion in the third quarter, with the majority of the backlog related to typical GCP contracts. We expect to recognize just over 55% of the total backlog as revenues over the next 24 months.

In Other Bets, revenues were $372 million, and the operating loss was $2 billion dollars as we continue to expand Waymo's business and invest in key Other Bets.

In Alphabet-level activities, the operating loss was $7.7 billion, driven by shared AI R&D expenses.

I'll now turn to our outlook for the remainder of 2026. First, in terms of revenues, at current spot rates, we would expect a slightly larger FX headwind to our consolidated revenues in Q4, relative to the prior quarter. This impact will be seen primarily in Search and YouTube Ads.

In Google Services, we will continue to lap the Search performance acceleration that began in the third quarter last year, which will pressure revenue growth in Q4.

In Google Cloud, we expect TPU system sales to continue to scale in Q4, followed by a meaningful revenue ramp in 2027. With respect to Cloud margins, given the supply-constraint environment, we started utilizing third-party compute capacity at the end of Q3 and will ramp usage in Q4. We expect margin headwinds in the near-term as we utilize this capacity.

Moving to investments, our full-year 2026 CapEx guidance range of $195-$205 billion remain unchanged. We continue to expect our CapEx to increase significantly in 2027, and we'll provide further details on our Q4 call.

In terms of expenses, the significant increase in our investment in technical infrastructure will continue to weigh on the P&L in the form of higher depreciation expenses and related data centers' operation costs such as energy.
```

[15] GOOGL | FY2026 Q3 | TRANSCRIPT | section: 2026 Q3 Earnings Call

```text
Speaker: Thomas Kurian, SVP and CEO, Google Cloud
To address the question on the rate of growth of Cloud, our Google Cloud segment delivered record growth in the third quarter with Cloud revenues up 103% to $30.7 billion.

The strongest growth contributor was Google Cloud Platform, with strength in both AI and Core GCP. So GCP accounted for the vast majority of revenue generated in Q3. And Cloud revenue growth, excluding the impact of TPU system sales, was again very, very strong.

Demand was also strong across our product lines. So we see three things going on.

One, as people use Gemini, we see revenue growth from Gemini itself; but we also see that as people use Gemini, it drives growth in other parts of our portfolio. For instance, as they build agents, they use compute to perform a task, and so they buy compute. They use storage to store memories of the agent. Database to serve up what's called context for the agent to reason on. And obviously given all the cybersecurity threats, we are seeing strong growth in our cybersecurity portfolio.

So we are confident about our growth and the diversity in our growth. We're also seeing new customers buy into GCP and exhilarating growth in new customer adoptions, so we also see that as a growth factor for us.

And we're always -- at any point in time, we could obviously serve a lot more demand with capacity, but we are managing all of these parameters and growth carefully.
```

---

## 22. q023-C (qualitative)

**Question:** What did Microsoft say about the effect of rising memory prices in Q2 FY2026?

**Gold answer:** Rising memory prices would affect capital expenditures, while their impact on Microsoft Cloud gross margins would build gradually as the assets depreciate over six years.

**Answer:**

> In Q2 FY2026, Microsoft indicated that rising memory prices could impact the PC market, particularly affecting Windows OEM revenue, which they expect to decline roughly 10 percent. They noted that the range of potential outcomes remains wider than normal due to this potential impact from increased memory pricing [1][3]. Additionally, they mentioned that rising memory prices would also affect capital expenditures and could create additional volatility in transactional purchasing within their on-premises server business [1].

**Cited sources (2):**

[1] MSFT | FY2026 Q2 | TRANSCRIPT | section: Microsoft FY26 Second Quarter Earnings Conference Call

```text
Speaker: AMY HOOD
Therefore, Windows OEM revenue should decline roughly 10 percent. The range of potential outcomes remains wider than normal, in part due to the potential impact on the PC market from increased memory pricing.

Search and news advertising ex-TAC revenue growth should be in the high single digits. Even as we work to improve execution, we expect continued share gains across Bing and Edge, with growth driven by volume. And we expect sequential growth moderation as the contribution from third-party partnerships continues to normalize.

And in Xbox content and services, we expect revenue to decline in the mid-single digits against a prior year comparable that benefited from strong content performance, partially offset by growth in Xbox Game Pass. And hardware revenue should decline year-over-year.

Now some additional thoughts on the rest of the fiscal year and beyond.

First, FX. Based on current rates, we expect FX to increase Q4 total revenue and COGS growth by less than 1 point with no impact to operating expense growth. Within the segments, we expect FX to increase revenue growth by roughly 1 point in Productivity and Business Processes and More Personal Computing, and less than one point in Intelligent Cloud.

With the strong work delivered in H1 to prioritize investment in key growth areas and the favorable impact from a higher mix of revenue in our Windows OEM and Commercial on-prem businesses, we now expect FY26 operating margins to be up slightly.

We mentioned the potential impact on Windows OEM and on premises server markets from increased memory pricing earlier. In addition, rising memory prices would impact capital expenditures, though the impact on Microsoft Cloud gross margins will build more gradually as these assets depreciate over six years.

In closing, we delivered strong top-line growth in H1 and are investing across every layer of the stack to continue to deliver high value solutions and tools to our customers.

With that, let’s go to Q&A, Jonathan.
```

[3] MSFT | FY2026 Q2 | TRANSCRIPT | section: Microsoft FY26 Second Quarter Earnings Conference Call

```text
Speaker: AMY HOOD
Now to segment guidance.

In Productivity and Business Processes we expect revenue of $34.25 to $34.55 billion, or growth of 14% to 15%.

In M365 commercial cloud, we expect revenue growth to be between 13% and 14% in constant currency, with continued stability in year-over-year growth rates on a large and expanding base. Accelerating Copilot momentum and ongoing E5 adoption will again drive ARPU growth.

M365 commercial products revenue should decline in the low-single digits, down sequentially assuming Office 2024 transactional purchasing trends normalize. As a reminder, M365 commercial products includes components that can be variable due to in-period revenue recognition dynamics.

M365 consumer cloud revenue growth should be in the mid to high-twenty percent range driven by growth in ARPU as well as continued subscription volume.

For LinkedIn, we expect revenue growth in the low-double digits.

And in Dynamics 365, we expect revenue growth to be in the high teens with continued growth across all workloads.

For Intelligent Cloud, we expect revenue of $34.1 to $34.4 billion, or growth of 27% to 29%.

In Azure, we expect Q3 revenue growth to be between 37% and 38% in constant currency against a prior year comparable that included significantly accelerating growth rates in both Q3 and Q4. As mentioned earlier, demand continues to exceed supply and we will need to continue to balance the incoming supply we can allocate here against other priorities. As a reminder, there can be quarterly variability in year-on-year growth rates depending on the timing of capacity delivery and when it comes online, as well as from in-period revenue recognition depending on the mix of contracts.

In our on-premises server business, we expect revenue to decline in the low-single digits as growth rates normalize following the launch of SQL Server 2025, though increased memory pricing could create additional volatility in transactional purchasing.

In More Personal Computing, we expect revenue to be $12.3 to $12.8 billion.

Windows OEM and Devices revenue should decline in the low teens. Growth rates will be impacted as the benefit from Windows 10 end of support normalizes and as elevated inventory levels come down through the quarter. Therefore, Windows OEM revenue should decline roughly 10 percent. The range of potential outcomes remains wider than normal, in part due to the potential impact on the PC market from increased memory pricing.

Search and news advertising ex-TAC revenue growth should be in the high single digits. Even as we work to improve execution, we expect continued share gains across Bing and Edge, with growth driven by volume. And we expect sequential growth moderation as the contribution from third-party partnerships continues to normalize.
```

---

## 23. q024-C (qualitative)

**Question:** Why did Meta raise its 2026 capex outlook in Q1 2026?

**Gold answer:** Higher expected component pricing and, to a lesser extent, extra data center costs for future capacity.

**Answer:**

> Meta raised its 2026 capital expenditures (capex) outlook due to expectations for higher component pricing and additional data center costs to support future capacity needs. The revised capex range is now anticipated to be between $125 billion and $145 billion, increased from the prior range of $115 billion to $135 billion [1][2].

**Cited sources (2):**

[1] META | FY2026 Q1 | TRANSCRIPT | p.7-8

```text
Speaker: Susan Li, CFO
Last month, we rolled out our affiliate partnerships offering on Facebook to more test partners, so creators can tag products from participating retailers on their posts and earn a commission when someone makes a purchase. We have also started testing similar experiences on Instagram.

We see a real opportunity to help people more easily discover and buy products within our services, particularly as we incorporate AI deeply across our platforms.

Next, I would like to discuss our approach to capital allocation.

Compute is becoming increasingly important as it determines the quality of the services we can provide, including powering more capable models and delivering innovative new products. It is also becoming more critical to how we work at Meta, as we are entering a world where employees are managing agents to help them generate new ideas, run experiments, execute tasks, and build products.

We are investing aggressively to meet our infrastructure needs and ensure we maximize our strategic flexibility over the coming years. This includes substantially expanding our own data center footprint and striking deals throughout the supply chain to secure necessary components for future capacity. We are also signing cloud deals that will come online over the course of this year and 2027, allowing us to scale more quickly. These multi-year cloud deals and our

infrastructure purchase agreements drove a $107 billion step up in our contractual commitments this quarter.

Our investments will support our training needs for future models, and most importantly, provide us the inference capacity necessary to deliver personal and business agents to billions of people around the world, along with several other AI product experiences we’re developing.

As we grow our infrastructure spend, we remain committed to operating efficiently, and we recently shared internally that we plan to reduce the size of our employee base in May. We believe a leaner operating model will allow us to move more quickly while also helping to offset the substantial investments we’re making.

Moving to our financial outlook.

We expect second quarter 2026 total revenue to be in the range of $58-61 billion. Our guidance assumes foreign currency is an approximately 2% tailwind to year-over-year total revenue growth, based on current exchange rates.

Turning to the expense and capex outlooks.

We expect full year 2026 total expenses to be in the range of $162-169 billion, unchanged from our prior outlook.

We continue to expect to deliver operating income this year that is above 2025 operating income.

We anticipate 2026 capital expenditures, including principal payments on finance leases, to be in the range of $125-145 billion, increased from our prior range of $115-135 billion. This reflects our expectations for higher component pricing this year and, to a lesser extent, additional data center costs to support future year capacity.

Absent any changes to our tax landscape, we expect our tax rate for the remaining quarters of 2026 to be between 13-16%.
```

[2] META | FY2026 Q1 | PRESS_RELEASE | section: CFO Outlook Commentary

```text
We expect second quarter 2026 total revenue to be in the range of $58-61 billion. Our guidance assumes foreign currency is an approximately 2% tailwind to year-over-year total revenue growth, based on current exchange rates.

We expect full year 2026 total expenses to be in the range of $162-169 billion, unchanged from our prior outlook.

We continue to expect to deliver operating income this year that is above 2025 operating income.

We anticipate 2026 capital expenditures, including principal payments on finance leases, to be in the range of $125-145 billion, increased from our prior range of $115-135 billion. This reflects our expectations for higher component pricing this year and, to a lesser extent, additional data center costs to support future year capacity.

Absent any changes to our tax landscape, we expect our tax rate for the remaining quarters of 2026 to be between 13-16%.

Lastly, we continue to monitor active legal and regulatory matters, including headwinds in the EU and the U.S. that could significantly impact our business and financial results. For example, we continue to see scrutiny on youth-related issues and have additional trials scheduled for this year in the U.S., which may ultimately result in a material loss.
```

---

## 24. q025-C (qualitative)

**Question:** What did Alphabet say about CapEx for 2027 on its Q1 2026 call?

**Gold answer:** Strong results reinforce its conviction to invest to capture the AI opportunity, so 2027 CapEx is expected to increase significantly versus 2026.

**Answer:**

> In the Q1 FY2026 earnings call, Alphabet indicated that they expect their capital expenditures (CapEx) for 2027 to significantly increase compared to 2026. The specific figures for 2027 were not quantified during the call, but the increase is attributed to the robust demand they are experiencing across their business, particularly in AI and Cloud services [1][3][6].

**Cited sources (3):**

[1] GOOGL | FY2026 Q1 | TRANSCRIPT | section: 2026 Q1 Earnings Call

```text
Speaker: Anat Ashkenazi, SVP and CFO, Alphabet and Google
The increase was driven by strong demand for our enterprise AI offerings and the inclusion of TPU hardware sales that Sundar referenced earlier. The majority of the backlog is related to typical GCP contracts, and we expect to recognize just over 50% of the backlog as revenue over the next 24 months.

In Other Bets, revenues were $411 million, and operating loss was $2.1 billion.

For the past few years, we have been working to prioritize our efforts and investments in the Other Bets. In Q1 of this year, Verily completed an external capital raise that resulted in its deconsolidation from Alphabet. GFiber announced plans to combine with Astound Broadband, which will result in its deconsolidation from Alphabet when the deal closes, which we expect to take place in Q4.

And we continue to allocate significant resources to businesses where we see meaningful opportunities to create value, such as Waymo.

Turning to our outlook, I would like to provide some commentary on factors that will impact our business performance in the second quarter and full year 2026.

First, in terms of revenues, we're pleased with the overall momentum of the business. At current spot rates, we would expect to see an FX tailwind of approximately one percentage point toward consolidated revenue in Q2, compared to three percentage points FX tailwinds in the first quarter.

In Google Cloud, as Sundar mentioned, we will begin to deliver TPU hardware to a select group of customers in their own data centers. We expect to begin recognizing a small percent of the revenues from these agreements later this year, with the vast majority of revenues to be realized in 2027.

It is important to keep in mind that revenues from TPU hardware sales will fluctuate from quarter to quarter, depending on when TPUs are shipped to customers.

And finally, we're excited to welcome the Wiz team to Google Cloud with the closing of the acquisition in March, and are very pleased with the performance to date.

A couple of items to highlight related to the acquisition. First, Wiz will be reporting in the Google Cloud segment. And second, we expect a low single digit percentage point headwind to Cloud's operating margin for the remainder of 2026 related to the acquisition.

Moving to investment, we are updating our full year 2026 CapEx guidance range to $180 to $190 billion, up from our previous estimate of $175 to $185 billion, to now include investment related to the acquisition of Intersect, which closed in March.

We are seeing unprecedented internal and external demand for AI compute resources. The investment we're making in AI is delivering strong growth, as evidenced by the record revenue and backlog growth in Google Cloud and strong performance in Google Services.
```

[3] GOOGL | FY2026 Q1 | TRANSCRIPT | section: 2026 Q1 Earnings Call

```text
Speaker: Anat Ashkenazi, SVP and CFO, Alphabet and Google
Thanks, Doug, for the question. Let me start with your first question on CapEx and how we think about CapEx increase going into 2027.

You've seen us over the past several years increase CapEx every year. And we have done it very thoughtfully to meet the demand that we are seeing, both from external customers as well as demands across the organization.

And you're seeing the proof point, the ROIC on that, in terms of just the growth rate we're seeing, whether it's growth rate within Search or certainly the Cloud business, and the opportunity we have within the Cloud backlog.

So as we're seeing that robust demand across the business, we are looking at what can we do to support that growing demand and the opportunity ahead of us, and increasing CapEx to meet that demand we’ll provide more clarity in a future earnings call about what that number will be.

But that's the opportunity we're seeing ahead of us. It's quite meaningful, and we want to make sure we capitalize [on] that and we do it in a way that's responsible, as we've done to date.
```

[6] GOOGL | FY2026 Q1 | TRANSCRIPT | section: 2026 Q1 Earnings Call

```text
Speaker: Anat Ashkenazi, SVP and CFO, Alphabet and Google
Sure. Let me help unpack the margin expansion. Obviously, we're pleased to see that.

There are pushes and pulls across the business, including within Cloud specifically. And I would start with the top line. When we see this robust, strong revenue growth, both in Cloud and Google Services, it does provide leverage all the way down to the bottom line within the income statement.

And you know we've been working hard to ensure we're running a productive and efficient organization. And it's not just how we operate the business, but even in areas such as our technical infrastructure, where we are investing these significant CapEx investments into our data centers and servers, we are looking at how we drive scientific process innovation within that organization. And that is reflected both in Cloud and Google Services as we allocate cost based on consumption.

In the past, I did talk about the depreciation associated with these investments that is hitting both Google Cloud and Google Services.

Google Cloud expanded margin quite significantly from a year ago, as you've seen in our numbers that we've just previewed. And a lot of it, again, is the topline growth that Google Cloud is providing or producing, as well as an incredibly efficient way of running the business.

I will give Thomas and that team a lot of credit for running a very productive organization and making sure that we are supporting our customers and providing those services and products that they want and benefit from, continue to drive topline growth, and doing this well within the middle of the income statement, all the way from very efficient technical infrastructure, thinking through how do we leverage AI across our business.

Sundar mentioned the use of coding internally, how Gemini helps us there, optimizing our real estate footprint. And we're going to continue to do this. We're not going to stop here. We're going to continue to push for more efficiency, knowing that we're going to have the headwind associated with the depreciation coming with a higher CapEx level.

Ron Josey (Citi): Thank you. Very helpful.
```

---

## 25. q032-A (unanswerable)

**Question:** What was Microsoft's revenue in the month of March 2026?

**Gold answer:** Not in the documents: Microsoft reports revenue by quarter, not by month.

**Answer:**

> For the three months ended March 31, 2026, Microsoft's total revenue was $82,886 million [1][6].

**Cited sources (2):**

[1] MSFT | FY2026 Q3 | FORM_10Q | section: More Personal Computing

```text
No sales to an individual customer or country other than the United States accounted for more than 10% of revenue for the three or nine months ended March 31, 2026 or 2025. Revenue, classified by the major geographic areas in which our customers were located, was as follows:

(In millions) | Three Months Ended March 31, | Nine Months Ended March 31,
2026 | 2025 | 2026 | 2025
United States (a) | $42,336 | $36,084 | $123,823 | $105,534
Other countries | 40,550 | 33,982 | 118,009 | 99,749
Total | $82,886 | $70,066 | $241,832 | $205,283

Includes billings to OEMs and certain multinational organizations because of the nature of these businesses and the impracticability of determining the geographic source of the revenue.

Revenue, classified by significant product and service offerings, was as follows:

(In millions) | Three Months Ended March 31, | Nine Months Ended March 31,
2026 | 2025 | 2026 | 2025
Server products and cloud services | $32,592 | $24,761 | $92,329 | $70,557
Microsoft 365 Commercial products and cloud services | 25,593 | 21,883 | 74,083 | 63,449
Gaming | 5,341 | 5,721 | 16,807 | 17,923
LinkedIn | 4,832 | 4,311 | 14,628 | 13,190
Windows and Devices | 4,041 | 4,144 | 13,071 | 12,985
Search advertising | 3,808 | 3,504 | 11,317 | 10,287
Microsoft 365 Consumer products and cloud services | 2,297 | 1,821 | 6,806 | 5,369
Dynamics products and cloud services | 2,292 | 1,929 | 6,632 | 5,691
Enterprise and partner services | 2,087 | 1,946 | 6,147 | 5,766
Other | 3 | 46 | 12 | 66
Total | $82,886 | $70,066 | $241,832 | $205,283
```

[6] MSFT | FY2026 Q3 | PRESS_RELEASE | section: MICROSOFT CORPORATION

```text
INCOME STATEMENTS

(In millions, except per share amounts) (Unaudited)

Three Months Ended March 31, | Nine Months Ended March 31,
2026 | 2025 | 2026 | 2025
Revenue:
Product | $15,089 | $15,319 | $47,462 | $46,810
Service and other | 67,797 | 54,747 | 194,370 | 158,473
Total revenue | 82,886 | 70,066 | 241,832 | 205,283
Cost of revenue:
Product | 2,733 | 3,037 | 9,160 | 10,187
Service and other | 24,095 | 18,882 | 67,689 | 53,630
Total cost of revenue | 26,828 | 21,919 | 76,849 | 63,817
Gross margin | 56,058 | 48,147 | 164,983 | 141,466
Research and development | 8,915 | 8,198 | 25,565 | 23,659
Sales and marketing | 6,814 | 6,212 | 19,115 | 18,369
General and administrative | 1,931 | 1,737 | 5,669 | 5,233
Operating income | 38,398 | 32,000 | 114,634 | 94,205
Other income (expense), net | 942 | (623) | 7,253 | (3,194)
Income before income taxes | 39,340 | 31,377 | 121,887 | 91,011
Provision for income taxes | 7,562 | 5,553 | 23,904 | 16,412
Net income | $31,778 | $25,824 | $97,983 | $74,599
Earnings per share:
Basic | $4.28 | $3.47 | $13.19 | $10.03
Diluted | $4.27 | $3.46 | $13.14 | $9.99
Weighted average shares outstanding:
Basic | 7,426 | 7,434 | 7,430 | 7,434
Diluted | 7,445 | 7,461 | 7,457 | 7,466

COMPREHENSIVE INCOME STATEMENTS
```

---

## 26. q012-A (multi_quarter)

**Question:** How did Meta's 2026 capital expenditure outlook change over the last four quarters?

**Gold answer:** $115-135B (Q4 2025), raised to $125-145B (Q1 2026), narrowed to $130-145B (Q2), narrowed to $135-145B (Q3).

**Answer:**

> Here is the change in Meta's capital expenditure outlook over the last four quarters:
> 
> **FY2025 Q4**
> - Anticipated capital expenditures for 2026 were in the range of $115-135 billion [2].
> 
> **FY2026 Q1**
> - The anticipated capital expenditures for 2026 were increased to a range of $125-145 billion [1][9].
> 
> **FY2026 Q2**
> - The anticipated capital expenditures for 2026 remained in the range of approximately $130-145 billion [4][10].
> 
> **FY2026 Q3**
> - The anticipated capital expenditures for 2026 were narrowed to a range of $135-145 billion [12].
> 
> In summary, the outlook for capital expenditures increased from $115-135 billion in FY2025 Q4 to a range of $135-145 billion by FY2026 Q3.

**Cited sources (6):**

[1] META | FY2026 Q1 | TRANSCRIPT | p.7-8

```text
Speaker: Susan Li, CFO
Last month, we rolled out our affiliate partnerships offering on Facebook to more test partners, so creators can tag products from participating retailers on their posts and earn a commission when someone makes a purchase. We have also started testing similar experiences on Instagram.

We see a real opportunity to help people more easily discover and buy products within our services, particularly as we incorporate AI deeply across our platforms.

Next, I would like to discuss our approach to capital allocation.

Compute is becoming increasingly important as it determines the quality of the services we can provide, including powering more capable models and delivering innovative new products. It is also becoming more critical to how we work at Meta, as we are entering a world where employees are managing agents to help them generate new ideas, run experiments, execute tasks, and build products.

We are investing aggressively to meet our infrastructure needs and ensure we maximize our strategic flexibility over the coming years. This includes substantially expanding our own data center footprint and striking deals throughout the supply chain to secure necessary components for future capacity. We are also signing cloud deals that will come online over the course of this year and 2027, allowing us to scale more quickly. These multi-year cloud deals and our

infrastructure purchase agreements drove a $107 billion step up in our contractual commitments this quarter.

Our investments will support our training needs for future models, and most importantly, provide us the inference capacity necessary to deliver personal and business agents to billions of people around the world, along with several other AI product experiences we’re developing.

As we grow our infrastructure spend, we remain committed to operating efficiently, and we recently shared internally that we plan to reduce the size of our employee base in May. We believe a leaner operating model will allow us to move more quickly while also helping to offset the substantial investments we’re making.

Moving to our financial outlook.

We expect second quarter 2026 total revenue to be in the range of $58-61 billion. Our guidance assumes foreign currency is an approximately 2% tailwind to year-over-year total revenue growth, based on current exchange rates.

Turning to the expense and capex outlooks.

We expect full year 2026 total expenses to be in the range of $162-169 billion, unchanged from our prior outlook.

We continue to expect to deliver operating income this year that is above 2025 operating income.

We anticipate 2026 capital expenditures, including principal payments on finance leases, to be in the range of $125-145 billion, increased from our prior range of $115-135 billion. This reflects our expectations for higher component pricing this year and, to a lesser extent, additional data center costs to support future year capacity.

Absent any changes to our tax landscape, we expect our tax rate for the remaining quarters of 2026 to be between 13-16%.
```

[2] META | FY2025 Q4 | PRESS_RELEASE | section: CFO Outlook Commentary

```text
We expect first quarter 2026 total revenue to be in the range of $53.5-56.5 billion. Our guidance assumes foreign currency is an approximately 4% tailwind to year-over-year total revenue growth, based on current exchange rates.

We expect full year 2026 total expenses to be in the range of $162-169 billion.

•The majority of expense growth will be driven by infrastructure costs, which includes third-party cloud spend, higher depreciation, and higher infrastructure operating expenses.

•The second-largest contributor to total expense growth is employee compensation, driven by investments in technical talent. This includes 2026 hires to support our priority areas, particularly AI, as well as a full year of expenses from 2025 hires.

•At a segment level, we expect expense growth to be driven by the Family of Apps, with Reality Labs operating losses remaining similar to 2025 levels.

We anticipate 2026 capital expenditures, including principal payments on finance leases, to be in the range of $115-135 billion, with year-over-year growth driven by increased investment to support our Meta Superintelligence Labs efforts and core business.

Despite the meaningful step up in infrastructure investment, in 2026 we expect to deliver operating income that is above 2025 operating income.

Absent any changes to our tax landscape, we expect our full year 2026 tax rate to be 13-16%.

Finally, we recently aligned with the European Commission on further changes to our Less Personalized Ads offering, which we will begin rolling out this quarter. However, we continue to monitor legal and regulatory headwinds in the EU and the U.S. that could significantly impact our business and financial results. For example, we continue to see scrutiny on youth-related issues and have a number of trials scheduled for this year in the U.S., which may ultimately result in a material loss.
```

[4] META | FY2026 Q2 | FORM_10Q | section: Cash Used in Investing Activities

```text
Cash used in investing activities during the six months ended June 30, 2026 mostly consisted of $49.11 billion of purchases of property and equipment as we continued to invest in servers, data centers, and network infrastructure, and $31.56 billion of net purchases of marketable securities. The increase in cash used in investing activities during the six months ended June 30, 2026, compared to the same period in 2025, was mostly due to increases in net purchases of marketable securities and property and equipment, partially offset by a decrease in non-marketable equity investments.

We anticipate making capital expenditures of approximately $130 billion to $145 billion in 2026 to support our AI efforts and core business.
```

[9] META | FY2026 Q1 | PRESS_RELEASE | section: CFO Outlook Commentary

```text
We expect second quarter 2026 total revenue to be in the range of $58-61 billion. Our guidance assumes foreign currency is an approximately 2% tailwind to year-over-year total revenue growth, based on current exchange rates.

We expect full year 2026 total expenses to be in the range of $162-169 billion, unchanged from our prior outlook.

We continue to expect to deliver operating income this year that is above 2025 operating income.

We anticipate 2026 capital expenditures, including principal payments on finance leases, to be in the range of $125-145 billion, increased from our prior range of $115-135 billion. This reflects our expectations for higher component pricing this year and, to a lesser extent, additional data center costs to support future year capacity.

Absent any changes to our tax landscape, we expect our tax rate for the remaining quarters of 2026 to be between 13-16%.

Lastly, we continue to monitor active legal and regulatory matters, including headwinds in the EU and the U.S. that could significantly impact our business and financial results. For example, we continue to see scrutiny on youth-related issues and have additional trials scheduled for this year in the U.S., which may ultimately result in a material loss.
```

[10] META | FY2026 Q2 | PRESS_RELEASE | section: Non-GAAP Financial Measures

```text
____________________________________

(1) The second quarter 2026 general and administrative expenses include $2.40 billion of charges related to legal proceedings.

META PLATFORMS, INC.
CONDENSED CONSOLIDATED BALANCE SHEETS
(In millions)
(Unaudited)
June 30, 2026 | December 31, 2025
Assets
Current assets:
Cash and cash equivalents | $15,462 | $35,873
Marketable securities | 74,798 | 45,719
Accounts receivable, net | 21,752 | 19,769
Prepaid expenses and other current assets | 13,463 | 7,361
Total current assets | 125,475 | 108,722
Non-marketable equity investments | 30,157 | 27,524
Property and equipment, net | 225,724 | 176,400
Operating lease right-of-use assets | 23,985 | 20,404
Goodwill | 23,406 | 24,534
Other assets | 21,209 | 8,437
Total assets | $449,956 | $366,021
Liabilities and stockholders' equity
Current liabilities:
Accounts payable | $15,889 | $8,894
Operating lease liabilities, current | 2,425 | 2,213
Accrued expenses and other current liabilities | 38,065 | 30,729
Total current liabilities | 56,379 | 41,836
Operating lease liabilities, non-current | 26,229 | 22,940
Long-term debt | 83,664 | 58,744
Long-term income taxes | 18,326 | 21,005
Other liabilities | 4,137 | 4,253
Total liabilities | 188,735 | 148,778
Commitments and contingencies
Stockholders' equity:
Common stock and additional paid-in capital | 103,981 | 95,793
Accumulated other comprehensive income (loss) | (603) | 271
Retained earnings | 157,843 | 121,179
Total stockholders' equity | 261,221 | 217,243
Total liabilities and stockholders' equity | $449,956 | $366,021

META PLATFORMS, INC.
CONDENSED CONSOLIDATED STATEMENTS OF CASH FLOWS
(In millions)
(Unaudited)
Three Months Ended June 30, | Six Months Ended June 30,
2026 | 2025 | 2026 | 2025
Cash flows from operating activities
Net income | $15,848 | $18,337 | $42,621 | $34,981
Adjustments to reconcile net income to net cash provided by operating activities:
```

[12] META | FY2026 Q3 | TRANSCRIPT | p.7-9

```text
Speaker: Susan Li, CFO
Meta One is rolling out gradually with more than 50

features across Instagram, Facebook, and WhatsApp, including Meta Business Agent and Meta AI, and has surpassed 15 million subscriptions and trials to date. The core experience across our apps and Meta AI will remain free, while subscriptions offer more specialized capabilities and expanded AI usage.

Beyond monetizing engagement across our existing services, we are building new experiences and revenue streams around consumer agents.

Mark highlighted the impressive early momentum with Muse. Users have found many reasons to use Muse with the top 3 categories being shopping, personal productivity and creativity. We are encouraged by the breadth of use cases, with shopping in particular being used by over 25% of Muse weekly actives.

While our near-term priority is to grow adoption and expand Muse's reach, we see multiple paths to monetization, beginning with subscriptions.

We also see a significant opportunity to extend Muse to small businesses. Based on a recent user survey, we estimate that at least 500 million self-identified small businesses use Meta's family of apps to reach customers and generate revenue. Muse could help these businesses acquire customers and operate more efficiently, while helping people complete transactions. Over time, consumer and business agents could connect and transact directly, creating opportunities around paid usage, commerce, and business services.

Alongside growth, we are focused on building sustainable unit economics. Muse's compute intensity means that expanding its user base will require significant investment, and we expect total serving costs to rise. At the same time, we expect to improve compute cost efficiency by more than tenfold over the next year, driven by better utilization of off-peak capacity, more efficient models and inference optimization.

As revenue scales, these efficiency gains give us confidence in our ability to build a highly profitable business over time.

Moving now to our financial outlook.

We expect fourth quarter 2026 total revenue to be in the range of $73-76 billion. Our guidance assumes foreign currency is an approximately 1% headwind to year-over-year total revenue growth, based on current exchange rates.

Turning to the expense and capex outlooks.

We are raising our expense outlook to incorporate the $9.9 billion charge related to legal proceedings recognized in Q3. We now expect full year 2026 total expenses to be in the range of $175-179 billion.

Excluding the Q3 legal charge, we continue to expect 2026 operating income to be above 2025 operating income.

At a segment level, we now expect Reality Labs losses to decline compared to 2025 to a range of $18-19 billion.

We anticipate 2026 capital expenditures, including principal payments on finance leases, to be in the range of $135-145 billion, narrowed from our prior outlook of $130-145 billion.
```

---

## 27. q013-A (multi_quarter)

**Question:** How did Alphabet's full-year 2026 CapEx guidance evolve from the Q4 2025 call to the Q3 2026 call?

**Gold answer:** $175-185B (Q4 2025), raised to $180-190B (Q1 2026, includes Intersect), raised to $195-205B (Q2), unchanged at $195-205B (Q3).

**Answer:**

> Here is the evolution of Alphabet's full-year 2026 CapEx guidance from the Q4 2025 call to the Q3 2026 call:
> 
> - **FY2025 Q4**: The CapEx guidance for 2026 was initially set at **$175 billion to $185 billion** [9].
>   
> - **FY2026 Q1**: The guidance was updated to **$180 billion to $190 billion**, reflecting the inclusion of investment related to the acquisition of Intersect [3].
> 
> - **FY2026 Q2**: The guidance was further revised to **$195 billion to $205 billion**, indicating an acceleration in the delivery of capacity to meet growing demand [7].
> 
> - **FY2026 Q3**: The CapEx guidance remained unchanged at **$195 billion to $205 billion** [4].
> 
> Thus, the CapEx guidance increased from $175-$185 billion in Q4 2025 to $195-$205 billion by Q2 2026, and it remained at that level through Q3 2026.

**Cited sources (4):**

[3] GOOGL | FY2026 Q1 | TRANSCRIPT | section: 2026 Q1 Earnings Call

```text
Speaker: Anat Ashkenazi, SVP and CFO, Alphabet and Google
Moving to investment, we are updating our full year 2026 CapEx guidance range to $180 to $190 billion, up from our previous estimate of $175 to $185 billion, to now include investment related to the acquisition of Intersect, which closed in March.

We are seeing unprecedented internal and external demand for AI compute resources. The investment we're making in AI is delivering strong growth, as evidenced by the record revenue and backlog growth in Google Cloud and strong performance in Google Services.

Looking ahead, these strong results reinforce our conviction to invest the capital required to continue to capture the AI opportunity. As a result, we expect our 2027 CapEx to significantly increase compared to 2026.

In terms of expenses, as we've discussed previously, the significant increase in our investments in technical infrastructure will continue to put pressure on the P&L in the form of higher depreciation expense and related data center operations costs, such as energy. We also expect to continue hiring in key investment areas such as AI and Cloud, and are investing in marketing to support our AI products.

To conclude, Q1 was an outstanding quarter for Alphabet, and our teams continue to execute with high level of discipline and velocity, delivering amazing innovation. We look forward to sharing more in the coming weeks at I/O, Google Marketing Live, and Brandcast.

I'm going to take this opportunity to thank our employees for their contributions to our performance.

Sundar, Philipp and I will now take your questions.
```

[4] GOOGL | FY2026 Q3 | TRANSCRIPT | section: 2026 Q3 Earnings Call

```text
Speaker: Anat Ashkenazi, SVP and CFO, Alphabet and Google
Moving to investments, our full-year 2026 CapEx guidance range of $195-$205 billion remain unchanged. We continue to expect our CapEx to increase significantly in 2027, and we'll provide further details on our Q4 call.

In terms of expenses, the significant increase in our investment in technical infrastructure will continue to weigh on the P&L in the form of higher depreciation expenses and related data centers' operation costs such as energy.

Before we move to Q&A, let me say a few words on how we think about return on invested capital and free cash flow.

We're in a unique position to be able to monetize our AI investments across many products that reach billions of users and enterprises. Our AI investments in Search are driving our highest query growth in half a decade. Google Cloud has generated tens of billions of dollars in AI revenues year-to-date, and Google One is our fastest-growing subscription product, driven by demand for AI plans.

Throughout this investment cycle, we expect to generate an attractive ROIC that remains meaningfully above our cost of capital and anticipate generating strong cash flow from operations.

Our sizable investments in technical infrastructure and inventory build of TPU systems as we scale that business will put pressure on free cash flows.

Q3 was a strong quarter, highlighted by our innovation and disciplined execution across all areas of the business, including the launch of Gemini 4.

I want to take this opportunity to thank our employees for their contributions to our performance.

We will now open the call for Q&A.
```

[7] GOOGL | FY2026 Q2 | TRANSCRIPT | section: 2026 Q2 Earnings Call

```text
Speaker: Anat Ashkenazi, SVP and CFO, Alphabet and Google
The increase was driven by strong demand for our Enterprise AI offerings. The majority of the backlog is related to typical GCP contacts for our broad mix of customers, and we expect to recognize just over 50% of the total backlog as revenue over the next 24 months.

In Other Bets, revenues were $382 million, and operating loss was $1.8 billion as we continue to expand Waymo's business and invest in key Other Bets.

In Alphabet Level Activities, the operating loss was $5.8 billion, driven by shared AI R&D expenses.

Turning to our outlook in business performance expectations in the second half of 2026.

First, in terms of revenues, at the current spot rates, we would expect a slight FX headwind to our consolidated revenue in Q3, compared to a one percentage point FX tailwind in Q2. This impact will be seen primarily in Search and YouTube Ads.

In Google Services, as Philipp mentioned, we continue to benefit from innovation in Search by improving the user and advertiser experience with AI. At the same time, in Q3, we will begin lapping an acceleration in Search performance that began in the third quarter last year.

In Google Cloud, we're seeing significant demand for products and services, which we expect to drive strong growth. As I mentioned earlier, we started delivering TPU systems to customer data centers in the second quarter. We continue to expect to recognize a relatively small portion of the revenues from our existing TPU system sales agreements this year, ramping as we exit 2026. We anticipate the vast majority of the revenues from these agreements will be realized in 2027.

And given the supply constraint environment, we plan to expand the use of third-party capacity in Q3 as a bridging strategy while we build out more internal capacity. This strategy allows us to keep growing our customer base and capture greater overall value. However, it will create modest margin pressure in the near-term as we utilize this capacity.

Moving to investments, we are updating our full year 2026 CapEx guidance range to $195-205 billion, up from our previous estimate of $180-190 billion. The increase in the range is primarily due to an acceleration in the delivery of capacity to meet growing demand.

As we previously shared, we continue to expect our CapEx to increase significantly in 2027, and we'll provide more details at a later date.

In terms of expenses, the significant increase in our investments and technical infrastructure will continue to put pressure on the P&L in the form of higher depreciation expense and related data center operations costs, such as energy. We also expect to continue hiring in key investment areas such as AI and cloud, and we are investing in marketing to support our AI products.
```

[9] GOOGL | FY2025 Q4 | TRANSCRIPT | section: 2025 Q4 Earnings Call

```text
Speaker: Anat Ashkenazi, SVP and CFO, Alphabet and Google
First, in terms of revenues, we're pleased with the overall momentum of the business. At current spot rates, we would expect to see an FX tailwind toward consolidated revenues in Q1. However, the volatility in exchange rates could affect the impact of FX on Q1 revenues.

In Google Services, we expect growth to be driven by ongoing innovation in the user experience, as well as improved ROI for advertisers, keeping in mind the normal seasonal pattern for advertising revenue.

In Google Cloud, we're seeing significant demand for our products and services, which we expect to continue to drive strong growth, despite the tight supply environment we're operating in.

Moving to investments, the investments we have been making in AI are already translating to strong performance across the business, as you've seen in our financial results. Our successful execution, coupled with strong performance, reinforces our conviction to make the investments required to further capitalize on the AI opportunity.

For the full year 2026, we expect CapEx to be in the range of $175 billion to $185 billion, with investments ramping over the course of the year.

We're investing in AI compute capacity to support frontier model development by Google DeepMind, ongoing efforts to improve the user experience, and drive higher advertiser ROI in Google Services, significant Cloud customer demand, as well as strategic investments in Other Bets.

Keep in mind that the availability of supply, pricing of components, and timing of cash payments can cause some variability in the reported CapEx number.

In terms of expenses, as we've discussed on previous calls, the significant increase in our investments in technical infrastructure will continue to put pressure on the P&L in the form of higher depreciation expense and related data centers' operations costs such as energy.

In 2025, depreciation increased by nearly $6 billion, or 38%, from $15.3 billion in 2024, to $21.1 billion in 2025. Given the increase in our CapEx investments in recent years, we expect the growth rate in 2026 depreciation to accelerate in Q1, and meaningfully increase for the full year.

We're also planning to continue hiring in key investment areas, such as AI and Cloud.

In 2025, our teams delivered amazing innovation, executing with a high level of discipline and velocity. These efforts provided great experiences for consumers and outstanding performance for creators, partners, and enterprise customers driving strong revenue growth.

I'm going to take this opportunity to thank our employees for their contribution to this impressive performance.

Now, Sundar, Philipp, and I will take your questions.
```

---

## 28. q014-A (multi_quarter)

**Question:** How did Azure growth change from Q2 to Q4 of Microsoft's fiscal 2026?

**Gold answer:** Azure and other cloud services grew 39% (Q2), 40% (Q3) and 43% (Q4).

**Answer:**

> In fiscal year 2026, the growth of Azure and other cloud services revenue changed as follows:
> 
> - **FY2026 Q2**: Azure and other cloud services revenue increased by 39% [1][3].
> - **FY2026 Q4**: Azure and other cloud services revenue increased by 43% [4][6].
> 
> Thus, the growth rate for Azure increased from 39% in Q2 to 43% in Q4.

**Cited sources (4):**

[1] MSFT | FY2026 Q2 | FORM_10Q | section: OVERVIEW

```text
Microsoft is a technology company committed to making digital technology and artificial intelligence (“AI”) available broadly and doing so responsibly, with a mission to empower every person and every organization on the planet to achieve more. We create platforms and tools, powered by AI, that deliver innovative solutions that meet the evolving needs of our customers.

We generate revenue by offering a wide range of cloud-based solutions, content, and other services to people and businesses; licensing and supporting an array of software products; delivering relevant online advertising to a global audience; and designing and selling devices. Our most significant expenses are related to compensating employees; supporting and investing in our cloud-based services, including datacenter operations; designing, manufacturing, marketing, and selling our other products and services; and income taxes.

Highlights from the second quarter of fiscal year 2026 compared with the second quarter of fiscal year 2025 included:

• Microsoft Cloud revenue increased 26% to $51.5 billion.

• Commercial remaining performance obligation increased 110% to $625 billion.

• Microsoft 365 Commercial cloud revenue increased 17%.

• Microsoft 365 Consumer cloud revenue increased 29%.

• LinkedIn revenue increased 11%.

• Dynamics 365 revenue increased 19%.

• Azure and other cloud services revenue increased 39%.

• Windows OEM and Devices revenue increased 1%.

• Xbox content and services revenue decreased 5%.

• Search and news advertising revenue excluding traffic acquisition costs increased 10%.
```

[3] MSFT | FY2026 Q2 | PRESS_RELEASE | section: Business Highlights

```text
Microsoft Cloud revenue was $51.5 billion and increased 26% (up 24% in constant currency), and commercial remaining performance obligation increased 110% to $625 billion.

Revenue in Productivity and Business Processes was $34.1 billion and increased 16% (up 14% in constant currency), with the following business highlights:

• Microsoft 365 Commercial cloud revenue increased 17% (up 14% in constant currency)

• Microsoft 365 Consumer cloud revenue increased 29% (up 27% in constant currency)

• LinkedIn revenue increased 11% (up 10% in constant currency)

• Dynamics 365 revenue increased 19% (up 17% in constant currency)

Revenue in Intelligent Cloud was $32.9 billion and increased 29% (up 28% in constant currency), with the following business highlights:

• Azure and other cloud services revenue increased 39% (up 38% in constant currency)

Revenue in More Personal Computing was $14.3 billion and decreased 3%, with the following business highlights:

• Windows OEM and Devices revenue increased 1% (relatively unchanged in constant currency)

• Xbox content and services revenue decreased 5% (down 6% in constant currency)

• Search and news advertising revenue excluding traffic acquisition costs increased 10% (up 9% in constant currency)

Microsoft returned $12.7 billion to shareholders in the form of dividends and share repurchases in the second quarter of fiscal year 2026, an increase of 32% compared to the second quarter of fiscal year 2025.
```

[4] MSFT | FY2026 Q4 | FORM_10K | section: OVERVIEW

```text
Microsoft is a technology company committed to making digital technology and artificial intelligence (“AI”) available broadly and doing so responsibly, with a mission to empower every person and every organization on the planet to achieve more. We create platforms and applications, powered by AI, that deliver innovative solutions that meet the evolving needs of our customers.

We generate revenue by offering a wide range of cloud-based solutions, content, and other services to people and businesses; licensing and supporting an array of software products; delivering relevant online advertising to a global audience; and designing and selling devices. Our most significant expenses are related to compensating employees; supporting and investing in our cloud-based services, including datacenter operations; designing, manufacturing, marketing, and selling our other products and services; and income taxes.

Highlights from fiscal year 2026 compared with fiscal year 2025 included:

• Microsoft Cloud revenue increased 27% to $214.4 billion.

• Commercial remaining performance obligation increased 84% to $678 billion.

• Microsoft 365 Commercial cloud revenue increased 17%.

• Microsoft 365 Consumer cloud revenue increased 28%.

• LinkedIn revenue increased 11%.

• Dynamics 365 revenue increased 18%.

• Azure and other cloud services revenue increased 41%.

• Windows OEM and Devices revenue decreased slightly.

• XBOX content and services revenue decreased 5%.

• Search advertising (formerly Search and news advertising) revenue excluding traffic acquisition costs increased 12%.
```

[6] MSFT | FY2026 Q4 | PRESS_RELEASE | section: Business Highlights

```text
Microsoft Cloud revenue was $59.3 billion and increased 27%, and commercial remaining performance obligation increased 84% to $678 billion.

Revenue in Productivity and Business Processes was $37.8 billion and increased 14%, with the following business highlights:

• Microsoft 365 Commercial cloud revenue increased 16% when adjusted for the prior year comparable that benefited from 2 points of in-period revenue recognition. On a reported basis, Microsoft 365 Commercial cloud revenue increased 14%.

• Microsoft 365 Consumer cloud revenue increased 24% (up 22% in constant currency)

• LinkedIn revenue increased 12% (up 10% in constant currency)

• Dynamics 365 revenue increased 13% (up 12% in constant currency)

Revenue in Intelligent Cloud was $39.3 billion and increased 32% (up 31% in constant currency), with the following business highlights:

• Azure and other cloud services revenue increased 43%

Revenue in More Personal Computing was $12.9 billion and decreased 4% (down 5% in constant currency), with the following business highlights:

• Windows OEM and Devices revenue decreased 7%

• XBOX content and services revenue decreased 10%

• Search advertising revenue excluding traffic acquisition costs increased 10% (up 9% in constant currency)

Microsoft returned $10.2 billion to shareholders in the form of dividends and share repurchases in the fourth quarter of fiscal year 2026.
```

---

## 29. q015-A (multi_quarter)

**Question:** How did Google Cloud revenue and growth develop from Q4 2025 to Q3 2026?

**Gold answer:** $17.7B (+48%), $20.0B (+63%), $24.8B (+82%), $30.7B (+103%): growth accelerated every quarter.

**Answer:**

> **FY2025 Q4**
> - Google Cloud revenue was $17.7 billion, an increase of 48% year over year [16].
> 
> **FY2026 Q1**
> - Google Cloud revenue increased by $7.8 billion from the three months ended March 31, 2025 to the three months ended March 31, 2026, reaching a total of $7.8 billion [3].
> 
> **FY2026 Q2**
> - Google Cloud revenue increased by $11.1 billion from the three months ended June 30, 2025 to the three months ended June 30, 2026, totaling $18.9 billion [10].
> 
> **FY2026 Q3**
> - Google Cloud revenue reached $30.7 billion, reflecting an increase of 103% year over year [12].
> 
> In summary, Google Cloud revenue grew from $17.7 billion in Q4 2025 to $30.7 billion in Q3 2026, with significant increases of $7.8 billion in Q1 2026 and $11.1 billion in Q2 2026.

**Cited sources (4):**

[3] GOOGL | FY2026 Q1 | FORM_10Q | section: Google Subscriptions, Platforms, and Devices

```text
Google subscriptions, platforms, and devices revenues increased $2.0 billion from the three months ended March 31, 2025 to the three months ended March 31, 2026. The growth was primarily driven by an increase in subscriptions revenues. This increase was primarily due to the contribution from growth in paid subscriptions across both YouTube services and Google One. Additionally, Google subscriptions, platforms, and devices revenues were favorably affected by changes in foreign currency exchange rates for the three months ended March 31, 2026.

Google Cloud revenues increased $7.8 billion from the three months ended March 31, 2025 to the three months ended March 31, 2026 primarily driven by growth in Google Cloud Platform largely from infrastructure and platform services.
```

[10] GOOGL | FY2026 Q2 | FORM_10Q | section: Segment Profitability

```text
We report our segment results as Google Services, Google Cloud, and Other Bets. Additionally, certain costs are not allocated to our segments because they represent Alphabet-level activities. For further details on our segments, see Note 15 of the Notes to Consolidated Financial Statements included in Item 1 of this Quarterly Report on Form 10-Q.

The following table presents segment operating income (loss) (in millions):

Three Months Ended | Six Months Ended
June 30, | June 30,
2025 | 2026 | 2025 | 2026
Operating income (loss):
Google Services | $33,063 | $39,544 | $65,745 | $80,133
Google Cloud | 2,826 | 8,814 | 5,003 | 15,412
Other Bets | (1,246) | (1,799) | (2,472) | (3,899)
Alphabet-level activities(1) | (3,372) | (5,789) | (6,399) | (11,180)
Total income from operations | $31,271 | $40,770 | $61,877 | $80,466

(1)Alphabet-level activities primarily reflect expenses related to our shared AI research and development.

Google Services operating income increased $6.5 billion and $14.4 billion from the three and six months ended June 30, 2025 to the three and six months ended June 30, 2026, respectively. The increase in operating income was primarily driven by an increase in revenues, partially offset by an increase in TAC.

Google Cloud operating income increased $6.0 billion and $10.4 billion from the three and six months ended June 30, 2025 to the three and six months ended June 30, 2026, respectively. The increase in operating income was primarily driven by an increase in revenues, partially offset by increases in usage costs for technical infrastructure and employee compensation expenses.
```

[12] GOOGL | FY2026 Q3 | TRANSCRIPT | section: 2026 Q3 Earnings Call

```text
Speaker: Thomas Kurian, SVP and CEO, Google Cloud
To address the question on the rate of growth of Cloud, our Google Cloud segment delivered record growth in the third quarter with Cloud revenues up 103% to $30.7 billion.

The strongest growth contributor was Google Cloud Platform, with strength in both AI and Core GCP. So GCP accounted for the vast majority of revenue generated in Q3. And Cloud revenue growth, excluding the impact of TPU system sales, was again very, very strong.

Demand was also strong across our product lines. So we see three things going on.

One, as people use Gemini, we see revenue growth from Gemini itself; but we also see that as people use Gemini, it drives growth in other parts of our portfolio. For instance, as they build agents, they use compute to perform a task, and so they buy compute. They use storage to store memories of the agent. Database to serve up what's called context for the agent to reason on. And obviously given all the cybersecurity threats, we are seeing strong growth in our cybersecurity portfolio.

So we are confident about our growth and the diversity in our growth. We're also seeing new customers buy into GCP and exhilarating growth in new customer adoptions, so we also see that as a growth factor for us.

And we're always -- at any point in time, we could obviously serve a lot more demand with capacity, but we are managing all of these parameters and growth carefully.
```

[16] GOOGL | FY2025 Q4 | TRANSCRIPT | section: 2025 Q4 Earnings Call

```text
Speaker: Anat Ashkenazi, SVP and CFO, Alphabet and Google
Operating income increased 16% to $35.9 billion, and operating margin was 31.6%. Both operating income and operating margin were negatively impacted by the $2.1 billion Waymo charge in the quarter.

Other income and expenses was $3.2 billion, primarily due to unrealized gains in our non-marketable equity securities portfolio.

Net income increased 30% to $34.5 billion, and earnings per share increased 31% to $2.82.

We generated record operating cash flow of $52.4 billion in the fourth quarter, and $164.7 billion for the full year. This translated into $24.6 billion of free cashflow in the fourth quarter and $73.3 billion for the full year. We ended the quarter with $126.8 billion in cash and marketable securities, and $46.5 billion in long-term debts.

Turning to segment results, Google Services revenues increased 14% to $95.5 billion, reflecting strong growth in Search and Subscriptions.

Google Search and Other advertising revenues increased by 17% to $63.1 billion, representing another strong quarter with continued growth across all major verticals, with the largest contribution from Retail.

YouTube advertising revenues increased 9% to $11.4 billion, driven by direct response advertising. Results were negatively affected from the lapping of the strong spend on U.S. election in the fourth quarter of 2024 that we've mentioned on previous earnings calls.

Network advertising revenues of $7.8 billion were down 2%.

Subscriptions, Platforms and Devices revenues increased 17% this quarter to $13.6 billion, due to strong growth in YouTube Subscriptions, particularly YouTube Music and Premium, and growth in Google One, which benefited from increased demand for AI plans.

Google Services operating income increased 22% to $40.1 billion, and operating margins was 41.9%.

The Google Cloud segment delivered outstanding results in the fourth quarter as the business continued to benefit from strong demand for our enterprise AI products.

Cloud revenue accelerated meaningfully and was up 48% to $17.7 billion. Revenues were driven by strong performance in GCP, which continued to grow at a rate that was much higher than Cloud's overall revenue growth rate. As Sundar noted, we're driving performance through strong growth and a win rate of new customers, signing larger customer commitments, and increasing spend with existing customers.

GCP's performance was driven by accelerating growth in enterprise AI products, which were generating billions in quarterly revenues. We had strong growth in both enterprise AI infrastructure driven by deployment of TPUs and GPUs, and enterprise AI solutions, which benefited from demand for our industry-leading models, including Gemini 3.
```

---

## 30. q016-A (multi_quarter)

**Question:** What was Meta's revenue in each of the first three quarters of 2026?

**Gold answer:** Q1 $56.31B (+33%), Q2 $60.80B (+28%), Q3 $64.60B (+26%).

**Answer:**

> Here are Meta's revenues for the first three quarters of 2026:
> 
> - **FY2026 Q1**: Revenue was $56,311 million [7].
> - **FY2026 Q2**: Revenue was $60,801 million [5].
> - **FY2026 Q3**: Revenue was $64,601 million [4].

**Cited sources (3):**

[4] META | FY2026 Q3 | PRESS_RELEASE | section: Non-GAAP Financial Measures

```text
We believe that this methodology can provide useful supplemental information to help investors better understand underlying trends in our business. Free cash flow is not intended to represent our residual cash flow available for discretionary expenditures.

For more information on our non-GAAP financial measures and a reconciliation of GAAP to non-GAAP measures, see the "Reconciliation of GAAP to Non-GAAP Results" table in this press release.

META PLATFORMS, INC.
CONDENSED CONSOLIDATED STATEMENTS OF INCOME
(In millions, except per share amounts)
(Unaudited)
Three Months Ended September 30, | Nine Months Ended September 30,
2026 | 2025 | 2026 | 2025
Revenue | $64,601 | $51,242 | $181,712 | $141,073
Costs and expenses:
Cost of revenue | 13,369 | 9,206 | 34,917 | 25,269
Research and development | 20,054 | 15,144 | 59,409 | 40,237
Marketing and sales | 3,333 | 2,845 | 9,672 | 8,581
General and administrative (1) | 14,857 | 3,512 | 23,079 | 8,455
Total costs and expenses | 51,613 | 30,707 | 127,077 | 82,542
Income from operations | 12,988 | 20,535 | 54,635 | 58,531
Interest and other income (expense), net | 13 | 1,128 | (1,126) | 2,047
Income before income taxes | 13,001 | 21,663 | 53,509 | 60,578
Provision for income taxes | 3,141 | 18,954 | 1,028 | 22,888
Net income | $9,860 | $2,709 | $52,481 | $37,690
Earnings per share:
Basic | $3.86 | $1.08 | $20.64 | $14.96
Diluted | $3.83 | $1.05 | $20.44 | $14.62
Weighted-average shares used to compute earnings per share:
Basic | 2,552 | 2,517 | 2,543 | 2,520
Diluted | 2,573 | 2,572 | 2,568 | 2,578

____________________________________

(1) The third quarter 2026 general and administrative expenses include a $9.89 billion charge related to the August 2026 agreement with U.S. State Attorneys General to resolve litigation related to teen mental health and social media.
```

[5] META | FY2026 Q2 | PRESS_RELEASE | section: Non-GAAP Financial Measures

```text
We believe that this methodology can provide useful supplemental information to help investors better understand underlying trends in our business. Free cash flow is not intended to represent our residual cash flow available for discretionary expenditures.

For more information on our non-GAAP financial measures and a reconciliation of GAAP to non-GAAP measures, see the "Reconciliation of GAAP to Non-GAAP Results" table in this press release.

META PLATFORMS, INC.
CONDENSED CONSOLIDATED STATEMENTS OF INCOME
(In millions, except per share amounts)
(Unaudited)
Three Months Ended June 30, | Six Months Ended June 30,
2026 | 2025 | 2026 | 2025
Revenue | $60,801 | $47,516 | $117,111 | $89,830
Costs and expenses:
Cost of revenue | 11,330 | 8,491 | 21,549 | 16,063
Research and development | 21,656 | 12,942 | 39,354 | 25,092
Marketing and sales | 3,431 | 2,979 | 6,339 | 5,735
General and administrative (1) | 5,609 | 2,663 | 8,222 | 4,943
Total costs and expenses | 42,026 | 27,075 | 75,464 | 51,833
Income from operations | 18,775 | 20,441 | 41,647 | 37,997
Interest and other income (expense), net | (19) | 93 | (1,139) | 919
Income before income taxes | 18,756 | 20,534 | 40,508 | 38,916
Provision (benefit) for income taxes | 2,908 | 2,197 | (2,113) | 3,935
Net income | $15,848 | $18,337 | $42,621 | $34,981
Earnings per share:
Basic | $6.23 | $7.28 | $16.79 | $13.87
Diluted | $6.18 | $7.14 | $16.62 | $13.56
Weighted-average shares used to compute earnings per share:
Basic | 2,543 | 2,518 | 2,538 | 2,522
Diluted | 2,566 | 2,570 | 2,565 | 2,580

____________________________________

(1) The second quarter 2026 general and administrative expenses include $2.40 billion of charges related to legal proceedings.
```

[7] META | FY2026 Q1 | PRESS_RELEASE | section: Non-GAAP Financial Measures

```text
We believe that this methodology can provide useful supplemental information to help investors better understand underlying trends in our business. Free cash flow is not intended to represent our residual cash flow available for discretionary expenditures.

For more information on our non-GAAP financial measures and a reconciliation of GAAP to non-GAAP measures, see the "Reconciliation of GAAP to Non-GAAP Results" table in this press release.

META PLATFORMS, INC.
CONDENSED CONSOLIDATED STATEMENTS OF INCOME
(In millions, except per share amounts)
(Unaudited)
Three Months Ended March 31,
2026 | 2025
Revenue | $56,311 | $42,314
Costs and expenses:
Cost of revenue | 10,218 | 7,572
Research and development | 17,699 | 12,150
Marketing and sales | 2,908 | 2,757
General and administrative | 2,614 | 2,280
Total costs and expenses | 33,439 | 24,759
Income from operations | 22,872 | 17,555
Interest and other income (expense), net | (1,120) | 827
Income before income taxes | 21,752 | 18,382
Provision (benefit) for income taxes | (5,021) | 1,738
Net income | $26,773 | $16,644
Earnings per share:
Basic | $10.57 | $6.59
Diluted | $10.44 | $6.43
Weighted-average shares used to compute earnings per share:
Basic | 2,534 | 2,527
Diluted | 2,564 | 2,590
```
