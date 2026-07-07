package io.github.mermagudyan.idlecraft.screen;

public enum SkillNodeCategory {
    SIMPLE("Simple", 0xFF55FF55),     // зелёный
    MEDIUM("Medium", 0xFFFFFF55),     // жёлтый
    COMPLEX("Complex", 0xFFFF5555);   // красный

    public final String displayName;
    public final int color;

    SkillNodeCategory(String name, int color) {
        this.displayName = name;
        this.color = color;
    }
}