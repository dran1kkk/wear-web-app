package com.example.wearapp;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;

/**
 * Превращает «плоскую» запись формул в текст с настоящими индексами:
 *  e^(−3x), e^x  -> степень;  P̃_l(x) -> нижний индекс;
 *  ⁽ⁿ⁻¹⁾, ₁, ₂ ... -> верхние/нижние индексы через span'ы;
 *  y' -> y′,  y'' -> y″.
 */
final class Fmt {
    private static final String SUP_FROM = "⁰¹²³⁴⁵⁶⁷⁸⁹⁺⁻⁼⁽⁾ⁿᵐᵏʳˡⁱˣʸ";
    private static final String SUP_TO   = "0123456789+−=()nmkrlixy";
    private static final String SUB_FROM = "₀₁₂₃₄₅₆₇₈₉₊₋₌₍₎ₙₘₗₖᵢᵣₓ";
    private static final String SUB_TO   = "0123456789+−=()nmlkirx";
    private static final float SCRIPT_SIZE = 0.72f;

    private Fmt() {}

    static CharSequence format(String in) {
        String s = in.replace("''", "″").replace("'", "′");
        SpannableStringBuilder b = new SpannableStringBuilder();
        int n = s.length();
        int i = 0;
        while (i < n) {
            char c = s.charAt(i);

            // степень: ^(...) или ^x
            if (c == '^' && i + 1 < n) {
                int j = i + 1;
                String body;
                int next;
                if (s.charAt(j) == '(') {
                    int depth = 0;
                    int k = j;
                    for (; k < n; k++) {
                        char ch = s.charAt(k);
                        if (ch == '(') {
                            depth++;
                        } else if (ch == ')') {
                            depth--;
                            if (depth == 0) break;
                        }
                    }
                    if (k >= n) { b.append(c); i++; continue; }
                    body = s.substring(j + 1, k);
                    next = k + 1;
                } else {
                    int k = j;
                    while (k < n && Character.isLetterOrDigit(s.charAt(k))) k++;
                    if (k == j) { b.append(c); i++; continue; }
                    body = s.substring(j, k);
                    next = k;
                }
                script(b, body, true);
                i = next;
                continue;
            }

            // нижний индекс: _l
            if (c == '_' && i + 1 < n && Character.isLetterOrDigit(s.charAt(i + 1))) {
                script(b, String.valueOf(s.charAt(i + 1)), false);
                i += 2;
                continue;
            }

            // юникодные верхние индексы
            if (SUP_FROM.indexOf(c) >= 0) {
                StringBuilder run = new StringBuilder();
                while (i < n && SUP_FROM.indexOf(s.charAt(i)) >= 0) {
                    run.append(SUP_TO.charAt(SUP_FROM.indexOf(s.charAt(i))));
                    i++;
                }
                script(b, run.toString(), true);
                continue;
            }

            // юникодные нижние индексы
            if (SUB_FROM.indexOf(c) >= 0) {
                StringBuilder run = new StringBuilder();
                while (i < n && SUB_FROM.indexOf(s.charAt(i)) >= 0) {
                    run.append(SUB_TO.charAt(SUB_FROM.indexOf(s.charAt(i))));
                    i++;
                }
                script(b, run.toString(), false);
                continue;
            }

            b.append(c);
            i++;
        }
        return b;
    }

    private static void script(SpannableStringBuilder b, String text, boolean sup) {
        int start = b.length();
        b.append(text);
        int end = b.length();
        if (sup) {
            b.setSpan(new SuperscriptSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            b.setSpan(new SubscriptSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        b.setSpan(new RelativeSizeSpan(SCRIPT_SIZE), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
}
