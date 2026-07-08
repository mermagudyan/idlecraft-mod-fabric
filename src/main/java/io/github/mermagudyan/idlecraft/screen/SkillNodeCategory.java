package io.github.mermagudyan.idlecraft.screen;

public enum SkillNodeCategory {
    SIMPLE("Simple", 0xFF55FF55),
    MEDIUM("Medium", 0xFFFFFF55),
    COMPLEX("Complex", 0xFFFF5555);

    public final String displayName;
    public final int color;

    SkillNodeCategory(String name, int color) {
        this.displayName = name;
        this.color = color;
    }
}