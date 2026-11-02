package com.earningslens.retrieval;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The companies in companies.yaml and their aliases (spec section 8 step 1).
 *
 * Aliases match case-insensitively as whole words: "Meta" or "Meta's" match, "metadata"
 * does not. Longer aliases are tried first, so "Meta Platforms" wins over "Meta".
 */
public class CompanyDirectory {

    private final Map<String, Company> byTicker = new LinkedHashMap<>();
    private final Map<String, String> tickerByAlias = new HashMap<>();
    private final Pattern aliasPattern;

    public CompanyDirectory(List<Company> companies) {
        for (Company c : companies) {
            if (byTicker.put(c.ticker(), c) != null) {
                throw new IllegalArgumentException("duplicate ticker " + c.ticker());
            }
            for (String alias : c.aliases()) {
                String key = alias.toLowerCase(Locale.ROOT);
                String previous = tickerByAlias.put(key, c.ticker());
                if (previous != null && !previous.equals(c.ticker())) {
                    throw new IllegalArgumentException("alias '" + alias + "' used by " + previous + " and " + c.ticker());
                }
            }
        }
        String alternatives = tickerByAlias.keySet().stream()
                .sorted(Comparator.comparingInt(String::length).reversed().thenComparing(Comparator.naturalOrder()))
                .map(Pattern::quote)
                .collect(Collectors.joining("|"));
        // Not preceded or followed by a letter or digit: a whole-word match.
        aliasPattern = Pattern.compile("(?<![\\p{L}\\p{N}])(?:" + alternatives + ")(?![\\p{L}\\p{N}])",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    /** Reads companies.yaml (SafeConstructor: plain maps and lists only). */
    public static CompanyDirectory load(InputStream yaml) {
        Object root = new Yaml(new SafeConstructor(new LoaderOptions())).load(yaml);
        if (!(root instanceof Map<?, ?> map) || !(map.get("companies") instanceof List<?> rows)) {
            throw new IllegalArgumentException("companies.yaml: expected a top-level 'companies' list");
        }
        List<Company> companies = new ArrayList<>();
        for (Object row : rows) {
            if (!(row instanceof Map<?, ?> m)) {
                throw new IllegalArgumentException("companies.yaml: expected a mapping per company");
            }
            List<String> aliases = new ArrayList<>();
            if (m.get("aliases") instanceof List<?> list) {
                list.forEach(a -> aliases.add(String.valueOf(a)));
            }
            companies.add(new Company(String.valueOf(m.get("ticker")), String.valueOf(m.get("name")),
                    ((Number) m.get("fiscal-year-end-month")).intValue(), aliases));
        }
        return new CompanyDirectory(companies);
    }

    public List<Company> all() {
        return List.copyOf(byTicker.values());
    }

    public Company byTicker(String ticker) {
        Company c = byTicker.get(ticker);
        if (c == null) {
            throw new IllegalArgumentException("unknown ticker " + ticker);
        }
        return c;
    }

    /** Tickers whose aliases appear in the text, sorted. */
    public Set<String> findTickers(String text) {
        Set<String> found = new TreeSet<>();
        Matcher m = aliasPattern.matcher(text);
        while (m.find()) {
            found.add(tickerByAlias.get(m.group().toLowerCase(Locale.ROOT)));
        }
        return found;
    }
}
