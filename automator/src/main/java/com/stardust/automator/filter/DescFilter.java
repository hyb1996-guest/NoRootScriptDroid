package com.stardust.automator.filter;

import android.view.accessibility.AccessibilityNodeInfo;

/**
 * Created by Stardust on 2017/3/9.
 */

public class DescFilter {

    private static final KeyGetter DESC_GETTER = new KeyGetter() {
        @Override
        public String getKey(AccessibilityNodeInfo nodeInfo) {
            CharSequence charSequence = nodeInfo.getContentDescription();
            return charSequence == null ? null : charSequence.toString();
        }
    };

    public static ListFilter equals(String text) {
        return new StringEqualsFilter(text, DESC_GETTER);
    }

    public static ListFilter contains(String str) {
        return new StringContainsFilter(str, DESC_GETTER);
    }

    public static ListFilter startsWith(String prefix) {
        return new StringStartsWithFilter(prefix, DESC_GETTER);
    }

    public static ListFilter endsWith(String suffix) {
        return new StringEndsWithFilter(suffix, DESC_GETTER);
    }
    public static ListFilter matches(String regex) {
        return new StringMatchesFilter(regex, DESC_GETTER);
    }

    private DescFilter(){

    }
}
