package com.github.indigopolecat.bingobrewers.util;

// This class is from the Dungeon Gang mod by cyoung
// https://github.com/Dungeons-Guide/Skyblock-Dungeons-Guide/blob/dg4.0/mod/src/main/java/kr/syeyoung/dungeonsguide/mod/party/MessageMatcher.java

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageMatcher {
    List<String> simpleEquals = new ArrayList<>();
    public List<PatternData> regexPatterns = new ArrayList<>();

    public static class PatternData {
        private final Pattern pattern;
        private final int flags;

        public PatternData(Pattern pattern, int flags) {
            this.pattern = pattern;
            this.flags = flags;
        }
    }

    public MessageMatcher(List<String> patterns) {
        for (String pattern : patterns) {
            if (pattern.startsWith("=")) simpleEquals.add(pattern.substring(1));
            else regexPatterns.add(new PatternData(Pattern.compile(pattern.substring(1), Pattern.DOTALL | Pattern.MULTILINE),
                    (pattern.contains("<p0>") ? 1 : 0) |
                            (pattern.contains("<p1>") ? 2 : 0) |
                            (pattern.contains("<p2>") ? 4 : 0)
            ));
        }
    }

    public boolean match(String str, Map<String, String> matchGroups) {
        if (matchGroups != null)
            matchGroups.clear();
        for (String simpleEqual : simpleEquals) {
            if (simpleEqual.equals(str)) return true;
        }

        for (PatternData regexPattern : regexPatterns) {
            Matcher m = regexPattern.pattern.matcher(str);
            if (m.matches()) {
                if (matchGroups != null) {
                    if ((regexPattern.flags & 4) > 0)
                        matchGroups.put("2", m.group("p2"));
                    if ((regexPattern.flags & 2) > 0)
                        matchGroups.put("1", m.group("p1"));
                    if ((regexPattern.flags & 1) > 0)
                        matchGroups.put("0", m.group("p0"));
                }
                return true;
            }
        }
        return false;
    }
}