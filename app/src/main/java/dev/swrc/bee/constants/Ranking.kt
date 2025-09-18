package dev.swrc.bee.constants

enum class Ranking(val rankName: String, val percentage: Double) {
    BEGINNER("Beginner", 0.00),
    GOOD_START("Good Start", 0.02),
    MOVING_UP("Moving Up", 0.05),
    GOOD("Good", 0.08),
    SOLID("Solid", 0.15),
    NICE("Nice", 0.25),
    GREAT("Great", 0.40),
    AMAZING("Amazing", 0.50),
    GENIUS("Genius", 0.70),
    QUEEN_BEE("Queen Bee", 1.0)
}