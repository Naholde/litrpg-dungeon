package com.andrey.litrpg.skill;

import com.andrey.litrpg.core.balance.Balance;
import com.andrey.litrpg.core.balance.SkillsBalance;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Навык. Каждый навык — свой класс со своей механикой в {@link #execute}.
 * Параметры объявляются в конструкторе через {@link #param}; описание — шаблон {@link #template()} с подстановкой
 * {@code {ключ}} → число из тех же параметров. Поэтому описание не может разойтись с тем, что навык делает.
 * <p>У каждого навыка есть параметры {@code cooldown} и {@code cast} (время чтения; 0 — мгновенно).
 */
public abstract class Skill {
    public final String id;
    public final String name;
    /** Цвет навыка (значок, эффекты). */
    public final int color;
    /** Символ значка, пока нет нарисованной иконки. */
    public final String glyph;
    private final Map<String, SkillParam> params = new LinkedHashMap<>();

    protected Skill(String id, String name, int color, String glyph, double cooldown, double cast) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.glyph = glyph;
        param("cooldown", cooldown, SkillParam.Unit.SECONDS, SkillParam.Scale.COOLDOWN);
        param("cast", cast, SkillParam.Unit.SECONDS, SkillParam.Scale.NONE);
    }

    protected final void param(String key, double def, SkillParam.Unit unit, SkillParam.Scale scale) {
        if (params.put(key, new SkillParam(key, def, unit, scale)) != null) {
            throw new IllegalStateException(id + ": параметр «" + key + "» объявлен дважды");
        }
    }

    // Короткие объявления для частых видов параметров.
    protected final void damage(String key, double share) {
        param(key, share, SkillParam.Unit.DAMAGE, SkillParam.Scale.POWER);
    }

    protected final void heal(String key, double share) {
        param(key, share, SkillParam.Unit.HEAL, SkillParam.Scale.POWER);
    }

    protected final void shield(String key, double share) {
        param(key, share, SkillParam.Unit.SHIELD, SkillParam.Scale.POWER);
    }

    protected final void seconds(String key, double s) {
        param(key, s, SkillParam.Unit.SECONDS, SkillParam.Scale.DURATION);
    }

    protected final void percent(String key, double share) {
        param(key, share, SkillParam.Unit.PERCENT, SkillParam.Scale.POWER);
    }

    protected final void blocks(String key, double b) {
        param(key, b, SkillParam.Unit.BLOCKS, SkillParam.Scale.NONE);
    }

    protected final void count(String key, double n) {
        param(key, n, SkillParam.Unit.COUNT, SkillParam.Scale.NONE);
    }

    public Map<String, SkillParam> params() {
        return Collections.unmodifiableMap(params);
    }

    /** Значение параметра с учётом skills.json и ранга. */
    public double value(String key, int rank) {
        SkillParam p = params.get(key);
        if (p == null) throw new IllegalArgumentException(id + ": нет параметра «" + key + "»");
        SkillsBalance b = Balance.get(Balance.SKILLS);
        double v = b.value(id, key, p.def());
        SkillsBalance.Common c = b.common();
        return switch (p.scale()) {
            case NONE -> v;
            case POWER -> v * (1 + c.rankPower() * rank);
            case COOLDOWN -> v * Math.max(0.2, 1 - c.rankCooldown() * rank);
            case DURATION -> v * (1 + c.rankDuration() * rank);
        };
    }

    public int ticks(String key, int rank) {
        return (int) Math.round(value(key, rank) * 20);
    }

    /** Как выбирается цель. */
    public abstract Targeting targeting();

    /** Можно ли сбить чтение движением, оглушением и немотой. У палача замах не сбить — в этом его идея. */
    public boolean interruptible() {
        return true;
    }

    /** Дальность для цели/точки (блоков). */
    public double range(int rank) {
        return params.containsKey("range") ? value("range", rank) : 4;
    }

    /** Шаблон описания: обычный текст и {@code {ключ}} параметров. */
    public abstract String template();

    /** Механика навыка (на сервере). Цифры брать только через {@code ctx.v("ключ")}. */
    public abstract void execute(SkillContext ctx);

    /** Ресурс, который тратит навык (у классов со своим ресурсом). */
    public double cost(int rank) {
        return params.containsKey("cost") ? value("cost", rank) : 0;
    }
}
