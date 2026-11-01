package com.andrey.litrpg.skill;

import com.andrey.litrpg.player.Role;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Подбор классов по стилю игры. Сходство — косинус между «что делал игрок» и «что любит класс».
 * Предлагается до трёх классов роли; классы, уже занятые товарищами по отряду, не предлагаются.
 */
public final class ClassOffers {
    private ClassOffers() {
    }

    public record Scored(PlayerClass cls, double match) {
    }

    public static double similarity(Map<Style, Integer> played, Map<Style, Double> likes) {
        double dot = 0, a = 0, b = 0;
        for (Style s : Style.values()) {
            double x = played.getOrDefault(s, 0), y = likes.getOrDefault(s, 0.0);
            dot += x * y;
            a += x * x;
            b += y * y;
        }
        return a == 0 || b == 0 ? 0 : dot / Math.sqrt(a * b);
    }

    /** До {@code count} лучших классов роли (без занятых). Пустой стиль — все равны, порядок реестра. */
    public static List<Scored> offer(Role role, Map<Style, Integer> played, Set<String> taken, int count) {
        List<Scored> all = new ArrayList<>();
        for (PlayerClass c : ClassRegistry.ofRole(role)) {
            if (taken.contains(c.id())) continue;
            all.add(new Scored(c, similarity(played, c.style())));
        }
        all.sort(Comparator.comparingDouble(Scored::match).reversed());
        return all.size() > count ? all.subList(0, count) : all;
    }
}
